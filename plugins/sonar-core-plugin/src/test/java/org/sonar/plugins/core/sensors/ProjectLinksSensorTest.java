/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.core.sensors;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.core.i18n.DefaultI18n;

import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class ProjectLinksSensorTest {

  @Test
  public void testToString() {
    assertThat(new ProjectLinksSensor(null, null).toString()).isEqualTo("ProjectLinksSensor");
  }

  @Test
  public void shouldExecuteOnlyForLatestAnalysis() {
    Project project = mock(Project.class);
    assertThat(new ProjectLinksSensor(null, null).shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void shouldSaveLinks() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.LINKS_HOME_PAGE, "http://home");
    DefaultI18n defaultI18n = mock(DefaultI18n.class);
    when(defaultI18n.message(Locale.getDefault(), "project_links.homepage", CoreProperties.LINKS_HOME_PAGE)).thenReturn("HOME");
    Project project = mock(Project.class);
    SensorContext context = mock(SensorContext.class);

    new ProjectLinksSensor(settings, defaultI18n).analyse(project, context);

    verify(context).saveLink(argThat(new MatchLink("homepage", "HOME", "http://home")));
  }

  @Test
  public void shouldDeleteLink() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.LINKS_HOME_PAGE, "");
    DefaultI18n defaultI18n = mock(DefaultI18n.class);
    when(defaultI18n.message(Locale.getDefault(), "project_links.homepage", CoreProperties.LINKS_HOME_PAGE)).thenReturn("HOME");
    Project project = mock(Project.class);
    SensorContext context = mock(SensorContext.class);

    new ProjectLinksSensor(settings, defaultI18n).analyse(project, context);

    verify(context).deleteLink("homepage");
  }

  private class MatchLink extends ArgumentMatcher<ProjectLink> {
    private String key;
    private String name;
    private String url;

    private MatchLink(String key, String name, String url) {
      this.key = key;
      this.name = name;
      this.url = url;
    }

    @Override
    public boolean matches(Object o) {
      ProjectLink link = (ProjectLink) o;
      return StringUtils.equals(link.getHref(), url) && StringUtils.equals(link.getKey(), key) && StringUtils.equals(link.getName(), name);
    }
  }

}
