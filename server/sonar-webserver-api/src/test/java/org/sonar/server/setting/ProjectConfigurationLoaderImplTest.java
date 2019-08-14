/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ProjectConfigurationLoaderImplTest {
  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);
  private PropertiesDao propertiesDao = mock(PropertiesDao.class);
  private MapSettings globalSettings = new MapSettings();
  private ProjectConfigurationLoaderImpl underTest = new ProjectConfigurationLoaderImpl(globalSettings, dbClient);

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(anyBoolean()))
      .thenThrow(new IllegalStateException("ProjectConfigurationLoaderImpl should not open DB session"));
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
  }

  @Test
  public void returns_empty_map_when_no_component() {
    assertThat(underTest.loadProjectConfigurations(dbSession, Collections.emptySet()))
      .isEmpty();

    verifyZeroInteractions(propertiesDao);
  }

  @Test
  public void return_configuration_with_just_global_settings_when_no_component_settings() {
    String key = randomAlphanumeric(3);
    String value = randomAlphanumeric(4);
    String componentDbKey = randomAlphanumeric(5);
    String componentUuid = randomAlphanumeric(6);
    globalSettings.setProperty(key, value);
    when(propertiesDao.selectProjectProperties(dbSession, componentDbKey))
      .thenReturn(emptyList());
    ComponentDto component = newComponentDto(componentDbKey, componentUuid);

    Map<String, Configuration> configurations = underTest.loadProjectConfigurations(dbSession, singleton(component));

    assertThat(configurations)
      .containsOnlyKeys(componentUuid);
    assertThat(configurations.get(componentUuid).get(key)).contains(value);
  }

  @Test
  public void return_configuration_with_global_settings_and_component_settings() {
    String globalKey = randomAlphanumeric(3);
    String globalValue = randomAlphanumeric(4);
    String componentDbKey = randomAlphanumeric(5);
    String componentUuid = randomAlphanumeric(6);
    String projectPropKey1 = randomAlphanumeric(7);
    String projectPropValue1 = randomAlphanumeric(8);
    String projectPropKey2 = randomAlphanumeric(9);
    String projectPropValue2 = randomAlphanumeric(10);
    globalSettings.setProperty(globalKey, globalValue);
    when(propertiesDao.selectProjectProperties(dbSession, componentDbKey))
      .thenReturn(ImmutableList.of(newPropertyDto(projectPropKey1, projectPropValue1), newPropertyDto(projectPropKey2, projectPropValue2)));
    ComponentDto component = newComponentDto(componentDbKey, componentUuid);

    Map<String, Configuration> configurations = underTest.loadProjectConfigurations(dbSession, singleton(component));

    assertThat(configurations)
      .containsOnlyKeys(componentUuid);
    assertThat(configurations.get(componentUuid).get(globalKey)).contains(globalValue);
    assertThat(configurations.get(componentUuid).get(projectPropKey1)).contains(projectPropValue1);
    assertThat(configurations.get(componentUuid).get(projectPropKey2)).contains(projectPropValue2);
  }

  @Test
  public void return_configuration_with_global_settings_main_branch_settings_and_branch_settings() {
    String globalKey = randomAlphanumeric(3);
    String globalValue = randomAlphanumeric(4);
    String mainBranchDbKey = randomAlphanumeric(5);
    String branchDbKey = mainBranchDbKey + ComponentDto.BRANCH_KEY_SEPARATOR + randomAlphabetic(5);
    String branchUuid = randomAlphanumeric(6);
    String mainBranchPropKey = randomAlphanumeric(7);
    String mainBranchPropValue = randomAlphanumeric(8);
    String branchPropKey = randomAlphanumeric(9);
    String branchPropValue = randomAlphanumeric(10);
    globalSettings.setProperty(globalKey, globalValue);
    when(propertiesDao.selectProjectProperties(dbSession, mainBranchDbKey))
      .thenReturn(ImmutableList.of(newPropertyDto(mainBranchPropKey, mainBranchPropValue)));
    when(propertiesDao.selectProjectProperties(dbSession, branchDbKey))
      .thenReturn(ImmutableList.of(newPropertyDto(branchPropKey, branchPropValue)));
    ComponentDto component = newComponentDto(branchDbKey, branchUuid);

    Map<String, Configuration> configurations = underTest.loadProjectConfigurations(dbSession, singleton(component));

    assertThat(configurations)
      .containsOnlyKeys(branchUuid);
    assertThat(configurations.get(branchUuid).get(globalKey)).contains(globalValue);
    assertThat(configurations.get(branchUuid).get(mainBranchPropKey)).contains(mainBranchPropValue);
    assertThat(configurations.get(branchUuid).get(branchPropKey)).contains(branchPropValue);
  }

  @Test
  public void loads_configuration_of_any_given_component_only_once() {
    String mainBranch1DbKey = randomAlphanumeric(4);
    String mainBranch1Uuid = randomAlphanumeric(5);
    String branch1DbKey = mainBranch1DbKey + ComponentDto.BRANCH_KEY_SEPARATOR + randomAlphabetic(5);
    String branch1Uuid = randomAlphanumeric(6);
    String branch2DbKey = mainBranch1DbKey + ComponentDto.BRANCH_KEY_SEPARATOR + randomAlphabetic(7);
    String branch2Uuid = randomAlphanumeric(8);
    String mainBranch2DbKey = randomAlphanumeric(14);
    String mainBranch2Uuid = randomAlphanumeric(15);
    String branch3DbKey = mainBranch2DbKey + ComponentDto.BRANCH_KEY_SEPARATOR + randomAlphabetic(5);
    String branch3Uuid = randomAlphanumeric(16);

    ComponentDto mainBranch1 = newComponentDto(mainBranch1DbKey, mainBranch1Uuid);
    ComponentDto branch1 = newComponentDto(branch1DbKey, branch1Uuid);
    ComponentDto branch2 = newComponentDto(branch2DbKey, branch2Uuid);
    ComponentDto mainBranch2 = newComponentDto(mainBranch2DbKey, mainBranch2Uuid);
    ComponentDto branch3 = newComponentDto(branch3DbKey, branch3Uuid);

    underTest.loadProjectConfigurations(dbSession, ImmutableSet.of(mainBranch1, mainBranch2, branch1, branch2, branch3));

    verify(propertiesDao, times(1)).selectProjectProperties(dbSession, mainBranch1DbKey);
    verify(propertiesDao, times(1)).selectProjectProperties(dbSession, mainBranch2DbKey);
    verify(propertiesDao, times(1)).selectProjectProperties(dbSession, branch1DbKey);
    verify(propertiesDao, times(1)).selectProjectProperties(dbSession, branch2DbKey);
    verify(propertiesDao, times(1)).selectProjectProperties(dbSession, branch3DbKey);
    verifyNoMoreInteractions(propertiesDao);
  }

  private ComponentDto newComponentDto(String componentDbKey, String componentUuid) {
    return new ComponentDto().setDbKey(componentDbKey).setUuid(componentUuid);
  }

  private PropertyDto newPropertyDto(String projectKey1, String projectValue1) {
    return new PropertyDto()
      .setKey(projectKey1)
      .setValue(projectValue1);
  }
}
