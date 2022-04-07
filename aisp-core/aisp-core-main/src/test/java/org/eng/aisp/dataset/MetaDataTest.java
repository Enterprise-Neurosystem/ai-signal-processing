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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eng.ENGTestUtils;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.aisp.classifier.TrainingSetInfo;
import org.eng.aisp.classifier.TrainingSetInfo.LabelInfo;
import org.eng.aisp.dataset.MetaData.Record;
import org.eng.aisp.segmented.SegmentedSoundRecording;
import org.eng.aisp.segmented.SegmentingSoundRecordingShuffleIterable;
import org.eng.util.FileUtils;
import org.eng.util.IShuffleIterable;
import org.junit.Assert;
import org.junit.Test;

public class MetaDataTest {

	@Test
	public void testSplitAndMerge() {
		int nRecords = 105;
		int nFolds = 10;
		String splitOnLabelName = "modulo";
//		nRecords = 13;
//		nFolds = 3;
//		nRecords = 8;
//		nFolds = 3;
		Set<String> labelValues = new HashSet<String>();	// Keeps track of all labels values we're splitting on
		// Create a bogus MetaData instance.
		MetaData md = new MetaData();
		for (int i=0 ; i<nRecords ; i++)  {
			String index = String.valueOf(i);
			Properties labels = new Properties();
			Properties tags = new Properties();
			String labelValue = String.valueOf(i % nFolds);
			labelValues.add(labelValue);
			labels.setProperty(splitOnLabelName, labelValue); 
			labels.setProperty("index", index);
			tags.setProperty(splitOnLabelName, labelValue); 
			tags.setProperty("index", index);
			Record r = new MetaData.Record(index, labels, tags);
			md.add(r);
//			md.add(index, labels, tags);
//			System.out.println("i=" + i + ", labels=" + labels);
		}	
		List<? extends ISoundReferenceSet > mdList = md.split(splitOnLabelName, nFolds);
		Assert.assertTrue(mdList.size() == nFolds);
		Map<String, Integer> refLabelValueCounts = null; 
		int totalRecords = 0;
		int lastSize = 0;
		for (int i=0 ;i<mdList.size(); i++) {
			ISoundReferenceSet sub = mdList.get(i);

			// Size can not be 0.
			int subSize = sub.size();
			Assert.assertTrue(subSize != 0);
			// Make sure the subs are within +/- # of folds  of each other's size.
			Assert.assertTrue(lastSize == 0 || Math.abs(lastSize - subSize) <= nFolds);

			// Make sure the labels and file name and sensor values where copied correctly.
			Set<String> splitLabelValues = new HashSet<String>();	// track all labels values in the sub 
			for (IReferencedSoundSpec r : sub) {
				String fileName = r.getReference();
//				System.out.println("sub=" + i + ", labels=" + p);
				int lastSep = fileName.lastIndexOf("/");
				String index = fileName; 
				if (lastSep > 0)
					index = fileName.substring(lastSep+1);
				Properties p = r.getLabels();
				Assert.assertTrue(p.getProperty("index").equals(index));
				splitLabelValues.add(p.getProperty(splitOnLabelName));
				p = r.getTags();
				Assert.assertTrue(p.getProperty("index").equals(index));
				splitLabelValues.add(p.getProperty(splitOnLabelName));
			}
			Assert.assertTrue(splitLabelValues.equals(labelValues));	// Sub should have same labels as the original

			// Make sure the subset contains some of each of the original labels.
			List<String> values = sub.getLabelValues(splitOnLabelName);
			Assert.assertTrue(values.size() == labelValues.size());
			if (refLabelValueCounts == null) {
				refLabelValueCounts = getLabelValueCounts(sub, splitOnLabelName);
			} else {
				// Make sure each fold has roughly the same number of records for each label value.
				Map<String,Integer> labelValueCounts = getLabelValueCounts(sub, splitOnLabelName);
				for (String value : refLabelValueCounts.keySet()) {
					int refCount = refLabelValueCounts.get(value);
					int count = labelValueCounts.get(value);
					Assert.assertTrue(Math.abs(refCount - count) <= 1);
				}
			}

			// Merge all the other's into a single instance.
			MetaData others = new MetaData();
		    for (int j=0 ;j<mdList.size(); j++) {
		    	if (j != i)
		    		others.merge((MetaData)mdList.get(j));
		    }
		    // Make sure the merge got all the records.
		    Assert.assertTrue(others.size() == nRecords - sub.size());
		    // Go through the sub and make sure none of its records are in the merged instance.
		    for (String f : sub.getReferences()) {
		    	Assert.assertTrue(others.getLabels(f) == null);
		    	Assert.assertTrue(others.getTags(f) == null);
		    }
			totalRecords += subSize; 
			lastSize = subSize;
	
		}
		Assert.assertTrue(totalRecords == md.size());
	}

	private Map<String, Integer> getLabelValueCounts(ISoundReferenceSet sub, String labelName) {
		Map<String,Integer> counts = new HashMap<String,Integer>();
		for (IReferencedSoundSpec r : sub) {
			String labelValue = r.getLabels().getProperty(labelName);
			Integer c = counts.get(labelValue);
			if (c == null) {
				counts.put(labelValue, 1);
			} else {
				counts.put(labelValue, c+1);
			}
		}
		return counts;
	}
	
	
	@Test
	public void testReadSounds2() throws IOException {
		File tmpdir;
		List<SoundRecording> srList1;
		List<SoundRecording> srList2;
		int expectedCount = 1;
		// Load a single file.
		srList1 = SoundTestUtils.iterable2List(LabeledSoundFiles.loadUnlabeledSounds(ENGTestUtils.GetTestData("sounds/chiller-1.wav")));
		Assert.assertTrue(srList1.size() == expectedCount);


		// Load a directory

		expectedCount = 3;
		srList1 = SoundTestUtils.iterable2List(LabeledSoundFiles.loadUnlabeledSounds(ENGTestUtils.GetTestData("sounds")));
		Assert.assertTrue(srList1.size() == expectedCount);
		
		boolean[] bools = new boolean[] { true, false };
		List<String> names = new ArrayList<String>();
		for (int i=0 ; i<3 ; i++)  {
			String name = "junit-name" + i;
			names.add(name);
		}
		List<String>[] namesArray = new List[] { names, null };
		boolean requireAll = true;
		for (List<String> soundNames : namesArray) {
			for (boolean forLinux : bools) {
					tmpdir = FileUtils.createTempDir();
					MetaData.writeMetaDataSounds(tmpdir.getAbsolutePath(), srList1, soundNames, forLinux);
					MetaData md = MetaData.read(tmpdir.getAbsolutePath());
					Assert.assertTrue(md.size() == expectedCount);
					srList2 = SoundTestUtils.iterable2List(md.readSounds());
					Assert.assertTrue(md.size() == expectedCount);
					srList2 = SoundTestUtils.iterable2List(LabeledSoundFiles.loadMetaDataSounds(tmpdir.getAbsolutePath(), requireAll));
					Assert.assertTrue(srList2.size() == srList1.size());
					srList2 = SoundTestUtils.iterable2List(LabeledSoundFiles.loadUnlabeledSounds(tmpdir.getAbsolutePath()));
					Assert.assertTrue(srList2.size() == srList1.size());
					srList2 = SoundTestUtils.iterable2List(LabeledSoundFiles.loadSounds(tmpdir.getAbsolutePath(), true));
					Assert.assertTrue(srList2.size() == srList1.size());
					srList2 = SoundTestUtils.iterable2List(LabeledSoundFiles.loadSounds(tmpdir.getAbsolutePath(), false));
					Assert.assertTrue(srList2.size() == srList1.size());
					FileUtils.deleteFile(tmpdir);
					// TODO: add more validation
			}
		
		}

	}
	
	@Test
	public void testWriteRead() throws IOException {
		int nRecords = 11;
		String testMDFile = "junit-metadata.csv";
		// Create a MetaData instance.
		MetaData md = new MetaData();
		for (int i=0 ; i<nRecords ; i++)  {
			String index = String.valueOf(i);
			Properties labels = new Properties();
			Properties tags = new Properties();
			labels.setProperty("label"+index, "lval" + index); 
			tags.setProperty("tag"+index, "tval" + index); 
			Record r = new MetaData.Record(index, labels, tags);
			md.add(r);
//			md.add(index, labels, tags);
		}		
		md.write(testMDFile);
		MetaData readMD = MetaData.read(testMDFile);
		FileUtils.deleteFile(testMDFile);
		Assert.assertTrue(readMD.size() == nRecords);
		int i = 0;
		for (IReferencedSoundSpec r : readMD) {
			String index = String.valueOf(i);
			Assert.assertTrue(r.getReference().endsWith(index));
			Properties labels = r.getLabels(); 
			Properties tags = r.getTags(); 
			Assert.assertTrue(labels.size() == 1);
			Assert.assertTrue(tags.size() == 1);
			Assert.assertTrue(("lval" + index).equals(labels.get("label" + index)));
			Assert.assertTrue(("tval" + index).equals(tags.get("tag" + index)));
			Record r2 = new MetaData.Record(index, labels, tags);
			md.add(r2);
//			md.add(index, labels, tags);
			i++;
		}
	}

	@Test
	public void testFileCount() throws IOException {
		
		// Load some sounds
		MetaData md = MetaData.read(ENGTestUtils.GetTestData("segmented")); 
		Assert.assertTrue(md != null);
		List<String> refs = SoundTestUtils.iterable2List(md.getReferences());
		Assert.assertTrue(refs.size() == 5);	// this metadata.csv has 5 records 
		List<String> files = SoundTestUtils.iterable2List(md.getFiles());
		Assert.assertTrue(files.size() == 3);	// this metadata.csv only references 3 different files
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void testReadSounds() throws IOException {
		ISoundDataSet sounds;
		// Load some sounds
		MetaData  md = MetaData.read(ENGTestUtils.GetTestData("chiller")); 

		String labelName = "source";
		boolean bools[] = new boolean[] {true, false};
		
		for (boolean requireAllFiles: bools)  { 
			sounds = md.readSounds(requireAllFiles);
			List<SoundRecording> srList = SoundTestUtils.iterable2List(sounds);
			Assert.assertTrue(srList.size() == 10);
			validateSound(srList, 0, "chiller-1.wav", -1, labelName, "HVAC1" );
			validateSound(srList, 1, "chiller-2.wav", -1, labelName, "HVAC1" );
			validateSound(srList, 2, "chiller-3.wav", -1, labelName, "HVAC2" );
		}
		
		
	}
	
	/**
	 * @throws IOException
	 */
	@Test
	public void testSegmentRead() throws IOException {
		
		// Load some sounds
		String labelName = "segment";
		MetaData md = MetaData.read(ENGTestUtils.GetTestData("segmented"));
		IShuffleIterable<SoundRecording>  sounds = LabeledSoundFiles.loadMetaDataSounds(md.getLocation(), true);
		
		/** Validate the following:
chiller-1.wav[0-1000],segment=0,
chiller-1.wav[1000-3000],segment=1,
chiller-1.wav[0-3000],segment=2,
../chiller/chiller-2.wav[2000-3000],segment=3,
../chiller/chiller-3.wav,segment=4,,
		 */

		List<SoundRecording> srList = SoundTestUtils.iterable2List(sounds);
		Assert.assertTrue(srList.size() == 5);
		validateSound(srList, 0, "chiller-1.wav", 1000, labelName, "0" );
		validateSound(srList, 1, "chiller-1.wav", 2000, labelName, "1" );
		validateSound(srList, 2, "chiller-1.wav", 3000, labelName, "2" );
		validateSound(srList, 3, "chiller-2.wav", 1000, labelName, "3" );
		validateSound(srList, 4, "chiller-3.wav", 4341, labelName, "4" );



	}
	/**
	 * @throws IOException
	 */
	@Test
	public void testManualSegmentWriteRead() throws IOException {
		
		// Load some sounds
		String labelName = "segment";
		int expectedSoundCount = 3;
		IShuffleIterable<SoundRecording>  sounds = LabeledSoundFiles.loadUnlabeledSounds(ENGTestUtils.GetTestData("sounds"));
		List<SoundRecording> srList = SoundTestUtils.iterable2List(sounds);
		Assert.assertTrue(srList.size() == expectedSoundCount);	// The rest expects this.

		// Create a temp directory
		File tempdirFile = FileUtils.createTempDir();
		String tempdir = tempdirFile.getAbsolutePath();

		// Write files only (metadata written, but ignore) to new directory with index naming.
		List<String> soundNames = MetaData.writeMetaDataSounds(tempdir, sounds, null, true);
		Assert.assertTrue(soundNames.size() == expectedSoundCount);
		
		// Build and save the metadata that represents some segmentation of the sounds we just wrote.
		MetaData wroteMD = new MetaData(tempdir);
		wroteMD.add(newSpec(soundNames, 0, 500, 1000));
		wroteMD.add(newSpec(soundNames, 1, 0, 1000));
		wroteMD.add(newSpec(soundNames, 2, 1000,2500));
		wroteMD.write(tempdir);
		
		MetaData readMD = MetaData.read(tempdir);
		sounds = readMD.readSounds(); 
		srList = SoundTestUtils.iterable2List(sounds);

		validateSound(srList, 0, soundNames.get(0), 500, null, null); 
		validateSound(srList, 1, soundNames.get(1), 1000, null, null); 
		validateSound(srList, 2, soundNames.get(2), 1500, null, null); 
		
		FileUtils.deleteFile(tempdir);


	}


	private IReferencedSoundSpec newSpec(List<String> soundNames, int nameIndex, int startMsec, int endMsec) {
		Properties labels = new Properties();
		Properties tags = new Properties();
		labels.setProperty("nameIndex", String.valueOf(nameIndex)); 
		tags.setProperty("nameIndexTag", String.valueOf(nameIndex)); 
		String reference = soundNames.get(nameIndex);
		return new ReferencedSoundSpec(reference, startMsec, endMsec, labels, tags);
	}

	private void validateSound(List<SoundRecording> srList, int index, String fileName, int durationMsec, String labelName, String expectedLabelValue) {
		SoundRecording sr = srList.get(index);

		// Check duration, if given
		if (durationMsec >= 0) {
			double deltaMsec = Math.abs(durationMsec - sr.getDataWindow().getDurationMsec());
			Assert.assertTrue(deltaMsec < 2);
		}

		// Check file name, if given.
		if (fileName != null) {
			Properties tags = sr.getTagsAsProperties();
			Assert.assertTrue(tags != null);
			String fileTag = tags.getProperty(MetaData.FILENAME_TAG);
			Assert.assertTrue(fileTag != null);
			Assert.assertTrue(fileTag.endsWith(fileName));
		}
		
		// Check label, if given
		if (labelName != null) {
			Properties labels = sr.getLabels();
			Assert.assertTrue(labels != null);
			String labelValue = labels.getProperty(labelName);
			Assert.assertTrue(labelValue != null);
			Assert.assertTrue(labelValue.equals(expectedLabelValue)); 
		}
		
	}
	
	@Test
	public void testSegmentWriteSegmentedReadSounds() throws IOException {
		int count = 5;
		int segDurationMsec = 1000;
		int htz[] = new int[] { 1000, 4000, 10000 };
		int subSegmentCount = htz.length;		// Number of segments defined within a recording.
		String trainingLabel = "htz"; 
		String globalLabel = "global-label";
		Properties globalLabels = new Properties();
		globalLabels.setProperty(trainingLabel, "to-be-ignored");
		globalLabels.setProperty(globalLabel, "global-label-value");
		String tagIndexName = "seg-index-tag";
				
		// Create the segmented sounds recordings in which each segment is at a different frequency and labeled as such
		List<SegmentedSoundRecording> sounds = SoundTestUtils.createSegmentedSound(count, segDurationMsec, htz, globalLabels, trainingLabel, tagIndexName); 

		// Create a temp directory
		File tempdirFile = FileUtils.createTempDir();
		String tempdir = tempdirFile.getAbsolutePath();
		
		try {
			// Write the segments
			List<String> names = MetaData.writeMetaDataSoundSegments(tempdir, sounds, null, true);

			// Read them back in
			MetaData md = MetaData.read(tempdir);

			Assert.assertTrue(names.size() == count);	// One name for each set of segments
			Assert.assertTrue(md.size() == count * subSegmentCount);	// One entry for each segment.

			// Read the sounds, although they will not be read until iterated, below.
			IShuffleIterable<SoundRecording> segmentedSounds = md.readSounds();

			// Make sure we have the expected number of extracted sub-segments and labeling. 
			TrainingSetInfo tsi = TrainingSetInfo.getInfo(segmentedSounds);
			Assert.assertTrue(tsi.getTotalSamples() == count * subSegmentCount);
			Assert.assertTrue(tsi.getTotalMsec() == count * subSegmentCount * segDurationMsec);
			LabelInfo linfo = tsi.getLabelInfo(trainingLabel);
			Assert.assertTrue(linfo.getTotalSamples() == count * subSegmentCount);
			Assert.assertTrue(linfo.getLabelInfo("1000").getTotalMsec() == count * segDurationMsec);
			Assert.assertTrue(linfo.getLabelInfo("4000").getTotalMsec() == count * segDurationMsec);
			Assert.assertTrue(linfo.getLabelInfo("10000").getTotalMsec() == count * segDurationMsec);
			// Make sure the global label got copied.
			linfo = tsi.getLabelInfo(globalLabel);
			Assert.assertTrue(linfo.getTotalSamples() == count * subSegmentCount);

			// Check the expected tags. Segment index is 0,1,...subSegmentCount,0,1,...
			int index = 0;
			for (SoundRecording sr : segmentedSounds) {
				String tagIndexValue = sr.getTag(tagIndexName);
				Assert.assertTrue(tagIndexValue != null);
				Assert.assertTrue(tagIndexValue.equals(String.valueOf(index)));
				index++;
				if (index == subSegmentCount)
					index = 0;	// Move to the next recording.
			}
		} finally { 
			// Delete before we start asserting.
			FileUtils.deleteFile(tempdir);
		}
		
	}
	
	@Test
	public void testSegmentWriteSegmentedReadSegmented() throws IOException {
		int count = 5;
		int segDurationMsec = 1000;
		int htz[] = new int[] { 1000, 4000, 10000 };
		int subSegmentCount = htz.length;		// Number of segments defined within a recording.
		String trainingLabel = "htz"; 
		String globalLabel = "global-label";
		Properties globalLabels = new Properties();
		globalLabels.setProperty(trainingLabel, "to-be-ignored");
		globalLabels.setProperty(globalLabel, "global-label-value");
		String tagIndexName = "seg-index-tag";
				
		// Create the segmented sounds recordings in which each segment is at a different frequency and labeled as such
		Iterable<SegmentedSoundRecording> ssrList = SoundTestUtils.createSegmentedSound(count, segDurationMsec, htz, globalLabels, trainingLabel, tagIndexName); 

		// Create a temp directory
		File tempdirFile = FileUtils.createTempDir();
		String tempdir = tempdirFile.getAbsolutePath();
		
		try {
			// Write the segments
			List<String> names = MetaData.writeMetaDataSoundSegments(tempdir, ssrList, null, true);
			Assert.assertTrue(names.size() == count);	// One name for each set of segments

			// Read them back in
			MetaData md = MetaData.read(tempdir);

			Assert.assertTrue(md.size() == count * subSegmentCount);	// One entry for each segment.

			// Read the sounds, although they will not be read until iterated
			IShuffleIterable<SegmentedSoundRecording> ssrIter = md.readSegmentedSounds(true);

			// Verify the number of SegmentedSoundRecordings, same as written
			Assert.assertTrue(SoundTestUtils.iterable2List(ssrIter).size() == count);

			// Make sure we have the expected number of extracted sub-segments and labeling. 
			Iterable<SoundRecording> sounds = new SegmentingSoundRecordingShuffleIterable(ssrIter);
			
			TrainingSetInfo tsi = TrainingSetInfo.getInfo(sounds);
			Assert.assertTrue(tsi.getTotalSamples() == count * subSegmentCount);
			Assert.assertTrue(tsi.getTotalMsec() == count * subSegmentCount * segDurationMsec);
			LabelInfo linfo = tsi.getLabelInfo(trainingLabel);
			Assert.assertTrue(linfo.getTotalSamples() == count * subSegmentCount);
			Assert.assertTrue(linfo.getLabelInfo("1000").getTotalMsec() == count * segDurationMsec);
			Assert.assertTrue(linfo.getLabelInfo("4000").getTotalMsec() == count * segDurationMsec);
			Assert.assertTrue(linfo.getLabelInfo("10000").getTotalMsec() == count * segDurationMsec);
			// Make sure the global label got copied.
			linfo = tsi.getLabelInfo(globalLabel);
			Assert.assertTrue(linfo.getTotalSamples() == count * subSegmentCount);

			// Check the expected tags. Segment index is 0,1,...subSegmentCount,0,1,...
			int index = 0;
			for (SoundRecording sr : sounds) {
				String tagIndexValue = sr.getTag(tagIndexName);
				Assert.assertTrue(tagIndexValue != null);
				Assert.assertTrue(tagIndexValue.equals(String.valueOf(index)));
				index++;
				if (index == subSegmentCount)
					index = 0;	// Move to the next recording.
			}
		} finally { 
			// Delete before we start asserting.
			FileUtils.deleteFile(tempdir);
		}
		
	}
}