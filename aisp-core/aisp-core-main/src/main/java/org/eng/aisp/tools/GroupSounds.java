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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.dataset.IReferencedSoundSpec;
import org.eng.aisp.dataset.MetaData;
import org.eng.aisp.dataset.MetaDataZip;
import org.eng.util.CommandArgs;

public class GroupSounds {

	public static String Usage = "Groups sounds specified in a metadata file into zip files \n"
			+ "grouped by common label value. Each output file is named with the label value.\n"
			+ "Usage : ... -d dir -l label [-m ] [-o dir]\n" 
			+ "Options:\n"
			+ "  -d <dir>  : sets the directory from which to load wav files and metadata. Required.\n"
			+ "  -m        : include a metadata.csv in the resulting zip.\n" 
			+ "  -o <dir>  : sets the directory to store zip files.\n"
			+ "              If not specified, then the source directory is used.\n"
			+ "  -l <label>: sets the label name to use when defining groups of sounds\n" 
			;

	public static void main(String[] args) throws IOException, AISPException {
		CommandArgs cmdargs = new CommandArgs(args);
		if (cmdargs.getFlag("-h") || cmdargs.getFlag("-help")) {
			System.out.println(Usage);
			return;
		}
		
		boolean includeMetaData = cmdargs.getFlag("-m");
		String labelName = cmdargs.getOption("-l");
		if (labelName == null) {
			System.err.println("Label name must be provided");
			return;
		}
		String srcDir = cmdargs.getOption("-d"); 
		File srcFile = new File(srcDir);
		if (!srcFile.exists()) {
			System.err.println("Directory " + srcDir + " does not exist.");
			return;
		}
		System.out.println("Reading PCM wav files in " + srcDir);	

		String destDir = cmdargs.getOption("o"); 
		if (destDir != null) {
			File destFile = new File(destDir);
			if (!destFile.exists()) 
				destFile.mkdirs();
		} else {
			destDir = srcDir;
		}
		destDir = new File(destDir).getAbsolutePath();
		int index = destDir.indexOf(":");
		if (index == 1)
			destDir = destDir.substring(2);
		System.out.println("Writing ZIP files in " + destDir);	
		destDir = destDir.replaceAll("\\\\", "/");

		File metaDataFile = new File(srcDir + "/metadata.csv");
		if (!metaDataFile.exists()) {
			System.err.println("Could not find metadata file : " + metaDataFile.getAbsolutePath());
			return;
		}
		MetaData metadata = MetaData.read(metaDataFile.getAbsolutePath());
		List<String> labelValues =  metadata.getLabelValues(labelName);
		if (labelValues.size() == 0) {
			System.err.println("No label values found for label name + " + labelName);
			return;
		}
		
		Map<String, String> env = new HashMap<>(); 
		env.put("create", "true");

		for (String labelValue : labelValues) {
			String zipFileName = destDir + "/" + labelValue + ".zip";
			List<String> soundFilesWithLabelValue = getSoundFiles(metadata, labelName, labelValue);

			System.out.print("Creating " + zipFileName + " containing " + soundFilesWithLabelValue.size() + " sounds labeled with " + labelName + "=" + labelValue + " ..." );
			if (includeMetaData) {
				FileOutputStream fos = new FileOutputStream(zipFileName);
				MetaDataZip mdz = new MetaDataZip(fos);
				for (String fname : soundFilesWithLabelValue) {
					SoundRecording sr = metadata.readSound(fname);
					mdz.add(fname, sr);
				}
				mdz.finalizeZip();
				fos.close();
			} else {
				URI uri = URI.create("jar:file:" + zipFileName);
				FileSystem zipfs = null; 
				try {
					zipfs = FileSystems.newFileSystem(uri, env);
					for (String soundFile : soundFilesWithLabelValue) {
						Path externalTxtFile = Paths.get(soundFile);
						String baseName = new File(soundFile).getName();
						Path pathInZipfile = zipfs.getPath(baseName);
						// copy a file into the zip file
						Files.copy(externalTxtFile, pathInZipfile, StandardCopyOption.REPLACE_EXISTING); 
					}
				} finally {
					if (zipfs != null)
						zipfs.close();
				}
			}
			System.out.println("done.");
		}

	}

	private static List<String> getSoundFiles(MetaData metadata, String labelName, String labelValue) {
		List<String> files = new ArrayList<String>();
		for (IReferencedSoundSpec r : metadata) {
			String lvalue = r.getLabels().getProperty(labelName);
			if (lvalue != null && lvalue.equals(labelValue)) {
				String file = metadata.getReferenceableFile(r.getReference());
				if (!files.contains(file))	// The metadata records can refer to the same file, so be sure to avoid dups.
					files.add(metadata.getReferenceableFile(r.getReference()));
			}
		}
		return files;
	}


}
