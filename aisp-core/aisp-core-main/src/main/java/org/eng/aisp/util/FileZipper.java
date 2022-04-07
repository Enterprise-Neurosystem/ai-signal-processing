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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;



/**
 * A utility class to make creating zip files less cumbersome.
 * 
 * @author dawood
 *
 */
public class FileZipper {

	protected ZipOutputStream zipStream;
	protected final Charset charset;
	public FileZipper(OutputStream os)  {
		this(os,"UTF-8");
	}

	public FileZipper(OutputStream os, String charsetName)  {

		if (os == null)
			throw new IllegalArgumentException("OutputStream can not be null");

		zipStream = new ZipOutputStream(os);
		this.charset = Charset.forName(charsetName);
	}
	
	protected static byte[] readEntry(ZipInputStream zis) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int count;
		while ((count = zis.read(buf)) > 0) {
			bos.write(buf,0, count);
		}
		bos.close();
		return bos.toByteArray();
	}

	private static void writeContents(ZipInputStream zipInputStream, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
		writeContents(zipInputStream, fileOutputStream);
	}

	/**
	 * @param zipInputStream
	 * @param fileOutputStream
	 * @throws IOException
	 */
	private static void writeContents(ZipInputStream zipInputStream, OutputStream outputStream) throws IOException {
		try {
			int len;
			byte[] content = new byte[1024];
			while ((len = zipInputStream.read(content)) > 0) {
				outputStream.write(content, 0, len);
			}
		} finally {
			outputStream.close();
		}
	}

	/**
	 * 
	 * @param zip stream of zipped bytes to explode into the given directory. Closed upon return.
	 * @param outputDirectory	destination of contents of zip.  may be null, in which case '.' is assumed.
	 * If not null, then directory must already exist.
	 * @return the list of names/entries in the zip file in the order they appeared there.
	 * @throws IOException
	 */
    public static List<String> unZip(InputStream zip, String outputDirectory) throws IOException {

    	final String outDir;
    	if (outputDirectory == null)
    		outDir = ".";
    	else
    		outDir = outputDirectory;
    	
//        ZipInputStream zipInputStream = new ZipInputStream(zip) ;

        StreamProvider provider = new StreamProvider() {
			@Override
			public OutputStream getOutputStream(String zipFile) throws FileNotFoundException {
				File unZippedFile = new File(outDir + File.separator + zipFile);
                //Create output directory
                unZippedFile.getParentFile().mkdirs();
				return new FileOutputStream(unZippedFile);
			}
        };
        	
        return  unZip(zip, provider);
    }
    
    public interface StreamProvider {
    	/**
    	 * Create a new outputStream for the file found in the zip.
    	 * @param file
    	 * @return a new OutputStream that will be closed by the caller.
    	 * @throws FileNotFoundException
    	 */
    	public OutputStream getOutputStream(String file) throws FileNotFoundException ;
    }

    /**
     * 
     * @param zip
     * @param provider Called on a new file found in the zip to creaet an OutputStream to which the unzipped bytes are sent.
     * @return
     * @throws IOException
     */
    public static List<String> unZip(InputStream zip, StreamProvider provider) throws IOException {

    	
    	List<String> fileNames = new ArrayList<String>();
        ZipInputStream zipInputStream = new ZipInputStream(zip) ;

        try {
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
//                System.out.printf("%d. Extracting content:%s\n",numberOfFiles, zipEntry.getName());
            	OutputStream ostream = provider.getOutputStream(zipEntry.getName());
                fileNames.add(zipEntry.getName());

                //Write contents to file
                writeContents(zipInputStream, ostream);
//                System.out.printf("Written content to file:%s\n\n",unZippedFile.getCanonicalPath());

                //Close current entry
                zipInputStream.closeEntry();
            }
//            System.out.printf("Finished execution, UnZipped file count:%d", numberOfFiles);
        } finally {
        	zipInputStream.close();
        }
        return fileNames;
    }

	/**
	 * A container class to hold the data provided by nextEntry().
	 * @author dawood
	 */
	public static class EntryData {
		public final String entryName;
		public final byte[] entryContents;
		
		public EntryData(String entryName, byte[] entryContents) {
			this.entryName = entryName;
			this.entryContents = entryContents;
		}

	}

	/**
	 * A helper method to get the filename and contents from the next entry.
	 * Use might be as follows:
	 * <pre>
	 * InputStream is = ...
	 * ZipInputStream stream = new ZipInputStream(is);
	 * EntryData ed;
	 * while ((ed = nextEntry(stream)) != null) {
	 *    String name = ed.entryName; 
	 *    data = ed.entryContents; 
	 *    ...
	 * }
	 * </pre>
	 * @param stream
	 * @return null when no more entries are available.
	 * @throws IOException
	 */
	public static EntryData nextEntry(ZipInputStream stream) throws IOException {
		ZipEntry entry = stream.getNextEntry();
		if (entry == null)
			return null;
		String name = entry.getName();
		byte[] data = readEntry(stream);
		return new EntryData(name, data);
	}


	/**
	 * @param fileName
	 * @param contents
	 * @throws IOException
	 */
	public void addFile(String fileName, byte[] contents) throws IOException {
		if (zipStream == null)
			throw new IOException("zip stream has been finalized/closed");
		ZipEntry e = new ZipEntry(fileName);
		e.setSize(contents.length);
		zipStream.putNextEntry(e);
		zipStream.write(contents);
		zipStream.closeEntry();
	}
	
	public void addFile(String fileName, String contents) throws IOException {
		byte[] b = contents.getBytes(charset);
		addFile(fileName,b);
	}
	
	/**
	 * finalize the ZIP stream and its underlying stream, including closing the ZipOutputStream and thus the OutputStream that is being used by this instance.
	 * Once this is called, future calls to {@link #addFile(String, byte[])} will throw an exception. 
	 */
	public synchronized void finalizeZip() throws IOException {
		if (zipStream == null)
			return;
		
		zipStream.flush();
		//add stream.finish() fix for model download bowen 2019-03-19
		zipStream.finish();
		zipStream.close();
		
		// Signal that this instance can no longer be used.
		zipStream = null;
	}

}
