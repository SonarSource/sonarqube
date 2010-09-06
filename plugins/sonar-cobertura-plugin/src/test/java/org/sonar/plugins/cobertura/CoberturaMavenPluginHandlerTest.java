/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.plugins.cobertura;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.resources.Project;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoberturaMavenPluginHandlerTest {
  protected CoberturaMavenPluginHandler handler;

  @Before
  public void before() {
    handler = new CoberturaMavenPluginHandler();
  }


  @Test
  public void notFixedVersion() {
    // first of all, version was fixed : see http://jira.codehaus.org/browse/SONAR-1055
    // but it's more reasonable to let users fix the version : see http://jira.codehaus.org/browse/SONAR-1310
    assertThat(new CoberturaMavenPluginHandler().isFixedVersion(), is(false));
  }

  @Test
  public void activateXmlFormat() {
    Project project = mock(Project.class);
    when(project.getConfiguration()).thenReturn(new PropertiesConfiguration());
    when(project.getPom()).thenReturn(new MavenProject());
    when(project.getExclusionPatterns()).thenReturn(new String[0]);

    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaMavenPluginHandler.GROUP_ID, CoberturaMavenPluginHandler.ARTIFACT_ID, null);
    handler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameter("formats/format"), is("xml"));
  }

  @Test
  public void setCoberturaExclusions() {
    Project project = mock(Project.class);
    when(project.getConfiguration()).thenReturn(new PropertiesConfiguration());
    when(project.getPom()).thenReturn(new MavenProject());
    when(project.getExclusionPatterns()).thenReturn(new String[]{"**/Foo.java", "com/*Test.*", "com/*"});

    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaMavenPluginHandler.GROUP_ID, CoberturaMavenPluginHandler.ARTIFACT_ID, null);
    handler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameters("instrumentation/excludes/exclude")[0], is("**/Foo.class"));
    assertThat(coberturaPlugin.getParameters("instrumentation/excludes/exclude")[1], is("com/*Test.*"));
    assertThat(coberturaPlugin.getParameters("instrumentation/excludes/exclude")[2], is("com/*.class"));

  }
}
