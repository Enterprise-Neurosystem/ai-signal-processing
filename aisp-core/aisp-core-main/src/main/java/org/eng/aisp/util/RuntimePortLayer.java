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
package org.eng.aisp.util;

import java.io.IOException;
import java.net.URL;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;

public class RuntimePortLayer implements IPortLayer {

	
	public static boolean isAndroid() {
		String val = System.getProperty("java.vm.vendor");
		if (val == null)
			val = System.getProperty("java.vendor");
		if (val == null)
			throw new IllegalStateException("Could not determine java vendor.");
		val = val.toLowerCase();
		return val.contains("android");
	}
	private static RuntimePortLayer instance;
	
	private final IPortLayer portLayer;

	public static RuntimePortLayer instance() { 
		if (instance == null) { 
			String className = System.getProperty("portlayer.classname");
			if (className == null) {
				if (isAndroid())
					className = "org.eng.aisp.util.AndroidPortLayer";
				else
					className = "org.eng.aisp.util.J2SEPortLayer";
			}
			Class<?> klass;
			try {
				klass = Class.forName(className);
				if (!IPortLayer.class.isAssignableFrom(klass)) 
					throw new RuntimeException("Class " + klass.getName() + " does not appear to implement " + IPortLayer.class.getName());
				IPortLayer pl = (IPortLayer)klass.newInstance();
				instance = new RuntimePortLayer(pl);
			} catch (Exception e) {
				throw new RuntimeException("Could not allocated instance of " + className, e);
			}

		}
		return instance;
	}
	private RuntimePortLayer(IPortLayer pl) {
		this.portLayer = pl;
	}

	public String toBase64(byte[] bytes) {
		return portLayer.toBase64(bytes);
	}

	public byte[] fromBase64(String base64) {
		return portLayer.fromBase64(base64);
		
	}
	@Override
	public SoundClip MP3toPCM(double startTimeMsec, URL url) throws IOException, AISPException {
		return portLayer.MP3toPCM(startTimeMsec, url);
	}

}
