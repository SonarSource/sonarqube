package org.sonar.plugins.cobertura.api;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;

import java.io.File;
import java.net.URISyntaxException;

public class CoberturaUtilsTest {
  @Test
  public void shouldGetReportPathFromProperty() throws URISyntaxException {
    DefaultProjectFileSystem fileSystem = mock(DefaultProjectFileSystem.class);
    when(fileSystem.resolvePath("foo")).thenReturn(getCoverageReport());

    Project project = mock(Project.class);
    when(project.getFileSystem()).thenReturn(fileSystem);
    when(project.getProperty(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY)).thenReturn("foo");

    File report = CoberturaUtils.getReport(project);
    verify(fileSystem).resolvePath("foo");
    assertNotNull(report);
  }

  @Test
  public void shouldGetReportPathFromPom() {
    MavenProject pom = MavenTestUtils.loadPom("/org/sonar/plugins/cobertura/CoberturaSensorTest/shouldGetReportPathFromPom/pom.xml");

    DefaultProjectFileSystem fileSystem = mock(DefaultProjectFileSystem.class);

    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(pom);
    when(project.getFileSystem()).thenReturn(fileSystem);

    CoberturaUtils.getReport(project);

    verify(fileSystem).resolvePath("overridden/dir");
  }

  private File getCoverageReport() throws URISyntaxException {
    return new File(getClass().getResource("/org/sonar/plugins/cobertura/CoberturaSensorTest/commons-chain-coverage.xml").toURI());
  }
}
