package nl.rabobank.riskshield.qa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

import com.riskshield.shared.csv.CsvReader;

public class QaUtil {

  //**************************************************************************************************
  //*** directory operations
  //**************************************************************************************************

  /**
   * Used by file admin code, deletes all files and directories in this directory, and keep the
   * directory
   * 
   * @param dir
   * @throws IOException
   */
  public static void deleteAllIn(File dir) throws IOException {
    File[] files = dir.listFiles();
    if(files == null)
      throw new IOException("The provided file must be a directory: " + dir.getAbsolutePath());
    for(File file:files) {
      if(file.isDirectory())
        deleteAllIn(file);
      delete(file);
    }
  }

  /**
   * Deletes an existing file
   * 
   * @param file
   * @throws IOException if it doesn't exist or if unable to delete
   */
  public static void delete(File file) throws IOException {
    if(!file.delete())
      throw new IOException("Could not delete file: " + file);
  }

  /**
   * Returns a File immediately if the given String names a rw-directory, otherwise tries to create
   * it
   * 
   * @param directory
   * @return File with the (assured usable) directory
   * @throws IOException if anything goes wrong
   */
  public static File ensureDirectory(String directory) throws IOException {
    return ensureDirectory(new File(directory));
  }

  public static File ensureDirectory(File f) throws IOException {
    if(f.exists()) {
      if(!f.isDirectory())
        throw new IOException("Need directory, but already exists as a file: " + f);
      if(!f.canRead() || !f.canWrite())
        throw new IOException("Directory is not read/write: " + f);
    } else {
      // try to create the directory
      if(!f.mkdirs()) {
        if(!f.exists())
          throw new IOException("Could not create directory: " + f);
        // maybe someone else created the directory in the same time
        if(!f.isDirectory())
          throw new IOException("Need directory, but already exists as a file: " + f);
        if(!f.canRead() || !f.canWrite())
          throw new IOException("Directory is not read/write: " + f);
      }
    }
    return f;
  }

  /**
   * deletes a directory and all subdirectories and files<br>
   * does nothing if <code>dirName</code> does not exist
   * 
   * @param directory directory which should be deleted
   * @throws IOException if deletion fails
   */
  public static void deleteDirectory(String dirName) throws IOException {
    File directory = new File(dirName);
    if(directory.exists()) {
      if(!directory.isDirectory())
        throw new IOException("[" + directory + "] is not a directory");
      deleteAllIn(directory);
      if(!directory.delete())
        throw new IOException("could not delete root directory [" + directory + "]");
    }
  }

  //**************************************************************************************************
  //*** file operations
  //**************************************************************************************************

  public static void copyFile(String from, String to) throws IOException {
    copyFile(new File(from), new File(to));
  }

  /**
   * Utility method, calls copyFile(FileChannel from, WritableByteChannel to)
   * 
   * @param from
   * @param to
   * @throws IOException
   */
  public static void copyFile(File from, File to) throws IOException {
    //Uninterruptible (for some data integrity), the signal will be set at exit
    try {
      FileInputStream in = new FileInputStream(from);
      try {
        FileOutputStream out = new FileOutputStream(to);
        try {
          copyFile(in.getChannel(), out.getChannel());
        } finally {
          out.close();
        }
      } finally {
        in.close();
      }
    } catch(ClosedByInterruptException e) {
      Thread.interrupted(); //clears interrupted signal
      copyFile(from, to);
      Thread.currentThread().interrupt(); //sets interrupted signal
    }
  }

  /**
   * Copies all data from an input FileChannel to an output FileChannel The Channels are closed when
   * finished
   * 
   * @param from
   * @param to
   * @throws IOException if anything goes wrong
   */
  public static void copyFile(FileChannel from, WritableByteChannel to) throws IOException {
    long fromSize = from.size();
    long pos = 0L;
    long chunk = 1024L * 1024L * 32L;//TODO: confirm that 32MB chunks are good
    try {
      // one call of transferTo is limited to 2 GB, there is no guarantee that count bytes are truly transferred => loop till fromSize is necessary
      while(pos < fromSize)
        pos += from.transferTo(pos, Math.min(chunk, fromSize - pos), to);
    } finally {
      from.close();
      to.close();
    }
  }

  //**************************************************************************************************
  //*** file comparisons
  //**************************************************************************************************

  /**
   * Compare the content of 2 csv files by comparing each value. If a difference is found an
   * <code>AssertionError</code> is thrown. The order of columns is not important, the columns are
   * compared by name.
   * 
   * @param leftFileName
   * @param rightFileName
   * @throws IOException
   * @throws CsvParserException
   */
  public static void compareCsvFiles(String leftFileName, String rightFileName, char sepChar, char quoteChar, Charset charset) throws IOException {
    Set<String> columnNames = new HashSet<String>();
    Map<String,List<String>> left = parseCsv(leftFileName, sepChar, quoteChar, charset);
    Map<String,List<String>> right = parseCsv(rightFileName, sepChar, quoteChar, charset);
    assert left.keySet().size() == right.keySet().size() : "number of columns " + left.keySet().size() + " in left file != number of columns " + right.keySet().size() + " in right file (left file='" + leftFileName + "', right file='" + rightFileName + "')";
    for(String leftColumnName:left.keySet()) {
      columnNames.add(leftColumnName);
      List<String> rightColumnValues = right.get(leftColumnName);
      assert rightColumnValues != null : "column " + leftColumnName + " not found in right file";
      List<String> leftColumnValues = left.get(leftColumnName);
      assert rightColumnValues.size() == leftColumnValues.size() : "number of rows on the right file " + rightColumnValues.size() + " does not match number of rows on the left file " + leftColumnValues.size();
      for(int i = 0; i < leftColumnValues.size(); i++) {
        String requestVal = leftColumnValues.get(i);
        String responseVal = rightColumnValues.get(i);
        assert requestVal.equals(responseVal) : "column " + leftColumnName + ", row " + i + " [" + requestVal + "] != [" + responseVal + "]" + " ([" + toHex(requestVal) + "] != [" + toHex(responseVal) + "])";
      }
    }
    for(String rightColumnName:right.keySet())
      assert columnNames.contains(rightColumnName) : "column " + rightColumnName + " not found in left file";
  }

  /**
   * Parse a csv file in a <code>Map</code> of columns.
   * 
   * @param csvFileName
   * @return
   * @throws IOException
   * @throws CsvParserException
   */
  public static Map<String,List<String>> parseCsv(String csvFileName, char sepChar, char quoteChar, Charset charset) throws IOException {
    Map<String,List<String>> result = new LinkedHashMap<String,List<String>>();
    InputStream is = openFile(new File(csvFileName), csvFileName.endsWith(".zip"));
    try {
      CsvReader parser = new CsvReader(new InputStreamReader(is, charset), sepChar, quoteChar, false, false);
      String[] header = parser.readLine();
      if(header != null) {
        for(String field:header)
          result.put(field, new ArrayList<String>());
        String[] rec = parser.readLine();
        while(rec != null) {
          for(int i = 0; i < header.length; i++)
            result.get(header[i]).add(i < rec.length ? rec[i] : "");
          rec = parser.readLine();
        }
      }
    } finally {
      is.close();
    }
    return result;
  }

  /**
   * Convert a String to a hexadecimal representation.
   * 
   * @param s
   * @return
   */
  public static String toHex(String s) {
    StringBuilder sb = new StringBuilder();
    char[] carray = s.toCharArray();
    for(char c:carray)
      sb.append(Integer.toHexString(c));
    return sb.toString().toUpperCase();
  }

  //**************************************************
  //*** log file
  //**************************************************

  public static void confirmNotContains(File logFile, String... targets) throws IOException {
    confirmNotContains(logFile, null, targets);
  }

  public static void confirmNotContains(File logFile, Charset charset, String... targets) throws IOException {
    try {
      countLogFile(logFile, charset, true, targets);
    } catch(IOException e) {
      throw new IOException("The log file \n" + logFile.getAbsolutePath() + "\n is expected to have no lines containing \"" + Arrays.toString(targets) + "\", but found: \n" + e.getMessage());
    }
  }

  public static void confirmContains(File logFile, String... targets) throws IOException {
    confirmContains(logFile, null, targets);
  }

  public static void confirmContains(File logFile, Charset charset, String... targets) throws IOException {
    try {
      countLogFile(logFile, charset, true, targets);
    } catch(IOException ok) {
      return;
    }
    throw new IOException("The log file \n" + logFile.getAbsolutePath() + "\n is expected to have at least one line containing \"" + Arrays.toString(targets) + "\"");

  }

  public static void confirmContains(File logFile, int count, String... targets) throws IOException {
    confirmContains(logFile, null, count, targets);
  }

  public static void confirmContains(File logFile, Charset charset, int count, String... targets) throws IOException {
    int result = countLogFile(logFile, charset, false, targets);
    if(result != count)
      throw new IOException("The log file \n" + logFile.getAbsolutePath() + "\n is expected to have " + count + " lines containing \"" + Arrays.toString(targets) + "\", but it has " + result + " lines");
  }

  public static int countLogFile(File logFile, boolean exceptOnFound, String... targets) throws IOException {
    return countLogFile(logFile, null, exceptOnFound, targets);
  }

  public static int countLogFile(File logFile, Charset charset, boolean exceptOnFound, String... targets) throws IOException {
    int count = 0;
    try {
      BufferedReader reader = new BufferedReader(charset == null ? new FileReader(logFile) : new InputStreamReader(new FileInputStream(logFile), charset));
      try {
        String line;
        while((line = reader.readLine()) != null) {
          for(String target:targets)
            if(line.contains(target)) {
              if(exceptOnFound)
                throw new IOException(line);
              count++;
              break;
            }
        }
      } finally {
        reader.close();
      }
    } catch(FileNotFoundException ok) {
      // return 0
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
    return count;
  }

  public static String[] getLines(File file, Charset charset, String... targets) throws IOException {
    List<String> lines = new ArrayList<String>();
    try {
      BufferedReader reader = new BufferedReader(charset == null ? new InputStreamReader(new FileInputStream(file)) : new InputStreamReader(new FileInputStream(file), charset));
      try {
        String line;
        while((line = reader.readLine()) != null) {
          if(targets.length > 0) {
            for(String target:targets) {
              if(line.contains(target)) {
                lines.add(line);
                break;
              }
            }
          } else {
            lines.add(line);
          }
        }
      } finally {
        reader.close();
      }
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
    return lines.toArray(new String[0]);
  }

  //******************************************************************
  //*** private
  //******************************************************************

  private static InputStream openFile(File file, boolean isZip) throws IOException {
    InputStream is = null;
    FileInputStream fis = new FileInputStream(file);
    try {
      if(isZip) {
        ZipInputStream zis = new ZipInputStream(fis);
        try {
          zis.getNextEntry();
          is = zis;
        } finally {
          if(is == null)
            zis.close();
        }
      } else {
        is = fis;
      }
    } finally {
      if(is == null)
        fis.close();
    }
    return is;
  }

}
