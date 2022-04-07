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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.segmented.LabeledSegmentSpec;
import org.eng.aisp.segmented.SegmentedSoundRecording;
import org.eng.aisp.util.PCMUtil;
import org.eng.cache.IMultiKeyCache;
import org.eng.cache.MemoryCache;
import org.eng.util.CSVTable;
import org.eng.util.DelegatingShuffleIterable;
import org.eng.util.IDereferencer;
import org.eng.util.IShuffleIterable;
import org.eng.util.StringUtil;

/**
 * Represents a CSV file of labels and tags to be applied to named sound files.
 * Extends the super class to support reading/writing of the CSV file and to read the sound files.
 * This class uses the file names with optional segment specification as the references to contained IReferencedSoundSpec objects.
 * <h3>Format</h3>
 * The metadata file is a CSV (comma separate value) file, named metadata.csv by default.  It has the following format:
 * <ul>
 * <li> 3 columns with optional header and columns as described above. 
 *     <ul>
 *     <li> Column 1 is the filename and optional segment specification 
 *     <li> Column 2 is the semi-colon separated list of labels 
 *     <li> Column 3 is the semi-colon separated list of tags
 *     </ul>
 * </ul>
 * An example might be the following:
 * <pre>
 * sound1.wav,s1,source=engine;mic=built-in, 
 * sound2.wav,s1,source=engine;mic=built-in,tag1=value1
 * ...
 * sound100.mp3,s2,source=cabin;mic=shotgun,tag2=value2;tag1=valueZ
 * ...
 * </pre>
 * Segments are now specified with an optional set of square brackets specifying the start and end offsets of the segment in milliseconds.
 * For example,
 * <pre>
 * sound1.wav[1000-2000],s1,source=engine;mic=built-in, 
 * ...
 * </pre> 
 * 
 * @author dawood
 */
//public class MetaData extends AbstractSoundReferenceSet<Record, MetaData> implements ISoundReferenceSet<Record> {
public class MetaData extends MemorySoundReferenceDataSet implements ISoundReferenceSet {

	/** Add when reading a file ot the tags for the resulting SoundRecording */
	public final static String FILENAME_TAG = "file";

	/** Label used to capture the start time of a SoundClip when be stored with an entry in a metadata file */
	public final static String DEFAULT_METADATA_FILE_NAME = "metadata.csv";
	public final static String FILE_COLUMN_NAME = "File";
	public final static String COLUMN_SEPARATOR = CSVTable.COMMA_STR; 
	public final static String OLD_FILE_COLUMN_INDEX = String.valueOf(1); 
	public final static String NEW_FILE_COLUMN_INDEX = String.valueOf(1); 	// Simple 1-based column index.
	public final static String SENSOR_COLUMN_NAME = "Sensor";
	public final static String LABELS_COLUMN_NAME = "Labels";
	public final static String OLD_LABELS_COLUMN_INDEX = String.valueOf(3); 
	public final static String NEW_LABELS_COLUMN_INDEX = String.valueOf(2); 
	public final static String TAGS_COLUMN_NAME = "Labels";
	public final static String TAGS_COLUMN_INDEX = String.valueOf(3); 
	public final static String LABEL_SEPARATOR= ";";
	/** Separator between label name and label value as in label=value */
	public final static String NAME_VALUE_SEPARATOR= "=";

	/** 
	 * The absolute path to the file this instance is considered to be located.  Set during {@link #read(String)} or a constructor.
	 * Never null.
	 */ 
	private final File metaDataFile;
	
	private final String absoluteParent;

	private final static String INDEX_FILE_NAME_FORMAT = "%07d";


	
	/**
	 * A simple extension of SoundReference that currently does not allow segmenting of the reference clip.
	 * @author DavidWood
	 */
	public static class Record extends ReferencedSoundSpec implements IReferencedSoundSpec {
		
		protected Record(IReferencedSoundSpec record, String fileName) {
			super(record, fileName); 
		}

		public Record(String fileName, Properties labels, Properties tags) {
			super(fileName, labels, tags);
		}

		public Record(String fileName, int startMsec, int endMsec, Properties labels, Properties tags) {
			super(fileName, startMsec, endMsec, labels, tags);
		}
	}


	/**
	 * Create an empty instance as if it was read from the JVM's current directory.
	 */
	public MetaData() {
		this((String)null);	
	}

	/**
	 * Create an empty instance that will be written in the given directory or file.
	 * @param filePath  the location of this instance.  May be absolute or relative or null.
	 */
	public MetaData(String metaDataFileOrDir) {
		super(); // This requires us to implement getReferences() to provide the references after construction.
		if (metaDataFileOrDir == null)
			metaDataFileOrDir = DEFAULT_METADATA_FILE_NAME;
		File t = new File(metaDataFileOrDir);
		if (t.isDirectory()) 
			metaDataFile = new File(t.getAbsolutePath() + "/" + DEFAULT_METADATA_FILE_NAME);
		else
			metaDataFile = t;

		this.absoluteParent = this.metaDataFile.getAbsoluteFile().getParent();
	}
	
	/**
	 * Create a data set from given list of files, all w/o any labels or tags.
	 * @param soundFiles list of files relative to the current directory.
	 */
	public MetaData(Iterable<String> soundFiles) {
		this(".");
		for (String ref : soundFiles)  {
			Record r = new Record(ref, null, null);
			this.add(r);
		}
	}
	


	/**
	 * Provided to support {@link #newIterable(Iterable)}, but otherwise not exposed.
	 * @param metaDataFile
	 * @param references
	 * @param labels
	 * @param tags
	 */
	private MetaData(MetaData metaData, Iterable<String> references) {
		super(references, metaData);
//		for (String ref : references)  {
//			Record soundRef = this.dereference(ref);
//			if (soundRef != null) {
//				this.orderedReferences.add(ref);
//				this.metaData.put(ref,soundRef);
//			}
//		}
		this.metaDataFile = metaData.metaDataFile;
		this.absoluteParent = this.metaDataFile.getAbsoluteFile().getParent();
    }


	/**
	 * Build a spec from the given file name and sound including augmentation tags.
	 * @param fileName
	 * @param sr
	 * @return a spec in which the contained reference is ONLY the given filename.
	 */
	protected static IReferencedSoundSpec newReferencedSoundSpec(String fileName, SoundRecording sr) {
		return newReferencedSoundSpec(fileName, sr, null);
	}
	
	/**
	 * Create the given 
	 * @param fileName
	 * @param sr
	 * @param segment
	 * @return
	 */
	protected static IReferencedSoundSpec newReferencedSoundSpec(String fileName, SoundRecording sr, LabeledSegmentSpec segment) { 
		Properties labels = new Properties();
		labels.putAll(sr.getLabels());
		Properties tags = getAugmentedTags(sr);	// These are global but will be attached to each segment.
		int startMsec, endMsec; 
		if (segment != null) {
			labels.putAll(segment.getLabels());
			tags.putAll(segment.getTags());
			startMsec = segment.getStartMsec();
			endMsec = segment.getEndMsec();
//			// Remove tags that might have been on the base SoundRecording, but don't need to be on the segment.
//			DataTypeEnum.removeType(tags);	// Don't copy type to segment as it is already on the 
//			tags.remove(FILENAME_TAG);
		} else {
			startMsec = endMsec = 0; 
		}	
	
		IReferencedSoundSpec ref = new ReferencedSoundSpec(fileName, startMsec, endMsec, labels, tags); 
		return ref;
	}
	
	public String add(String fileName, SoundRecording sr) {
		return add(fileName, sr, null);
	}
	/**
	 * Create an entry in this instance and include startMsec data from the given recording in the labels.
	 * @param fileName the location of the file relative to this instance's location. 
	 * @param sr
	 * @param segment optional param to specify a segment within the given recording.
	 */
	public String add(String fileName, SoundRecording sr, LabeledSegmentSpec segment) { 
		IReferencedSoundSpec ref = newReferencedSoundSpec(fileName, sr, segment); 
		return this.add(ref);
	}


	/**
	 * Get the sound file referenced in the metadata file, in order as they appeared in the metadata file. 
	 * Note that because some records may refer to the same file due to segmentation, the number of files
	 * may not be the same as the number of records in this instance.
	 * @return list of file names relative to this instance.  Use {@link #getReferenceableFile(String)} to turn
	 * the returned value into a path relative to the relative location of this instance.
	 */
	public Iterable<String> getFiles() {
		Set<String> files = new HashSet<String>();
		for (IReferencedSoundSpec spec : this) 
			files.add(spec.getReference());	// The spec contains the pure file name (no segment info).
		return files;
	}
	
	/**
	 * Get the path to a file listed in the metadata file using either file or a reference..
	 * @param fileOrReference file or reference.  File values typically come from {@link #getFiles()} and reference values
	 * from {@link #getReferences()}.
	 * @return if not an absolute path, then the file is assumed to be relative to this instance and the instance's location
	 * is prepended. 
	 */
	public String getReferenceableFile(String fileOrReference) {
		String file;
		try {
			file = parseFileNameFromReference(fileOrReference);
		} catch (ParseException e) {
			// Ignore and let the user handle it when they try to use the return value
			file = fileOrReference;
		}
		if (file.startsWith("/") || file.startsWith("\\") || new File(file).isAbsolute())
			return file;
		return this.absoluteParent + "/" + file;
	}



	/**
	 * Get the labels for the reference in this instance.
	 * @param reference
	 * @return null if not found. 
	 */

	/**
	 * Read multiple metadata files into a single instance using {@link #merge(MetaData)}.
	 * @param metaDataCSV
	 * @return
	 * @throws IOException
	 */
	public static MetaData read(String[] metaDataCSV) throws IOException {
		MetaData r =  new MetaData();
		for (String file : metaDataCSV) {
			MetaData md = read(file);
			r.merge(md);
		}
		return r;
	}

	/**
	 * Rad the metadata from the given file or directory.
	 * @param metaDataCSV if a directory then the directly is expected to contain a file named {@value #DEFAULT_METADATA_FILE_NAME}
	 * containing the metadata.
	 * @return
	 * @throws IOException
	 */
	public static MetaData read(String metaDataCSV) throws IOException {
		File f = getMetaDataFile(metaDataCSV, true);
		
		FileReader reader = new FileReader(f.getAbsolutePath());
		String metaDataLoc = f.getPath();
		MetaData md = new MetaData(metaDataLoc);
		read(reader, md);
		reader.close();
		return md; 
	}

	
	/**
	 * Read a csv file in one of the following formats. 
	 * <ul>
	 * <li> 3 columns with the Header 'File,Sensor,Labels' (with no tags and we ignore the sensor column).
	 *     <ul>
	 *     <li> Column 1 is the filename and optional segment specification 
	 *     <li> Column 2 is the sensor name which is deprecated and ignore. 
	 *     <li> Column 3 is the semi-colon separated list of labels
	 *     </ul>
	 * <li> 3 columns with optional header and columns as described above. 
	 *     <ul>
	 *     <li> Column 1 is the filename and optional segment specification 
	 *     <li> Column 2 is the semi-colon separated list of labels 
	 *     <li> Column 3 is the semi-colon separated list of tags
	 *     </ul>
	 * </ul>
	 * An example of the older format might be the following:
	 * <pre>
	 * File,Sensor,Labels
	 * sound1.wav,s1,source=engine;mic=built-in
	 * sound2.wav,s1,source=engine;mic=built-in
	 * ...
	 * sound100.mp3,s2,source=cabin;mic=shotgun
	 * ...
	 * </pre>
	 * An example of the newer format might be the following:
	 * <pre>
	 * sound1.wav,s1,source=engine;mic=built-in, 
	 * sound2.wav,s1,source=engine;mic=built-in,tag1=value1
	 * ...
	 * sound100.mp3,s2,source=cabin;mic=shotgun,tag2=value2;tag1=valueZ
	 * ...
	 * </pre>
	 * Segments are now specified with an optional set of square brackets specifying the start and end offsets of the segment in milliseconds.
	 * For example,
	 * <pre>
	 * sound1.wav[1000-2000],s1,source=engine;mic=built-in, 
	 * ...
	 * </pre> 
	 * 
	 * @param reader
	 * @param md the MetaData object into which records are placed.
	 * @return
	 * @throws IOException
	 */
	private static MetaData read(Reader reader, MetaData md) throws IOException {
		CSVTable table = readCanonicalCSV(reader);

		for (CaseInsensitiveMap row : table) {
			String reference = (String)row.get(NEW_FILE_COLUMN_INDEX);
			if (reference != null) {
				if (md.containsReference(reference))
					throw new IOException("Metadata file contains duplicate reference which is not allowed. " + reference);
				IReferencedSoundSpec spec;
				try {
					spec = parseReference(reference);
				} catch (ParseException e) {
					throw new IOException("Could not parse reference: " + reference);
				} 
				String file = spec.getReference();
				Properties labels = null, tags = null;
				String cell = (String)row.get(NEW_LABELS_COLUMN_INDEX);
				if (cell != null)
					labels = parseNameValuePairs(cell, LABEL_SEPARATOR);
				cell = (String)row.get(TAGS_COLUMN_INDEX);
				if (cell != null)
					tags = parseNameValuePairs(cell, LABEL_SEPARATOR);
				ReferencedSoundSpec record = new ReferencedSoundSpec(file, spec.getStartMsec(), spec.getEndMsec(), labels, tags);
				md.add(record);
			}
		}
		return md;
	}



	/**
	 * Read the CVS file into a canonical format as follows:
	 * <ul>
	 * <li> Column 1 is the filename and has name {@link #FILE_COLUMN_INDEX}. 
	 * <li> Column 2 is the semi-colon separated list of labels and has name {@link #LABELS_COLUMN_INDEX}. 
	 * <li> Column 3 is the semi-colon separated list of tags and has name {@link #TAGS_COLUMN_INDEX}. 
	 * </ul>
	 * We need to support old and new formats as follows:
	 * <ul>
	 * <li> 3 columns with the Header 'File,Sensor,Labels' (with no tags and we ignore the sensor column).
	 * <li> 3 columns with optional header and columns as described above. 
	 * </ul>
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private static CSVTable readCanonicalCSV(Reader reader) throws IOException {
		// Build an empty canonical table with the correct columns in the correct order.
		String canonicalHeaders[] = { NEW_FILE_COLUMN_INDEX, NEW_LABELS_COLUMN_INDEX,TAGS_COLUMN_INDEX} ;
		CSVTable canonicalTable = new CSVTable();
		for (int i=0 ; i<canonicalHeaders.length ; i++)
			canonicalTable.appendColumn(canonicalHeaders[i]);	

		// Always read the table w/o headers.  column names will be 1-based indexed of the columns.
		CSVTable table = CSVTable.read(reader, 0);
		
		// IF an Empty table, then just return the unfilled canonical table.
		if (table.getRowCount() == 0)
			return canonicalTable;

		if (table.getColumnCount() != 3) 
			throw new IOException("Input does not have the required 3 columns.");
		List<String> colNames = table.getColumnNames();


		// See if we have the old style header
		boolean oldHeader = true;
		String oldHeaders[] = { FILE_COLUMN_NAME,SENSOR_COLUMN_NAME,LABELS_COLUMN_NAME } ;
		CaseInsensitiveMap firstRow = table.getRow(0);
		for (String name : oldHeaders) 
			oldHeader = firstRow.containsValue(name) && oldHeader;
		// See if we have the new style header, which is optional for the new format.
		boolean newHeader = true;
		String newHeaders[] = { FILE_COLUMN_NAME, LABELS_COLUMN_NAME,TAGS_COLUMN_NAME } ;
		for (String name : newHeaders)  
			newHeader = firstRow.containsValue(name) && oldHeader;	



		// Create the canonical table from the table on disk.
		int rowCount = table.getRowCount();
		int startRow = oldHeader || newHeader ? 1 : 0;
		for (int i=startRow ; i<rowCount ; i++) {
			CaseInsensitiveMap row = table.getRow(i);
			Map<String,Object> canonicalRow = new HashMap<String,Object>();
			if (oldHeader) {
				canonicalRow.put(NEW_FILE_COLUMN_INDEX, row.get(OLD_FILE_COLUMN_INDEX)); 
				canonicalRow.put(NEW_LABELS_COLUMN_INDEX, row.get(OLD_LABELS_COLUMN_INDEX)); 
				canonicalRow.put(TAGS_COLUMN_INDEX, ""); 
			} else { 
				canonicalRow.putAll(row);
			}
			canonicalTable.appendRow(canonicalRow);
		}
		return canonicalTable;
	}


	
	/**
	 * Write the given sound ref spec to the given writer. 
	 * @param writer a writer opened on a metadata file.
	 * @param record the record to append to the writer/metadata file.  This includes the reference, optional segment info, labels and tags.
	 * @param absoluteSoundReferences if true, then use an absolute file reference based on the reference in the given record.
	 * @param forLinux if true, then don't include Windows disk specifiers.
	 * @throws IOException
	 */
	private static void writeRecord(Writer writer, IReferencedSoundSpec record, boolean absoluteSoundReferences, boolean forLinux) throws IOException {
//		String ref = record.getReference();
		String labels = formatNameValuePairs(record.getLabels(), LABEL_SEPARATOR);
		String tags = formatNameValuePairs(record.getTags(), LABEL_SEPARATOR);
		IReferencedSoundSpec spec = record;
//		try {
//			spec = parseReference(ref);
//		} catch (ParseException e) {
//			throw new IOException("Could not parse reference: " + ref, e);
//		}
		String fileName = spec.getReference();	// This is now the raw file reference w/o start/end segment 
		if (absoluteSoundReferences)
			fileName = new File(fileName).getAbsolutePath();

//		if (forLinux) {
//			int colonIndex = fileName.indexOf(':');
//			if (colonIndex == 1) 
//				fileName = fileName.substring(colonIndex+1);
//			fileName = fileName.replaceAll("\\\\", "/");
//		}
		StringBuilder sb = new StringBuilder();
		String referenceKey = formatReference(spec, forLinux);
		sb.append(referenceKey);

		sb.append(COLUMN_SEPARATOR);	// no extra spaces
		sb.append(labels);
		sb.append(COLUMN_SEPARATOR);	// no extra spaces
		sb.append(tags);
		sb.append("\n");
		writer.write(sb.toString());
		
	}



	/**
	 * A convenience on {@link #write(String, boolean)} with forLinux=true.
	 * @param fileName
	 * @throws IOException
	 */
	public void write(String fileName) throws IOException {
		this.write(fileName,true);
	}

	/**
	 * Write the instance to a comma-separated-value file.
	 * @param fileName the name of the file to write this instance to.
	 * If an absolute path, write the file there.
	 * If not an absolute path/file then the location is relative to that of this instance's location (per the constructor
	 * or {@link #read(String)}.  In either case, if the file name represents a different location than 
	 * is assigned to this instance, that is a move of the instance, then the filenames written to the resulting file
	 * will be absolute. 
	 * @param forLinux if true, then we remove leading 'x:' and change backslash (\) to forward slash (/).
	 * @throws IOException
	 * @see {@link #read(String)} for format produced.
	 */
	public void write(String fileName, boolean forLinux) throws IOException {
		// Determine the absolute path and if this is different than this.filePath.
		File file = getMetaDataFile(fileName, false);
//		File f = new File(fileName);
//		if (f.isDirectory()) {
//			fileName = fileName + "/" + DEFAULT_METADATA_FILE_NAME;
//			f = new File(fileName);
//		}
		boolean moving;
		File parentFile = file.getParentFile();
		if (file.isAbsolute()) {
			if (parentFile == null) {	// Root of file system?
				moving = absoluteParent.equals(File.pathSeparator); 
			} else {
				String parentPath = parentFile.getAbsolutePath(); 
				moving = !parentPath.equals(absoluteParent);
			}
		} else {	// path relative to this.filePath 
			if (parentFile == null) {	// fileName is just a filename with no path
				moving = false;
				fileName = this.absoluteParent + "/" + file.getName();
				file = new File(fileName);
			} else {	// a file not in the filePath directory
				String parentPath = parentFile.getAbsolutePath(); 
				moving = !parentPath.equals(absoluteParent);
			}
		}
		// At this point, we know the destination and if it is moving relative to this.filePath.
		FileWriter fw = new FileWriter(file.getAbsolutePath());
		write(fw, forLinux, moving);
		fw.close();
	}
	

	/**
	 * @param stream not close on return. 
	 * @param forLinux remove : and change \ to / in sound file names.
	 * @param absoluteSoundReferences use absolute paths when writing names of sound files.
	 * @throws IOException
	 */
	public void write(OutputStream stream, boolean forLinux, boolean absoluteSoundReferences) throws IOException {
		write(new OutputStreamWriter(stream), forLinux, absoluteSoundReferences); 
	}

	/**
	 * Write out this instance to the given writer optionally for linux and/or with absolute file paths.
	 * @param writer not closed on return.
	 * @param forLinux remove : and change \ to / in sound file names.
	 * @param absoluteSoundReferences use absolute paths when writing names of sound files.
	 * @throws IOException
	 */
	public void write(Writer writer, boolean forLinux, boolean absoluteSoundReferences) throws IOException {

		for (IReferencedSoundSpec record: this) 
			writeRecord(writer, record, absoluteSoundReferences, forLinux);
		writer.flush();
	}


	/**
	 * A convenience over {@link #writeMetaDataSounds(String, Iterable, Iterable,boolean)} in which no names are given so that
	 * only indexes are used to name the written sound files.
	 */
	public static List<String> writeMetaDataSounds(String dest, Iterable<SoundRecording> sounds) throws IOException {
		return writeMetaDataSounds(dest,sounds,null, true);
	}	
	
	/**
	 * Write the given sounds and associated metadata file to the given directory.
	 * Sounds are written as WAV files with .wav extension. The names of the sounds
	 * are integer indexes, for example, 000001.wav unless names are provided.
	 * @param dest a directory or the name of a metadata file with a .csv extension.  If the latter sounds are written to the directory specified.
	 * The directory must exist and if there is already a metadata file there, it is overwritten.
	 * @param sounds
	 * @param fnames names to be used to store the corresponding sounds.  If null, empty or smaller than the iterable of sounds, then an 
	 * incremented index is used to name the files.  The iterable may contain null values in which case the corresponding sound will be saved
	 * in an indexed file name.
	 * @param writeSounds if true then also write the given sounds using the optional names or indexed names if not provided.
	 * @param forLinux if true then do not include '[X]:' from file name written into the metadata file.
	 * @return never null. the names of the files written into the dest directory. 
	 * @throws IOException
	 */
	public static List<String> writeMetaDataSounds(String dest, Iterable<SoundRecording> sounds, Iterable<String> fnames, boolean forLinux) throws IOException {
		File metaDataCSV = getMetaDataFile(dest, false);
	
		// Clean up any existing file.
		if (metaDataCSV.exists() && !metaDataCSV.delete()) 
			throw new IOException("Could not remove existing metadata file: " + metaDataCSV.getAbsolutePath());
		
		String destDir = metaDataCSV.getParent();

		// If quick, then save records in the MetaDat object, otherwise write them to the metadata file as we go.
		FileWriter mdWriter = new FileWriter(metaDataCSV,true);
		
		Iterator<String> nameIter = fnames == null ?  null : fnames.iterator(); 
		int index = getMaxIndexedFileName(destDir) + 1;	// Always do this in case we run out of names in the names list.

		List<String> namesUsed = new ArrayList<String>();
		try {
			for (SoundRecording sr : sounds) {
				// Get the name for this sound.
				String name; 
				if (nameIter != null && nameIter.hasNext()) {
					name = nameIter.next();
					if (!name.endsWith(".wav"))
						name = name + ".wav";
				} else {
					name = String.format(INDEX_FILE_NAME_FORMAT,index) + ".wav";
					index++;
				}

				// Write out the sound  
				PCMUtil.PCMtoWAV(destDir + "/" + name, sr.getDataWindow());
				IReferencedSoundSpec spec = MetaData.newReferencedSoundSpec(name, sr);

				// Write out/capture the record for this sound.
				writeRecord(mdWriter, spec, false, forLinux);

				// Keep the name of the file to return to the caller.
				namesUsed.add(name);
			}
		} finally {
			mdWriter.close();
		}
	
		return namesUsed;
	}
	/**
	 * Write the given segmented sounds and associated metadata file to the given directory.
	 * Sounds are written as WAV files with .wav extension. The names of the sounds
	 * are integer indexes, for example, 000001.wav unless names are provided.  The resulting
	 * metadata file has one line for each segment, but multiple rows will generally refer to the same wav file when they segments of the same source sound.
	 * For example,
	 * <pre>
	 * 000001.wav[0-1000],...
	 * 000001.wav[1000-2000],...
	 * 000002.wav,...
	 * 000003.wav[0-2000],...
	 * 000003.wav[2000-3000],...
	 * </pre>
	 * If a SegmentedSoundRecording has now segments defined, the sound is treated as if the whole sound is one segment.
	 * Labels from segments are merged with those on the base SoundRecording.  Same for tags.
	 * @param dest a directory or the name of a metadata file with a .csv extension.  If the latter sounds are written to the directory specified.
	 * The directory must exist and if there is already a metadata file there, it is overwritten.
	 * @param sounds
	 * @param fnames names to be used to store the corresponding sounds.  If null, empty or smaller than the iterable of sounds, then an 
	 * incremented index is used to name the files.  The iterable may contain null values in which case the corresponding sound will be saved
	 * in an indexed file name.
	 * @param writeSounds if true then also write the given sounds using the optional names or indexed names if not provided.
	 * @param forLinux if true then do not include '[X]:' from file name written into the metadata file.
	 * @return never null. the names of the files written into the dest directory. 
	 * @throws IOException if failed to remove an existing metadata file or other errors writing either the wav or metadata file.
	 */
	public static List<String> writeMetaDataSoundSegments(String dest, Iterable<SegmentedSoundRecording> sounds, Iterable<String> fnames, boolean forLinux) throws IOException {
		File metaDataCSV = getMetaDataFile(dest, false);
	
		// Clean up any existing file.
		if (metaDataCSV.exists() && !metaDataCSV.delete()) 
			throw new IOException("Could not remove existing metadata file: " + metaDataCSV.getAbsolutePath());
		
		String destDir = metaDataCSV.getParent();

		// If quick, then save records in the MetaDat object, otherwise write them to the metadata file as we go.
		FileWriter mdWriter = new FileWriter(metaDataCSV,true);
		
		Iterator<String> nameIter = fnames == null ?  null : fnames.iterator(); 
		int index = getMaxIndexedFileName(destDir) + 1;	// Always do this in case we run out of names in the names list.

		List<String> namesUsed = new ArrayList<String>();
		try {
			for (SegmentedSoundRecording ssr : sounds) {
				SoundRecording sr = ssr.getEntireLabeledDataWindow();

				// Get the name for this sound.
				String name; 
				if (nameIter != null && nameIter.hasNext()) {
					name = nameIter.next();
					if (!name.endsWith(".wav"))
						name = name + ".wav";
				} else {
					name = String.format(INDEX_FILE_NAME_FORMAT,index) + ".wav";
					index++;
				}

				// Write out the sound  
				PCMUtil.PCMtoWAV(destDir + "/" + name, sr.getDataWindow());
				
				// Write out the record(s) for this sound.
				List<LabeledSegmentSpec> segments = ssr.getSegmentSpecs();
				if (segments.isEmpty()) {
					IReferencedSoundSpec spec = MetaData.newReferencedSoundSpec(name, sr, null);
					writeRecord(mdWriter, spec, false, forLinux);
				} else {
					for (LabeledSegmentSpec segment : segments) {
						IReferencedSoundSpec spec = MetaData.newReferencedSoundSpec(name, sr, segment);
						writeRecord(mdWriter, spec, false, forLinux);
					}
				}

				// Keep the name of the file to return to the caller.
				namesUsed.add(name);
			}
		} finally {
			mdWriter.close();
		}
	
		return namesUsed;
	}
		
	private static String writeIndexedRecord(Writer metaDataFW, String destDir, int index, SoundRecording sr, boolean forLinux) throws IOException {
		String fileName = String.format(INDEX_FILE_NAME_FORMAT,index) + ".wav";
//		PCMUtil.PCMtoWAV(destDir + "/" + fileName, sr.getDataWindow());
//		if (metaDataFW != null) {
//			IReferencedSoundSpec spec = MetaData.getReferencedSoundSpec(fileName, sr);
//			MetaData.writeRecord(metaDataFW, spec, false, forLinux);
//		}
		writeRecordAndSound(metaDataFW, destDir, fileName, sr, forLinux);
		return fileName;
	}
	


	/**
	 * Write the wave file and optionally append a record to the metadata file.
	 * @param metaDataFW writer opened on a metadata file.
	 * @param destDir directory holding the metadata file and to which sound will be written.
	 * @param fileName the name of the sound file to write in the dest directory. 
	 * @param sr the sound to be written.
	 * @param forLinux if true then the metadata file will not contain Windows disk drives.
	 * @throws IOException
	 */
	private static void writeRecordAndSound(Writer metaDataFW, String destDir, String fileName, SoundRecording sr, boolean forLinux) throws IOException {
		PCMUtil.PCMtoWAV(destDir + "/" + fileName, sr.getDataWindow());
		if (metaDataFW != null) {
			IReferencedSoundSpec spec = MetaData.newReferencedSoundSpec(fileName, sr);
			MetaData.writeRecord(metaDataFW, spec, false, forLinux);
		}
	}

	/**
	 * Append the sound as a wav file to the meta data file at the given location and write it to disk.
	 * @param location a directory containing {@value #DEFAULT_METADATA_FILENAME} file or the name of a metadata file.
	 * @param sound
	 * @return name of file that can be used as a key in the metadata file at the given location. 
	 * @throws IOException
	 */
	public static String appendMetaDataSound(String location, SoundRecording sound) throws IOException {
		List<SoundRecording> srList = new ArrayList<SoundRecording>();
		srList.add(sound);
		List<String> files = appendMetaDataSounds(location, srList);
		if (files == null || files.size() != 1)
			return null;
		return files.get(0);
	}	
	/**
	 * Append the sounds as .wav files to the meta data file at the given location and write them to disk in wav format.
	 * @param location a directory containing {@value #DEFAULT_METADATA_FILENAME} file or the name of a metadata file.
	 * @param sounds
	 * @return names of files written relative to the metadata file at the given location. names can be used as keys into the metadata file at the given location.
	 * @throws IOException
	 */
	public static List<String> appendMetaDataSounds(String location, Iterable<SoundRecording> sounds) throws IOException {
		File metaDataCSV = getMetaDataFile(location,false);
		MetaData metadata; 
		if (metaDataCSV.exists())  {
			metadata = MetaData.read(metaDataCSV.getAbsolutePath());
		} else  {
			metadata = new MetaData(metaDataCSV.getParent());
		}
		FileWriter fw = new FileWriter(metaDataCSV, true);
		List<String> fileNames = new ArrayList<String>();
		try {
			String destDir = metaDataCSV.getParent();
			int index = getMaxIndexedFileName(metadata) + 1;
			for (SoundRecording sr : sounds)  {
				String name = writeIndexedRecord(fw, destDir, index++, sr, false);
				fileNames.add(name);
			}
		} finally  {
			fw.close();
		}
		return fileNames;
	}
	
	private static int getMaxIndexedFileName(MetaData metadata) {
		int maxIndex = 0;
		for (String filename : metadata.getFiles()) {
			File f = new File(filename);
			String baseName = f.getName();
			// Try and parse integer out of "%07d.wav"
			baseName = baseName.replace(".wav", "");
			try {
				int t = Integer.parseInt(baseName);
				if (t > maxIndex)
					maxIndex = t;
			} catch (Exception e) {
				;	// ignore
			}
		}
		return maxIndex;
	}
	
	private static int getMaxIndexedFileName(String dir) {
		int maxIndex = 0;
		File dirFile = new File(dir);
		if (!dirFile.isDirectory())
			dirFile = dirFile.getParentFile();
		String files[] = dirFile.list();
		for (String filename : files) {
			File f = new File(filename);
			String baseName = f.getName();
			// Try and parse integer out of "%07d.wav"
			baseName = baseName.replace(".wav", "");
			try {
				int t = Integer.parseInt(baseName);
				if (t > maxIndex)
					maxIndex = t;
			} catch (Exception e) {
				;	// ignore
			}
		}
		return maxIndex;
	}

	/**
	 * Get the meta data file from the given dest, which is either a directory name or a metadata file name.
	 * if A directory name, then we expect to find the file named {@value #DEFAULT_METADATA_FILENAME}.
	 * @param dest a directory or the name of a metadata file with a .csv extension.  
	 * @param requireMetaData if true and file does not exist, throw the exception.  Else if false always return the File
	 * representing the metadata file. 
	 * @return  never null and the File for a metadata file.  If requiredMetaDataFile is false, then
	 * the file may or may not exist.
	 * @throws IOException  if the meta data file does not exist at the given location.
	 */
	static File getMetaDataFile(String location, boolean requireMetaDataFile) throws IOException {
		String fname;
		File locationFile = new File(location);
		if (locationFile.isDirectory()) {
			fname = location + "/" + MetaData.DEFAULT_METADATA_FILE_NAME; 
		} else if (location.endsWith(".csv")) {
			fname = location;
		} else {
			throw new IllegalArgumentException("Location is not a directory and therefore is the name of a meta data file, "
					+"but the meta data file name (" + location + ") does not end with .csv extension.");
		}
		File metaDataCSV = new File(fname);
		if (requireMetaDataFile && !metaDataCSV.exists())
			throw new IOException("File " + fname + " does not exist."); 
		return metaDataCSV;
	}


	/**
	 * Write out a set of name=value pairs each separated by the given separator (typically a semi-colon).
	 * @param labels
	 * @param labelValueSeparator
	 * @return
	 */
	public static String formatNameValuePairs(Properties props, String labelValueSeparator) {
		if (props == null)
			return "";
		StringBuilder sb = new StringBuilder();
		
	
		for (Object key : props.keySet()) {
			if (sb.length() != 0)	// Not first time
				sb.append(labelValueSeparator);
			sb.append(key.toString());
			sb.append(NAME_VALUE_SEPARATOR);
			String value = props.getProperty(key.toString());
			String escapedValue = StringUtil.escape(value, COLUMN_SEPARATOR, CSVTable.ESCAPE_CHAR);
			sb.append(escapedValue);
		}
		return sb.toString();
	}

	/**
	 * Parse a list of name=value pairs separated by the given separator (typically semi-colon).
	 * @param nameValues
	 * @param nameValueSeparator
	 * @return never null.
	 */
	public static Properties parseNameValuePairs(String nameValues, String nameValueSeparator) {
		Properties p = new Properties();
		String splits[] = nameValues.split(nameValueSeparator);
		for (String s : splits) {
			int index = s.indexOf(NAME_VALUE_SEPARATOR);
			if (index > 0) {
				String name = s.substring(0,index);
				String value = s.substring(index+1);
				p.put(name.trim(), value.trim());
			} else {
//				 System.err.println("Could not parse label from " + s);
				// No labels present
			}
		}
		return p;
	}


	/**
	 * This is the class that turns a reference from an {@link IReferencedSoundSpec#getReference()} into a SoundRecording.
	 * @author DavidWood
	 */
	private static class RecordingDereferencer implements IDereferencer<IReferencedSoundSpec, SoundRecording> {

		private final MetaData metaData;
		IMultiKeyCache cache = new MemoryCache();
//		private final boolean applyReference;

		/**
		 * @param metaData
		 * @param applyReference set to true, when you know another class will be calling 
		 */
		public RecordingDereferencer(MetaData metaData) {
			this.metaData = metaData;
		}
		
		/**
		 * This reference should include the file name so load it and apply IReferencedSoundSpec. 
		 * @param reference this the IReferencedSoundSpec for the SoundRecording to be loaded.  Its apply() method is called 
		 * to apply it metadata.
		 * @throws IOException 
		 */
		@Override
		public SoundRecording loadReference(IReferencedSoundSpec reference) throws IOException {
			String fileName = reference.getReference();
			
			// Read the file, try and find it locally or relative to the MetaData file.
			fileName = MetaData.getExistingFile(this.metaData.absoluteParent, fileName, true);
			SoundClip clip = (SoundClip) cache.get(fileName); 
			if (clip == null) {
				clip = SoundClip.readClip(fileName);
				cache.put(clip, fileName);
			}

			// Create the recording with the labels and tags from our MetaData instance.
			Properties labels = null; 
			Properties tags   = null; 
			SoundRecording sr = new SoundRecording(clip, labels,tags); 
			
			// Per contract, let the IReferencedSoundSpec apply its metadata.
			try {
				sr = reference.apply(sr);
			} catch (AISPException e) {
				throw new IOException("Could not apply transformation to recording" , e);
			}

			// Then remove any internal tags
			sr = adjustAugmentedTags(sr);

			// Add the filename tag.
			sr.addTag(FILENAME_TAG, fileName); 

			// Done
			return sr;
		}	
	}

	/**
	 * Read the sound contained in this instance.
	 * @param reference a reference to a Record for which the sound is being requested.
	 * @return never null
	 * @throws IOException
	 */
	public SoundRecording readSound(String reference) throws IOException { 
		IReferencedSoundSpec sref = this.dereference(reference);
		if (sref == null) {
			reference = getExistingFile(this.absoluteParent, reference, false);
			if (reference != null) 
				sref = this.dereference(reference);
			if (sref == null)
				throw new IOException("File " + reference + " is not contained in this instance.");

		}
		RecordingDereferencer rd = new RecordingDereferencer(this);
		SoundRecording sr = rd.loadReference(sref);
		return sr;
	}

	/**
	 * Get an iterable over all the sounds referenced by this instance.
	 * @return never null
	 * @throws IOException 
	 */
	public ISoundDataSet readSounds() throws IOException {
		return readSounds(true); 
	}

	/**
	 * Get the sounds referenced by this instance.
	 * @param requireAllFiles if false, then allow files specified in this instance to be missing.
	 * @return never null.
	 * @throws IOException
	 */
	public ISoundDataSet  readSounds(boolean requireAllFiles) throws IOException {
		if (requireAllFiles) 
			return new ComposedSoundDataSet(this.getReferences(), this, new RecordingDereferencer(this), false);	// false because RecordingDereferencer applies the segment spec.
		
		// If not requiring all files, then go through and check existence.
		MetaData md = new MetaData(this.getFileName());
		for (String ref: this.getReferences()) {
			IReferencedSoundSpec r = this.dereference(ref);
			if (r == null)
				continue;	// Should not happen, but just in case.
			String existingFile = MetaData.getExistingFile(this.absoluteParent, r.getReference(), false);
			if (existingFile == null)	// only true if requireAllFiles == false
				continue;
			// Copy the record except for the reference.
			r = new Record(r, existingFile);
			md.add(r);
		}
	
		ISoundDataSet recordingIterable = 
				new ComposedSoundDataSet(md.getReferences(), md, new RecordingDereferencer(md),false);	// false because RecordingDereferencer applies the segment spec.
		return recordingIterable;
	}
	
	
	/**
	 * Converts a single file name referenced into the SegmentedSoundRecording using a list of associated segmentations.
	 */
	private static class SoundSegmentDereferencer implements IDereferencer<String, SegmentedSoundRecording> {

		/** use to read the file under the reference  */
		MetaData metadata;
		IMultiKeyCache cache = new MemoryCache();
		
		/** A map of filename references to a list of segments */
		Map<String, List<LabeledSegmentSpec>> segmentations;
		
		public SoundSegmentDereferencer(MetaData metadata , Map<String, List<LabeledSegmentSpec>> segmentations) {
			this.metadata = metadata;
			this.segmentations = segmentations;
		}

		/**
		 * The reference is the name of a file which needs to be read and then married with the segment specs provided to the constructor.
		 */
		@Override
		public SegmentedSoundRecording loadReference(String reference) throws IOException {
			String file = metadata.getReferenceableFile(reference);
			SegmentedSoundRecording ssr = (SegmentedSoundRecording) cache.get(reference);
			if (ssr != null) 
				return ssr;
			
			SoundClip clip = SoundClip.readClip(file);
			Properties tags = new Properties();
			Properties labels = null; 
			List<LabeledSegmentSpec> segments = segmentations.get(reference);
			tags.setProperty(MetaData.FILENAME_TAG, file);

			// If single egment is present that is NOT a sub-segment, then the tags and labels appy to the whole SoundRecording. 
			if (segments.size() == 1) {
				LabeledSegmentSpec spec = segments.get(0);
				if (!spec.isSubSegment()) {
					labels = spec.getLabels();
					tags.putAll(spec.getTags());
				}	
			}
			SoundRecording sr = new SoundRecording(clip, labels, tags); 
			ssr = new SegmentedSoundRecording(sr, segments);
			ssr = MetaData.adjustAugmentedTags(ssr);
			cache.put(ssr, reference);
			return ssr;
		}


	}


	
	/**
	 * Read the metadata sounds back as an iterable over SegmentedSoundRecording, such that we get one SegmentedSoundRecording for
	 * each unique file in the metadata.  
	 * @param requireAllFiles
	 * @return never null.
	 * @throws IOException
	 */
	public IShuffleIterable<SegmentedSoundRecording> readSegmentedSounds(boolean requireAllFiles) throws IOException {

		/** A list of segments for each filename in this instance */
		Map<String, List<LabeledSegmentSpec>> segmentations = new HashMap<String,List<LabeledSegmentSpec>>();
		List<String> fileNames = new ArrayList<String>();
		for (IReferencedSoundSpec spec : this) {
			String filename = spec.getReference();

			// Check if file exists first
			File filenameFile = new File(this.getReferenceableFile(filename));
			if (!filenameFile.exists()) {
				if (requireAllFiles)
					throw new IOException("File "  + filenameFile.getAbsolutePath() + " not found");
				continue;
			}
			
			// File exists, so add this reference to the list of segments for this file.
			List<LabeledSegmentSpec> segList = segmentations.get(filename);
			if (segList == null) {
				segList = new ArrayList<LabeledSegmentSpec>();
				segmentations.put(filename, segList);
			}
			LabeledSegmentSpec segSpec = new LabeledSegmentSpec(spec.getStartMsec(), spec.getEndMsec(), spec.getLabels(), spec.getTags());
			segList.add(segSpec);
			if (!fileNames.contains(filename))
				fileNames.add(filename);
		}

		/**
		 * Create the iterator over the file names to produce one SegmentedSoundRecording for each name.
		 */
		return new DelegatingShuffleIterable<SegmentedSoundRecording>(fileNames, new SoundSegmentDereferencer(this, segmentations));

	}	
	/**
	 * Override to map the file name reference to absolute file names in the returned instance.
	 * This in case the two data sets are not in the same location.
	 * @param dataSet
	 * @return this instance.
	 */
//	@Override	
	public MetaData merge(MetaData dataSet) {
		if (dataSet == this)
			return this;
		for (String ref : dataSet.getReferences()) {
			IReferencedSoundSpec spec = dataSet.dereference(ref);
			String file = spec.getReference();
			String absFile = dataSet.getReferenceableFile(file);
			spec = new ReferencedSoundSpec(spec, absFile);
			this.add(ref, spec);
		}
		return  this;
	}
	

	/**
	 * 
	 * @return
	 */
	public String getLocation() {
		return this.absoluteParent;
	}
	
	/**
	 * Get the base name of the metadata file for this instance.
	 * @return never null.
	 */
	public String getFileName() {
		return this.metaDataFile.getName();
	}


	@Override
	public MetaData newIterable(Iterable<String> references) {
		return new MetaData(this, references);
	}

	
//	@Override
//	protected Record newSoundMetaData(String reference, Properties labels, Properties tags) {
//		return new Record(reference,labels,tags);
//	}

	@Override
	protected MetaData newInstance() {
		return new MetaData(this.absoluteParent);
	}

	/**
	 * Add a record containing a reference string that is the path to the file relative to this instance location.
	 * The reference created is that contained in the given dataRef and should be the name of a file.
	 * @param dataRef
	 */
	@Override
	public String add(IReferencedSoundSpec dataRef) {
		String ref = formatReference(dataRef, true); 
		super.add(ref, dataRef);
		return ref;		
	}


	
	/**
	 * Return the references per super class in the order as listed in the metadata file or added to this instance.
	 */
	@Override
	public Iterable<String> getReferences() {
		this.references = this.orderedReferences;
		return this.references;
	}


	private boolean containsReference(String reference) {
		return this.dereference(reference) != null;
	}

	/**
	 * Look for a existing file represented by this istances file name/reference used in this instance.
	 * If not absolute, then look first in the given containing directory  and then
	 * the JVM's current directory.
	 * @param containingDir 
	 * @param reference contains a filename used to find an existing file. 
	 * @param throwException if file is not found, then throw an exception. 
	 * @return null if could not find a matching file and throwException is false. if throwException
	 * is true then never return null. 
	 * @throws IOException
	 */
	static String getExistingFile(String containingDir, String reference, final boolean throwException) throws IOException {
		String fileName;
		try {
			fileName = parseFileNameFromReference(reference);
		} catch (ParseException e) {
			throw new IOException("Could not parse reference", e);
		}
		File file = new File(fileName);
		String foundFile = null;
		if (file.isAbsolute()) {	// Just make sure it exists.
			if (file.exists()) 
				foundFile = fileName;
		} else {					// Relative, 
			// 1st try relative to this.absoluteParent, else relative to current dir.
			File instanceRelativeFile = new File(containingDir + "/" + fileName);
			if (instanceRelativeFile.exists())   
				foundFile = instanceRelativeFile.getPath();
			else if (file.exists())  
				foundFile = fileName; 
		}
		if (foundFile == null && throwException)
			throw new IOException("File " + file + " could not be located.");
		return foundFile;
	}


	private final static String SEGMENT_START = "[";
	private final static String SEGMENT_END= "]";
	private final static String SEGMENT_START_END_SEPARATOR= "-";
//	private final static String REFERENCE_FORMAT = "%s" + SEGMENT_START + "%d" + SEGMENT_START_STOP_SEPARATOR + "%d" + SEGMENT_END;
	private final static String REFERENCE_FORMAT = "{0}" + SEGMENT_START + "{1,number,#}" + SEGMENT_START_END_SEPARATOR + "{2,number,#}" + SEGMENT_END;
	private final static MessageFormat ReferenceMessageFormat = new MessageFormat(REFERENCE_FORMAT);

	/**
	 * Get the filename portion of the reference.
	 * @param reference
	 * @return
	 * @throws ParseException 
	 */
	private static String parseFileNameFromReference(String reference) throws ParseException {
		return parseReference(reference).getReference();
	}
	
	/**
	 * Parse the filename and start/end times from the reference.
	 * @param reference
	 * @return never null. an instance with only the reference and start/end times sets.  The contained 
	 * reference does NOT include the start/end time.  If the start/end are not present in
	 * the reference then the returned instance has 0 for both start/end time.
	 * @throws ParseException 
	 */
	private static IReferencedSoundSpec parseReference(String reference) throws ParseException {
		int index = reference.indexOf(SEGMENT_START);
		int startMsec = 0, endMsec = 0;
		if (index >= 0)  {
			Object[] fields = ReferenceMessageFormat.parse(reference);
			if (fields == null || fields.length != 3)
				throw new ParseException("Reference does not contain expected number of arguments (3): " + reference,0);
			reference = fields[0].toString();
			if (!(fields[1] instanceof Number))
				throw new ParseException("Reference segment start is not a number: " + reference,0);
			if (!(fields[2] instanceof Number))
				throw new ParseException("Reference segment end is not a number: " + reference,0);
			startMsec = ((Number)fields[1]).intValue(); 
			endMsec = ((Number)fields[2]).intValue(); 
		}
		return new ReferencedSoundSpec(reference, startMsec, endMsec, null,null);
	}

	/**
	 * Use the filename portion of the contained reference together with the spec's start/stop time to get a reference string.
	 * @param spec	 spec reference contains the raw file name.
	 * @param forLinux if true then remove any windows disk specification from the filename.
	 * @return
	 * @throws ParseException 
	 */
	private static String formatReference(IReferencedSoundSpec spec, boolean forLinux) { 
		// Parse filename out of the contained reference.
		String fileName = spec.getReference();

		if (forLinux) {
			int colonIndex = fileName.indexOf(':');
			if (colonIndex == 1) 
				fileName = fileName.substring(colonIndex+1);
			fileName = fileName.replace("\\", "/");
		}
		
		// If no segment spec, then just return the raw file. 
		int endMsec = spec.getEndMsec();
		if (spec.getEndMsec() <= 0)
			return fileName; 
		int startMsec = spec.getStartMsec();
		String reference = MessageFormat.format(REFERENCE_FORMAT, fileName, new Integer(startMsec), new Integer(endMsec));
		return reference;
	}
	
	/**
	 * Get the set of augmentation tags to store with the given sound recording (in case they tags aren't stored when the recording is stored).
	 * @param sr
	 * @return
	 * @see {@link #adjustAugmentedTags(SoundRecording)}. 
	 */
	protected static Properties getAugmentedTags(SoundRecording sr) {
		Properties tags = sr.getTagsAsProperties(); 
		double startMsec = sr.getDataWindow().getStartTimeMsec();
		if (startMsec > 0) {
			Properties p = new Properties();
			p.putAll(sr.getTags());
			p.put(AbstractSoundReferenceSet.START_TIME_MSEC_LABEL, String.valueOf(startMsec));
			tags= p;
		}
		return tags;
	}

	/**
	 * Look for any special tags (or labels) and if found, a) then recreate a new recording based on them b) remove them in the modified recording. 
	 * @param sr a recording that may containg special annotation tags (i.e. START_TIME_MSEC_LABEL, etc).
	 * @return never null.  If not tags are found, then the given instance is returned.
	 * @see #getAugmentedTags(SoundRecording)
	 */
	protected static SoundRecording adjustAugmentedTags(SoundRecording sr) {
		Properties finalLabels = new Properties();
		Properties finalTags = new Properties();
		
		// Copy the tags/labels to not change the ones on the given recording.
		finalLabels.putAll(sr.getLabels());
		finalTags.putAll(sr.getTags());
	
		// See if we need to adjust the start time of the clip and remove/adjust any tags/labels.. 
		String startMsecValue = finalTags.getProperty(AbstractSoundReferenceSet.START_TIME_MSEC_LABEL);
		if (startMsecValue == null)  
			startMsecValue = finalLabels.getProperty(AbstractSoundReferenceSet.START_TIME_MSEC_LABEL);
		if (startMsecValue != null)  {
			finalLabels.remove(AbstractSoundReferenceSet.START_TIME_MSEC_LABEL);
			finalTags.remove(AbstractSoundReferenceSet.START_TIME_MSEC_LABEL);
			double startMsec = Double.valueOf(startMsecValue);
			if (startMsec != 0) {
				SoundClip clip = new SoundClip( startMsec, sr.getDataWindow());
				sr = new SoundRecording(clip, finalLabels, finalTags);
			}
		}
	
		return sr;
	}
	
	/**
	 * Similar to {@link #adjustAugmentedTags(SoundRecording)}, but looks in the segment tags for start times
	 * and applies them to create a new recording and removes them from the segment tags.
	 * This is because when we write the segment tags, they include the start time of the original referenced sound.
	 * @param ssr
	 * @return
	 */
	private static SegmentedSoundRecording adjustAugmentedTags(SegmentedSoundRecording ssr) {
		List<LabeledSegmentSpec> segSpecs = ssr.getSegmentSpecs();
		String startMsecValue = null; 
		for (LabeledSegmentSpec spec : segSpecs) {
			Properties tags = spec.getTags();
			String thisStart = tags.getProperty(AbstractSoundReferenceSet.START_TIME_MSEC_LABEL);
			if (startMsecValue == null) {
				startMsecValue = thisStart;
			} else if (!startMsecValue.equals(thisStart))  {
				// Hope we never see this.
				AISPLogger.logger.warning("Got different start times in tags on label segment");
			}
			tags.remove(START_TIME_MSEC_LABEL);
		}
		// We found a start time on the segment tags, so use it to adjust the start time of the full segment.
		if (startMsecValue != null) {
			SoundRecording sr = ssr.getEntireLabeledDataWindow();
			sr.addTag(START_TIME_MSEC_LABEL, startMsecValue);
			sr = adjustAugmentedTags(sr);
			ssr = new SegmentedSoundRecording(ssr,sr);
		}
		return ssr;
	}

//	@Override
//	public IDereferencer<IReferencedSoundSpec, SoundRecording> getSoundRecordingDereferencer() {
//		return new RecordingDereferencer(this);
//	}
	
//	@Override
	public MetaData select(Iterable<String> references) {
		MetaData dataSet = this.newInstance();
		for (String ref : references) {
			IReferencedSoundSpec dataRef = this.dereference(ref);
			if (dataRef != null)
				dataSet.add(dataRef);
		}
		return dataSet;
	}
	
	
	/**
	 * Create a new version of the this MetaData with its entries sorted by the order of references in the sortBy MetaData.
	 * Any entries in this instance not found in the sortBy instance, will not be included in the output MetaDAta.
	 * @param sortBy a metadata instance that contains matching IReferencedSound
	 * @return a new MetaData instance in the same location as the this instance, with entries ordered by those found in the sortBy MetaData
	 */
	public MetaData sort(MetaData sortBy) {
		MetaData sortedMD = new MetaData(this.getLocation());
		for (String ref : sortBy.getReferences()) {
			String matchingMDRef = this.findMatchingReference(ref);
			if (matchingMDRef  != null)
				sortedMD.add(this.dereference(matchingMDRef));
		}
		return sortedMD;
	}

	/**
	 * Look for a reference that matches the given reference independent of the absolute file path.
	 * @param refToMatch
	 * @return null if none found.
	 */
	private String findMatchingReference(String refToMatch) {
		String matchingMDRef = null;
		String baseRefToMatch = new File(refToMatch).getName();
		for (String mdRef : this.orderedReferences) {
			if (mdRef.contains(refToMatch) || refToMatch.contains(mdRef)) {
				matchingMDRef =  mdRef;
				break;
			} else { 
				String baseMDRef = new File(mdRef).getName();
				if (baseMDRef.equals(baseRefToMatch))  {
					matchingMDRef = mdRef;
					break;
				}
			}
		}
		return matchingMDRef;
	}
}
