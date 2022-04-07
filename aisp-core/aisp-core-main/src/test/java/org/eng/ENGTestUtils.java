package org.eng;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.SoundTestUtils;
import org.eng.util.ClassUtilities;
import org.junit.Assert;


public class ENGTestUtils {

	/**
	 * Created a data serialization file for the given object in the given directory.
	 */
	public static void generateSerialization(String destDir, String baseFileName, Serializable obj) throws IOException {
		File fdir = new File(destDir);
		if (!fdir.exists()) 
			fdir.mkdirs();
		Date now  = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd-yyyy");
		String date = "_" + sdf.format(now);
		String fileName = destDir + "/" + baseFileName + date + ".ser";
		byte[] bytes = ClassUtilities.serialize(obj);
		FileUtils.writeByteArrayToFile(new File(fileName), bytes);
	}

	/**
	 * Read serializations of the class of the given object found in the given directory - most likely created
	 * by {@link #generateSerialization(String, Serializable)}. 
	 * @param destDir
	 * @param obj
	 * @return the list of all serializables loaded from the given directory that are of the same class as the
	 * given obj.
	 * @throws PMException
	 * @throws IOException
	 */
	public static List<Serializable> verifyPastSerializations(String destDir, String baseFileName,  Serializable obj, boolean failOnNoneFound) throws IOException {
		File dir = new File(destDir);
		List<Serializable>  slist = new ArrayList<>();
		Class<?> expectedClass = obj.getClass();
		if (dir.exists())  {
			String prefix = baseFileName + "_";	// seperator as used above in generateSerialization().
			File [] files = dir.listFiles();
			for (File file : files) { 
				String name = file.getName();
				if (name.startsWith(prefix)) {
					byte[] bytes = FileUtils.readFileToByteArray(file);
					try { 
						Serializable dobj = ClassUtilities.deserialize(bytes);
						assertTrue(dobj != null);
						assertTrue(expectedClass.isAssignableFrom(dobj.getClass()));
						slist.add(dobj);
					} catch (IOException e) {
						e.printStackTrace();
						fail("Could not deserialize");
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						fail("Could not deserialize");
					}
				}
			}
		}
		if (failOnNoneFound)
			Assert.assertTrue("No serializations found for class " + expectedClass.getName() + ". Make sure you have called generateSerializations() to create the serialization file(s).",
					slist.size() != 0);
		return slist;
	}
	/**
	 * Look for the given file relative to the classpath.
	 * @param file a file relative to the classpath. If it does not have a leading "/", then one is added.
	 * @return the absoluate path to the file.
	 * @throws FileNotFoundException if not found on the classpath.
	 */
	public static String getResourceFile(String file) throws FileNotFoundException {
		if (!file.startsWith("/"))
			file = "/" + file;
		URL fileurl = ENGTestUtils.class.getResource(file);
//		Path currentRelativePath = Paths.get("");
//		String cwd = currentRelativePath.toAbsolutePath().toString();
		if (fileurl == null) {
//			System.out.println("Current dir: " +  cwd + "'. Could not find relative to class path." + file );
			throw new FileNotFoundException(file);
//		} else {
//			System.out.println("Current dir: " +  cwd + "'. Found relative to classpath " + file );
		}
		return fileurl.getFile();
	}
	/** So test can be run in parallel directories */
	public static String TEST_DATA_DIR = "test-data/";
	public final static String SerializationsDir = GetTestData("serializations");
	public static List<Integer> makeList(int count) {
		List<Integer> intList = new ArrayList<Integer>();
		for (int i=0 ; i<count; i++)
			intList.add(i);
		return intList;
	}

	/**
	 * Get a list of sound recordings, one for each label value.
	 * @param labelValues
	 * @return
	 */
	public static List<SoundRecording> generateTestRecordings(String trainingLabel, String[] labelValues) {
		int channels = 1;
		int samplingRate = 100;
		int bitsPerSample = 8;
		int startMsec = 0;
		int durationMsec = 100;
		int spacingMsec = 0;
		int htz = 10;
		boolean differentiate = true;
	
		List<SoundRecording> srList = new ArrayList<SoundRecording>();
		for (String labelValue : labelValues) {
			if (differentiate)
				startMsec += durationMsec;
			List<SoundRecording> sr = SoundTestUtils.createTrainingRecordings(1, channels, samplingRate, bitsPerSample, startMsec, durationMsec, spacingMsec, htz, trainingLabel, labelValue); 
			srList.addAll(sr);
		}	
		return srList;
	}

	public static String GetTestData(String path) {
		return TEST_DATA_DIR + path;
	}

}
