/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.scanner.analysis.DefaultAnalysisMode;
import org.sonar.scanner.bootstrap.GlobalMode;
import org.sonar.scanner.bootstrap.GlobalProperties;
import org.sonar.scanner.bootstrap.GlobalSettings;
import org.sonar.scanner.repository.settings.SettingsLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectSettingsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  private ProjectDefinition project;
  private GlobalSettings bootstrapProps;

  private GlobalMode globalMode;
  private DefaultAnalysisMode mode;
  private SettingsLoader settingsLoader;

  @Before
  public void prepare() {
    project = ProjectDefinition.create().setKey("struts");
    globalMode = mock(GlobalMode.class);
    mode = mock(DefaultAnalysisMode.class);
    settingsLoader = mock(SettingsLoader.class);
    bootstrapProps = new GlobalSettings(new GlobalProperties(Collections.<String, String>emptyMap()), new PropertyDefinitions(), settingsLoader, globalMode);
  }

  @Test
  public void should_load_project_props() {
    project.setProperty("project.prop", "project");

    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, settingsLoader, mode);

    assertThat(batchSettings.getString("project.prop")).isEqualTo("project");
  }

  @Test
  public void should_load_project_root_settings() {
    when(settingsLoader.load("struts")).thenReturn(ImmutableMap.of(
      "sonar.cpd.cross", "true",
      "sonar.java.coveragePlugin", "jacoco"));
    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, settingsLoader, mode);
    assertThat(batchSettings.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");
  }

  @Test
  public void should_load_project_root_settings_on_branch() {
    project.setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "mybranch");

    when(settingsLoader.load("struts:mybranch")).thenReturn(ImmutableMap.of(
      "sonar.cpd.cross", "true",
      "sonar.java.coveragePlugin", "jacoco"));

    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, settingsLoader, mode);

    assertThat(batchSettings.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");
  }

  @Test
  public void should_not_fail_when_accessing_secured_properties() {
    when(settingsLoader.load("struts")).thenReturn(ImmutableMap.of(
      "sonar.foo.secured", "bar",
      "sonar.foo.license.secured", "bar2"));

    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, settingsLoader, mode);

    assertThat(batchSettings.getString("sonar.foo.license.secured")).isEqualTo("bar2");
    assertThat(batchSettings.getString("sonar.foo.secured")).isEqualTo("bar");
  }

  @Test
  public void should_fail_when_accessing_secured_properties_in_issues_mode() {
    when(settingsLoader.load("struts")).thenReturn(ImmutableMap.of(
      "sonar.foo.secured", "bar",
      "sonar.foo.license.secured", "bar2"));

    when(mode.isIssues()).thenReturn(true);

    ProjectSettings batchSettings = new ProjectSettings(new ProjectReactor(project), bootstrapProps, settingsLoader, mode);

    assertThat(batchSettings.getString("sonar.foo.license.secured")).isEqualTo("bar2");
    thrown.expect(MessageException.class);
    thrown
      .expectMessage(
        "Access to the secured property 'sonar.foo.secured' is not possible in issues mode. The SonarQube plugin which requires this property must be deactivated in issues mode.");
    batchSettings.getString("sonar.foo.secured");
  }

}
