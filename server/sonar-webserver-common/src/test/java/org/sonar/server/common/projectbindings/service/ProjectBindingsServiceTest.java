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
package org.sonar.server.common.projectbindings.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingQuery;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.SearchResults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProjectBindingsServiceTest {

  private static final String UUID = "uuid";
  public static final String REPO_QUERY = "repoQuery";
  public static final String ALM_SETTING_UUID_QUERY = "almSettingUuidQuery";

  @Mock
  private DbSession dbSession;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;

  @InjectMocks
  private ProjectBindingsService underTest;

  @Captor
  private ArgumentCaptor<ProjectAlmSettingQuery> daoQueryCaptor;

  @BeforeEach
  void setup() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  @Test
  void findProjectBindingByUuid_whenNoResult_returnsOptionalEmpty() {
    when(dbClient.projectAlmSettingDao().selectByUuid(dbSession, UUID)).thenReturn(Optional.empty());

    assertThat(underTest.findProjectBindingByUuid(UUID)).isEmpty();
  }

  @Test
  void findProjectBindingByUuid_whenResult_returnsIt() {
    ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    when(dbClient.projectAlmSettingDao().selectByUuid(dbSession, UUID)).thenReturn(Optional.of(projectAlmSettingDto));

    assertThat(underTest.findProjectBindingByUuid(UUID)).contains(projectAlmSettingDto);
  }

  @Test
  void findProjectBindingsByRequest_whenResults_returnsThem() {
    ProjectAlmSettingDto projectAlmSettingDto1 = mockProjectAlmSettingDto("1");
    ProjectAlmSettingDto projectAlmSettingDto2 = mockProjectAlmSettingDto("2");
    List<ProjectAlmSettingDto> projectAlmSettings = List.of(projectAlmSettingDto1, projectAlmSettingDto2);

    when(dbClient.projectAlmSettingDao().selectProjectAlmSettings(eq(dbSession), daoQueryCaptor.capture(), eq(12), eq(42)))
      .thenReturn(projectAlmSettings);
    when(dbClient.projectAlmSettingDao().countProjectAlmSettings(eq(dbSession), any()))
      .thenReturn(projectAlmSettings.size());

    ProjectDto mockProjectDto1 = mockProjectDto("1");
    ProjectDto mockProjectDto2 = mockProjectDto("2");

    when(dbClient.projectDao().selectByUuids(dbSession, Set.of("projectUuid_1", "projectUuid_2")))
      .thenReturn(List.of(mockProjectDto1, mockProjectDto2));

    ProjectBindingsSearchRequest request = new ProjectBindingsSearchRequest(REPO_QUERY, ALM_SETTING_UUID_QUERY, 12, 42);

    List<ProjectBindingInformation> expectedResults = List.of(projectBindingInformation("1"), projectBindingInformation("2"));

    SearchResults<ProjectBindingInformation> actualResults = underTest.findProjectBindingsByRequest(request);

    assertThat(daoQueryCaptor.getValue().repository()).isEqualTo(REPO_QUERY);
    assertThat(daoQueryCaptor.getValue().almSettingUuid()).isEqualTo(ALM_SETTING_UUID_QUERY);
    assertThat(actualResults.total()).isEqualTo(projectAlmSettings.size());
    assertThat(actualResults.searchResults()).containsExactlyInAnyOrderElementsOf(expectedResults);
  }

  @Test
  void findProjectBindingsByRequest_whenPageSize0_returnsOnlyTotal() {
    when(dbClient.projectAlmSettingDao().countProjectAlmSettings(eq(dbSession), any()))
      .thenReturn(12);

    ProjectBindingsSearchRequest request = new ProjectBindingsSearchRequest(null, null, 42, 0);
    SearchResults<ProjectBindingInformation> actualResults = underTest.findProjectBindingsByRequest(request);

    assertThat(actualResults.total()).isEqualTo(12);
    assertThat(actualResults.searchResults()).isEmpty();

    verify(dbClient.projectAlmSettingDao(), never()).selectProjectAlmSettings(eq(dbSession), any(), anyInt(), anyInt());
  }

  private static ProjectAlmSettingDto mockProjectAlmSettingDto(String i) {
    ProjectAlmSettingDto dto = mock();
    when(dto.getUuid()).thenReturn("uuid_" + i);
    when(dto.getAlmSettingUuid()).thenReturn("almSettingUuid_" + i);
    when(dto.getProjectUuid()).thenReturn("projectUuid_" + i);
    when(dto.getAlmRepo()).thenReturn("almRepo_" + i);
    when(dto.getAlmSlug()).thenReturn("almSlug_" + i);
    return dto;
  }

  private static ProjectDto mockProjectDto(String i) {
    ProjectDto dto = mock();
    when(dto.getUuid()).thenReturn("projectUuid_" + i);
    when(dto.getKey()).thenReturn("projectKey_" + i);
    return dto;
  }

  private static ProjectBindingInformation projectBindingInformation(String i) {
    return new ProjectBindingInformation("uuid_" + i,
      "almSettingUuid_" + i,
      "projectUuid_" + i,
      "projectKey_" + i,
      "almRepo_" + i,
      "almSlug_" + i);
  }

}
