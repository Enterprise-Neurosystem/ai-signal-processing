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
package org.eng.aisp.dataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Currently not exposed and used only for testing and MetaData.
 * @author DavidWood
 *
 */
class MemorySoundReferenceDataSet extends AbstractSoundReferenceSet {

	List<String> orderedReferences = new ArrayList<String>();
	Map<String, IReferencedSoundSpec> referenceMap = new HashMap<String, IReferencedSoundSpec>();
	
	public MemorySoundReferenceDataSet() {
	}

	protected MemorySoundReferenceDataSet(Iterable<String> references, MemorySoundReferenceDataSet dataSet ) {
		for (String ref : references)
			this.orderedReferences.add(ref);
		this.referenceMap.putAll(dataSet.referenceMap);
	}


	@Override
	public MemorySoundReferenceDataSet newIterable(Iterable<String> references) {
		return new MemorySoundReferenceDataSet(references, this);
	}

	@Override
	public IReferencedSoundSpec dereference(String reference) {
		IReferencedSoundSpec spec = this.referenceMap.get(reference);
		return spec;
	}

	protected void add(String ref, IReferencedSoundSpec dataRef) {
		this.orderedReferences.add(ref);
		this.referenceMap.put(ref,  dataRef);
		
	}
	@Override
	protected String add(IReferencedSoundSpec dataRef) {
		String ref = UUID.randomUUID().toString();
		this.add(ref,dataRef);
		return ref;
	}

	@Override
	protected MemorySoundReferenceDataSet newInstance() {
		return new MemorySoundReferenceDataSet();
	}
	
	public void removeRecord(String reference) {
		this.orderedReferences.remove(reference);
		this.referenceMap.remove(reference);
	}	
	

}
