package org.sonar.plugins.findbugs;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.utils.SonarException;

import edu.umd.cs.findbugs.Project;

import java.io.File;

public class FindbugsExecutorTest {

  @Test
  public void canGenerateXMLReport() throws Exception {
    FindbugsConfiguration conf = mockConf();
    File reportFile = new File("target/test-tmp/findbugs-report.xml");
    when(conf.getTargetXMLReport()).thenReturn(reportFile);

    new FindbugsExecutor(conf).execute();

    assertThat(reportFile.exists(), is(true));
    String report = FileUtils.readFileToString(reportFile);
    assertThat("Report should contain bug instance", report, containsString("<BugInstance"));
    assertThat("Report should be generated with messages", report, containsString("<Message>"));
    assertThat(report, containsString("synthetic=\"true\""));
  }

  @Test(expected = SonarException.class)
  public void shouldTerminateAfterTimeout() throws Exception {
    FindbugsConfiguration conf = mockConf();
    when(conf.getTimeout()).thenReturn(1L);

    new FindbugsExecutor(conf).execute();
  }

  private FindbugsConfiguration mockConf() throws Exception {
    FindbugsConfiguration conf = mock(FindbugsConfiguration.class);
    Project project = new Project();
    project.addFile(new File("test-resources/classes").getCanonicalPath());
    project.addSourceDir(new File("test-resources/src").getCanonicalPath());
    project.setCurrentWorkingDirectory(new File("test-resources"));
    when(conf.getFindbugsProject()).thenReturn(project);
    when(conf.saveExcludeConfigXml()).thenReturn(new File("test-resources/findbugs-exclude.xml").getAbsolutePath());
    when(conf.saveIncludeConfigXml()).thenReturn(new File("test-resources/findbugs-include.xml").getAbsolutePath());
    when(conf.getEffort()).thenReturn("default");
    when(conf.getTimeout()).thenReturn(FindbugsConstants.FINDBUGS_TIMEOUT_DEFAULT_VALUE);
    return conf;
  }

}
