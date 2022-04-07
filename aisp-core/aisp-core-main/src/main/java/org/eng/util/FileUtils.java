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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
//import java.nio.file.Files;
//import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class FileUtils {
  
  public static File validateDir(String dirName) throws IOException {
    File dirFile = new File(dirName);
    FileUtils.validateDir(dirFile);
    return dirFile;
  }
  
  /**
   * Determine if the given path is absolute (on unix or windows).
   * @param path
   * @return
   */
  public static boolean isAbsolutePath(String path) {
	  return path != null && (path.startsWith("/") || path.startsWith("~")  || (path.indexOf(':') == 1));
  }
  
  /**
   * Make sure the directory exists, is in fact a directory and is readable.
   * @param dirFile
   * @throws IOException
   */
  public static void validateDir(File dirFile) throws IOException {
    if ( ! dirFile.exists()) {
      throw new IOException("Directory does not exist: "+dirFile.getPath());
    }
    if (! dirFile.isDirectory()) {
      throw new IOException("File is not a directory: "+dirFile.getPath());
    }
    if (! dirFile.canRead()) {
      throw new IOException("Directory cannot be read: "+dirFile.getPath());
    }
  }

  /** 
   * read a (small) text file into a String
   * @param fileName
   * @return
   * @throws IOException
   */
  public static String readTextFileIntoString(String fileName) throws IOException {
    return FileUtils.readTextFileIntoString(new File(fileName));
  }
  
  /**
   *  read a (small) text file into a String
   * @param file
   * @return
   * @throws IOException
   */
  public static String readTextFileIntoString(File file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    try {
		byte[] data = new byte[(int)file.length()];
		fis.read(data);
		return new String(data,"UTF-8");
    } finally {
    	fis.close();
    }
  }

  /** 
   * write a (small) string into a text file
   * @param text
   * @param file
   * @throws IOException
   */
  public static void writeStringToTextFile(String text, File file) throws IOException {
	PrintWriter out = new PrintWriter(file);
	try {
		out.println(text);
	} finally {
		out.close();
	}
	
  }

  /** 
   * write a (small) string into a text file
   * @param text
   * @param fileName
   * @throws IOException
   */
  public static void writeStringToTextFile(String text, String fileName) throws IOException {
    FileUtils.writeStringToTextFile(text,new File(fileName));
  }

  public static byte[] readFileIntoByteArray(String fileName) throws IOException {
    return FileUtils.readFileIntoByteArray(new File(fileName));
  }
  
  public static void writeByteArrayToFile(String fileName, byte[] data) throws IOException {
    OutputStream output = null;
    FileOutputStream fos = new FileOutputStream(fileName);
    try {
      output = new BufferedOutputStream(fos);
      output.write(data);
    }
    finally {
      if (output != null) 
    	  output.close();
      fos.close();
    }
  }
  
  public static byte[] readFileIntoByteArray(File file) throws IOException {
    
    DataInputStream inputStream = new DataInputStream(new FileInputStream(file));
    byte[] byteArray = new byte[(int)file.length()];
    try {
    	inputStream.readFully(byteArray);
    } finally {
    	inputStream.close();
    }
    
    return byteArray;
  }

  /**
   * delete a file - if it is a directory it will delete all contents first (recursively)
   * @param name
   */
  public static void deleteFile(String name) {
    FileUtils.deleteFile(new File(name));
  }

  /**
   * delete a file - if it is a directory it will delete all contents first (recursively)
   * @param file
   */
  public static void deleteFile(File file) {
//	if (file != null && (file.exists() || Files.isSymbolicLink(Paths.get(file.toURI())))) {	 // Don't use java.nio for Android API level 25 and below
	if (file != null && (file.exists() || isSymlink(file))) { 
		FileUtils.deleteDirContents(file);
		boolean r = file.delete();
	}
  }
  
  public static boolean isSymlink(File file) {
	  if (file == null)
	    throw new NullPointerException("File must not be null");
	  File canon;
	  try {
		  if (file.getParent() == null) {
			canon = file;
		  } else {
			File canonDir = file.getParentFile().getCanonicalFile();
			canon = new File(canonDir, file.getName());
		  }
		  return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
	  } catch (Exception e) {
		  return false;
	  }
	}

  /**
   * Recursively delete the contents of a directory, without deleting the directory itself.
   * @param file
   */
  public static void deleteDirContents(File file) {
	if (file != null && file.isDirectory()) {
      File[] fileList = file.listFiles();
      for (File fileInDir : fileList) {
        FileUtils.deleteFile(fileInDir);
      }
    }
  }
  
  /**
   * Get the list of directories inside the given path, which should be a directory.
   * @param dirName
   * @return never null.
   */
  public static List<String> getDirList(String dirName) {
	  ArrayList<String> dirList = new ArrayList<String>(); 
	  if (dirName != null) {
		  File dir = new File(dirName);
		  if (dir.exists() && dir.isDirectory()) {
		      File[] fileList = dir.listFiles();
		      for (File fileInDir : fileList) {
		    	  if (fileInDir.isDirectory()) {
		    		  dirList.add(fileInDir.getName());
		    	  }
		      }			  
		  }
	  }
	  return dirList;
  }
  
  /**
   * Get the list of non-directory files in the given path, which should be a existing directory.
   * @param dirName
   * @return never null.
   */
  public static List<String> getFileList(String dirName) {
	  ArrayList<String> dirList = new ArrayList<String>(); 
	  if (dirName != null) {
		  File dir = new File(dirName);
		  if (dir.exists() && dir.isDirectory()) {
		      File[] fileList = dir.listFiles();
		      for (File fileInDir : fileList) {
		    	  if (!fileInDir.isDirectory()) {
		    		  dirList.add(fileInDir.getName());
		    	  }
		      }			  
		  }
	  }
	  return dirList;
  }
  
  /** 
   * move a file to a certain directory
   * @param fileName
   * @param destDir
   * @throws IOException
   */
  public static void moveFile(String fileName, String destDir) throws IOException {
    File sourceFile = new File(fileName);
    File destFile   = new File(destDir + File.separator + sourceFile.getName()); 
    if ( ! sourceFile.renameTo(destFile)) {
      throw new IOException("Error moving "+fileName+" to "+destDir);
    }
  }

  public static void copyFile(String sourceFileName, String targetFileName) throws IOException
  {
	FileInputStream  inputStream  = new FileInputStream(sourceFileName);
	FileOutputStream  outputStream = new  FileOutputStream(targetFileName);
	try {
		byte[] byteBuf = new byte[2048];
		int numRead;
		while ((numRead = inputStream.read(byteBuf)) != -1) {
		  outputStream.write(byteBuf,0,numRead);
		}
	} finally {
		try {
			inputStream.close();
		} finally {
			outputStream.close();
		}
	}
  }

  /** 
   * create a temp dir in a parent directory
   * @param parentDir
   * @return
   * @throws IOException
   */
  public static File createTempDir(String parentDir) throws IOException {
    if (parentDir == null) {
      parentDir = System.getProperty("java.io.tmpdir");
    }
    File tempDir = new File(parentDir + File.separator + "temp-" + UUID.randomUUID().toString());
    if ( ! tempDir.mkdir()) {
      throw new IOException("Error creating temp directory: "+tempDir.getPath());
    }
    return tempDir;
  }
  
  /** 
   * create a temp dir in the java.io.tmpdir directory
   * @return
   * @throws IOException
   */
  public static File createTempDir() throws IOException {
    return FileUtils.createTempDir(null);
  }
  
  /** 
   * get a sub-directory; will create it if it does not exist
   * @param parentDirName
   * @param subDirName
   * @return
   * @throws IOException
   */
  public static File getSubDir(String parentDirName, String subDirName) throws IOException {
    return FileUtils.createDir(parentDirName + File.separator + subDirName);
  }
  
  /**
   * Create the directory with the given name, non-recursive.
   * @param dirName
   * @return
   * @throws IOException
   */
  public static File createDir(String dirName) throws IOException {
    File dir = new File(dirName);
    if ( ! dir.exists() || ! dir.isDirectory()) {
      if ( ! dir.mkdir()) {
        throw new IOException("Failed to create sub-directory: "+dir.getPath());
      }
    }
    return dir;
  }
  
	public static void createDirsToFile(String filename) {
		int lastPathSep = Math.max(filename.lastIndexOf("/"), filename.lastIndexOf("\\"));
		if (lastPathSep >= 0) {
			String path = filename.substring(0,lastPathSep);
			File f = new File(path);
			f.mkdirs();
		}
		
	}
  
  /**
   * get the file name extension INCLUDING the "."
   * @param fileName
   * @return "" if not present.
   */
  public static String getFileNameExtension(String fileName) {
    int indexOfExtension = fileName.lastIndexOf(".");
    if (indexOfExtension == -1) {
      return "";
    }
    return fileName.substring(indexOfExtension);
  }
  
  /** 
   * get a file name without its last extension
   * @param fileName
   * @return
   */
  public static String getFileNameWithoutExtension(String fileName) {
    int indexOfExtension = fileName.lastIndexOf(".");
    if (indexOfExtension == -1) {
      return fileName;
    }
    return fileName.substring(0,indexOfExtension);
  }
 
  /**
   * Get the name of the file without any directory information. 
   * Handles both / and \ separators.
   * @param fileName
   * @return never null.
   */
  public static String getFileName(String fileName) {
	 int index= fileName.lastIndexOf('/'); 
	 if (index < 0)
		 index = fileName.lastIndexOf('\\');
	 if (index >=0 )
		 fileName = fileName.substring(index+1);;
	 return fileName;
  }

  /**
   *  get a filename removing everythign after the first '.' and the '.'.
   * @param fileName
   * @return
   */
  public static String getFileNameWithoutAllExtensions(String fileName) {
    int indexOfExtension = fileName.indexOf(".");
    if (indexOfExtension == -1) {
      return fileName;
    }
    return fileName.substring(0,indexOfExtension);
  }
  
  public static final int RENAME_RETRY_COUNT = 10;
  
  public static boolean rename(String source, String target) {
    if (source.equals(target)) {
      return true;
    }
    return rename(new File(source), new File(target));
  }
  
  public static boolean rename(File source, File target) {
    if (!source.exists()) {
      return false;
    }
    if (target.exists()) {
      target.delete();
    }
    boolean success = false;
    for (int i = 0; !(success |= source.renameTo(target)) && i < RENAME_RETRY_COUNT; i++) {
      System.err.println(String.format("Error renaming file. Retrying %s out of %s times.", i+1, RENAME_RETRY_COUNT));
      System.gc();
      Thread.yield();
    }
    source.delete();
    return success;
  }
  
	public static void unZipFile(String zipFile, String outputDirName) {
		FileUtils.unZipFile(new File(zipFile),outputDirName);
	}
	
	public static void unZipFile(File zipFile, String outputDirName) {
		 
		byte[] buffer = new byte[1024];
	 
	    ZipInputStream zis = null; 
	    FileInputStream fis = null;
	    IOException caughtException = null; 
		try {
	 
			// create output directory is not exists
	    	File outputDir = FileUtils.createDir(outputDirName);
	    	// get the zip file content
	    	fis = new FileInputStream(zipFile);
	    	zis = new ZipInputStream(fis);

	    	// loop over the zipped file list entries
	    	ZipEntry ze  = null;
	    	while ((ze = zis.getNextEntry()) != null) {
	 
	    	   String fileName = ze.getName();
	           File newFile = new File(outputDir.getPath() + File.separator + fileName);
	 
	           System.out.println("file unzip : "+ newFile.getAbsoluteFile());
	 
	            // ccreate all non exists folders else we get a FileNotFoundException for compressed folders
	            new File(newFile.getParent()).mkdirs();
	            
	            FileOutputStream fos = new FileOutputStream(newFile);             
	            try {
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
	            } finally {
	            	fos.close();   
	            }
	    	}
	 
	        zis.closeEntry();
	 
	    	System.out.println("Done");
	 
	    }catch(IOException ex){
	    	caughtException = ex;
	       ex.printStackTrace(); 
	    } finally {
	    	try {
				if (zis != null)
					zis.close();
				else if (fis != null)
					fis.close();
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    }
	       
	}
	
	/**
	  * Read the stream until EOF, but do not close the stream.
	  * @param is
	  * @return never null.
	  * @throws IOException
	  */
	 public static byte[] readByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] byteArray = new byte[1000];
		try {
		    int count;
		    while ( (count = is.read(byteArray, 0, byteArray.length)) != -1) {
		    	bos.write(byteArray,0,count);
		    }
		    byteArray = bos.toByteArray();
		} finally {
		    bos.close();
		}
		    
		return byteArray;
	  }

}
