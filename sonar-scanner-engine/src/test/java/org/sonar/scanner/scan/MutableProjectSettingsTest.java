/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
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
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.bootstrap.GlobalConfigurationProvider;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.GlobalProperties;
import org.sonar.scanner.bootstrap.MutableGlobalSettings;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.repository.settings.SettingsLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MutableProjectSettingsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  private ProjectRepositories projectRef;
  private ProjectDefinition project;
  private GlobalConfiguration bootstrapProps;
  private Table<String, String, FileData> emptyFileData;
  private Table<String, String, String> emptySettings;

  private GlobalAnalysisMode globalMode;

  @Before
  public void prepare() {
    emptyFileData = ImmutableTable.of();
    emptySettings = ImmutableTable.of();
    project = ProjectDefinition.create().setKey("struts");
    globalMode = mock(GlobalAnalysisMode.class);
    bootstrapProps = new GlobalConfigurationProvider().provide(mock(SettingsLoader.class), new GlobalProperties(Collections.<String, String>emptyMap()), new PropertyDefinitions(),
      globalMode);
  }

  @Test
  public void should_load_project_props() {
    project.setProperty("project.prop", "project");

    projectRef = new ProjectRepositories(emptySettings, emptyFileData, null);
    MutableProjectSettings batchSettings = new MutableProjectSettings(new ProjectReactor(project), new MutableGlobalSettings(bootstrapProps), projectRef, globalMode);

    assertThat(batchSettings.getString("project.prop")).isEqualTo("project");
  }

  @Test
  public void should_load_project_root_settings() {
    Table<String, String, String> settings = HashBasedTable.create();
    settings.put("struts", "sonar.cpd.cross", "true");
    settings.put("struts", "sonar.java.coveragePlugin", "jacoco");

    projectRef = new ProjectRepositories(settings, emptyFileData, null);
    MutableProjectSettings batchSettings = new MutableProjectSettings(new ProjectReactor(project), new MutableGlobalSettings(bootstrapProps), projectRef, globalMode);
    assertThat(batchSettings.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");
  }

  @Test
  public void should_load_project_root_settings_on_branch() {
    project.setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "mybranch");

    Table<String, String, String> settings = HashBasedTable.create();
    settings.put("struts:mybranch", "sonar.cpd.cross", "true");
    settings.put("struts:mybranch", "sonar.java.coveragePlugin", "jacoco");

    projectRef = new ProjectRepositories(settings, emptyFileData, null);

    MutableProjectSettings batchSettings = new MutableProjectSettings(new ProjectReactor(project), new MutableGlobalSettings(bootstrapProps), projectRef, globalMode);

    assertThat(batchSettings.getString("sonar.java.coveragePlugin")).isEqualTo("jacoco");
  }

  @Test
  public void should_not_fail_when_accessing_secured_properties() {
    Table<String, String, String> settings = HashBasedTable.create();
    settings.put("struts", "sonar.foo.secured", "bar");
    settings.put("struts", "sonar.foo.license.secured", "bar2");

    projectRef = new ProjectRepositories(settings, emptyFileData, null);
    MutableProjectSettings batchSettings = new MutableProjectSettings(new ProjectReactor(project), new MutableGlobalSettings(bootstrapProps), projectRef, globalMode);

    assertThat(batchSettings.getString("sonar.foo.license.secured")).isEqualTo("bar2");
    assertThat(batchSettings.getString("sonar.foo.secured")).isEqualTo("bar");
  }

  @Test
  public void should_fail_when_accessing_secured_properties_in_issues_mode() {
    Table<String, String, String> settings = HashBasedTable.create();
    settings.put("struts", "sonar.foo.secured", "bar");
    settings.put("struts", "sonar.foo.license.secured", "bar2");

    when(globalMode.isIssues()).thenReturn(true);

    projectRef = new ProjectRepositories(settings, emptyFileData, null);
    MutableProjectSettings batchSettings = new MutableProjectSettings(new ProjectReactor(project), new MutableGlobalSettings(bootstrapProps), projectRef, globalMode);

    assertThat(batchSettings.getString("sonar.foo.license.secured")).isEqualTo("bar2");
    thrown.expect(MessageException.class);
    thrown
      .expectMessage(
        "Access to the secured property 'sonar.foo.secured' is not possible in issues mode. The SonarQube plugin which requires this property must be deactivated in issues mode.");
    batchSettings.getString("sonar.foo.secured");
  }

}
