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
package org.eng.aisp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.ConfusionMatrix;
import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.aisp.classifier.Classification.LabelValue;
import org.eng.aisp.segmented.LabeledSegmentSpec;
import org.eng.aisp.segmented.SegmentedSoundRecording;
import org.eng.aisp.sensor.DeployedSensor;
import org.eng.aisp.sensor.SensorEnvironment;
import org.eng.aisp.sensor.SensorProfile;
import org.eng.aisp.user.CarrierReference;
import org.eng.aisp.user.NotifyTarget;
import org.eng.aisp.user.NotifyTarget.NotificationType;
import org.eng.aisp.user.UserProfile;
import org.eng.aisp.util.PCMUtil;
import org.eng.aisp.util.VectorUtils;
import org.eng.storage.IItemStorage;
import org.eng.storage.INamedItemStorage;
import org.eng.storage.StorageException;
import org.eng.util.CollectionUtils;
import org.eng.util.ConstantSignalGenerator;
import org.eng.util.ISignalGenerator;
import org.eng.util.SingleFrequencySignalGenerator;
import org.junit.Assert;

public class SoundTestUtils {

	

//	public static class SingleFrequencySignalGenerator2 extends SingleFrequencySignalGenerator {
//	
//		public SingleFrequencySignalGenerator2(int samplingRate, double amp, double offset, double htz, boolean addWhiteNoise, boolean addFreqNoise) {
//			super(samplingRate, amp, offset, htz, addWhiteNoise, addFreqNoise);
//		}
//	
//		/**
//		 * Override to mix in more than one frequency.
//		 */
//		@Override
//		public Double next() {
//			int count = 3;
//			double sum = 0;
//			updateCurrentHtz();
//			double localHtz = currentHtz;
//			for (int i = 0; i < count; i++) {
//				double r = nextSignalValue(localHtz);
//				// System.out.println("nextCalls=" + nextCalls + ", htz=" +
//				// localHtz + ", samplingRate=" + samplingRate + ", r = " + r);
//				sum += r;
//				localHtz *= 1.39;
//			}
//			nextCalls++;
//			return sum / count;
//		}
//	
//	
//	}
	
	public enum VerifyMode {
		CompareEqual,
		CompareNotEqual,
		ExpectNoClassification
	}

	public final static String EXTENDED_RUNTIME_TESTING_PROPERTY_NAME = "EXTENDED_RUNTIME_TESTING";
//	public final static String PRODUCT_ONLY_TESTING_PROPERTY_NAME = "MainTestSuite.productOnlyTesting";
	/** Assume sms is only tested a phone number that is from Verizon in the US  */ 
	static CarrierReference carrier = new CarrierReference("United States", "Verizon");

	public SoundTestUtils() {
	}

	/**
	 * Used in AI product to turn off testing of some extended features. 
	 * @return Consults the {@link #EXTENDED_RUNTIME_TESTING_PROPERTY_NAME} system property or env var and returns its values
	 * or false if unset.
	 */
	public static boolean isExtendedRuntimeTesting() {
		String value = System.getProperty(EXTENDED_RUNTIME_TESTING_PROPERTY_NAME);
//		if (value == null)
//			value = System.getProperty(PRODUCT_ONLY_TESTING_PROPERTY_NAME);
		if (value == null) {
			value = System.getenv(EXTENDED_RUNTIME_TESTING_PROPERTY_NAME);
			if (value == null)
				return false;
		}
		return Boolean.valueOf(value);
	}


	public static UserProfile createUser(String username, String smsPhoneNumber, String emailAddress) {
		UserProfile user;
		List<NotifyTarget> targetList = new ArrayList<NotifyTarget>();
		NotifyTarget target;
		if (smsPhoneNumber != null) {
	
			target = new NotifyTarget(NotificationType.SMS, smsPhoneNumber, carrier);
			targetList.add(target);
		}
		if (smsPhoneNumber != null) {
			target = new NotifyTarget(NotificationType.Email, emailAddress, null);
			targetList.add(target);
		}
		user = new UserProfile(username, targetList);
			
		return user;
	}

	public static List<SoundClip> createClips(int n, double msecStart, double msecSpacing, double msecDuration, int channels, int samplingRate, int bitsPerSample, double amp, double offset, int htz, boolean addFreqNoise) {
		return createClips(n, msecStart, msecSpacing, msecDuration, channels, samplingRate, bitsPerSample, amp, offset, htz, true, addFreqNoise);
	}

	public static List<SoundClip> createClips(int n, double msecStart, double msecSpacing, double msecDuration, int channels, int samplingRate, 
			int bitsPerSample, double amp, double offset, int htz, boolean addAmpNoise, boolean addFreqNoise) {
		ISignalGenerator siggen = new SingleFrequencySignalGenerator(samplingRate, amp, offset, htz, addAmpNoise, addFreqNoise);	
		return createClips(n, msecStart, msecSpacing, msecDuration, channels, samplingRate, bitsPerSample, siggen);
	}

	public static List<SoundClip> createClips(int n, double msecStart, double msecSpacing, double msecDuration, int channels, double samplingRate, int bitsPerSample, ISignalGenerator siggen) {
		List<SoundClip> clipList = new ArrayList<SoundClip>();
		int samples = (int)(msecDuration / 1000.0 * samplingRate + .5);
	
		for (int i=0 ; i<n ; i++) {
			double signal[] = new double[samples];
			for (int j=0 ; j<signal.length ; j++)
				if (siggen.hasNext())
					signal[j] = siggen.next();
				else
					throw new IllegalStateException("hasNext() is false for signal generator.");
//			OnlineStats stats = new OnlineStats();
//			stats.addSamples(signal);
//			AISPLogger.logger.info("mean = " + stats.getMean());
			byte pcm[] = PCMUtil.double2PCM(signal, channels, bitsPerSample); 
			SoundClip clip = new SoundClip(msecStart, channels, bitsPerSample, samplingRate, pcm); 
//			stats.reset();
//			stats.addSamples(clip.getData());
//			AISPLogger.logger.info("pcm data mean = " + stats.getMean());
			clipList.add(clip);
			msecStart += msecSpacing + msecDuration;
		}
		return clipList;
	}

	public static List<SoundClip> createClips(int n, double msecStart, int msecSpacing, int msecDuration, double value) {
		return createClips(n,msecStart, msecSpacing, msecDuration, 1, 44000, 8, new ConstantSignalGenerator(value));
	}

	/**
	 * Convenience on {@link #createNormalAbnormalTrainingRecordings(String, String, int, int, String, int)} using 2 second sounds.
	 * @param trainingLabel
	 * @param normalLabelValue
	 * @param normalCount
	 * @param abnormalLabelValue
	 * @param abnormalCount
	 * @return
	 */
	public static List<SoundRecording> createNormalAbnormalTrainingRecordings(String trainingLabel, String normalLabelValue, int normalCount, String abnormalLabelValue, int abnormalCount) {
		return createNormalAbnormalTrainingRecordings(trainingLabel, normalLabelValue, 2000, normalCount, abnormalLabelValue, abnormalCount);
	}

	/**
	 * Creates training dat using 
	 * @param trainingLabel specifies one or more labels.  Multiple labels are specified as comma-separated values.  If using multiple labels, their values
	 * will mimic each other.
	 * @param normalLabelValue
	 * @param sampleDurationMsec
	 * @param normalCount
	 * @param abnormalLabelValue
	 * @param abnormalCount
	 * @return
	 */
	public static List<SoundRecording> createNormalAbnormalTrainingRecordings(String trainingLabel, String normalLabelValue, int sampleDurationMsec, 
				int normalCount, String abnormalLabelValue, int abnormalCount) {
	//		String trainingLabel = "status";
			String trainingLabels[] = trainingLabel.split(",");
			Properties abnormal = new Properties();
			Properties normal = new Properties();
			for (int i=0 ; i<trainingLabels.length ; i++) {
				abnormal.put(trainingLabels[i], abnormalLabelValue); 
				normal.put(trainingLabels[i],  normalLabelValue);
			}
			int sampleSpacingMsec = 100;
			int sampleStartMsec = 0;
			double normalAmp = 1;
			byte normalOffset = 0;
			int  normalHtz = 1000;
			double abnormalAmp = 1;
			byte abnormalOffset = 0;
			int abnormalHtz = 4000;
			int channels = 1;
			
			// Train a model on 0's as normal 
			List<SoundRecording> srList = new ArrayList<SoundRecording>();
			int samplingRate = 44000;
			int bitsPerSample = 8;
			// TODO: Should use createTrainingRecordings below
			List<SoundClip> normalClipList = SoundTestUtils.createClips(normalCount, sampleStartMsec, sampleSpacingMsec, sampleDurationMsec, channels, samplingRate, bitsPerSample, normalAmp, normalOffset, normalHtz,false);
	
			for (SoundClip clip : normalClipList) {
				SoundRecording sr = new SoundRecording(clip, normal);
				srList.add(sr);
			}
	
			// TODO: Should use createTrainingRecordings below
			List<SoundClip> abnormalClipList = SoundTestUtils.createClips(abnormalCount, sampleStartMsec, sampleSpacingMsec, sampleDurationMsec, channels, samplingRate, bitsPerSample, abnormalAmp, abnormalOffset, abnormalHtz,false);
			for (SoundClip clip : abnormalClipList) {
				SoundRecording sr = new SoundRecording(clip, abnormal);
				srList.add(sr);
			}	
			
			return srList;
		}

	public static List<SoundRecording> createTrainingRecordings(int startMsec, int durationMsec, int spacingMsec, int htz, int count, String trainingLabel, String labelValue) {
		return createTrainingRecordings(count, 1, 44000, 8, startMsec, durationMsec, spacingMsec, htz, trainingLabel, labelValue);
	}

	/**
	 * 
	 * @param count
	 * @param channels
	 * @param samplingRate
	 * @param bitsPerSample
	 * @param startMsec
	 * @param durationMsec
	 * @param spacingMsec
	 * @param htz
	 * @param trainingLabel
	 * @param labelValue
	 * @return
	 */
	public static List<SoundRecording> createTrainingRecordings(int count, int channels, int samplingRate, int bitsPerSample, int startMsec, int durationMsec, int spacingMsec, int htz, String trainingLabel, String labelValue) {
		Properties normal = new Properties();
		normal.put(trainingLabel,  labelValue);
		return createTrainingRecordings(count, channels, samplingRate, bitsPerSample, startMsec, durationMsec, spacingMsec, htz, normal);
	}

	public static List<SoundRecording> createTrainingRecordings(int count, long startMsec, int durationMsec, int spacingMsec, int htz, Properties labels) {
		return createTrainingRecordings(count, 1, 44000, 8, startMsec, durationMsec, spacingMsec, htz, labels);
	}

	public static List<SoundRecording> createTrainingRecordings(int count, long startMsec, int durationMsec, int spacingMsec, int htz, Properties labels, boolean addNoise) {
		return createTrainingRecordings(count, 1, 44000, 8, startMsec, durationMsec, spacingMsec, htz, labels, addNoise);
	}

	/**
	 * @param count
	 * @param startMsec
	 * @param durationMsec
	 * @param spacingMsec
	 * @param htz
	 * @param labels
	 * @return
	 */
	public static List<SoundRecording> createTrainingRecordings(int count, int channels, int samplingRate, int bitsPerSample, long startMsec, int durationMsec, int spacingMsec, int htz, Properties labels) {
		return createTrainingRecordings(count, channels, samplingRate, bitsPerSample, startMsec, durationMsec, spacingMsec, htz, labels, false);
	}
	
	public static List<SoundRecording> createTrainingRecordings(int count, int channels, int samplingRate, int bitsPerSample, int startMsec, int durationMsec, int spacingMsec, int htz,
			String label, String labelValue, boolean addNoise) {
		Properties labels = new Properties();
		labels.setProperty(label,  labelValue);
		return createTrainingRecordings(count, channels, samplingRate, bitsPerSample, startMsec, durationMsec, spacingMsec, htz, labels, addNoise);
	}

	public static List<SoundRecording> createTrainingRecordings(int count, int channels, int samplingRate, int bitsPerSample, double startMsec, 
			double durationMsec, int spacingMsec, int htz, Properties labels, boolean addNoise) {
		double amp = 1;
		return createTrainingRecordings(count, channels, samplingRate, bitsPerSample, startMsec, durationMsec, spacingMsec, amp, htz, labels, addNoise);
	}

	public static List<SoundRecording> createTrainingRecordings(int count, int channels, int samplingRate, int bitsPerSample, double startMsec, 
			double durationMsec, int spacingMsec, double amp, int htz, Properties labels, boolean addNoise) {
		Assert.assertTrue(amp > 0 && amp <=1);
//		return createTrainingRecordings(count, channels, samplingRate, bitsPerSample, startMsec, 
//				durationMsec, spacingMsec, htz, labels, addNoise, null) ;
//	}
//
//	public static List<SoundRecording> createTrainingRecordings(int count, int channels, int samplingRate, int bitsPerSample, int startMsec, 
//				int durationMsec, int spacingMsec, int htz, Properties labels, boolean addNoise, String sensorID) {
			double offset = 0;
	//		int samplingRate = 44000;
	//		int bitsPerSample = 8;
			
	//		String sensorID = "junit-test-sensor-id";
			List<SoundRecording> srList = new ArrayList<SoundRecording>();
			
			List<SoundClip> normalClipList = SoundTestUtils.createClips(count, startMsec, spacingMsec, durationMsec, channels, samplingRate, bitsPerSample, amp, offset, htz, addNoise);
			for (SoundClip clip : normalClipList) {
				SoundRecording sr = new SoundRecording(clip, labels);
				srList.add(sr);
			}
			
			return srList;
		}

	public static DeployedSensor createSensor(String uprofileID, String name, String location, Properties p) {
		SensorProfile sp = new SensorProfile(name, "testing-type", p);
		SensorEnvironment env = new SensorEnvironment(location,"sub-" + location);
		DeployedSensor sensor = new DeployedSensor(uprofileID, sp,env);
		return sensor;
	}

	public static List<SoundRecording> createTrainingRecordings(int count, int durationMsec, int htz, Properties labels, boolean addNoise) {
		double amp = 1;
		return createTrainingRecordings(count, 2, 44000, 16, 0, durationMsec, 0, amp, htz, labels, addNoise);
	}

	public static List<SoundRecording> createTrainingRecordings(int count, int durationMsec, double amp, int htz, Properties labels, boolean addNoise) {
		return createTrainingRecordings(count, 2, 44000, 16, 0, durationMsec, 0, amp, htz, labels, addNoise);
	}

	public static SoundRecording createTrainingRecording(double startMsec, int durationMsec, int htz, Properties labels, boolean addNoise) {
		return createTrainingRecordings(1, 2, 44000, 16, startMsec, durationMsec, 0, htz, labels, addNoise).get(0);
	}

	/**
	 * @param storageClient
	 * @throws AISPException
	 * @throws StorageException 
	 */
	public static void cleanUpStorage(IItemStorage<?> storageClient) throws StorageException {
		storageClient.clear();
		storageClient.disconnect();
	}

	/**
	 * Pull the contents of the iterable into a list.
	 * @param iterable
	 * @return null if the given iterable is null.
	 */
	public static <T> List<T> iterable2List(Iterable<T> iterable) {
		return CollectionUtils.asList(iterable);
	}
	
	private static boolean AI_ISSUE_593_WORKAROUND = false;
	
	public static void verifyClassifications(IFixedClassifier<double[]> classifier, Iterable<SoundRecording> srList, String labelName, VerifyMode verifyMode, String compareLabelValue) throws AISPException {
		for (SoundRecording sr : srList) {
			verifyClassification(classifier, sr, labelName, verifyMode, compareLabelValue);
		}
	}

	public static void verifyClassifications(IFixedClassifier<double[]> classifier, Iterable<SoundRecording> srList, String labelName, VerifyMode verifyMode) throws AISPException {
		int count = 0;
		for (SoundRecording sr : srList) {
			verifyClassification(classifier, sr, labelName, verifyMode, null);
			count++;
		}
		Assert.assertTrue("No sounds provided for testing - test invalid.", count != 0);	// Sanity check
	}

	/**
	 * Make sure the classifier gets the labels attached to the given sounds for the given label name.
	 * @param classifier
	 * @param srList
	 * @param labelName
	 * @throws AISPException
	 */
	public static void verifyClassifications(IFixedClassifier<double[]> classifier, Iterable<SoundRecording> srList, String labelName) throws AISPException {
		int count = 0;
		for (SoundRecording sr : srList) {
			verifyClassification(classifier, sr, labelName, VerifyMode.CompareEqual, null);
			count++;
		}
		Assert.assertTrue("Test invalid. No sounds provided for verification", count != 0);
	}

	/**
	 * @param classifier
	 * @param sr
	 * @param labelName
	 * @param compareLabelValue if null, then use the label found on the given recording
	 * @param compareEqual
	 * @throws AISPException
	 */
	public static void verifyClassification(IFixedClassifier<double[]> classifier, SoundRecording sr, String labelName,
			VerifyMode verifyMode, String compareLabelValue) throws AISPException {
		Map<String,Classification> cmap = classifier.classify(sr.getDataWindow());
		Classification c = cmap.get(labelName);
		if (verifyMode == VerifyMode.ExpectNoClassification) {
			Assert.assertTrue(c == null);
			return;
		} else {
			Assert.assertTrue(c != null);
		}
		Properties labels = sr.getLabels();
		if (compareLabelValue == null)
			compareLabelValue  = labels.getProperty(labelName);
		String labelValue = c.getLabelValue();
		String labelFromRecording = sr.getLabels().getProperty(labelName);
		Assert.assertTrue(labelValue != null);
		if (compareLabelValue == null)
			compareLabelValue = labelFromRecording; 
//		System.out.println("True label=" + sr.getLabels().getProperty(labelName) + ", labelValue=" + labelValue + ", expectedValue=" + compareLabelValue);
		String prefix = classifier.getClass().getName() + ":True label='" + labels.getProperty(labelName) + "', labelValue='" + labelValue;
		if (verifyMode == VerifyMode.CompareEqual) {
			Assert.assertTrue(prefix + "' != expectedValue=" + compareLabelValue, labelValue.equals(compareLabelValue));
		} else {
			Assert.assertTrue(prefix + "' == expectedValue=" + compareLabelValue, !labelValue.equals(compareLabelValue));
		}

		// Make sure the ranked values, if provided, have confidences that sum to 1.
		List<LabelValue> rankedValues = c.getRankedValues();
		if (rankedValues != null) {
			Assert.assertTrue(rankedValues.get(0).getLabelValue().equals(c.getLabelValue()));
			Assert.assertTrue(rankedValues.get(0).getConfidence() == c.getConfidence());
			
			//When providing ranked values, the confidence values should sum up to one
			double sumConfidence = 0.0;
			for (LabelValue lv : rankedValues) {
				sumConfidence += lv.getConfidence();
			}
			Assert.assertTrue("Sum confidence value not equal to 1, sum value is " + sumConfidence, 
					sumConfidence >= 0.99999 && sumConfidence <= 1.00001);
		} 
	}

	/**
	 * Check the given confusion matrix against the inputs.
	 * @param cm
	 * @param trainingLabel
	 * @param size if 0 or larger, then check the number of samples in the confusion matrix.
	 */
	public static void validateConfusionMatrix(ConfusionMatrix cm, String trainingLabel, int size) {
		Assert.assertTrue(cm != null);
		Assert.assertTrue(size < 0 || cm.getTotalSamples() == size);
		Assert.assertTrue(cm.getLabelName().equals(trainingLabel));
	}
	
	public static List<SoundRecording> create3DTrainingSounds(String trainingLabel, int count, int durationMsec) {
		return create3DTrainingSounds(trainingLabel,count,durationMsec, 40, 10);
	}

	/**
	 * Convenience on {@link #create3DTrainingSounds(String, int, int, int, int, int)} with startMsec=0.
	 */
	public static List<SoundRecording> create3DTrainingSounds(String trainingLabel, int count, int durationMsec, int samplingRate, int htz) {
		return create3DTrainingSounds(trainingLabel, count, 0, durationMsec, samplingRate, htz);
	}

	/**
	 * Create sounds for each of the x,y,z directions labeled with "x", "y", "z", respectively.
	 * @param trainingLabel
	 * @param count number of sounds for each dimension
	 * @param startMsec
	 * @param durationMsec 
	 * @param samplingRate
	 * @param htz same frequency used for each direction.
	 * @return
	 */
	public static List<SoundRecording> create3DTrainingSounds(String trainingLabel, int count, double startMsec, double durationMsec, int samplingRate, int htz) {
		Properties xLabels = new Properties();
		xLabels.put(trainingLabel, "x");
//		List<SoundRecording> srList = SoundTestUtils.createTrainingRecordings(count, durationMsec, 4000, xLabels, true);
		List<SoundRecording> srList = SoundTestUtils.createTrainingRecordings(count, 1, samplingRate, 16, startMsec, durationMsec, 0, htz, xLabels, true);
		setDataType(srList, DataTypeEnum.VibrationXYZ);
		List<SoundRecording> trainingSounds = interleave(srList, 0, 3);

		Properties yLabels = new Properties();
		yLabels.put(trainingLabel, "y");
//		srList = SoundTestUtils.createTrainingRecordings(count, durationMsec, 4000, yLabels, true);
		srList = SoundTestUtils.createTrainingRecordings(count, 1, samplingRate, 16, startMsec, durationMsec, 0, htz, yLabels, true);
		setDataType(srList, DataTypeEnum.VibrationXYZ);
		trainingSounds.addAll(interleave(srList, 1, 3));

		Properties zLabels = new Properties();
		zLabels.put(trainingLabel, "z");
//		srList = SoundTestUtils.createTrainingRecordings(count, durationMsec, 4000, zLabels, true);
		srList = SoundTestUtils.createTrainingRecordings(count, 1, samplingRate, 16, startMsec, durationMsec, 0, htz, zLabels, true);
		setDataType(srList, DataTypeEnum.VibrationXYZ);
		trainingSounds.addAll(interleave(srList, 2, 3));
		return trainingSounds;
	}
	
	public static void setDataType(List<SoundRecording> srList, DataTypeEnum type) {
		for (SoundRecording sr : srList) {
			Properties tags = DataTypeEnum.setDataType(sr.getTagsAsProperties(), type);
			sr.addTags(tags);
		}
	}
	
	private static List<SoundRecording> interleave(List<SoundRecording> srList, int dim, int nDims) {
		List<SoundRecording> interleavedSR = new ArrayList<SoundRecording>();
		for (SoundRecording sr : srList) {
			SoundClip clip = sr.getDataWindow();
			double[] data = clip.getData();
			double newData[] = new double[data.length * nDims];
			Arrays.fill(newData,0);
			for (int i=0, j=dim ; j<newData.length ; i++, j += nDims)
				newData[j] = data[i];
			SoundClip newClip = new SoundClip(clip.getStartTimeMsec(), clip.getEndTimeMsec(), newData, nDims);
			SoundRecording newSR = new SoundRecording(sr, newClip);
			interleavedSR.add(newSR);
		}
		return interleavedSR;
	}

	public static <ITEM> List<String> storeItems(IItemStorage<ITEM> storage, Iterable<? extends ITEM> items) throws StorageException {
		List<String> ids = new ArrayList<String>();
		for (ITEM item: items) {
			String id = storage.add(item);
			ids.add(id);
		}
		return ids;
	}
	
	public static <ITEM> List<String> storeNamedItems(INamedItemStorage<ITEM> storage, String name, Iterable<? extends ITEM> items) throws StorageException {
		List<String> ids = new ArrayList<String>();
		for (ITEM item: items) {
			String id = storage.addNamed(name, item);
			ids.add(id);
		}
		return ids;
	}

//	public static <ITEM> List<String> storeItems(IPartitionedItemStorage<ITEM> storage, String partition, Iterable<? extends ITEM> items) throws StorageException {
//		List<String> ids = new ArrayList<String>();
//		for (ITEM item: items) {
//			String id = storage.add(partition, item);
//			ids.add(id);
//		}
//		return ids;
//	}

	public static SoundClip mergeRecordingsAsClip(List<SoundRecording> srList) {
		List<SoundClip> clipList = new ArrayList<SoundClip>();
		for (SoundRecording sr: srList)
			clipList.add(sr.getDataWindow());
		return DoubleWindow.concatWindows(clipList, new SoundClipFactory());
	}

//	public static <DW extends IDataWindow<double[]>> DW mergeClips(List<SoundClip> clipList, IDataWindowFactory<double[], DW> dwFactory) {
//		double[] mergedData = null;
//		double samplingRate = 0;
//		double duration = 0;
//		for (SoundClip clip : clipList) {
//			double[]  data = clip.getData();
//			if (mergedData != null) {
//				if (samplingRate != clip.getSamplingRate())
//					throw new IllegalArgumentException("Sampling rates are not the same for these clips");
//				int newDataLen = mergedData.length+ data.length;
//				double[] newData = new double[newDataLen];
//				System.arraycopy(mergedData, 0, newData, 0, mergedData.length);
//				System.arraycopy(data, 0, newData, mergedData.length, data.length);
//				mergedData = newData;
//			} else {
//				samplingRate = clip.getSamplingRate();
//				mergedData = data; 
//			}
//			duration += clip.getDurationMsec();
//		}
////		return new SoundClip(0,duration, mergedData);
//		return dwFactory.newDataWindow(0, duration, mergedData);
//	}

	/**
	 * Create a sound with N segments defined by the inputs.
	 * @param segDurationMsec length of each segment  for each of the htz values
	 * @param htz list of frequencies.  1 segment will be created for each frequency.
	 * @return a single clip with htz.length segments each of the requessted length.
	 */
	public static SoundClip createChangePointSound(int durationMsec, int[] htz) {
		Properties noLabels = new Properties();
		List<SoundClip> segments = new ArrayList<SoundClip>();
		for (int freq : htz) {
			SoundClip  segment = createTrainingRecordings(1, durationMsec, freq, noLabels, false).get(0).getDataWindow();
			segments.add(segment);
		}
		SoundClip mergedClip = DoubleWindow.concatWindows(segments, new SoundClipFactory());
		return mergedClip;
	}

	/**
	 * Create a list of segmented sounds each made up of segments of segments at the given htz values.
	 * @param count the size of the returned list.
	 * @param segDurationMsec the duration of each sub-segment.
	 * @param htz an array of frequencies to assign to each segment.  Also defines the number of segments with each SegmentedSoundRecording.
	 * @param globalLabels labels applied to each SegmentedSoundRecording sound.
	 * @param labelName name of label holding the %d frequency values.
	 * @param tagIndexName if given, then this is the name of tag holding the 0-based segment index for each segment.
	 * @return
	 */
	public static List<SegmentedSoundRecording> createSegmentedSound(int count, int segDurationMsec, int[] htz, Properties globalLabels, String labelName, String tagIndexName) {
		List<SegmentedSoundRecording> ssrList = new ArrayList<SegmentedSoundRecording>();
		
		// Create labels for each htz value.
		String htzLabels[] = new String[htz.length];
		for (int i=0 ; i<htz.length ; i++) {
			htzLabels[i] = String.valueOf(htz[i]);
		}

		for (int i=0 ; i<count ; i++) {
			SoundClip clip = createChangePointSound(segDurationMsec, htz);	// A sound clip segmented by the given frequencies
			SoundRecording sr = new SoundRecording(clip, globalLabels, null);
			List<LabeledSegmentSpec> segSpecs = createSegments(sr, labelName, htzLabels, tagIndexName);
			SegmentedSoundRecording ssr = new SegmentedSoundRecording(sr, segSpecs);
			ssrList.add(ssr);
		}

		
		return ssrList;
	}
	
	/**
	 * Create the requested number of segments that span the given recording and add a single label and tag that contains a value specific to the 0-based segment index. 
	 * @param sr
	 * @param segments
	 * @param string
	 * @param string2
	 * @return
	 */
	public static List<LabeledSegmentSpec> createSegments(SoundRecording sr, String labelName, String[] labelValues, String tagIndexName) {
		int segments = labelValues.length;
		if (segments == 0)
			throw new IllegalArgumentException("segments must be larger than 0");
		double durationMsec = sr.getDataWindow().getDurationMsec();
		if (segments > durationMsec)
			throw new IllegalArgumentException("segments must be less than the duration of the given sound in msec");
			
		int segDurationMsec = (int)(durationMsec / segments);
		int startMsec = 0, endMsec = segDurationMsec;
		List<LabeledSegmentSpec> specList = new ArrayList<LabeledSegmentSpec>();
		for (int i=0 ; i<segments ; i++) {
			Properties labels = new Properties();
			Properties tags = new Properties();
			labels.setProperty(labelName, labelValues[i]); 
			if (tagIndexName != null)
				tags.setProperty(tagIndexName, String.valueOf(i)); 
			LabeledSegmentSpec spec = new LabeledSegmentSpec(startMsec, endMsec, labels, tags);
			specList.add(spec);
			startMsec = endMsec; 
			if (i + 1 == segments) {	// Last one
				endMsec = (int)durationMsec; 
			} else {
				endMsec += segDurationMsec; 
			}
		}
		return specList;
	}

	/**
	 * Ceate sounds with fixed levels for each segments (no htz).
	 * @param durationMsec
	 * @param levels an array of 1 or more with values in the range -1 to 1.
	 * @return
	 */
	public static SoundClip createContantChangePointSound(int durationMsec, double[] levels) {
		List<SoundClip> segments = new ArrayList<SoundClip>();
		int msecStart = 0, msecSpacing = 0, channels = 1, samplingRate = 44100, bitsPerSample =16, amp=1, htz = 0 ;
		boolean addAmpNoise = false, addFreqNoise = false;
		
		for (double level: levels) {
			SoundClip segment = SoundTestUtils.createClips(1, msecStart, msecSpacing, durationMsec, channels, samplingRate, bitsPerSample, amp, level, htz, addAmpNoise, addFreqNoise).get(0);
			segments.add(segment);
		}
		SoundClip mergedClip = DoubleWindow.concatWindows(segments, new SoundClipFactory());
		return mergedClip;
	}

	public static List<SoundClip> createClips(int clipCount, int msecDuration, int htz) {
		int msecStart=0, msecSpacing = 0, channels = 1, samplingRate=44100, bitsPerSample=16, amp=1, offset=0;
		boolean addFreqNoise = true;
		return SoundTestUtils.createClips(clipCount, msecStart, msecSpacing, msecDuration, channels, samplingRate, bitsPerSample, 
				amp, offset, htz, addFreqNoise);
	}

	/**
	 * Compare 2 double value to see if they are within some percentage of the 1st value.
	 * @param d1
	 * @param d2
	 * @param percentage a number ranging from 0 and 1.
	 * @return
	 */
	public static boolean doublePercentEquality(double d1, double d2, double percentage) {
		if (percentage <= 0 || percentage > 1)
			throw new IllegalArgumentException("percentage must be less than 1");
		double diff = Math.abs(d1-d2);
		return diff == 0 || diff < percentage * Math.abs(d1);
	}

	public static boolean doubleEpsilonEquality(double d1, double d2, double epsilon) {
		return Math.abs(d1-d2) <= epsilon ;
	}

	private static class StartTimeCompare implements Comparator<SoundClip> {

		@Override
		public int compare(SoundClip o1, SoundClip o2) {
			return Double.compare(o1.getStartTimeMsec(), o2.getStartTimeMsec());
		}
		
	}

	/**
	 * Sort the clips by time with the most recent last in the resulting list.
	 * @param clips modified on return.
	 * @return the same instance restorted.
	 */
	public static List<SoundClip> sort(List<SoundClip> clips) {
		Collections.sort(clips, new StartTimeCompare());
		return clips;
	}

	/**
	 * Determine if all the key/value pairs of the 2nd argument are present in the first.
	 * Requires that both the keys and values implement equals().
	 * @param container
	 * @param contained
	 * @return
	 */
	public static boolean containsAlls(Map<?,?>container, Map<?,?> contained) {
		for (Object key : contained.keySet()) {
			Object containerValue = container.get(key);
			if (containerValue == null)
				return false;
			Object containedValue = contained.get(key);
			if (!containerValue.equals(containedValue))
				return false;
		}
		return true;
	}

	/**
	 * Make sure the size and values in the given list match the expected values.
	 * @param values
	 * @param expectedValues
	 */
	public static void validateList(List<String> values, String[] expectedValues) {
		Assert.assertTrue(values!= null);
		Assert.assertTrue(values.size() == expectedValues.length);
		for (String expectedValue : expectedValues) {
			Assert.assertTrue("List is missing value " + expectedValue, values.contains(expectedValue));
		}
	}

	private static Properties getFrequencyLabels(String labelName, int hz) {
		Properties labels = new Properties();
		labels.setProperty(labelName, String.valueOf(hz)); 
		return labels;
	}
	/**
	 * Create sound recordings at the requested frequencies with each frequency having a label set to the frequency value.
	 * @param count number to generate at each frequency.
	 * @param durationMsec the length of each clip.
	 * @param htz and array of 1 or more frequencies
	 * @param labelName label to attach to each sound. The value of the label will be "%d" of the htz value.
	 * @return list of length count * htz.length
	 */
	public static List<SoundRecording> createTrainingData(int count, int durationMsec, int[] htz, String labelName) {
		List<SoundRecording> srList = new ArrayList<SoundRecording>();
		for (int freq : htz) {
			Properties labels = getFrequencyLabels(labelName, freq); 
			List<SoundRecording> fList = SoundTestUtils.createTrainingRecordings(count, durationMsec, freq, labels, true);
			srList.addAll(fList);
		}
		return srList;
	}

	public static <ITEM> void verifySubSet(Collection<ITEM> superset, Collection<ITEM> subset) {
		for (ITEM item : subset) {
			Assert.assertTrue("Item " + item + " not found in list.", superset.contains(item));
		}
	}

//
//	public static List<SoundRecording> createMultiFrequencyTrainingRecordings(int count, int durationMsec, List<Double> ampList, List<Integer> htz, boolean addAmpNoise, Properties labels, Properties tags) {
//		List<SoundRecording> srList = new ArrayList<SoundRecording>();
//		List<SoundClip> clipList = createMultiFrequencySounds(count, durationMsec,  ampList, htz, addAmpNoise); 
//		for (SoundClip clip : clipList) {
//			SoundRecording sr = new SoundRecording(clip, labels, tags);
//			srList.add(sr);
//		}
//		return srList;
//	}
//
//	public static  List<SoundClip> createMultiFrequencySounds(int count, int durationMsec, int htz[], boolean addAmpNoise) {
//		List<Integer> htzList = new ArrayList<Integer>();
//		for (int h : htz) 
//			htzList.add(Integer.valueOf(h));
//		return createMultiFrequencySounds(count, durationMsec, null, htzList, addAmpNoise);
//	}


	/**
	 * For each amplitude paired with corresponding hz, create count sound clips. 
	 * @param count
	 * @param durationMsec
	 * @param ampList may be null in which case 1 is used.
	 * @param htzList may be null in which case 1000 is used.  htzList and ampList can not BOTH be null.
	 * @param addAmpNoise
	 * @return
	 */
	public static  List<SoundClip> createMultiFrequencySounds(int count, int durationMsec, double offset, List<Double> ampList, List<Integer> htzList, boolean addAmpNoise) {
		if (ampList == null && htzList == null)
			throw new IllegalArgumentException("One of ampList and htzList must not be null");
		if (ampList != null && htzList != null && ampList.size() != htzList.size())
			throw new IllegalArgumentException("If both are provided, ampList and htzList must be same size ");
		int msecStart = 0;
		int msecGap = 0;
		int channels = 1;
		int bitsPerSample = 16;
		int samplingRate = 44100;
		int mixtureCount = htzList == null ? ampList.size() : htzList.size();
		SoundClip clip= null; 
//		OnlineStats stats = new OnlineStats();
//		IDistanceFunction<double[]> df = new L1DistanceFunction();

		List<SoundClip> mixtures[] = new List[mixtureCount];
		for (int i=0 ; i< mixtureCount; i++) {
			int htz = htzList == null ? 1000 : htzList.get(i); 
			double amp = ampList == null ? 1 : ampList.get(i); 
			List<SoundClip> clipList = SoundTestUtils.createClips(count, msecStart, msecGap, durationMsec, channels, samplingRate, 
					bitsPerSample, amp, offset, htz, addAmpNoise,false); 
			mixtures[i] = clipList;

//			stats.addSample(df.distance(clipList.get(0).getData()));
//			AISPLogger.logger.info("stats=" + stats);
//			stats.reset();
		}

//		stats.reset();
//		OnlineStats stats2 = new OnlineStats();

		List<SoundClip> clipList = new ArrayList<>();
		for (int i=0 ; i<count; i++) {
			List<double[]> clipDataList = new ArrayList();
			for (List<SoundClip> mixture : mixtures) {
				double[] data = mixture.get(i).getData();
//				stats2.addSample(df.distance(data));
				clipDataList.add(data);
			}
			double[] average = VectorUtils.average(clipDataList);
			clip = new SoundClip(msecStart, msecStart + durationMsec,average);
			clipList.add(clip);
//			stats.addSample(df.distance(average));
		}
//		AISPLogger.logger.info("average stats=" + stats);
//		AISPLogger.logger.info("average stats2=" + stats2);

		return clipList;
	}
	
}
