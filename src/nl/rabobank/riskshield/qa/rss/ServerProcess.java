package nl.rabobank.riskshield.qa.rss;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import com.inform_ac.ExitCode;

/**
 * Create and shutdown a RiskShield-Server process.
 */
public class ServerProcess {
  private final File        workDir;
  private final Process     rssProcess;
  private final PrintStream logStream;
  private final String      classpath;

  /**
   * Create <b>and</b> start a new Java process as RiskShield-Server instance.
   * 
   * @param runtimeDirName the root directory of the RiskShield-Server runtime, which at least must
   *        contain the "lib" and the "bin" folder of the RiskShield-Server distribution
   * @param workDirName a relative or absolute path used as working directory by the Java process of
   *        the RiskShield-Server instance
   * @param logFileName the name (including an absolute or relative path) of the file where the
   *        stdout and stderr streams will be piped to
   * @param serverIni the name (including an absolute or relative path) of the RiskShield-Server
   *        initialization file
   * @param serverId the instance ID used by RiskShield-Server to identify different instance in one
   *        log file (see LoggingMaster/-Slave in the RiskShield-Server manual for more details)
   * @param testMethodName add -Dtest.method=${testMethodName} to the command line to make the
   *        starting method visible in the task manager
   * @throws IOException if an I/O error occurs during the creation of the Java process
   */
  public ServerProcess(String runtimeDirName, String workDirName, String logFileName, String serverIni, int serverId, String testMethodName) throws IOException {
    File runtimeDir = new File(runtimeDirName);
    String libraryPath = new File(runtimeDir, "bin").getCanonicalPath();
    classpath = new File(runtimeDir, "lib").getCanonicalPath() + System.getProperty("file.separator") + "*" + System.getProperty("path.separator") + System.getProperty("java.class.path");
    workDir = new File(workDirName);
    File logFile = new File(logFileName);
    String serverIniPath = new File(serverIni).getCanonicalPath();

    ProcessBuilder builder = new ProcessBuilder("java", "-Dtest.method=" + testMethodName, "-Djava.library.path=" + libraryPath, "-classpath", classpath, "com.riskshield.server.Starter", "-i" + serverIniPath, "-s" + serverId);
    builder.directory(workDir);
    boolean success = false;
    rssProcess = builder.start();
    try {
      logStream = new PrintStream(logFile);
      OutputListener outputListener = new OutputListener(rssProcess, logStream, logStream);
      outputListener.readUntil("SYS0002"); // SYS0002 indicates that the server has successfully started
      outputListener.readUntilEnd(); // read non-blocking until shutdown
      success = true;
    } finally {
      if(!success)
        rssProcess.destroy(); // kill the process if anything bad happens
    }
  }

  /**
   * Shutdown the previously created RiskShield-Server process by the use of the SHUTDOWN command
   * send to the server's command port.
   * 
   * @param port the command port of the RiskShield-Server which handles the SHUTDOWN command
   * @param timeout time in milliseconds after which the shutdown process will be terminated if no
   *        response is received from the RiskShield-Server instance
   * @return one of the {@link ExitCode} constants
   *         <table border="1">
   *         <tr>
   *         <th>Exit Code</th>
   *         <th>Value</th>
   *         <th>Description</th>
   *         </tr>
   *         <tr>
   *         <td>OK</td>
   *         <td>0</td>
   *         <td>tool execution successful</td>
   *         </tr>
   *         <tr>
   *         <td>USAGE</td>
   *         <td>1</td>
   *         <td>wrong command line arguments (print usage)</td>
   *         </tr>
   *         <tr>
   *         <td>ERROR</td>
   *         <td>2</td>
   *         <td>error during execution</td>
   *         </tr>
   *         <tr>
   *         <td>FATAL</td>
   *         <td>3</td>
   *         <td>fatal error during execution</td>
   *         </tr>
   *         <tr>
   *         <td>SYSTEM_ERROR</td>
   *         <td>4</td>
   *         <td>could not instantiate tool</td>
   *         </tr>
   *         </table>
   * @throws IOException if an I/O error occurs
   * @throws InterruptedException if the current thread is interrupted by another thread while it is
   *         waiting, then the wait is ended and an {@link InterruptedException} is thrown.
   */
  public int shutdown(int port, int timeout) throws IOException, InterruptedException {
    int ret = 0;
    try {
      ProcessBuilder builder = new ProcessBuilder("java", "-classpath", classpath, "com.riskshield.server.Shutdown", "-p" + Integer.toString(port), "-t" + timeout);
      builder.directory(workDir);
      boolean success = false;
      Process shutdownProcess = builder.start();
      try {
        ret = shutdownProcess.waitFor();
        success = true;
      } finally {
        if(!success) {
          shutdownProcess.destroy(); // make sure that the process will be killed what ever happens
          ret = shutdownProcess.waitFor();
        }
      }
    } finally {
      try {
        logStream.close();
      } finally {
        if(ret != 0)
          rssProcess.destroy();
      }
    }
    return ret;
  }
}
