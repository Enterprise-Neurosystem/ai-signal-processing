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
package org.eng.aisp.util;

import java.io.IOException;
import java.net.URL;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;

public interface IPortLayer {

	public String toBase64(byte[] bytes) ;

	public byte[] fromBase64(String base64);
	
	/**
	 * Convert the MP3 at the given URL to a SoundClip.
	 * @param startTimeMsec
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws AISPException
	 */
	public SoundClip MP3toPCM(double startTimeMsec, URL url) throws IOException, AISPException;

}
