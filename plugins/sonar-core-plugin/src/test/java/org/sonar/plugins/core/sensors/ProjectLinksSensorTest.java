/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.test.MavenTestUtils;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProjectLinksSensorTest {

  @Test
  public void shouldSaveLinks() {
    SensorContext context = mock(SensorContext.class);
    MavenProject pom = MavenTestUtils.loadPom("/org/sonar/plugins/core/sensors/ProjectLinksSensorTest/shouldSaveLinks.xml");
    Project project = mock(Project.class);

    new ProjectLinksSensor(pom).analyse(project, context);

    verify(context).saveLink(argThat(new MatchLink(ProjectLinksSensor.KEY_HOME, "Home", "http://sonar.codehaus.org")));
    verify(context).saveLink(argThat(new MatchLink(ProjectLinksSensor.KEY_ISSUE_TRACKER, "Issues", "http://jira.codehaus.org/browse/SONAR")));
    verify(context).saveLink(argThat(new MatchLink(ProjectLinksSensor.KEY_CONTINUOUS_INTEGRATION, "Continuous integration", "http://bamboo.ci.codehaus.org/browse/SONAR/")));
    verify(context).saveLink(argThat(new MatchLink(ProjectLinksSensor.KEY_SCM, "Sources", "http://svn.sonar.codehaus.org")));
    verify(context).saveLink(argThat(new MatchLink(ProjectLinksSensor.KEY_SCM_DEVELOPER_CONNECTION, "Developer connection", "scm:svn:https://svn.codehaus.org/sonar/trunk")));
  }

  @Test
  public void shouldDeleteMissingLinks() {
    SensorContext context = mock(SensorContext.class);
    MavenProject pom = MavenTestUtils.loadPom("/org/sonar/plugins/core/sensors/ProjectLinksSensorTest/shouldDeleteMissingLinks.xml");
    Project project = mock(Project.class);

    new ProjectLinksSensor(pom).analyse(project, context);

    verify(context).deleteLink(ProjectLinksSensor.KEY_HOME);
    verify(context).deleteLink(ProjectLinksSensor.KEY_ISSUE_TRACKER);
    verify(context).deleteLink(ProjectLinksSensor.KEY_CONTINUOUS_INTEGRATION);
    verify(context).deleteLink(ProjectLinksSensor.KEY_SCM);
    verify(context).deleteLink(ProjectLinksSensor.KEY_SCM_DEVELOPER_CONNECTION);
  }

  private class MatchLink extends BaseMatcher<ProjectLink> {
    private String key;
    private String name;
    private String url;

    private MatchLink(String key, String name, String url) {
      this.key = key;
      this.name = name;
      this.url = url;
    }

    public boolean matches(Object o) {
      ProjectLink link = (ProjectLink) o;
      return StringUtils.equals(link.getHref(), url) && StringUtils.equals(link.getKey(), key) && StringUtils.equals(link.getName(), name);
    }

    public void describeTo(Description description) {

    }
  }

}
