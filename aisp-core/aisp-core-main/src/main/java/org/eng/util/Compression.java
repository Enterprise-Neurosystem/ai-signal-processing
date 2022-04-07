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
package org.eng.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compression {

	public static byte[] compress(byte[] input) throws Exception {
		byte[] compressedData = null;
	
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(input.length);
			GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
			zipStream.write(input);
			zipStream.close();
			byteStream.close();
			compressedData = byteStream.toByteArray();
		} catch (Exception e) {
			throw e;
		}
		return compressedData;
	}
	
	public static byte[] decompress(byte[] input) throws Exception {
		byte[] uncompressedData = null;
	
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length*2);
			ByteArrayInputStream byteStream = new ByteArrayInputStream(input);
			GZIPInputStream zipStream = new GZIPInputStream(byteStream);
			byte[] b = new byte[input.length];
			int count;
			while ((count = zipStream.read(b)) != -1) {
				bos.write(b, 0, count);
			}
			zipStream.close();
			byteStream.close();
			uncompressedData = bos.toByteArray();
		} catch (Exception e) {
			throw e;
		}
		return uncompressedData;
	}
}
