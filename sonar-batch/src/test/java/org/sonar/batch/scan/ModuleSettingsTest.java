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
package org.sonar.batch.scan;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.batch.bootstrap.BatchSettings;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModuleSettingsTest {

  @Test
  public void testOrderedProjects() {
    ProjectDefinition grandParent = ProjectDefinition.create();
    ProjectDefinition parent = ProjectDefinition.create();
    ProjectDefinition child = ProjectDefinition.create();
    grandParent.addSubProject(parent);
    parent.addSubProject(child);

    List<ProjectDefinition> hierarchy = ModuleSettings.getTopDownParentProjects(child);
    assertThat(hierarchy.get(0)).isEqualTo(grandParent);
    assertThat(hierarchy.get(1)).isEqualTo(parent);
    assertThat(hierarchy.get(2)).isEqualTo(child);
  }

  @Test
  public void test_loading_of_module_settings() {
    BatchSettings batchSettings = mock(BatchSettings.class);
    when(batchSettings.getDefinitions()).thenReturn(new PropertyDefinitions());
    when(batchSettings.getProperties()).thenReturn(ImmutableMap.of(
      "overridding", "batch",
      "on-batch", "true"
    ));
    when(batchSettings.getModuleProperties("struts-core")).thenReturn(ImmutableMap.of(
      "on-module", "true",
      "overridding", "module"
    ));

    ProjectDefinition module = ProjectDefinition.create().setKey("struts-core");
    Configuration deprecatedConf = new PropertiesConfiguration();

    ModuleSettings moduleSettings = new ModuleSettings(batchSettings, module, deprecatedConf);

    assertThat(moduleSettings.getString("overridding")).isEqualTo("module");
    assertThat(moduleSettings.getString("on-batch")).isEqualTo("true");
    assertThat(moduleSettings.getString("on-module")).isEqualTo("true");

    assertThat(deprecatedConf.getString("overridding")).isEqualTo("module");
    assertThat(deprecatedConf.getString("on-batch")).isEqualTo("true");
    assertThat(deprecatedConf.getString("on-module")).isEqualTo("true");
  }
}
