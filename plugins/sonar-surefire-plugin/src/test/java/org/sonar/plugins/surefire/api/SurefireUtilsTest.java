package org.sonar.plugins.surefire.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;

public class SurefireUtilsTest {

  @Test
  public void shouldGetReportsFromProperty() {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "shouldGetReportsFromProperty/pom.xml");
    assertThat(SurefireUtils.getReportsDirectory(project).exists(), is(true));
    assertThat(SurefireUtils.getReportsDirectory(project).isDirectory(), is(true));
  }

  @Test
  public void shouldGetReportsFromPluginConfiguration() {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "shouldGetReportsFromPluginConfiguration/pom.xml");
    assertThat(SurefireUtils.getReportsDirectory(project).exists(), is(true));
    assertThat(SurefireUtils.getReportsDirectory(project).isDirectory(), is(true));
  }

}
