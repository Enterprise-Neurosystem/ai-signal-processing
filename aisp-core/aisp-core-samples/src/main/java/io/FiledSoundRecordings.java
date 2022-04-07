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
package io;

import java.io.IOException;

import org.eng.aisp.SoundRecording;

public class FiledSoundRecordings {

	public static void main(String args[]) throws IOException {
	    //  The location of the Sounds project (in the iot-sounds repo)
	    String srcDir = "sample-sounds/chiller";
	    
	    // Load the sounds and require a meta-data file providing labels & sensor ids.
	    // The sounds acquire their status labels from the metadata.csv file.
	    Iterable<SoundRecording> sounds = SoundRecording.readMetaDataSounds(srcDir);
	    
	    // Now write them out to the local directory along with the metadata file.
	    String destDir = ".";
	    int count = SoundRecording.writeMetaDataSounds(destDir, sounds).size();
	    System.out.println("Wrote " + count + " sounds and metadata.csv to " + destDir);
	}
}
