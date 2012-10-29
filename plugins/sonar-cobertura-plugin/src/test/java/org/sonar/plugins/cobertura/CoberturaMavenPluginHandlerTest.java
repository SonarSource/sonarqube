/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.resources.Project;
import org.sonar.plugins.cobertura.api.CoberturaUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoberturaMavenPluginHandlerTest {

  private CoberturaMavenPluginHandler handler;
  private CoberturaSettings settings;

  @Before
  public void before() {
    settings = mock(CoberturaSettings.class);
    handler = new CoberturaMavenPluginHandler(settings);
  }

  @Test
  public void users_could_change_version() {
    // first of all, version was fixed : see http://jira.codehaus.org/browse/SONAR-1055
    // but it's more reasonable to let users change the version : see http://jira.codehaus.org/browse/SONAR-1310
    assertThat(handler.isFixedVersion()).isFalse();
  }

  @Test
  public void test_metadata() {
    assertThat(handler.getGroupId()).isEqualTo("org.codehaus.mojo");
    assertThat(handler.getArtifactId()).isEqualTo("cobertura-maven-plugin");
    assertThat(handler.getVersion()).isEqualTo("2.5.1");
    assertThat(handler.getGoals()).containsOnly("cobertura");
  }

  @Test
  public void should_enable_xml_format() {
    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(new MavenProject());
    when(project.getExclusionPatterns()).thenReturn(new String[0]);

    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaUtils.COBERTURA_GROUP_ID, CoberturaUtils.COBERTURA_ARTIFACT_ID, null);
    handler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameter("formats/format")).isEqualTo("xml");
  }

  @Test
  public void should_set_cobertura_exclusions() {
    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(new MavenProject());
    when(project.getExclusionPatterns()).thenReturn(new String[]{"**/Foo.java", "com/*Test.*", "com/*"});

    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaUtils.COBERTURA_GROUP_ID, CoberturaUtils.COBERTURA_ARTIFACT_ID, null);
    handler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameters("instrumentation/excludes/exclude")).isEqualTo(new String[]{
      "**/Foo.class", "com/*Test.*", "com/*.class"
    });
  }

  @Test
  public void should_set_max_memory() {
    when(settings.getMaxMemory()).thenReturn("128m");
    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaUtils.COBERTURA_GROUP_ID, CoberturaUtils.COBERTURA_ARTIFACT_ID, null);

    Project project = mock(Project.class, Mockito.RETURNS_MOCKS);
    handler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameter("maxmem")).isEqualTo("128m");
  }
}
