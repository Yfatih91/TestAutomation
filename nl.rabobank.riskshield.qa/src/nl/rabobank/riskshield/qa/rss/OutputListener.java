package nl.rabobank.riskshield.qa.rss;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Reads the output (stdout and stderr) of a process in two separate threads.
 */
public class OutputListener {
  private final BufferedReader mOutReader;
  private final PrintStream    mOutStream;

  /**
   * All output of the process is piped to System.out and System.err.
   * <p>
   * The stderr reader is started immediately in a separate thread.
   * <p>
   * The stdout reader must be started by calling either {@link #readUntil()} or
   * {@link #readUntilEnd()} method.
   * 
   * @param process for which the output shall be piped
   */
  public OutputListener(Process process) {
    this(process, System.out, System.err);
  }

  /**
   * Pipe the process stdout to out and the process stderr to err.
   * <p>
   * The stderr reader is started immediately in a separate thread.
   * <p>
   * The stdout reader must be started by calling either {@link #readUntil()} or
   * {@link #readUntilEnd()} method.
   * 
   * @param process for which the output shall be piped
   * @param out PrintStream in which stdout of the process will be written
   * @param err PrintStream in which stderr of the process will be written
   */
  public OutputListener(final Process process, final PrintStream out, final PrintStream err) {
    mOutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    mOutStream = out;
    Thread t = new Thread("OutputListener: stderr") {
      @Override
      public void run() {
        try {
          BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
          String line;
          while((line = reader.readLine()) != null)
            err.println(line);
        } catch(IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
    t.setDaemon(true);
    t.start();
  }

  /**
   * Reads stdout of the process blocking until <code>target</code> is found.
   * 
   * @param target String to search for (e.g. "SYS0002"="initialization finished")
   * @throws IOException If an I/O error occurs
   */
  public void readUntil(String target) throws IOException {
    String line;
    while((line = mOutReader.readLine()) != null) {
      mOutStream.println(line);
      if(line.contains(target))
        break;
    }
    if(line == null)
      throw new EOFException("could not find '" + target + "': unexpected EOF occured");
  }

  /**
   * None blocking read of the process stdout stream.
   */
  public void readUntilEnd() {
    Thread t = new Thread("OutputListener: stdout") {
      @Override
      public void run() {
        try {
          String line;
          while((line = mOutReader.readLine()) != null)
            mOutStream.println(line);
        } catch(IOException e) {
          e.printStackTrace();
        }
      }
    };
    t.setDaemon(true);
    t.start();
  }

}
