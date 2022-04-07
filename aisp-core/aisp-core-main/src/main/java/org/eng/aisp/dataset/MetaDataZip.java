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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.segmented.LabeledSegmentSpec;
import org.eng.aisp.segmented.SegmentedSoundRecording;
import org.eng.aisp.util.FileZipper;
import org.eng.aisp.util.PCMUtil;
import org.eng.util.FileUtils;
import org.eng.util.IMutator;
import org.eng.util.IShuffleIterable;
import org.eng.util.MutatingShuffleIterable;
import org.eng.util.ShufflizingIterable;

/**
 * Provides a way to write one or more sounds with and metadata file into a ZIP stream.
 * Start and end times are preserved.
 * @author dawood
 *
 */
public class MetaDataZip extends FileZipper { // implements Closeable {
	


	protected MetaData metadata = new MetaData();
	public MetaDataZip(OutputStream os)  {
		super(os);
	}
	
	/**
	 * Enables iteration over an iterable of NamedSounds to produce SoundRecordings, 1 for 1.
	 */
	private static class NamedSound2SoundRecordingMutator  implements IMutator<NamedSound,SoundRecording> {

		@Override
		public List<SoundRecording> mutate(NamedSound item) {
			SegmentedSoundRecording ssr = item.getSegmentedSoundRecording();
			List<SoundRecording> l; 
			try {
				l = ssr.getSegmentedLabeledDataWindows();
			} catch (AISPException e) {
				l = null;	// TODO: should we raise an exception  or warning?
			}
			return l;
		}
		
	}
	
	private static NamedSound2SoundRecordingMutator namedSoundMutuator = new NamedSound2SoundRecordingMutator();
	
	/**
	 * 
	 * Enables iteration over an iterable of NamedSounds to produce SegmentedSoundRecordings, 1 for 1.
	 */
	private static class NamedSound2SegmentedSoundRecordingMutator  implements IMutator<NamedSound,SegmentedSoundRecording> {

		@Override
		public List<SegmentedSoundRecording> mutate(NamedSound item) {
			SegmentedSoundRecording ssr = item.getSegmentedSoundRecording();
			List<SegmentedSoundRecording> l = new ArrayList<>(); 
			l.add(ssr);
			return l;
		}
		
	}
	
	private static NamedSound2SegmentedSoundRecordingMutator namedSegmentedSoundMutuator = new NamedSound2SegmentedSoundRecordingMutator();

	/**
	 * A convenience on {@link #readNamedSounds(InputStream)}.
	 * Read a zip stream containing wav files and optionally a metadata.csv file.
	 * If no metadata.csv is provided, then 1) sounds will not have any labels and 
	 * 2) the start times make the sounds consecutive in time from the time the first sound is read.
	 * Restore start times in from the hidden label {@link #START_TIME_MSEC_LABEL} if present.
	 * @param zippedSounds stream of zipped bytes containing wav files and a metdata file.
	 * @return the recordings represented in the stream zipped bytes provided.
	 * @throws IOException
	 */
	public static IShuffleIterable<SoundRecording> readSoundRecordings(InputStream zippedSounds) throws IOException {
		IShuffleIterable<NamedSound> namedSounds = readNamedSounds(zippedSounds);
		return new MutatingShuffleIterable<NamedSound,SoundRecording>(namedSounds, namedSoundMutuator, false);
	}
	/**
	 * A convenience on {@link #readNamedSounds(InputStream)}.
	 * Read a zip stream containing wav files and optionally a metadata.csv file.
	 * If no metadata.csv is provided, then 1) sounds will not have any labels and 
	 * 2) the start times make the sounds consecutive in time from the time the first sound is read.
	 * Restore start times in from the hidden label {@link #START_TIME_MSEC_LABEL} if present.
	 * @param zippedSounds stream of zipped bytes containing wav files and a metdata file.
	 * @return the recordings represented in the stream zipped bytes provided.
	 * @throws IOException
	 */
	public static IShuffleIterable<SegmentedSoundRecording> readSegmentedSounds(InputStream zippedSounds) throws IOException {
		IShuffleIterable<NamedSound> namedSounds = readNamedSounds(zippedSounds);
		return new MutatingShuffleIterable<NamedSound,SegmentedSoundRecording>(namedSounds, namedSegmentedSoundMutuator, true);
	}

	/**
	 * Holds the name (typically a file name from a zip file) associated with the sound.
	 */
	public static class NamedSound {
		protected final String name;
		protected final SegmentedSoundRecording segmentedSoundRecording;

		public NamedSound(String name, SoundRecording soundRecording) {
			this(name, new SegmentedSoundRecording(soundRecording, null));
		}

		public NamedSound(String name, SegmentedSoundRecording soundRecording) {
			this.name = name;
			this.segmentedSoundRecording = soundRecording;
		}
		public String getName() {
			return name;
		}
		public SegmentedSoundRecording getSegmentedSoundRecording() {
			return segmentedSoundRecording;
		}
	}
	
	private static abstract class TempDirDeletingMutator  { 

		private File tmpDir;

		public TempDirDeletingMutator(File tmpDir) {
			this.tmpDir = tmpDir;
		}
		
		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			try {
				FileUtils.deleteDirContents(tmpDir);
				FileUtils.deleteFile(tmpDir);
			} catch (Exception e) {
				;
			}
		}
		
	}
	
	/**
	 * Turns an absolute sound file name into a NamedSound by reading it in from disk and 
	 * setting no labels or tags.  All start times are 0.
	 * @author DavidWood
	 *
	 */
	private static class SoundFiles2NamedSoundMutator extends TempDirDeletingMutator implements IMutator<String, NamedSound> {

		public SoundFiles2NamedSoundMutator(File tmpDir) {
			super(tmpDir);
		}

		@Override
		public List<NamedSound> mutate(String item) {
			List<NamedSound> nsList = new ArrayList<NamedSound>();
			SoundRecording sr;
			try {
				SoundClip clip = SoundClip.readClip(item);
				Properties tags = new Properties();
				tags.setProperty(MetaData.FILENAME_TAG,item);
				sr = new SoundRecording(clip, null, tags); 
				item = new File(item).getName();	// remove the absolute path.
			} catch (IOException e) {
				return nsList;
			}
			nsList.add(new NamedSound(item, sr));
			return nsList;
		}
	
	}

	/**
	 * Wraps a SegmentedSoundRecording with a NamedSound.
	 * @author DavidWood
	 *
	 */
	private static class SegmentedSounds2NamedSoundMutator extends TempDirDeletingMutator implements IMutator<SegmentedSoundRecording, NamedSound> {

		private boolean fromTempZip;

		public SegmentedSounds2NamedSoundMutator(File tmpDir, boolean fromTempZip) {
			super(tmpDir);
			this.fromTempZip = fromTempZip;
		}

		@Override
		public List<NamedSound> mutate(SegmentedSoundRecording item) {
			List<NamedSound> nsList = new ArrayList<NamedSound>();
			// The files are being read perhaps
			String file = item.getTag(MetaData.FILENAME_TAG);
			if (this.fromTempZip)	 {
				file = new File(file).getName();	// remove the absolute path.
//				item.removeTag(MetaData.FILENAME_TAG);
				item.addTag(MetaData.FILENAME_TAG, file);
			} else {
				file = new File(file).getAbsolutePath();
			}

			nsList.add(new NamedSound(file, item));
			return nsList;
		}
		
	}
	/**
	 * Turns an sound file name into a NamedSound by reading it using the contained MetaData instance. 
	 * @author DavidWood
	 *
	 */
	private static class MetaDataSounds2NamedSoundMutator extends TempDirDeletingMutator implements IMutator<String, NamedSound> {

		final MetaData metaData;
		double lastEndTimeMsec = -1;

		public MetaDataSounds2NamedSoundMutator(File tmpDir, MetaData metaData) {
			super(tmpDir);
			this.metaData = metaData;
		}

		@Override
		public List<NamedSound> mutate(String reference) {
			List<NamedSound> nsList = new ArrayList<NamedSound>();
			SoundRecording sr;
			String filename ;
			try {
				sr = metaData.readSound(reference);
//				sr.removeTag(MetaData.FILENAME_TAG);
				sr = adjustStartTime(sr);
				IReferencedSoundSpec spec = metaData.loadReference(reference);
				filename = spec.getReference();	// The actual file name
			} catch (IOException e) {
				return nsList;
			}
			nsList.add(new NamedSound(filename, sr));
			return nsList;
		}

		/**
		 * if the start time of the sound was not set in the metadata file, then make the sounds
		 * sequential and adjacent in time. 
		 * @param sr
		 * @return a new sr if the start time was adjusted.
		 */
		private SoundRecording adjustStartTime(SoundRecording sr) {
			double startTimeMsec = sr.getDataWindow().getStartTimeMsec();
			SoundRecording newSR;
			if (startTimeMsec == 0) {
				if (lastEndTimeMsec < 0) 	// First time, set the first sound to start now.
					startTimeMsec = System.currentTimeMillis();
				else						// Make subsequent sounds start right after the previous.
					startTimeMsec = lastEndTimeMsec; 
				newSR = new SoundRecording(sr,startTimeMsec);	// Shallow copy to reset start time.
				lastEndTimeMsec = startTimeMsec + sr.getDataWindow().getDurationMsec(); 
			} else {
				newSR = sr;
				lastEndTimeMsec = sr.getDataWindow().getEndTimeMsec();
			}
			return newSR;
		}
		

		
	}
	
//	private static class TempDirDeletingMutatingShuffleIterable<INPUT,OUTPUT> extends MutatingShuffleIterable<INPUT,OUTPUT> {
//
//		private File tmpDir;
//
//		public TempDirDeletingMutatingShuffleIterable(File tmpDir, IShuffleIterable<INPUT> soundFiles, IMutator<INPUT,OUTPUT> mutator) {
//			super(soundFiles, mutator, true);
//			this.tmpDir = tmpDir;
//		}
//	}
	
	/**
	 * Read a zip stream containing wav files and optionally a metadata.csv file.
	 * If no metadata.csv is provided, then 1) sounds will not have any labels and 
	 * 2) the start times will all be the time the first sound file is read. 
	 * If there is a metadata.csv and the tags or labels contain the hidden label {@link #START_TIME_MSEC_LABEL},
	 * then the start times will be set from it.  This is added during {@link MetaDataZip#write(OutputStream, Iterable, String)}.
	 * @param is stream of zipped bytes containing wav files and a metdata file.
	 * @return  a list of the recordings represented in the stream zipped bytes together with their names in the order they appear in the zip stream.
	 * @throws IOException if zip does not contain metadata.csv.
	 */
	public static IShuffleIterable<NamedSound> readNamedSounds(InputStream zippedSounds) throws IOException {
		// Create a temporary directory into which we will unzip the stream.
		File tmpDir = FileUtils.createTempDir();
		tmpDir.deleteOnExit();
		
		// Unzip the stream into our temp directory.
		List<String> fileNames = FileZipper.unZip(zippedSounds, tmpDir.getAbsolutePath());
		if (fileNames.size() == 0)
			throw new IOException("Zip stream did not contain any files");

		String metaDataName = tmpDir.getAbsolutePath() + "/" + MetaData.DEFAULT_METADATA_FILE_NAME;
		boolean hasMetaDataFile = new File(metaDataName).exists();
		MetaData metaData;
		IShuffleIterable<SegmentedSoundRecording> ssr; 
		if (hasMetaDataFile) {
			metaData = MetaData.read(metaDataName);
			ssr = metaData.readSegmentedSounds(false);
		} else {
			metaData = null;
			ssr = null;
		}


		// Try and make sure these get removed on exit.
		List<String> soundFiles = new ArrayList<String>(); 
		for (String fileName : fileNames) {
			File f = new File(tmpDir.getAbsoluteFile() + "/" + fileName);
			f.deleteOnExit();
			if (PCMUtil.isFileFormatSupported(fileName)) {
				soundFiles.add(f.getAbsolutePath());
			}
		}
		
 
		
		if (metaData != null) {
			// Read the metadata.csv file
//			return new MutatingShuffleIterable<String,NamedSound> (shuffledSounds, new MetaDataSounds2NamedSoundMutator(tmpDir, metaData), true);
			boolean fromTempZip = true;	// Because read from a zip file
			return new MutatingShuffleIterable<SegmentedSoundRecording,NamedSound> (ssr, new SegmentedSounds2NamedSoundMutator(tmpDir, fromTempZip), true);
		} else {
			// Make the list of names shufflable
			IShuffleIterable<String> shuffledSounds;
//			if (metaData == null)
				shuffledSounds = new ShufflizingIterable<String>(soundFiles); 
//			else
//				shuffledSounds = new ShufflizingIterable<String>(metaData.getReferences());
			return new MutatingShuffleIterable<String,NamedSound> (shuffledSounds, new SoundFiles2NamedSoundMutator(tmpDir), true);
		}
			
		
	}
	


	/**
	 * Write the zip bytes representing the given recordings to the given stream  using {@link #add(String, SoundRecording)}.
	 * Preserve start times in a hidden label {@link #START_TIME_MSEC_LABEL}.
	 * @param os stream to write zip bytes.  closed during {@link #finalizeZip()} which must be called to complete/flush the zip stream.
	 * @param sounds sounds to write to stream
	 * @param baseName base namem to which the 0-based index of the sound will be appended.  May be null.
	 * @throws IOException
	 */
	public static void write(OutputStream os, Iterable<SoundRecording> sounds, String baseName) throws IOException {
		int count = 0;
		if (baseName == null)
			baseName = "";
		MetaDataZip mdz = new MetaDataZip(os);
		for (SoundRecording sr : sounds) {
			String name = baseName + count;
			mdz.add(name,  sr);
			count++;
		}
		mdz.finalizeZip();
		// Let the caller close the stream.
	}

	/**
	 * Write the zip bytes representing the given recordings to the given stream  using {@link #add(String, SoundRecording)}.
	 * Preserve start times in a hidden label {@link #START_TIME_MSEC_LABEL}.
	 * @param os stream to write zip bytes.  closed during {@link #finalizeZip()} which must be called to complete/flush the zip stream.
	 * @param sounds sounds to write to stream
	 * @param baseName base namem to which the 0-based index of the sound will be appended.  May be null.
	 * @throws IOException
	 */
	public static void writeSegmentedSounds(OutputStream os, Iterable<SegmentedSoundRecording> sounds, String baseName) throws IOException {
		int count = 0;
		if (baseName == null)
			baseName = "";
		MetaDataZip mdz = new MetaDataZip(os);
		for (SegmentedSoundRecording sr : sounds) {
			String name = baseName + count;
			mdz.add(name,  sr);
			count++;
		}
		mdz.finalizeZip();
		// Let the caller close the stream.
	}
	
	/**
	 * Append the given recording and assign it the given name in the zip stream.
	 * The recording and its labels are appended to the instance's MetaData object
	 * and the start time of the clip is included as a hidden label with the name
	 * {@link #START_TIME_MSEC_LABEL}.
	 * @param fileName a unique name as it should appear in the zip file.  
	 * If not unique, then an exception is thrown.
	 * If this includes a path, the path will be stripped. If it does not have the ".wav" extension, it
	 * will be added.
	 * @param sr
	 * @throws IOException
	 */
	public synchronized void add(String fileName, SoundRecording sr) throws IOException {


		// Convert the name to a base name as it will appear in the zip.
		File f = new File(fileName);
		fileName = f.getName();
		if (!fileName.endsWith(".wav"))
			fileName += ".wav";
		
		// Make sure the file is not already used in this stream.
		if (metadata.getLabels(fileName) != null)
			throw new IOException("File with name " + fileName + " already added.");
		
		// Add the entry to the metadata (mapping labels to sounds).
		metadata.add(fileName, sr); 

		// Add a new zip entry for the PCM/wav file 
		byte[] wav = PCMUtil.PCMtoWAV(sr.getDataWindow());
		addFile(fileName, wav);
	}
	
	/**
	 * Append the given segmented recording and its segments and assign it the given name in the zip stream.
	 * The recording and its labels are appended to the instance's MetaData object
	 * and the start time of the clip is included as a hidden label with the name
	 * {@link #START_TIME_MSEC_LABEL}.
	 * @param fileName a unique name as it should appear in the zip file.  
	 * If not unique, then an exception is thrown.
	 * If this includes a path, the path will be stripped. If it does not have the ".wav" extension, it
	 * will be added.
	 * @param sr
	 * @throws IOException
	 */
	public synchronized void add(String fileName, SegmentedSoundRecording ssr) throws IOException {


		// Convert the name to a base name as it will appear in the zip.
		File f = new File(fileName);
		fileName = f.getName();
		if (!fileName.endsWith(".wav"))
			fileName += ".wav";
		
		// Add the entry to the metadata (mapping labels to sounds).
		SoundRecording sr = ssr.getEntireLabeledDataWindow();
		List<LabeledSegmentSpec> segmentList = ssr.getSegmentSpecs();
		if (segmentList.isEmpty()) {
			this.add(fileName,  sr);
		} else {
			for (LabeledSegmentSpec segment : segmentList) 
				metadata.add(fileName, sr, segment); 
		}

		// Add a new zip entry for the PCM/wav file 
		byte[] wav = PCMUtil.PCMtoWAV(sr.getDataWindow());
		addFile(fileName, wav);
	}

	/**
	 * Override the super class to add the metadata file and then call to the super class. 
	 */
	public synchronized void finalizeZip() throws IOException {
		if (zipStream == null)
			return;
		
		// Create a new zip entry for the finalized metadata.
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		metadata.write(bos, true, false); 
		byte bytes[] = bos.toByteArray();

//		ZipEntry e = new ZipEntry(MetaData.DEFAULT_METADATA_FILE_NAME);
//		e.setSize(bytes.length);
//		zipStream.putNextEntry(e);
//		zipStream.write(bytes);
//		zipStream.closeEntry();
		addFile(MetaData.DEFAULT_METADATA_FILE_NAME, bytes);
		metadata = null;
		
		super.finalizeZip();
	}

	/**
	 * A convenience method on {@link #readSoundRecordings(InputStream)}. 
	 * @param zippedSounds byte array representing the zip of wav sounds and metadata.csv.
	 * @return List of SoundRecordings 
	 * @throws IOException
	 */
	public static IShuffleIterable<SoundRecording> readSoundRecordings(byte[] zippedSounds) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(zippedSounds);
		return readSoundRecordings(bis);
	}
	
	/**
	 * A convenience on {@link #readNamedSounds(InputStream)}.
	 * @param zippedSounds
	 * @return map of sounds keyed by the name in the zip. 
	 * @throws IOException
	 */
	public static IShuffleIterable<NamedSound> readNamedSounds(byte[] zippedSounds) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(zippedSounds);
		return readNamedSounds(bis);
	}

	/**
	 * A convenience method on {@link #write(OutputStream, Iterable, String)}.
	 * @param sounds
	 * @param baseName base name to which a 0-based index will be added when created the file names.  May be null.
	 * @return byte array representing a zip of the wav files and labels in metadata.csv.
	 * @throws IOException
	 */
	public static byte[] writeBytes(Iterable<SoundRecording> sounds, String baseName) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		write(bos, sounds, baseName);
		return bos.toByteArray();
	}
	
	public static void main(String[] args) throws IOException {
		Iterable<SoundRecording> sounds = SoundRecording.readMetaDataSounds("test-data/chiller");
		FileOutputStream fos = new FileOutputStream("metadatazip.zip");
		MetaDataZip.write(fos, sounds, null);
	}


}
