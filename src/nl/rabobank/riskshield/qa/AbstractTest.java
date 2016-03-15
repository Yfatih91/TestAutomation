package nl.rabobank.riskshield.qa;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * All tests shall extend this base class.
 * <p>
 * All tests that are extended from this class shall have an own test directory. This test directory
 * shall contain the test class as well as the configuration files and maybe other sub-folders for
 * test data.<br>
 * A working directory for the test is (re-)created automatically before the test is executed as
 * sub-directory of the test directory and deleted automatically after the test. The working
 * directory can be preserved for manual checks by setting the {@link AbstractTest#preserveWorkDir}
 * flag within the test. Please note that only one working directory exists for the whole test
 * class.
 */
public abstract class AbstractTest {
  private static final Logger log  = Logger.getLogger(AbstractTest.class);

  @Rule
  public TestName             name = new TestName();

  private final String        testDir;
  private final String        workDir;

  private boolean             preserveWorkDir;

  protected AbstractTest() {
    testDir = "src/" + getClass().getPackage().getName().replace(".", "/") + "/";
    workDir = testDir + "work/";
  }

  //*****************************************************************************
  //*** JUnit
  //*****************************************************************************

  @Before
  public void beforeTest() throws Exception {
    log.info("Executing: " + getClass().getSimpleName() + "." + name.getMethodName());
    // TODO
    //QaUtil.deleteDirectory(workDir);
    //QaUtil.ensureDirectory(workDir);
  }

  @After
  public void afterTest() throws IOException {
    if(preserveWorkDir)
      preserveWorkDir = false;
    else
      QaUtil.deleteDirectory(workDir);
  }

  //*****************************************************************************
  //*** protected
  //*****************************************************************************

  /**
   * @return the base directory of the test
   */
  protected String getTestDir() {
    return testDir;
  }

  /**
   * @return temporary working directory of the test
   */
  protected String getWorkDir() {
    return workDir;
  }

  /**
   * Call this method to prevent the deletion of the working directory after the test.
   */
  protected void preserveWorkDir() {
    preserveWorkDir = true;
  }

}
