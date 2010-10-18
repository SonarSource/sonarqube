package org.sonar.plugins.findbugs;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.utils.SonarException;

import edu.umd.cs.findbugs.Project;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindbugsExecutorTest {

  @Test
  public void canGenerateXMLReport() throws Exception {
    FindbugsConfiguration conf = mockConf();
    File report = new File("target/test-tmp/findbugs-report.xml");
    when(conf.getTargetXMLReport()).thenReturn(report);

    new FindbugsExecutor(conf).execute();

    assertThat(report.exists(), is(true));
    assertThat(FileUtils.readFileToString(report), containsString("<BugInstance"));
  }

  @Test(expected = SonarException.class)
  public void shouldTerminateOnTimeout() throws Exception {
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
    when(conf.saveExcludeConfigXml()).thenReturn(new File("test-resources/findbugs-exclude.xml"));
    when(conf.saveIncludeConfigXml()).thenReturn(new File("test-resources/findbugs-include.xml"));
    when(conf.getEffort()).thenReturn("default");
    when(conf.getTimeout()).thenReturn(FindbugsConstants.FINDBUGS_TIMEOUT_DEFAULT_VALUE);
    return conf;
  }

}
