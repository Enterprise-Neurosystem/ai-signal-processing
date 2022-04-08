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
package org.eng.aisp.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.eng.aisp.AISPException;
import org.eng.aisp.AISPRuntime;
import org.eng.aisp.dataset.IReferencedSoundSpec;
import org.eng.aisp.dataset.MetaData;
import org.eng.aisp.dataset.ReferencedSoundSpec;
import org.eng.util.CommandArgs;

public class Partition {
	
	static final String DEFAULT_MODEL_SPEC = "ensemble";
	static final int DEFAULT_SUBWINDOW_MSEC = 5000; 

	public final static String Usage = 
			  "Partition a set of sounds defined in a metadata file into 2 or more partitions.\n"
			+ "For each label value of the specified label, partitions are balanced on the \n"
			+ "number of sound files for each label value.\n"
//            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
			+ "Usage: ... -label <name> -dest-dir <dir> [-partitions <n>] [-seed <n>] \\\n"
			+ "           <dir or metadata file>\n"
			+ "Required options:\n"
			+ "  -label name : label on sounds used to attempt a balanced partitioning.\n" 
			+ "  -dest-dir directory : directory into which to place partition sub-directories\n"
			+ "     containing the metdata file and optionally the sounds in the partition.\n"
//			+ "  -verbose : cause extra messaging to be dipslayed on the console.\n"
			+ "Optional options:\n"
			+ "  -partitions <n> : sets the number of partitions to n. Default is 2.\n"
			+ "  -seed <n> : use another random seed to change randomization.\n" 
			+ "  -balance : overrides -maxLabelValues option to create paritions that \n"
			+ "    contain the maximum possible number of sounds for each label value.\n"
			+ "  -maxLabelValues <n> : set the maximum number of label values in partitions.\n"
			+ "     Default is to have no maximum. Set to -1 to use the largest allowed value.\n" 
			+ "  -copy  : copy the sound files to the partition directories.\n" 
			+ "     Default is to reference the original files in the metadata.csv files\n"
			+ "     written to the partition directories.\n" 
			+ "Examples: \n"
			+ "  ... -label status -dest-dir mydir mysoundsdir \n" 
			+ "  ... -label status -dest-dir mydir -copy mysoundsdir \n" 
			+ "  ... -label status -dest-dir mydir -partitions 4 mysoundsdir \n" 
			;

	public static void main(String args[]) {
		// Force any framework initialization messages to come out first.
		AISPRuntime.getRuntime();
		System.out.println("\n");	// blank line to separate copyrights, etc.
		CommandArgs cmdargs = new CommandArgs(args);

		// Check for help request
		if (cmdargs.getFlag("h") || cmdargs.getFlag("-help") ) {
			System.out.println(Usage);
			return;
	    }

		boolean verbose = cmdargs.getFlag("v") || cmdargs.getFlag("verbose");

		try {
			if (!doMain(cmdargs, verbose))
				System.err.println("Use the -help option to see usage");;
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			if (verbose)
				e.printStackTrace();
		}
		
	}
	

	
	protected static boolean doMain(CommandArgs cmdargs, boolean verbose) throws AISPException, IOException {
		
		// Get the name of the model to use.
		int partitions = cmdargs.getOption("partitions", 2);
		int maxLabelValueCount = cmdargs.getOption("maxLabelValues", 0);
		boolean balanced = cmdargs.getFlag("balance") || cmdargs.getFlag("balanced");
		if (balanced)
			maxLabelValueCount = -1;
//		String requireMetaDataOption = cmdargs.getOption("metadata", "some");
//		boolean requireAllMetaData = requireMetaDataOption.equals("all");
//		String soundDir = cmdargs.getOption("sound-dir");
		String destDir = cmdargs.getOption("dest-dir");
		String labelName = cmdargs.getOption("label");
		boolean copySoundFiles = cmdargs.getFlag("copy");
		int seed = cmdargs.getOption("seed", 458909320);
		

		if (labelName == null) {
			System.err.println("label must be provided");
			return false;
		}
		if (partitions < 2) {
			System.err.println("The number of parititions must be larger than 1");
			return false;
			
		}
		if (destDir == null)  {
			System.err.println("Destination directory for partitions must be defined.");
			return false;
		}
		File destFile = new File(destDir);
		if (!destFile.exists()) {
			System.err.println("Creating directory " + destDir );
			destFile.mkdirs();
		} else if (!destFile.isDirectory()) {
			System.err.println( destDir + " is not a directory.");
			return false;
		}
		
		String[] remainingArgs = cmdargs.getArgs();
		if (remainingArgs.length == 0) {
			System.err.println("metadata.csv file must be provided."); 
			return false;
		} else if (remainingArgs.length != 1) {
			System.err.println("Only a single metadata.csv file is allowed.");
			return false;
		}

		String metaDataFileName = remainingArgs[0];
		File metaDataFile = new File(metaDataFileName);
		if (!metaDataFile.exists()) {
			System.err.println("Metadata file " + metaDataFileName + " was not found."); 
			return false;
		}

		// Create a map of label values to metadata references having that label value.
		System.out.println("Reading metadata file in " + metaDataFileName);
		MetaData srcMetaData = MetaData.read(metaDataFileName);
		Map<String, List<String>> labelsToReferenceMap = new HashMap<String, List<String>>(); 
		int count = 0;
		for (String r : srcMetaData.getReferences()) {
			Properties labels = srcMetaData.getLabels(r);
			String labelValue = labels.getProperty(labelName);
			if (labelValue != null) {
				count++;
				List<String> refs = labelsToReferenceMap.get(labelValue); 
				if (refs == null) {
					refs = new ArrayList<String>();
					labelsToReferenceMap.put(labelValue, refs);
				}
				refs.add(r);
			}
		}
		if (count == 0) {
			System.out.println("No sounds labeled with " + labelName);
			return false;
		} else {
			System.out.println("Found " + count + " sounds with label " +  labelName);
		}

		// Create the partition directories and associated metadata objects
		System.out.println("Creating the parition directories in " + destFile.getAbsolutePath());
		List<MetaData> partitionMD = new ArrayList<MetaData>();
		List<File> partitionDirFiles = new ArrayList<File>();
		for (int i=0 ; i<partitions ; i++) {
			if (partitionMD.size() == i) {
				File f = new File(destDir + "/partition" + i);
				FileUtils.deleteQuietly(f);
				f.mkdirs();
				partitionDirFiles.add(f);
				MetaData md = new MetaData(f.getAbsolutePath());
				partitionMD.add(md);
			}
		}
		
		if (maxLabelValueCount < 0) {	
			// Set the maxLabelValueCount to the largest value, which is the smallest number of labels spread across all partitions.
			maxLabelValueCount = Integer.MAX_VALUE;
			for (String labelValue : labelsToReferenceMap.keySet()) {
				List<String> references = labelsToReferenceMap.get(labelValue);
				int perPartitionCount = references.size() / partitions;
				if (perPartitionCount < maxLabelValueCount)
					maxLabelValueCount = perPartitionCount;
			}
		}

		// For each label value, distribute the files evenly and randomly across the partitions.
		System.out.println("Distributing labels values across partitions");
		Random sortSeed = new Random(seed);
		for (String labelValue : labelsToReferenceMap.keySet()) {
			List<String> references = labelsToReferenceMap.get(labelValue);
			Collections.shuffle(references, sortSeed);
			if (references.size() < partitions)  {
				System.out.println("Number of sound file records with label value '" + labelValue + "' is smaller than the number of paritions. Skipping."); 
				continue;
			}
			int partition = 0;
			Map<Integer, Integer> labelValueCounts = new HashMap<Integer, Integer>();
			for (String ref : references) {
				if (partition == partitionMD.size()) 
					partition = 0;

				// Get the current count of label values for this partition.
				Integer c = labelValueCounts.get(partition);
				c = c == null ? 0 : c;

				// When we have enough label values for each partition, stop adding files to the metadata for that label value.
				if (partition == 0 && maxLabelValueCount > 0 && c  >= maxLabelValueCount)
					break;
				
				MetaData md = partitionMD.get(partition);
//				Properties labels = srcMetaData.getLabels(ref);
//				Properties tags = srcMetaData.getTags(ref);
				String refFile = srcMetaData.getReferenceableFile(ref);
				String destName = new File(refFile).getName();
				// Copy the sound file to the future location of the metadata file.
				IReferencedSoundSpec spec = srcMetaData.dereference(ref);
				File refFileFile = new File(refFile);
				if (copySoundFiles)  {
					File partDestDir = partitionDirFiles.get(partition);
					File partDestFile = new File(partDestDir.getAbsolutePath() + "/" + destName);
					if (!partDestFile.exists()) 	// Don't recopy segmented metdata sounds.
						FileUtils.copyFileToDirectory(refFileFile,  partDestDir);
					refFile = destName; 
				} else {
					refFile = refFileFile.getAbsolutePath();
				}
				IReferencedSoundSpec r = new ReferencedSoundSpec(spec, refFile); 
				md.add(r);

				// Update the count of label values in this partition.
				labelValueCounts.put(partition, c + 1);
				partition++;

			}
			
			System.out.print("Label value " + labelValue + " has the following numbers across the partitions (");
			for (Integer p : labelValueCounts.keySet()) {
				Integer c = labelValueCounts.get(p);
				if (p != 0)
					System.out.print(",");
				System.out.print(c);
			}
			System.out.println(")");
		}
		
		/**
		 * Finally write out the metadata files.
		 * Sort the enties we write out by the same ordering as in the source metadata.
		 */
		for (MetaData md : partitionMD) {
			MetaData sorted = md.sort(srcMetaData);
			sorted.write(sorted.getFileName());
		}
		
		return true;

	}


}
