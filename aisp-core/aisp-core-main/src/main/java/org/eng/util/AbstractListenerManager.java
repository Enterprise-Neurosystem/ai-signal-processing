/*******************************************************************************
 * Copyright [2022] [IBM]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.eng.util;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;



/**
 * Maintains a list of keyed listeners.  
 * Listeners can be added and removed and called.  
 * Subclass simply implements {@link #callListener(Object, Object, Object)} to call the interface-defined LISTENER method,
 * and makes a call to {@link #notifyListeners(Object, Object)} when the listeners are to be called.
 * @author dawood
 * 
 * @param LISTENER - listener interface that will be called {@link #callListener(LISTENER, DYN_DATA, NOTIFY_DATA)}.
 * @param DYN_DATA - data available when listeners are called starting with {@link #notifyListeners(DYN_DATA, NOTIFY_DATA)}
 * @param NOTIFY_DATA - data passed into {@link #notifyListeners(DYN_DATA, NOTIFY_DATA)} and then {@link #callListener(LISTENER, DYN_DATA, NOTIFY_DATA)}.
 *
 */
public abstract class AbstractListenerManager<LISTENER, DYN_DATA, NOTIFY_DATA> {

	protected final ComponentLogger logger;

	protected AbstractListenerManager(ComponentLogger logger) {
		this.logger = logger;
	}
	
	private class ListenerAndData {
		private final LISTENER listener;

		ListenerAndData(LISTENER l) {
			this.listener = l;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((listener == null) ? 0 : listener.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof AbstractListenerManager.ListenerAndData))
				return false;
			ListenerAndData other = (ListenerAndData) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (listener == null) {
				if (other.listener != null)
					return false;
			} else if (!listener.equals(other.listener))
				return false;
			return true;
		}

		private AbstractListenerManager getOuterType() {
			return AbstractListenerManager.this;
		}
	}
	protected  Vector<ListenerAndData> listeners = new Vector<ListenerAndData>();

	/**
	 * Add a listener and associated static data that will be called when {@link #notifyListeners(Object, Object)}
	 * is called with the given key.  
	 * @param key
	 * @param l
	 * @param staticUserData
	 * @return
	 */
	public boolean addListener(LISTENER l) {
		ListenerAndData lad = new ListenerAndData(l);
		return listeners.add(lad);
	}

	/**
	 * Remove the given listener added using the given key and static data.
	 * @param key
	 * @param l
	 * @param staticUserData
	 * @return
	 */
	public boolean removeListener(LISTENER l) {
		return listeners.remove(new ListenerAndData(l));
	}
	
	/**
	 * Get the number of listeners registered.
	 * @return
	 */
	public int getListenerCount() {
		return listeners.size();
	}

	/**
	 * Call all listeners that were registered with the given key.  The given dynamic data
	 * and static data provided at registration time is passed to all listeners implied.
	 * @param key
	 * @param dynamicCallbackData
	 * @param notifyData passed to the {@link #callListener(Object, Object, Object)}
	 * method. Might be used to tell that method which callback method on an interface to call.
	 */
	public void notifyListeners(DYN_DATA dynamicCallbackData, NOTIFY_DATA notifyData) {
		List<ListenerAndData> locallisteners;
		locallisteners = (Vector<ListenerAndData>)(listeners.clone());	// To avoid concurrent updates/iteration
		Iterator<ListenerAndData> iter = locallisteners.iterator();
		while (iter.hasNext()) {
			// TODO: we might want to spawn a thread here.
			ListenerAndData lad = iter.next();
			try {
				callListener(lad.listener, dynamicCallbackData, notifyData);
			} catch (Throwable t) {
				logger.severe("Caught exception from listener : ", t.getMessage());
				t.printStackTrace();
			}
		}
	}

	protected abstract void callListener(LISTENER listener, DYN_DATA dynamicCallbackData, NOTIFY_DATA notifyData);
}
