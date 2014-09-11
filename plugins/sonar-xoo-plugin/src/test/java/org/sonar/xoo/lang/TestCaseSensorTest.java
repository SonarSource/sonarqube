package org.sonar.xoo.lang;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.test.TestCase;
import org.sonar.api.batch.sensor.test.internal.DefaultTestCaseBuilder;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestCaseSensorTest {

  private TestCaseSensor sensor;
  private SensorContext context = mock(SensorContext.class);
  private DefaultFileSystem fileSystem;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    sensor = new TestCaseSensor();
    fileSystem = new DefaultFileSystem();
    when(context.fileSystem()).thenReturn(fileSystem);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfNoTestFile() {
    DefaultInputFile testFile = new DefaultInputFile("foo", "test/fooTest.xoo").setAbsolutePath(new File(baseDir, "test/fooTest.xoo").getAbsolutePath()).setLanguage("xoo")
      .setType(Type.TEST);
    fileSystem.add(testFile);
    sensor.execute(context);
  }

  @Test
  public void testExecution() throws IOException {
    File testPlan = new File(baseDir, "test/fooTest.xoo.testplan");
    FileUtils.write(testPlan, "test1:UNIT:OK:::10\ntest2:INTEGRATION:ERROR:message:stack:15\n\n#comment");
    DefaultInputFile testFile = new DefaultInputFile("foo", "test/fooTest.xoo").setAbsolutePath(new File(baseDir, "test/fooTest.xoo").getAbsolutePath()).setLanguage("xoo")
      .setType(Type.TEST);
    fileSystem.add(testFile);

    when(context.testCaseBuilder(testFile, "test1")).thenReturn(new DefaultTestCaseBuilder(testFile, "test1"));
    when(context.testCaseBuilder(testFile, "test2")).thenReturn(new DefaultTestCaseBuilder(testFile, "test2"));

    sensor.execute(context);

    verify(context).addTestCase(new DefaultTestCaseBuilder(testFile, "test1").durationInMs(10).build());
    verify(context).addTestCase(
      new DefaultTestCaseBuilder(testFile, "test2").type(TestCase.Type.INTEGRATION).status(TestCase.Status.ERROR).message("message").stackTrace("stack").durationInMs(15).build());
  }

}
