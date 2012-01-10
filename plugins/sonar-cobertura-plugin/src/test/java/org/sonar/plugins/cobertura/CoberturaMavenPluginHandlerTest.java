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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.plugins.cobertura.api.CoberturaUtils;

public class CoberturaMavenPluginHandlerTest {

  protected CoberturaMavenPluginHandler handler;

  @Before
  public void before() {
    handler = new CoberturaMavenPluginHandler(new Settings(new PropertyDefinitions(CoberturaPlugin.class)));
  }

  @Test
  public void notFixedVersion() {
    // first of all, version was fixed : see http://jira.codehaus.org/browse/SONAR-1055
    // but it's more reasonable to let users change the version : see http://jira.codehaus.org/browse/SONAR-1310
    assertThat(new CoberturaMavenPluginHandler(null).isFixedVersion(), is(false));
  }

  @Test
  public void activateXmlFormat() {
    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(new MavenProject());
    when(project.getExclusionPatterns()).thenReturn(new String[0]);

    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaUtils.COBERTURA_GROUP_ID, CoberturaUtils.COBERTURA_ARTIFACT_ID, null);
    handler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameter("formats/format"), is("xml"));
  }

  @Test
  public void setCoberturaExclusions() {
    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(new MavenProject());
    when(project.getExclusionPatterns()).thenReturn(new String[] { "**/Foo.java", "com/*Test.*", "com/*" });

    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaUtils.COBERTURA_GROUP_ID, CoberturaUtils.COBERTURA_ARTIFACT_ID, null);
    handler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameters("instrumentation/excludes/exclude")[0], is("**/Foo.class"));
    assertThat(coberturaPlugin.getParameters("instrumentation/excludes/exclude")[1], is("com/*Test.*"));
    assertThat(coberturaPlugin.getParameters("instrumentation/excludes/exclude")[2], is("com/*.class"));
  }

  @Test
  // http://jira.codehaus.org/browse/SONAR-2897: there used to be a typo in the parameter name (was "sonar.cobertura.maxmen")
  public void checkOldParamNameCompatibility() {
    Settings settings = new Settings(new PropertyDefinitions(CoberturaPlugin.class));
    settings.setProperty("sonar.cobertura.maxmen", "FOO");
    CoberturaMavenPluginHandler coberturaMavenPluginHandler = new CoberturaMavenPluginHandler(settings);

    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(new MavenProject());
    when(project.getExclusionPatterns()).thenReturn(new String[0]);

    MavenPlugin coberturaPlugin = new MavenPlugin(CoberturaUtils.COBERTURA_GROUP_ID, CoberturaUtils.COBERTURA_ARTIFACT_ID, null);

    coberturaMavenPluginHandler.configure(project, coberturaPlugin);

    assertThat(coberturaPlugin.getParameter("maxmem"), is("FOO"));
  }
}
