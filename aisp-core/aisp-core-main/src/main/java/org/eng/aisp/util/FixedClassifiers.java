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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.eng.aisp.classifier.IFixedClassifier;
import org.eng.util.ClassUtilities;

/**
 * Provides general operations on FixedClassifiers, especially reading/writing.
 * @author dawood
 *
 */
public class FixedClassifiers {

	/**
	 * Write the given classifier to the given stream.
	 * The stream is NOT closed upon return.
	 * @param ostream
	 * @param classifier
	 * @throws IOException
	 */
	public static void write(OutputStream ostream, IFixedClassifier<?> classifier) throws IOException {
		byte[] b = ClassUtilities.serialize(classifier);
		ostream.write(b);
		ostream.flush();
	}

	public static void write(String fileName, IFixedClassifier<?> classifier) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		write(fos,classifier);
		fos.close();
	}

	/**
	 * Read the classifier from the given stream.
	 * @param istream not closed on return.
	 * @return never null 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static IFixedClassifier<?> read(InputStream istream) throws IOException, ClassNotFoundException {
		byte b[] = null;

		final int BUFSIZE=8192;

		byte buf[] = new byte[BUFSIZE];
		int count;
		do {
			count = istream.read(buf,0,BUFSIZE);
			if (count > 0) {
				if (b == null)  {
					b = new byte[count];
					System.arraycopy(buf, 0, b, 0, count);
				} else {
					int oldLen = b.length;
					b = Arrays.copyOf(b, b.length + count);
					System.arraycopy(buf, 0, b, oldLen, count);
				}
			}
		} while (count > 0);
		Object o = ClassUtilities.deserialize(b);
		if (o != null && o instanceof IFixedClassifier) {
			return (IFixedClassifier<?>)o;
		} else {
			String className;
			if (o == null)
				className = "null";
			else
				className = o.getClass().getName();
			throw new IOException("Deserialized object (" + className + ") is not an instance of " + IFixedClassifier.class.getName());
		}
	}

	/**
	 * Read the fixed classifier from the given file created with {@link #write(String, IFixedClassifier)}.
	 * @param fileName
	 * @return never null
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static IFixedClassifier<?> read(String fileName) throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream(fileName);
		IFixedClassifier<?> fc = read(fis);
		fis.close();
		return fc;
	}
}
