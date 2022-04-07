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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.DataTypeEnum;
import org.eng.aisp.IDataWindow;
import org.eng.aisp.ILabeledDataWindow;
import org.eng.util.IShuffleIterable;
import org.eng.util.IthIterable;
import org.eng.util.IthShuffleIterable;

/**
 * Extends the superclass to support the implementation of IFixableClassifier.
 * This class expects the subclass to create an instance of IFixedClassifier during its
 * implementation of {@link #trainFixedClassifierOnData(String, Iterable)}.  The returned IFixedClassifier is
 * then used to implement {@link #classify(IDataWindow)}, {@link #showModel()} and
 * {@link #getFixedClassifier()} for the subclasses.
 * 
 * @author dawood
 */
public abstract class AbstractFixableClassifier<WINDATA> extends AbstractClassifier<WINDATA> implements IFixableClassifier<WINDATA> {

	private static final long serialVersionUID = -5368359716729693338L;

	protected IFixedClassifier<WINDATA> classifier;
	protected final boolean preShuffleData;
	
	/**
	 * A convenience on {@link #AbstractFixableClassifier(boolean)} with preShufflingData=false.
	 */
	protected AbstractFixableClassifier() {
		this(false);
	}

	/**
	 * 
	 * @param preShuffleData if true, then {@link #train(String, Iterable)} shuffles that data before calling {@link #trainFixedClassifierOnData(String, Iterable)}.
	 */
	protected AbstractFixableClassifier(boolean preShuffleData) {
		super();
		this.preShuffleData = preShuffleData;
	}

	/**
	 * Check to make sure there is some data, then call the subclass implementation of {@link #trainFixedClassifierOnData(String, Iterable)}.
	 * After training, set the data-type tag on this instance from the first piece of data, if present.
	 */
	@Override
	public void  train(String trainingLabel, Iterable<? extends ILabeledDataWindow<WINDATA>> data) throws AISPException {
//		AISPLogger.logger.info("Training " + this.getClass().getSimpleName() + " on " + TrainingSetInfo.getInfo(data).getTotalSamples() + " pieces of data");
		this.classifier = null ; // Free up some memory in case this is a retrain request.
		if (data == null || data.iterator().hasNext() == false) 
			throw new AISPException("Training data set is null or empty");

		// Try and determine the data type
		ILabeledDataWindow<?> ldw = data.iterator().next();
		DataTypeEnum dataType = DataTypeEnum.getDataTypeTag(ldw);

		// Shuffle and train.
		if (preShuffleData)
			data = shuffleData(data);
		this.classifier = trainFixedClassifierOnData(trainingLabel, data);
		
		// Set the data type, if we found one.
		if (dataType != null && this.classifier != null) {
			DataTypeEnum.setDataTypeTag(this, dataType);
			DataTypeEnum.setDataTypeTag(this.classifier, dataType);
		}
	}

	private static boolean warned = false;
	
	protected static <WINDATA> Iterable<ILabeledDataWindow<WINDATA>> shuffleData(Iterable<? extends ILabeledDataWindow<WINDATA>> data) {
		Iterable<ILabeledDataWindow<WINDATA>>  shuffled;
		int seed = 32490431;
		if (data instanceof IShuffleIterable) {
			shuffled = ((IShuffleIterable)data).shuffle(seed);	// Make the results repeatable
		} else {
			// Need to pull all data into memory so we can shuffle it.
			if (!warned) {
				AISPLogger.logger.warning("The use of an Iterable other than IShuffleIterable has higher memory requirements."); 
				warned = true;
			}
			List<ILabeledDataWindow<WINDATA>> dataList = new ArrayList<ILabeledDataWindow<WINDATA>>();
			for (ILabeledDataWindow<WINDATA> ldw : data)
				dataList.add(ldw);
			Collections.shuffle(dataList, new Random(seed));  //randomize the data sequence
			shuffled = dataList;
		}
		return shuffled;
	}

	/**
	 * Build the classifier from the given set of data.
	 * If this is called, the iterable has at least one piece of data in it.
	 * @param trainingLabel TODO
	 * @param data data passed to {@link #train(Iterable)} that has at least one item. 
	 * @return never null.
	 * @throws AISPException
	 */
	protected abstract IFixedClassifier<WINDATA> trainFixedClassifierOnData(String trainingLabel, Iterable<? extends ILabeledDataWindow<WINDATA>> data) throws AISPException;
	
	
	/**
	 * Classify using the IFixedClassifier created in {@link #train(Iterable)}.
	 */
	@Override
	public Map<String, Classification> classify(IDataWindow<WINDATA> sample) throws AISPException {
		if (classifier == null)
			throw new AISPException("Model is not initialized");
		return classifier.classify(sample);
	}

//	/**
//	 * Show the model of the classifier created in {@link #train(Iterable)}.
//	 */
//	@Override
//	public String showModel() {
//		String str;
//		if (classifier == null)
//			str = "Uninitialized";
//		else
//			str = classifier.showModel();
//		return str; 
//	}

	/**
	 * Return the classifier created in {@link #train(Iterable)}.
	 */
	@Override
	public IFixedClassifier<WINDATA> getFixedClassifier() { 
		return classifier; 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
		result = prime * result + (preShuffleData ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof AbstractFixableClassifier))
			return false;
		AbstractFixableClassifier other = (AbstractFixableClassifier) obj;
		if (classifier == null) {
			if (other.classifier != null)
				return false;
		} else if (!classifier.equals(other.classifier))
			return false;
		if (preShuffleData != other.preShuffleData)
			return false;
		return true;
	}
	
	@Override
	public String getTrainedLabel() {
		if (classifier == null)	// Not trained yet
			return null;
		return classifier.getTrainedLabel();
	}

	/**
	 * Get a subset of the given ITEMs for either training or testing.
	 * Training data consists of features in the N-1 folds and the test data is from the 1 fold not used for training. 
	 * This method tries to preserver the shuffle-ability of the iterable by creating an IthShuffleIterable if
	 * the given iterable is an instance of IShuffleIterable.  This is important for training on streaming data.
	 * @param features
	 * @param folds
	 * @param testFoldIndex
	 * @param forTraining
	 * @return never null.
	 */
	protected static <ITEM> Iterable<ITEM> getItemFolds(Iterable<ITEM> items, int folds, int testFoldIndex, boolean forTraining) {
		Iterable<ITEM>  r;
		if (items instanceof IShuffleIterable) {
			r = new IthShuffleIterable<ITEM>((IShuffleIterable<ITEM>)items, folds, testFoldIndex, forTraining);
		} else {
			r = new IthIterable<ITEM>(items, folds, testFoldIndex, forTraining);
		}
		return r;
	}

	@Override
	public String toString() {
		return "AbstractFixableClassifier [classifier=" + classifier + ", preShuffleData=" + preShuffleData + "]";
	}

}
