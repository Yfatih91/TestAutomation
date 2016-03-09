package nl.rabobank.riskshield.qa.test.RssUtil_Test;

import java.io.File;
import java.nio.charset.Charset;

import org.junit.Test;

import com.riskshield.server.tools.File2Server;

import nl.rabobank.riskshield.qa.AbstractTest;
import nl.rabobank.riskshield.qa.QaUtil;
import nl.rabobank.riskshield.qa.RssUtil;
import nl.rabobank.riskshield.qa.rss.ServerProcess;

/**
 * This is an example for writing a test with the usage of the classes
 * <ul>
 * <li>{@link AbstractTest}</li>
 * <li>{@link RssUtil}</li>
 * <li>{@link QaUtil}</li>
 * <li>{@link File2Server}</li>
 * </ul>
 */
public class RssUtil_Test extends AbstractTest {

  /**
   * Test if the RiskShield-Server process can start, stop and receive data.
   */
  @Test
  public void startupAndShutdown() throws Exception {
    preserveWorkDir(); // uncomment this line to manually check the processing results if something unexpected happens (or during development)

    // the try {} finally {} is important to avoid orphan RiskShield-Server Java processes
    ServerProcess server = RssUtil.startup(getWorkDir(), getTestDir() + "config/server.ini");
    try {
      // send a CSV test file to the server and write the response to the working directory of the test
      File2Server.send("127.0.0.1", 50001, getTestDir() + "data/request1.csv", getWorkDir() + "response1.csv");
    } finally {
      RssUtil.shutdown(server, 55556);
    }

    // check that the rss.log does _not_ contain any warnings, errors or fatals  
    QaUtil.confirmNotContains(new File(getWorkDir() + "rss.log"), "| W |", "| E |", "| F |");

    // compare the response file with the expected result 
    QaUtil.compareCsvFiles(getTestDir() + "data/response1.csv", getWorkDir() + "response1.csv", ',', '"', Charset.forName("UTF-8"));
  }

}
