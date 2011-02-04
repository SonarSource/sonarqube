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
package org.sonar.plugins.clover;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CloverMavenPluginHandlerTest {

  private CloverMavenPluginHandler handler;
  private Project project;
  private MavenPlugin plugin;

  @Before
  public void init() {
    handler = new CloverMavenPluginHandler(new PropertiesConfiguration());
  }

  private void configurePluginHandler(String pom) {
    project = MavenTestUtils.loadProjectFromPom(getClass(), pom);
    plugin = MavenPlugin.getPlugin(project.getPom(), handler.getGroupId(), handler.getArtifactId());
    handler.configure(project, plugin);
  }

  @Test
  public void overrideConfiguration() throws Exception {
    configurePluginHandler("overrideConfiguration.xml");

    assertThat(plugin.getParameter("generateXml"), is("true"));
    assertThat(plugin.getParameter("foo"), is("bar"));
    String configuredReportPath = project.getConfiguration().getString(CoreProperties.SUREFIRE_REPORTS_PATH_PROPERTY);
    assertThat(configuredReportPath, notNullValue());
    configuredReportPath = configuredReportPath.replace('\\', '/');
    assertThat(configuredReportPath, endsWith("clover/surefire-reports"));
  }

  @Test
  public void shouldSkipCloverWithPomConfig() throws Exception {
    configurePluginHandler("shouldSkipCloverWithPomConfig.xml");

    assertThat(project.getConfiguration().getString(CoreProperties.SUREFIRE_REPORTS_PATH_PROPERTY), nullValue());
  }

  @Test
  public void shouldSkipCloverWithPomProperty() throws Exception {
    configurePluginHandler("shouldSkipCloverWithPomProperty.xml");

    assertThat(project.getConfiguration().getString(CoreProperties.SUREFIRE_REPORTS_PATH_PROPERTY), nullValue());
  }

}
