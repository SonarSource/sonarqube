/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectConfigurationLoaderImplIT {
  @Rule
  public DbTester db = DbTester.create();

  private final static String GLOBAL_PROP_KEY = "GLOBAL_PROP_KEY";
  private final static String GLOBAL_PROP_VALUE = "GLOBAL_PROP_VALUE";
  private final static String MAIN_BRANCH_PROP_KEY = "MAIN_BRANCH_PROP_KEY";
  private final static String MAIN_BRANCH_PROP_VALUE = "MAIN_BRANCH_PROP_VALUE";
  private final static String BRANCH_PROP_KEY = "BRANCH_PROP_KEY";
  private final static String BRANCH_PROP_VALUE = "BRANCH_PROP_VALUE";

  private final static String MAIN_BRANCH_UUID = "MAIN_BRANCH_UUID";
  private final static String BRANCH_UUID = "BRANCH_UUID";
  private final MapSettings globalSettings = new MapSettings();
  private ProjectConfigurationLoaderImpl underTest;

  @Before
  public void setUp() {
    underTest = new ProjectConfigurationLoaderImpl(globalSettings, db.getDbClient());
  }

  @Test
  public void return_configuration_with_just_global_settings_when_no_component_settings() {
    globalSettings.setProperty(MAIN_BRANCH_PROP_KEY, MAIN_BRANCH_PROP_VALUE);

    BranchDto mainBranch = insertBranch(MAIN_BRANCH_UUID, MAIN_BRANCH_UUID, true);
    Configuration configuration = underTest.loadBranchConfiguration(db.getSession(), mainBranch);

    assertThat(configuration.get(MAIN_BRANCH_PROP_KEY)).contains(MAIN_BRANCH_PROP_VALUE);
  }

  @Test
  public void return_configuration_with_global_settings_and_component_settings() {
    String projectPropKey1 = "prop_key_1";
    String projectPropValue1 = "prop_key_value_1";
    String projectPropKey2 = "prop_key_2";
    String projectPropValue2 = "prop_key_value_2";
    globalSettings.setProperty(GLOBAL_PROP_KEY, GLOBAL_PROP_VALUE);
    db.properties().insertProperty(projectPropKey1, projectPropValue1, MAIN_BRANCH_UUID);
    db.properties().insertProperty(projectPropKey2, projectPropValue2, MAIN_BRANCH_UUID);
    BranchDto mainBranch = insertBranch(MAIN_BRANCH_UUID, MAIN_BRANCH_UUID, true);

    Configuration configuration = underTest.loadBranchConfiguration(db.getSession(), mainBranch);

    assertThat(configuration.get(GLOBAL_PROP_KEY)).contains(GLOBAL_PROP_VALUE);
    assertThat(configuration.get(projectPropKey1)).contains(projectPropValue1);
    assertThat(configuration.get(projectPropKey2)).contains(projectPropValue2);
  }

  @Test
  public void return_configuration_with_global_settings_main_branch_settings_and_branch_settings() {
    globalSettings.setProperty(GLOBAL_PROP_KEY, GLOBAL_PROP_VALUE);

    db.properties().insertProperty(MAIN_BRANCH_PROP_KEY, MAIN_BRANCH_PROP_VALUE, MAIN_BRANCH_UUID);
    db.properties().insertProperty(BRANCH_PROP_KEY, BRANCH_PROP_VALUE, BRANCH_UUID);

    BranchDto mainBranch = insertBranch(MAIN_BRANCH_UUID, MAIN_BRANCH_UUID, true);
    BranchDto branch = insertBranch(BRANCH_UUID, MAIN_BRANCH_UUID, false);

    Configuration configuration = underTest.loadBranchConfiguration(db.getSession(), branch);

    assertThat(configuration.get(GLOBAL_PROP_KEY)).contains(GLOBAL_PROP_VALUE);
    assertThat(configuration.get(MAIN_BRANCH_PROP_KEY)).contains(MAIN_BRANCH_PROP_VALUE);
    assertThat(configuration.get(BRANCH_PROP_KEY)).contains(BRANCH_PROP_VALUE);
  }

  public BranchDto insertBranch(String uuid, String projectUuid, boolean isMain){
    BranchDto dto = new BranchDto();
    dto.setProjectUuid(projectUuid);
    dto.setUuid(uuid);
    dto.setIsMain(isMain);
    dto.setBranchType(BranchType.BRANCH);
    dto.setKey("key_"+uuid);
    db.getDbClient().branchDao().insert(db.getSession(), dto);
    return dto;
  }

}
