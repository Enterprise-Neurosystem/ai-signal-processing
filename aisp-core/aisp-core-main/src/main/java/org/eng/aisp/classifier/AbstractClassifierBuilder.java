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
package org.eng.aisp.classifier;

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.IFeatureExtractor;
import org.eng.aisp.feature.processor.IFeatureProcessor;
import org.eng.aisp.transform.ITrainingWindowTransform;

/**
 * Supports implementers of IClassifierBuilder.
 * 
 * @author dawood
 *
 * @param <WINDATA>
 * @param <FDATA>
 */
public abstract class AbstractClassifierBuilder<WINDATA, FDATA> implements IClassifierBuilder<WINDATA, FDATA> {

	protected ITrainingWindowTransform<WINDATA> transform = null;
	private IFeatureGramDescriptor<WINDATA, FDATA> featureGramDescriptor;

	// The extract, processor and window size settings are mirroed in the featureGramDescriptor.
	protected IFeatureExtractor<WINDATA, FDATA> featureExtractor;
	protected IFeatureProcessor<FDATA> featureProcessor;
	protected  int windowShiftMsec = AbstractClassifier.DEFAULT_WINDOW_SHIFT_MSEC;
	protected  int windowSizeMsec = AbstractClassifier.DEFAULT_WINDOW_SIZE_MSEC;
	/**
	 * If this is called, then {@link #setFeatureExtractor(IFeatureExtractor)} or {@link #setFeatureGramDescriptor(IFeatureGramDescriptor)} must 
	 * be called before calling {@link #build()}.
	 */
	protected AbstractClassifierBuilder() {
	}

	/**
	 * A convenience on {@link #AbstractClassifierBuilder(IFeatureExtractor, IFeatureProcessor)} that does not set a feature processor.
	 * @param fe
	 */
	protected AbstractClassifierBuilder(IFeatureExtractor<WINDATA, FDATA> fe) {
		this(fe, null);
	}
	
	protected AbstractClassifierBuilder(IFeatureGramDescriptor<WINDATA, FDATA> fge) {
		this.setFeatureGramDescriptor(fge);
	}


	/**
	 * Set the training label, extractor and default window size and shift.
	 * @param fe
	 * @param processor
	 */
	protected AbstractClassifierBuilder(IFeatureExtractor<WINDATA, FDATA> fe, IFeatureProcessor<FDATA> processor) {
		this.featureExtractor = fe;
		this.featureProcessor = processor;
		this.featureGramDescriptor = null;
	}
	
	
	@Override
	public IClassifierBuilder<WINDATA, FDATA> setFeatureExtractor(IFeatureExtractor<WINDATA, FDATA> fe) {
		featureExtractor  = fe;
		this.featureGramDescriptor = null;
		return this;
	}
	
	//	@Override
	public IFeatureExtractor<WINDATA, FDATA>  getFeatureExtractor() {
		return featureExtractor;
	}

	protected List<IFeatureGramDescriptor<WINDATA,FDATA>> getFeatureGramExtractors() {
		if (featureGramDescriptor == null) 
			this.featureGramDescriptor = new FeatureGramDescriptor<WINDATA,FDATA>(this.windowSizeMsec, this.windowShiftMsec, this.featureExtractor, this.featureProcessor);
		List<IFeatureGramDescriptor<WINDATA,FDATA>> fgeList = new ArrayList<IFeatureGramDescriptor<WINDATA,FDATA>>();
		fgeList.add(this.featureGramDescriptor);
		return fgeList;
	}
	
	/**
	 * A shallow clone of the field values.
	 */
	@SuppressWarnings("unchecked")
	public AbstractClassifierBuilder<WINDATA, FDATA> clone() {
		Object o;
		try {
			o = super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			o=null;
		}
		return (AbstractClassifierBuilder<WINDATA, FDATA>)o;
	}

	@Override
	public IClassifierBuilder<WINDATA, FDATA> setWindowShiftMsec(int msec) {
		this.windowShiftMsec = msec;
		this.featureGramDescriptor = null;
		return this;
	}

	@Override
	public IClassifierBuilder<WINDATA, FDATA> setWindowSizeMsec(int msec) {
		this.windowSizeMsec = msec;
		this.featureGramDescriptor = null;
		return this;
	}

	@Override
	public IClassifierBuilder<WINDATA, FDATA> setFeatureProcessor(IFeatureProcessor<FDATA> featureProcessor) {
		this.featureProcessor = featureProcessor;
		this.featureGramDescriptor = null;
		return this;
	}

	/**
	 * @return the windowShiftMsec
	 */
	@Override
	public int getWindowShiftMsec() {
		return windowShiftMsec;
	}

	/**
	 * @return the windowSizeMsec
	 */
	@Override
	public int getWindowSizeMsec() {
		return windowSizeMsec;
	}

	/**
	 * @return the featureProcessor
	 */
	@Override
	public IFeatureProcessor<FDATA> getFeatureProcessor() {
		return featureProcessor;
	}

	/**
	 * @return the transforms
	 */
	@Override
	public ITrainingWindowTransform<WINDATA> getTransform() {
		return transform;
	}

	/**
	 * @param transform the transform to set
	 */
	@Override
	public IClassifierBuilder<WINDATA, FDATA> setTransform(ITrainingWindowTransform<WINDATA> transforms) {
		this.transform = transforms;
		return this;
	}

	@Override
	public IClassifierBuilder<WINDATA, FDATA> setFeatureGramDescriptor(IFeatureGramDescriptor<WINDATA, FDATA> fge) {
		this.featureGramDescriptor = fge;
		this.windowSizeMsec = (int)fge.getWindowSizeMsec();
		this.windowShiftMsec = (int)fge.getWindowShiftMsec();
		this.featureExtractor = fge.getFeatureExtractor();
		this.featureProcessor = fge.getFeatureProcessor();
		return this;
	}

//	@Override
//	public List<IClassifier<WINDATA>> enumerateClassifiers(String trainingLabel, IFeatureExtractor<WINDATA,FDATA>[] extractors, IFeatureProcessor<FDATA>[] processors, 
//						int[] windowSizesMsec, double[] windowShiftPercents) throws AISPException {
//		// We make a clone of this instance because it appears that call setPrimaryTrainingLabel() on this instance causes a trainingLabel=null exception later
//		// See issue #287, https://github.ibm.com/IoT-Sound/iot-sound/issues/287.
//		AbstractClassifierBuilder<WINDATA,FDATA> thisClone = this.clone();
//		
//		// Capture existing values we will be changing so we can return the builder to its original state
//		IFeatureExtractor<WINDATA,FDATA> oldExtractor = thisClone.getFeatureExtractor();
//		IFeatureProcessor<FDATA> oldProcessor = thisClone.getFeatureProcessor();
//		int oldWindowSize = thisClone.getWindowSizeMsec();
//		int oldWindowShift = thisClone.getWindowShiftMsec();
//		String oldTrainingLabel = thisClone.getPrimaryTrainingLabel();
//
//		// Allow any of the input arrays to be null by defining them with values from the current state of the builder.
//		if (trainingLabel == null) {
//			trainingLabel = oldTrainingLabel;
//			if (trainingLabel == null)
//				throw new IllegalArgumentException("training label can not be null if not already set in this instance");
//		}
//		if (extractors == null || extractors.length == 0) { 
//			if (oldExtractor == null)
//				throw new IllegalArgumentException("feature extractor array can not be null if not already set in this instance");
//			extractors = new IFeatureExtractor[] { oldExtractor };
//		}
//		if (processors == null || processors.length == 0)  
//			processors = new IFeatureProcessor[] { oldProcessor };
//		
//		if (windowSizesMsec == null || windowSizesMsec.length == 0)  {
//			if (oldWindowSize == 0)
//				throw new IllegalArgumentException("window size can not be null if not already set in this instance");
//			windowSizesMsec = new int[] { oldWindowSize };
//		}
//		for (double t : windowSizesMsec)  {
//			if (t <= 0) 
//				throw new IllegalArgumentException("window size value of " + t + " must be larger than 0");
//		}
//		if (windowShiftPercents == null || windowShiftPercents.length == 0) 
//			windowShiftPercents = new double[] { (100.0 * oldWindowShift / oldWindowSize) };
//		for (double t : windowShiftPercents)  {
//			if (t < 0 || t > 100) 
//				throw new IllegalArgumentException("window shift percentage value of " + t + " must be between 0 and 100");
//		}
//		
//		
//		// Iterate over all combinations of extractors, processors, windows sizes and shifts.
//		// SInce the order of window processing is feature extraction then processors, we order the returned list 
//		// so that feature extraction varies least frequently and processing most frequently. 
//		thisClone.setPrimaryTrainingLabel(trainingLabel);
//		List<IClassifier<WINDATA>> classifiers = new ArrayList<IClassifier<WINDATA>>();
//		for (IFeatureExtractor<WINDATA,FDATA> extractor : extractors) {
//			thisClone.setFeatureExtractor(extractor);
//			for (double windowShiftPercent: windowShiftPercents) {	 
//				for (int windowSizeMsec : windowSizesMsec) {
//					if (windowSizeMsec > 0) {
//						int windowShiftMsec = (int)(windowSizeMsec * windowShiftPercent/100.0 + .5); 
//						thisClone.setWindowSizeMsec(windowSizeMsec);
//						thisClone.setWindowShiftMsec(windowShiftMsec);
//					} else {
//						thisClone.setWindowSizeMsec(-1);
//						thisClone.setWindowShiftMsec(0);
//					}
//					for (IFeatureProcessor<FDATA> processor: processors) {
//						thisClone.setFeatureProcessor(processor);
//						IClassifier<WINDATA> c = thisClone.build();
//						classifiers.add(c);
//					}
//				}
//			}
//		}
//		
////		// Restore builder to original values.
////		this.setPrimaryTrainingLabel(oldTrainingLabel);
////		this.setFeatureExtractor(oldExtractor);
////		this.setFeatureProcessor(oldProcessor);
////		this.setWindowSizeMsec(oldWindowSize);
////		this.setWindowShiftMsec(oldWindowShift);
//		
//		return classifiers;
//	}

}
