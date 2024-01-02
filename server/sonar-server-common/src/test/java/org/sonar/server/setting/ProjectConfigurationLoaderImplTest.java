/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.setting;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertiesDao;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectConfigurationLoaderImplTest {
  @Rule
  public DbTester db = DbTester.create();

  private final String globalPropKey = randomAlphanumeric(9);
  private final String globalPropValue = randomAlphanumeric(10);
  private final String mainBranchPropKey = randomAlphanumeric(7);
  private final String mainBranchPropValue = randomAlphanumeric(8);
  private final String branchPropKey = randomAlphanumeric(9);
  private final String branchPropValue = randomAlphanumeric(10);

  private final String mainBranchUuid = randomAlphanumeric(6);
  private final String branchUuid = randomAlphanumeric(6);
  private final MapSettings globalSettings = new MapSettings();
  private ProjectConfigurationLoaderImpl underTest;

  @Before
  public void setUp() {
    underTest = new ProjectConfigurationLoaderImpl(globalSettings, db.getDbClient());
  }

  @Test
  public void return_configuration_with_just_global_settings_when_no_component_settings() {
    globalSettings.setProperty(mainBranchPropKey, mainBranchPropValue);
    ComponentDto component = newComponentDto(mainBranchUuid);

    Configuration configuration = underTest.loadProjectConfiguration(db.getSession(), component);

    assertThat(configuration.get(mainBranchPropKey)).contains(mainBranchPropValue);
  }

  @Test
  public void return_configuration_with_global_settings_and_component_settings() {
    String projectPropKey1 = randomAlphanumeric(7);
    String projectPropValue1 = randomAlphanumeric(8);
    String projectPropKey2 = randomAlphanumeric(9);
    String projectPropValue2 = randomAlphanumeric(10);
    globalSettings.setProperty(globalPropKey, globalPropValue);
    db.properties().insertProperty(projectPropKey1, projectPropValue1, mainBranchUuid);
    db.properties().insertProperty(projectPropKey2, projectPropValue2, mainBranchUuid);
    ComponentDto component = newComponentDto(mainBranchUuid);

    Configuration configuration = underTest.loadProjectConfiguration(db.getSession(), component);

    assertThat(configuration.get(globalPropKey)).contains(globalPropValue);
    assertThat(configuration.get(projectPropKey1)).contains(projectPropValue1);
    assertThat(configuration.get(projectPropKey2)).contains(projectPropValue2);
  }

  @Test
  public void return_configuration_with_global_settings_main_branch_settings_and_branch_settings() {
    globalSettings.setProperty(globalPropKey, globalPropValue);

    db.properties().insertProperty(mainBranchPropKey, mainBranchPropValue, mainBranchUuid);
    db.properties().insertProperty(branchPropKey, branchPropValue, branchUuid);

    ComponentDto component = newComponentDto(branchUuid, mainBranchUuid);
    Configuration configuration = underTest.loadProjectConfiguration(db.getSession(), component);

    assertThat(configuration.get(globalPropKey)).contains(globalPropValue);
    assertThat(configuration.get(mainBranchPropKey)).contains(mainBranchPropValue);
    assertThat(configuration.get(branchPropKey)).contains(branchPropValue);
  }

  private ComponentDto newComponentDto(String uuid) {
    return newComponentDto(uuid, null);
  }

  private ComponentDto newComponentDto(String uuid, @Nullable String mainBranchUuid) {
    return new ComponentDto().setUuid(uuid).setBranchUuid(uuid).setMainBranchProjectUuid(mainBranchUuid);
  }
}
