/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.analysis.DefaultAnalysisMode;
import org.sonar.scanner.bootstrap.GlobalMode;
import org.sonar.scanner.bootstrap.GlobalProperties;
import org.sonar.scanner.bootstrap.GlobalSettings;
import org.sonar.scanner.report.AnalysisContextReportPublisher;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.repository.settings.SettingsLoader;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

public class ModuleSettingsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultInputModule module;
  private DefaultAnalysisMode mode;

  @Before
  public void before() {
    mode = mock(DefaultAnalysisMode.class);
    module = new DefaultInputModule(ProjectDefinition.create().setKey("struts-core"));

  }

  private ProjectRepositories createSettings(String module, Map<String, String> settingsMap) {
    Table<String, String, FileData> fileData = ImmutableTable.of();
    Table<String, String, String> settings = HashBasedTable.create();

    for (Map.Entry<String, String> e : settingsMap.entrySet()) {
      settings.put(module, e.getKey(), e.getValue());
    }
    return new ProjectRepositories(settings, fileData, null);
  }

  @Test
  public void testOrderedProjects() {
    DefaultInputModule grandParent = new DefaultInputModule(ProjectDefinition.create().setKey("a"));
    DefaultInputModule parent = new DefaultInputModule(ProjectDefinition.create().setKey("b"));
    DefaultInputModule child = new DefaultInputModule(ProjectDefinition.create().setKey("c"));

    Map<DefaultInputModule, DefaultInputModule> map = new HashMap<>();
    map.put(child, parent);
    map.put(parent, grandParent);

    DefaultInputModuleHierarchy hierarchy = new DefaultInputModuleHierarchy(map);

    ModuleSettings moduleSettings = new ModuleSettings(newGlobalSettings(Collections.emptyMap()), child, hierarchy, mock(ProjectRepositories.class),
      mode, mock(AnalysisContextReportPublisher.class));

    List<DefaultInputModule> h = moduleSettings.getTopDownParentProjects();
    assertThat(h.get(0).key()).isEqualTo(grandParent.key());
    assertThat(h.get(1).key()).isEqualTo(parent.key());
    assertThat(h.get(2).key()).isEqualTo(child.key());
  }

  @Test
  public void test_loading_of_module_settings() {
    GlobalSettings globalSettings = newGlobalSettings(ImmutableMap.of(
      "overridding", "batch",
      "on-batch", "true"));

    ProjectRepositories projRepos = createSettings("struts-core", ImmutableMap.of("on-module", "true", "overridding", "module"));
    ModuleSettings moduleSettings = new ModuleSettings(globalSettings, module, mock(DefaultInputModuleHierarchy.class),
      projRepos, mode, mock(AnalysisContextReportPublisher.class));

    assertThat(moduleSettings.getString("overridding")).isEqualTo("module");
    assertThat(moduleSettings.getString("on-batch")).isEqualTo("true");
    assertThat(moduleSettings.getString("on-module")).isEqualTo("true");
  }

  // SONAR-6386
  @Test
  public void test_loading_of_parent_module_settings_for_new_module() {
    GlobalSettings globalSettings = newGlobalSettings(ImmutableMap.of(
      "overridding", "batch",
      "on-batch", "true"));

    ProjectRepositories projRepos = createSettings("struts", ImmutableMap.of("on-module", "true", "overridding", "module"));
    DefaultInputModule parent = new DefaultInputModule(ProjectDefinition.create().setKey("struts"));
    DefaultInputModuleHierarchy hierarchy = new DefaultInputModuleHierarchy(parent, module);

    ModuleSettings moduleSettings = new ModuleSettings(globalSettings, module, hierarchy,
      projRepos, mode, mock(AnalysisContextReportPublisher.class));

    assertThat(moduleSettings.getString("overridding")).isEqualTo("module");
    assertThat(moduleSettings.getString("on-batch")).isEqualTo("true");
    assertThat(moduleSettings.getString("on-module")).isEqualTo("true");
  }

  @Test
  public void should_not_fail_when_accessing_secured_properties() {
    GlobalSettings globalSettings = newGlobalSettings(ImmutableMap.of("sonar.foo.secured", "bar"));

    ProjectRepositories projSettingsRepo = createSettings("struts-core", ImmutableMap.of("sonar.foo.license.secured", "bar2"));

    ModuleSettings moduleSettings = new ModuleSettings(globalSettings, module, mock(DefaultInputModuleHierarchy.class),
      projSettingsRepo, mode, mock(AnalysisContextReportPublisher.class));

    assertThat(moduleSettings.getString("sonar.foo.license.secured")).isEqualTo("bar2");
    assertThat(moduleSettings.getString("sonar.foo.secured")).isEqualTo("bar");
  }

  @Test
  public void should_fail_when_accessing_secured_properties_in_issues() {
    GlobalSettings globalSettings = newGlobalSettings(ImmutableMap.of("sonar.foo.secured", "bar"));
    ProjectRepositories projSettingsRepo = createSettings("struts-core", ImmutableMap.of("sonar.foo.license.secured", "bar2"));
    when(mode.isIssues()).thenReturn(true);

    ModuleSettings moduleSettings = new ModuleSettings(globalSettings, module, mock(DefaultInputModuleHierarchy.class),
      projSettingsRepo, mode, mock(AnalysisContextReportPublisher.class));
    assertThat(moduleSettings.getString("sonar.foo.license.secured")).isEqualTo("bar2");

    thrown.expect(MessageException.class);
    thrown.expectMessage(
      "Access to the secured property 'sonar.foo.secured' is not possible in issues mode. The SonarQube plugin which requires this property must be deactivated in issues mode.");
    moduleSettings.getString("sonar.foo.secured");
  }

  private GlobalSettings newGlobalSettings(Map<String, String> props) {
    GlobalProperties globalProps = new GlobalProperties(props);
    return new GlobalSettings(globalProps, new PropertyDefinitions(),
      mock(SettingsLoader.class), new GlobalMode(globalProps));
  }
}
