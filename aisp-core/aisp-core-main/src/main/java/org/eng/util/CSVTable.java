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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;




/**
 * Provides operations on a comma-separated value file.
 * The file must include the names of the columns in the first row. 
 * Any number of rows may follow.  All rows must have the same number of columns as the header row.
 * Rows may be iterated over or may be searched using database join-like features.
 * Any column of data may also be extracted.
 * The values in rows are accessed by case-insenstive keys defined by the column headers.
 * @author dawood
 *
 */
public class CSVTable implements Iterable<CaseInsensitiveMap> {

	protected final List<String> columnNames = new ArrayList<String>();
	protected final List<CaseInsensitiveMap> mappedRowList = new ArrayList<CaseInsensitiveMap>();
	protected final String valueWrapper;
	
	public final static String COMMA_STR = ",";
	public final static char COMMA_CHAR = ',';
	public final static char ESCAPE_CHAR = '\\';
	
	public CSVTable() {
		this((String[])null,null);
	}	
	public CSVTable(String[] columnNames, List<Map<String,String>> rows) { 
		this(columnNames, rows, null);
	}

	public CSVTable(String[] columnNames, List<Map<String,String>> rows, String valueWrapper) { 
		if (columnNames != null) {
			for (String s : columnNames) {
				this.columnNames.add(s);
			}
		}
		if (rows != null) {
			for (Map<String, String> row : rows) {
				CaseInsensitiveMap cim = new CaseInsensitiveMap();
				cim.putAll(row);
				this.mappedRowList.add(cim);
			}
		}
		this.valueWrapper = valueWrapper;
	}
	
	public CSVTable(String[] columnNames) {
		this(Arrays.asList(columnNames), null);
	}
	
	public CSVTable(List<String> columnNames) {
		this(columnNames, null);
	}

	public CSVTable(List<String> columnNames, List<Map<String,String>> rows) { 
		this(columnNames, rows,null);

	}

	public CSVTable(List<String> columnNames, List<Map<String,String>> rows, String valueWrapper) { 
		if (columnNames != null) {
			for (String s: columnNames)
				this.columnNames.add(s);
		}
		if (rows != null) {
			for (Map<String, String> row : rows) {
				CaseInsensitiveMap cim = new CaseInsensitiveMap();
				cim.putAll(row);
				this.mappedRowList.add(cim);
			}
		}
		this.valueWrapper = valueWrapper;
	}


	/**
	 * 
	 * @param colName
	 * throws IllegalArgumentException if column does not exist.
	 */
	protected void validateColumnExists(String colName) {
		for (String name : columnNames) {
			if (name.equalsIgnoreCase(colName))
				return;
		}
		throw new IllegalArgumentException("Column " + colName + " not found.");
	}

	private void validateColumnNotExist(String colName) {
		for (String name : columnNames) {
			if (name.equalsIgnoreCase(colName))
				throw new IllegalArgumentException("Column " + colName + " already exists.");
		}
	}

	/**
	 * Get all the values from the named column.
	 * @param columnName
	 * @return never null.
	 * @throws IllegalArgumentException of column does not exist.
	 */
	public List<String> getColumn(String columnName) {
		validateColumnExists(columnName);

		List<String> column = new ArrayList<String>();
		
		for  (CaseInsensitiveMap row : mappedRowList) {
			String value = (String) row.get(columnName);
			column.add(value);
		}
		
		return column;
	}
	
	/**
	 * Get the names of the columns.
	 * @return never null.
	 */
	public List<String> getColumnNames() {
		return columnNames;
		
	}
	
	/**
	 * Get the 0-based index of the given named column.
	 * @param colName case-insensitive spec for the column name.
	 * @return -1 if not found, otherwise a 0-based index.
	 */
	public int getColumnIndex(String colName) {
		int i=0 ;
		for (String name : getColumnNames()) {
			//System.out.println("Name:" + name +  " colName:" + colName);
			if (name.equalsIgnoreCase(colName))
				return i;
			i++;
		}
		return -1;
	}
	
	/**
	 * Determine if the table has the given column.
	 * A convenience method on {@link #getColumnIndex(String)}.
	 * @param colName case-insensitive spec for the column name.
	 * @return
	 */
	public boolean hasColumn(String colName) {
		return getColumnIndex(colName) >= 0;
	}
	
	/**
	 * Search for rows having the 1 or more matching column values.  
	 * @param columnValues an array of strings with the the even elements the names of columns and the odd elements the values for the
	 * column named in the preceding elements.
	 * @return never null, but perhaps empty.
	 */
	public CSVTable  getRows(String...columnNameValues) {
		CaseInsensitiveMap matches = new CaseInsensitiveMap();
		if ((columnNameValues.length & 1) != 0)
			throw new IllegalArgumentException("Number of strings must be even");
		for (int i=0 ; i<columnNameValues.length ; ) {
			matches.put(columnNameValues[i], columnNameValues[i+1]);
			i += 2;
		}
		return getRows(matches);
	}

//	/**
//	 * Search for rows having the given named column value
//	 * @param columnName column to search
//	 * @param columnValue value of column in the row.
//	 * @return never null, but perhaps empty.
//	 */
//	public CSVTable getRows(String columnName, String columnValue) {
//		return getRows(new String[] { columnName, columnValue });
//	}

	/**
	 * Search for rows having the given named values.
	 * @param where a map of column names to values that must be matched in the returned list of rows.
	 * May be null or empty, in which case all rows are returned.
	 * @return never null, but perhaps empty.
	 */
	public CSVTable getRows(Map<String,String> where) {
		return getRows(where,false, false);
	}

	/**
	 * @param where a set of columnName=value pairs such that all must be matched (and'ed together) to select a row.
	 * @param notWhere  if false, then the rows returned match the where clause.  If true, then get the rows
	 * that don't match any 1 or all of the columnName=value pairs.
	 * @return a new table.
	 */
	protected CSVTable getRows(Map<String,String> where, boolean notWhere, boolean removeMatched) {

		List<Map<String,String>> matchedRows = new ArrayList<Map<String,String>>();
		
		for (CaseInsensitiveMap row : mappedRowList) {
			boolean isRowMatch = true;
			if (where != null) {
				for (String key : where.keySet()) {
					String matchValue = where.get(key);
					String rowValue = (String) row.get(key);
					if (!matchValue.equalsIgnoreCase(rowValue)) {
						isRowMatch = false;
						break;
					}
				}
			}
			boolean match = (isRowMatch && !notWhere)   || (!isRowMatch && notWhere) ;
			if (match) 
//			if (isRowMatch && !notWhere) 
				matchedRows.add(row);
//			else if (!isRowMatch && notWhere) 
//				matchedRows.add(row);
		}
		if (removeMatched) 
			this.mappedRowList.removeAll(matchedRows);
		return new CSVTable(this.columnNames, matchedRows);
		
	}
	
	/**
	 * Select rows that don't match any one of the name=values pairs provided and remove them from this instance.
	 * @param where a map of column=value pairs to match when selecting rows for removal.
	 * @return a new table containing the rows that were removed from this instance. 
	 */
	public CSVTable removeRows(Map<String, String> where) {
		return getRows(where,false, true);
	}

	/**
	 * Get an iterator over the rows in the file.
	 * The returned iterator does not include the header.
	 */
	@Override
	public Iterator<CaseInsensitiveMap> iterator() {
		return this.mappedRowList.iterator();
	}
	
	/**
	 * Append a column of values to this instance.
	 * @param columnName name of column
	 * @param values values in column.  The number of values must match the number of rows.
	 * @throws IllegalArgumentException of column exists.
	 */
	public void appendColumn(String columnName, List<Object> values) {
//		try {
			validateColumnNotExist(columnName);
//		} catch (Exception e) {
//			return;
//		}
		if (mappedRowList.size() == 0) { 	// no rows in table yet.
			for (Object v : values) 
				mappedRowList.add(new CaseInsensitiveMap());
		} 

		if (mappedRowList.size() != values.size())
			throw new IllegalArgumentException("Number of column values (" + values.size() + ") does not match number of rows(" + mappedRowList.size() + ")");
		Iterator<Object> valIterator = values.iterator();
		for (CaseInsensitiveMap row : mappedRowList) 
			row.put(columnName,  valIterator.next());
		columnNames.add(columnName);
	}
	
	/**
	 * Append a empty column of values to this instance.
	 * @param columnName name of column
	 */
	public void appendColumn(String columnName) {
		int count = mappedRowList.size();
		String emptyValue = "";
		List<Object> columnValues = new ArrayList<Object>();
		for (int i=0 ; i<count ; i++)
			columnValues.add(emptyValue);
		appendColumn(columnName, columnValues);
	}
	
	/**
	 * Write the table as CSV to the file with the given name. 
	 * The valueWrapper that was used to read the table is used to write the table.
	 * @param filename
	 * @throws IOException
	 */
	public void write(String filename) throws IOException {
		FileWriter fw = new FileWriter(filename);
		write(fw);
		fw.close();
	}

	public void write(String filename, String valueWrapper) throws IOException {
		FileWriter fw = new FileWriter(filename);
		write(fw, valueWrapper);
		fw.close();
	}

	/**
	 * Write the table as CSV to the given writer which is closed upon return.
	 * The valueWrapper that was used to read the table is used to write the table.
	 * @param w writer that is not closed on return.
	 * @throws IOException
	 */
	public void write(Writer w) throws IOException {
		this.write(w,this.valueWrapper);
	}
	
	/**
	 * @param w not closed on return.
	 * @param valueWrapper
	 * @throws IOException
	 */
	public void write(Writer w, String valueWrapper) throws IOException {
		writeRow(w, this.columnNames, valueWrapper);
		List<String >rowValues = new ArrayList<String>();

		for (CaseInsensitiveMap row : mappedRowList) {
			rowValues.clear();
			for (String col : this.columnNames) {				
				String value = row.get(col).toString();
				if (value == null)	// Should never get here, but
					value = "";
				rowValues.add(value);
			}
			writeRow(w, rowValues, valueWrapper);
		}
		w.flush();
//		w.close();
	}

	private void writeRow(Writer w, List<String> values, String valueWrapper) throws IOException {
		StringBuilder sb = new StringBuilder();
		if (valueWrapper == null)
			valueWrapper = "";
		boolean first=true;
		for (String s : values) {
			if (!first)
				sb.append(COMMA_STR);
			sb.append(valueWrapper);
			s = StringUtil.escape(s, COMMA_STR, ESCAPE_CHAR);
			sb.append(s);
			sb.append(valueWrapper);
			first = false;
		}
		sb.append('\n');
		w.write(sb.toString());
		
	}



	/**
	 * Write the table to the given stream. 
	 * The valueWrapper that was used to read the table is used to write the table.
	 * @param out stream that is not closed on return.
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		write(new OutputStreamWriter(out));
	}

	/**
	 * Reader the table from the given reader.
	 * @param reader input source providing the CSV text data.
	 * @param headerLines the number of lines to ignore before reading columnar data.  The last
	 * of the header lines is expected to contain the actual column names.  if this is zero,
	 * then column names are set to the 1-based indexes of the columns. 
	 * @return never null
	 * @throws IOException
	 */
	public static CSVTable read(Reader reader, int headerLines) throws IOException {		
		return read(reader, headerLines, null );
	}

	/**
	 * Read the table from the given reader.
	 * @param reader
	 * @param headerLines the number of lines to ignore before reading columnar data.  The last
	 * of the header lines is expected to contain the actual column names.  if this is zero,
	 * then column names are set to the 1-based indexes of the columns. 
	 * @param wrapper optional character that surrounds fields to allow embedded commas.
	 * Use null to try and automatically determine the wrapper when present.  If auto-determining
	 * the character then the character is not allowed to be a character that is valid in a Java identifier. 
	 * Common delimiters are single quote, double quote, or pipe (|).
	 * @return never null.
	 * @throws IOException
	 */
	public static CSVTable read(Reader reader, int headerLines, String wrapper) throws IOException {		
		BufferedReader br = new BufferedReader(reader);
		List<Map<String, String>> mappedRowList = new ArrayList<Map<String,String>>();
		String[] columnNames = null;	

			
		String line = null;
		int lineNumber = 0; // 1-based line number 
		int firstDataLine = headerLines + 1;
		while ((line = br.readLine()) != null) {
			lineNumber++;
			if (lineNumber < headerLines)  	// Skip the first n-1 lines 
				continue ;
			
			if ((lineNumber == firstDataLine || lineNumber == headerLines) && wrapper == null)
				wrapper = determineWrapper(line);
			String[] values = getFields(line, wrapper, lineNumber);
			if (lineNumber == headerLines) {	// Last header line contains the columns
				columnNames = new String[values.length];
				for (int i = 0; i < values.length; i++)
					columnNames[i] = "undefined"; 
				for (int i = 0; i < values.length; i++)
					columnNames[i] = values[i].replace("\"", "").trim();	// TODO not sure this is necessary now that we support wrappers.
			} else { // No header or data row
//				if (columnNames == null && lineNumber == firstDataLine) { // Create pseudo column names
				if (columnNames == null) { // Create pseudo column names
					columnNames = new String[values.length];
					for (int i = 0; i < values.length; i++)
						columnNames[i] = String.valueOf(i + 1);
				}
				Map<String, String> map = new HashMap<String, String>();
				if (values.length < columnNames.length)
					throw new IllegalArgumentException("row at line number " + lineNumber
							+ " does not have  at least the number of fields that the header has");
				for (int i = 0; i < columnNames.length; i++) {
//					values[i] = values[i].replace("\"", "").trim();
					values[i] = values[i].trim();
					// System.out.println("i=" + i + " fieldName=" +
					// fieldNames[i] + " value=" + values[i]);
					map.put(columnNames[i], values[i]);
				}
				mappedRowList.add(map);
			}
			if (values.length == 0)
				throw new IOException("No columns found in row " + lineNumber);
		}
		br.close();
		return new CSVTable(columnNames, mappedRowList, wrapper);
	}

	/**
	 * Try to automatically figure out the character that is being used to surround field/cell values.
	 * Allowed characters are those that are not allowed as part of a Java identifier.
	 * For example ', |, ", .
	 * @param line
	 * @return null if no field wrapper or could not be determined.
	 */
	private static String determineWrapper(String line) {
		line = line.trim();
		String candidate = line.substring(0,1);
		char c = candidate.charAt(0);
		if (Character.isJavaIdentifierPart(c) || candidate.equals(COMMA_STR))
			return null;
		
		// The first and last character in the line must be equal.
		int lineLen = line.length();
		String last = line.substring(lineLen-1, lineLen);
		if (!last.equals(candidate))
			return null;
		
		return candidate;
	}

	/**
	 * @param line
	 * @return
	 * @throws IOException 
	 */
	private static String[] getFields(String line, String wrapper, int lineNumber) throws IOException {
		String[] values;
		if (wrapper == null) {	// Old way
//			String cvsSplitBy = ",";
			line = line.replaceAll(",,", ", ,");
			line = line + " ";	// In case last character is the , 
			List<String> valueList = new ArrayList<String>();
			String currentValue = null;
			int len = line.length();
			boolean wasEscape = false, wasValueSeparator = false;
			for (int i=0 ; i<len ; i++) {
				char c = line.charAt(i);
				if (wasEscape) {		// Always slurp the next character.
					if (currentValue == null)
						currentValue = "";
					currentValue += c;
					wasEscape = false;
					wasValueSeparator = false;
				} else if (c == COMMA_CHAR) {	// Value is complete
					if (currentValue != null) {	// Just finished value.
						valueList.add(currentValue);
						currentValue = null;
					}
					wasEscape = false;
					wasValueSeparator = true;
				} else if (c == ESCAPE_CHAR) {
					wasEscape = true;
					wasValueSeparator = false;
				} else {		// A good character to keep.
					if (currentValue == null)
						currentValue = "";
					currentValue += c;
					wasEscape = false;
					wasValueSeparator = false;
				}
			}
			if (currentValue != null) 
				valueList.add(currentValue);
			values = new String[valueList.size()];
			valueList.toArray(values);
				
		} else {
			List<String> wvalues = new ArrayList<String>();
			int wrapperLen = wrapper.length();
			while (line.length() > 0) {
				int[] startend = getFirstField(line, wrapper, lineNumber);
				if (startend == null)
					break;
				String v;
				if (startend[1] == -1) {
					v = "";
					line = line.substring(startend[0] + 2*wrapper.length(), line.length());
				} else {
					v = line.substring(startend[0], startend[1] + 1);
					v = v.replaceAll("\\\\" + wrapper, wrapper);
					line = line.substring(startend[1] + wrapperLen + 1);
//					if (line.length() > 0)
//						line.substring(1);	// Skip over the wrapper at the end of this field.
				}
				wvalues.add(v);	
			}
			values = new String[wvalues.size()];
			wvalues.toArray(values);
		}
		return values;
	}

	/**
	 * @param line 
	 * @param wrapper
	 * @return null if not present, or array of length 2 that contains a) the start and end indexes of the field without its wrappers.
	 * or b) the index of the first wrapper,-1 if field is zero length.
	 * @throws IOException 
	 */
	private static int[] getFirstField(String line, String wrapper, int lineNumber) throws IOException {
		int start = 0; 
		int end; 
//		System.out.println("input line=|" + line + "|");
		if (wrapper != null) {
			int wrapperLen = wrapper.length();
			start = findNextWrapper(line, wrapper, start);
			if (start < 0) { 	// Not found
//				System.out.println("returning null");
				return null;
			} 
			start += wrapperLen;	// Go after the wrapper;
			end = start + wrapperLen; 
			if (end <= line.length() && line.substring(start, end).equals(wrapper)) {
				// Empty value.
				start = start  - wrapper.length();
				end = -1;
			} else {
				end =   findNextWrapper(line, wrapper, start); 
				if (end == -1)
					throw new IOException("Missing terminating wrapper at line " + lineNumber + " at/after character " + start);
				end--;		// Get in front of the wrapper;
			}
		} else {
			start = line.indexOf(COMMA_STR);
			if (start < 0)
				start = 0;
			// TODO: Need to fix to find first _non-escaped comma_
			end = line.indexOf(COMMA_STR, start);
			if (end < 0)
				end = line.length();
		}
//		System.out.println("start=" + start + ", end=" + end);
		return new int[] { start, end }; 
	}

	/**
	 * Get the index of the first wrapper in the given line at or after the start index, that isn't escaped by backslash.
	 * @param line
	 * @param wrapper
	 * @param start
	 * @return -1 if not found.
	 */
	private static int findNextWrapper(String line, String wrapper, int start) {
		int index = -1;
		while (start < line.length()) {
			index = line.indexOf(wrapper, start);
			if (index >= 0) {
				if (index != 0 && line.charAt(index-1) == '\\') 
					start = index + 1;
				else
					break;
			} else {
				break;
			}
		}
		return index;
	}

	/**
	 * Reader the table from the given file.
	 * @param filename the file to read the CSV text from. 
	 * @param headerLines the number of lines to ignore before reading columnar data.  The last
	 * of the header lines is expected to contain the actual column names.  if this is zero,
	 * then column names are set to the 1-based indexes of the columns. 
	 * @return never null
	 * @throws IOException
	 */
	public static CSVTable readFile(String filename, int headerLines) throws IOException {
		return readFile(filename, headerLines, null);
	}

	/**
	 * Read the table from the given file.
	 * @param filename
	 * @param headerLines the number of lines to ignore before reading columnar data.  The last
	 * of the header lines is expected to contain the actual column names.  if this is zero,
	 * then column names are set to the 1-based indexes of the columns. 
	 * @param valueWrapper optional character surrounding records in file. Provide null to indicate there is
	 * no wrapper around fields.
	 * @return never null.
	 * @throws IOException
	 */
	public static CSVTable readFile(String filename, int headerLines, String valueWrapper) throws IOException {
		File file =  new File(filename);
		return read(new FileReader(file), headerLines, valueWrapper);
	}
	
//	/**
//	 * Reader the table from the given file.
//	 * @param filename the file to read the CSV text from. 
//	 * @param hasHeader true if the 1st line of the CSV file specifies the column names.  If false, then
//	 * the row data starts on the first line and column names will be automatically generated from the column index starting at 1.
//	 * @param isResourceDirRelative this is provided for used in a web application environment.  If this is true and the
//	 * filename is not absolute, then the file will be read relative to the web application's <code>resources</code> directory.
//	 * @return never null
//	 * @throws IOException
//	 */
//	public static CSVTable readFile(String filename, boolean hasHeader, boolean isResourceDirRelative) throws IOException {
//		File f = new File(filename);
//		if (isResourceDirRelative && !f.isAbsolute())
//			filename = Initializer.getResourcesDir() + filename;
//		return readFile(filename, hasHeader);
//	}

	public int getRowCount() {
		return this.mappedRowList.size();
	}

	/**
	 * Get the row with the given index.
	 * @param index 0-based index into the list of rows.
	 * @return never null.
	 */
	public CaseInsensitiveMap getRow(int index) {
		return this.mappedRowList.get(index);
	}

	public String write() {
		StringWriter sw = new StringWriter();
		try {
			this.write(sw);
			return sw.toString();
		} catch (IOException e) {
			return null; 	// never get here right?
		}
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 5;
		return "CSVTable [columnNames="
				+ (columnNames != null ? columnNames.subList(0, Math.min(columnNames.size(), maxLen)) : null)
				+ ", mappedRowList="
				+ (mappedRowList != null ? mappedRowList.subList(0, Math.min(mappedRowList.size(), maxLen)) : null)
				+ "]";
	}

//	private double columnToDouble(String columnName, Function<List<Double>, Double> f) {
//		List<Double> columnValues = new ArrayList<Double>();
//		
//		for (CaseInsensitiveMap row : this.mappedRowList) {
//			String value = row.get(columnName).toString();
//			Double zero = new Double(0);
//			Double d;
//			try {
//				d = Double.valueOf(value);
//			} catch (Exception e) {	// format error we treat it as zero
//				d = zero; 
//			}
//			columnValues.add(d);
//		}
//		columnValues.stream().reduce(0.0, (x,y) -> x+y);
//		return f.apply(columnValues);
//	}
//	
//	private double columnToDouble(String columnName, double initialValue, BinaryOperator<Double> f) {
//		this.validateColumnExists(columnName);
//		List<Double> columnValues = new ArrayList<Double>();
//		
//		for (CaseInsensitiveMap row : this.mappedRowList) {
//			String value = row.get(columnName).toString();
//			Double zero = new Double(0);
//			Double d;
//			try {
//				d = Double.valueOf(value);
//			} catch (Exception e) {	// format error we treat it as zero
//				d = zero; 
//			}
//			columnValues.add(d);
//		}
//		return columnValues.stream().reduce(initialValue, f); 
//	}
	
//	public class SumFunction implements Function<List<Double>, Double> {
//
//		@Override
//		public Double apply(List<Double> t) {
//			double sum = 0;
//			for (Double v: t)
//				sum += v;
//			return sum;
//		}
//		
//	}

//	/**
//	 * Get the average of the values in a column.
//	 * Columns that do not have number values, are taken as 0.
//	 * @param columnName name of column from which values are taken.
//	 * @return
//	 */
//	public double average(String columnName) {
//		return sum(columnName) / this.getRowCount(); 
//	}

//	/**
//	 * Get the sum of the values in a column.
//	 * Columns that do not have number values, are taken as 0.
//	 * @param columnName name of column from which values are taken.
//	 * @return
//	 */
//	public double sum(String columnName) {
//		return columnToDouble(columnName, 0.0, (x,y) -> x+y); 
//	}
//
//	/**
//	 * Get the minimum of the values in a column.
//	 * Columns that do not have number values, are taken as 0.
//	 * @param columnName name of column from which values are taken.
//	 * @return
//	 */
//	public double min(String columnName) {
//		return columnToDouble(columnName, Double.MAX_VALUE, Double::min); 
//	}
//	/**
//	 * Get the minimum of the values in a column.
//	 * Columns that do not have number values, are taken as 0.
//	 * @param columnName name of column from which values are taken.
//	 * @return
//	 */
//	public double max(String columnName) {
//		return columnToDouble(columnName, Double.MIN_VALUE, Double::max); 
//	}

	/**
	 * Get the unique values out of a given column.
	 * @param columnName
	 * @return never null.
	 * @throws IllegalArgumentException of column does not exist.
	 */
	public List<String> unique(String columnName) {
		validateColumnExists(columnName);
		List<String> values = new ArrayList<String>();
		for (CaseInsensitiveMap row : this) {
			String value = row.get(columnName).toString();
			if (values != null && !values.contains(value))
				values.add(value);
		}
		return values;

	}

	/**
	 * Get the current number of columns.
	 * @return zero or more
	 */
	public int getColumnCount() {
		return this.columnNames.size();
	}

	/**
	 * Add the given row to the end of the table.
	 * The row must have keys matching each of the columns currently in the table.
	 * Keys are case-insensitive.
	 * If this is the first row and columns have not been defined, then define the columns
	 * as the sorted list of keys from the given row.
	 * @param row the row to append to the table.
	 */
	public void appendRow(Map<String,Object> row) {
		// Set up columns if this is the first row and no columns have been defined yet
		if (this.getRowCount() == 0 && this.getColumnCount() == 0) {
			List<String> cnames = new ArrayList<String>();
			cnames.addAll(row.keySet());
			Collections.sort(cnames);
			this.columnNames.addAll(cnames);
		} else if (row.size() < this.getColumnCount())
			throw new IllegalArgumentException("Number of elements in row is less than the number of columns");

		CaseInsensitiveMap inRow = new CaseInsensitiveMap(row);
		CaseInsensitiveMap outRow = new CaseInsensitiveMap();
		for (String col : this.getColumnNames()) {
			Object value = inRow.get(col);
			if (value != null) {
				outRow.put(col, value);
			}
		}
		if (outRow.size() != getColumnCount())
			throw new IllegalArgumentException("Columns are missing from the given input row");

		this.mappedRowList.add(outRow);
	}

	/**
	 * Append the row of ordered cell values.
	 * The order of the values is assumed to be the same as the order of the columns
	 * as returned by {@link #getColumnNames()}.
	 * @param cells an array of String values with a size equal to the return value  of {@link #getColumnCount()}.
	 * @throws IllegalArgumentException if the number of cells does not equal the number of columns.
	 */
	public void appendRow(String...cells) {
		if (cells.length != this.getColumnCount())
			throw new IllegalArgumentException("Number of values provide must be equal to the number of columsn (" + getColumnCount() +")");

		Map<String,Object> row = new HashMap<String,Object>();
		for (int i=0 ; i<cells.length ; i++) {
			row.put(this.columnNames.get(i), cells[i]);
		}
		appendRow(row);
	}
	
	/**
	 * Remove the named column from the table.
	 * @param columnName
	 * @throws IllegalArgumentException if the column does not exist.
	 */
	public void removeColumn(String columnName) {
		validateColumnExists(columnName);

		columnNames.remove(columnName);
		for (CaseInsensitiveMap row : mappedRowList) 
			row.remove(columnName);
	}

//	/**
//	 * Class to try and add/append two strings together.
//	 * @author dawood
//	 */
//	private static class StringAddition implements BinaryOperator<String> {
//
//		@Override
//		public String apply(String val1, String val2) {
//			Double dval1 = getNumber(val1);
//			Double dval2 = getNumber(val2);
//			String addition;
//			if (dval1 != null && dval2 != null) {	// 2 numbers
//				addition = String.valueOf(dval1.doubleValue() +  dval2.doubleValue());
//			} else  {	// 1 or both is not a number, treat as a string and concatentate.
//				addition = val1 + "\n" + val2;
//			}
//			return addition;
//		}
//		
//		/**
//		 * Convert string to Double object or null if could not be converted.
//		 * @param val
//		 * @return
//		 */
//		private static Double getNumber(String val) {
//			try {
//				return Double.valueOf(val);
//			} catch (Exception e) {
//				return null;
//			}
//		}
//		
//	}
//	
//	private static BinaryOperator<String> stringAddition = new StringAddition();

//	/**
//	 * Create a new table by collapsing all equals values in a given column into a single row.
//	 * Rows are collapsed together differently depending on the type of the data in the cell.
//	 * If numbers, then the values are added together, otherwise the values are concatentated
//	 * using a newline character.
//	 * @param columnName
//	 * @return a new table, this one is unmodified.
//	 */
//	public CSVTable collapse(String columnName) {
//		validateColumnExists(columnName);
//		CSVTable table = new CSVTable(getColumnNames());
//		List<String> values = this.unique(columnName);
//
//		for (String colValue : values) {
//			CSVTable matches = this.getRows(columnName, colValue);
//			CaseInsensitiveMap aggregation = null;
//			for (CaseInsensitiveMap row : matches) {
//				aggregation = collapse(aggregation, row, stringAddition);
//			}
//			table.appendRow(aggregation);
//		}
//		return table;
//	}

//	/**
//	 * Merge the values 
//	 * @param map1
//	 * @param map2
//	 * @return
//	 */
//	private static CaseInsensitiveMap collapse(CaseInsensitiveMap map1, CaseInsensitiveMap map2, BinaryOperator<String> f) {
//		CaseInsensitiveMap result = new CaseInsensitiveMap();
//		
//		if (map1 == null && map2 == null) {
//			;
//		} else if (map1 == null) {
//			result.putAll(map2);
//		} else if (map2 == null) {
//			result.putAll(map1);
//		} else {
//			for (Object colName : map1.keySet()) {
//				String val1 = map1.get(colName).toString();
//				String val2 = map2.get(colName).toString();
//				String addition = f.apply(val1,val2);
//				result.put(colName, addition);
//			}
//		}
//			
//		return result;
//	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnNames == null) ? 0 : columnNames.hashCode());
		result = prime * result + ((mappedRowList == null) ? 0 : mappedRowList.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof CSVTable))
			return false;
		CSVTable other = (CSVTable) obj;
		if (columnNames == null) {
			if (other.columnNames != null)
				return false;
		} else if (!columnNames.equals(other.columnNames))
			return false;
		if (mappedRowList == null) {
			if (other.mappedRowList != null)
				return false;
		} else if (!mappedRowList.equals(other.mappedRowList))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public CSVTable transpose() {
		CSVTable newTable = new CSVTable();
		List<String> colNames = this.getColumnNames();
		int colIndex = 1;
		for (Map<String, String> row : this) {
			List<Object> newColumn = new ArrayList<Object>();
			for (String colName : colNames) {	// Over each row value in column order
				String val= row.get(colName);
				newColumn.add(val);
			}
			newTable.appendColumn(String.valueOf(colIndex), newColumn);
			colIndex++;
		};
		
		return newTable;
	}
	
	/**
	 * Append the given table of rows to this instance.
	 * Both tables must have the same set of columns.
	 * @param src
	 */
	public void appendTable(CSVTable src) {
		for (CaseInsensitiveMap row : src) {
			this.appendRow(row);
		}
	}
	
	/**
	 * Append a column of constant values.
	 * @param columnName
	 * @param constant
	 */
	public void appendColumn(String columnName, Object constant) {
		int rows = this.getRowCount();
		List<Object> values = new ArrayList<Object>();
		for (int i=0 ; i<rows ; i++)
			values.add(constant);
		this.appendColumn(columnName, values);
	}
	
	public void appendEmptyRow() {
		this.appendConstantRow("");
	}
	
	public void appendConstantRow(Object constant) {
		int columns = this.getColumnCount();
		Map<String, Object> emptyRow = new  HashMap<String,Object>();
		for (String name : this.getColumnNames()) {
			emptyRow.put(name,constant);
		}
		this.appendRow(emptyRow);
	}


//	public static void main(String[] args) throws IOException {
//		
//		CSVTable doa = CSVTable.readFile("WebContent" + File.separatorChar + 
//			"resources" + File.separatorChar + "tyco" + File.separatorChar + "doa.csv", 1);
//		doa.write(System.out);
//
//		CSVTable location = CSVTable.readFile("WebContent" + File.separatorChar + 
//			"resources" + File.separatorChar + "tyco" + File.separatorChar + "locationgrid.csv", 1);
//		location.write(System.out);
//		
//		
//	}

}

