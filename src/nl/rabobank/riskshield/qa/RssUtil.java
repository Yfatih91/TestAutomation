package nl.rabobank.riskshield.qa;

import java.io.IOException;

import org.apache.log4j.Logger;

import nl.rabobank.riskshield.qa.rss.ServerProcess;

/**
 * Utility class to start and stop a RiskShield-Server instance with defaults for the current
 * project.
 * <ul>
 * <li><b>RuntimePath</b>: lib/rss (relative to the project root)</li>
 * <li><b>LogFileName</b>: ${workDirName}/rss.log (see {@link #startup(String, String)})</li>
 * <li><b>InstanceID</b>: 0</li>
 * <li><b>ShutdownTimeout</b>: 2s</li>
 * </ul>
 */
public class RssUtil {
  private static final Logger log = Logger.getLogger(RssUtil.class);

  public static ServerProcess startup(String workDirName, String serverIni) throws IOException {
    log.info("starting RiskShield-Server...");
    StackTraceElement elem = Thread.currentThread().getStackTrace()[2];
    ServerProcess server = new ServerProcess("lib/rss/", workDirName, workDirName + "rss.log", serverIni, 0, elem.getClassName() + "." + elem.getMethodName());
    log.info("RiskShield-Server started");
    return server;
  }

  public static int shutdown(ServerProcess server, int port) throws Exception {
    log.info("stopping RiskShield-Server...");
    int ret = server.shutdown(port, 2000);
    log.info("RiskShield-Server stopped with return code " + ret);
    return ret;
  }

}
