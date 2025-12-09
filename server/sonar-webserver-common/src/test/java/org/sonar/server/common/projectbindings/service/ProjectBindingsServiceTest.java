/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

    ProjectBindingsSearchRequest request = new ProjectBindingsSearchRequest(REPO_QUERY, ALM_SETTING_UUID_QUERY, null, 12, 42);

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

    ProjectBindingsSearchRequest request = new ProjectBindingsSearchRequest(null, null, null, 42, 0);
    SearchResults<ProjectBindingInformation> actualResults = underTest.findProjectBindingsByRequest(request);

    assertThat(actualResults.total()).isEqualTo(12);
    assertThat(actualResults.searchResults()).isEmpty();

    verify(dbClient.projectAlmSettingDao(), never()).selectProjectAlmSettings(eq(dbSession), any(), anyInt(), anyInt());
  }

  @Test
  void findProjectBindingsByRequest_whenRepositoryUrlProvided_usesGitUrlSearch() {
    when(dbClient.projectAlmSettingDao().selectProjectAlmSettings(eq(dbSession), any(), eq(1), eq(Integer.MAX_VALUE)))
      .thenReturn(List.of());
    when(dbClient.projectDao().selectByUuids(dbSession, Set.of()))
      .thenReturn(List.of());

    ProjectBindingsSearchRequest request = new ProjectBindingsSearchRequest(null, null, "https://github.com/org/repo", 0, 50);

    SearchResults<ProjectBindingInformation> actualResults = underTest.findProjectBindingsByRequest(request);

    assertThat(actualResults.searchResults()).isEmpty();
    assertThat(actualResults.total()).isZero();

    verify(dbClient.projectAlmSettingDao(), never()).countProjectAlmSettings(eq(dbSession), any());
  }

  @Test
  void findProjectFromBinding_whenProjectExists_returnsIt() {
    ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    when(projectAlmSettingDto.getProjectUuid()).thenReturn("project-uuid-123");

    ProjectDto projectDto = mock(ProjectDto.class);
    when(dbClient.projectDao().selectByUuid(dbSession, "project-uuid-123")).thenReturn(Optional.of(projectDto));

    Optional<ProjectDto> result = underTest.findProjectFromBinding(projectAlmSettingDto);

    assertThat(result).isPresent().contains(projectDto);
  }

  @Test
  void findProjectFromBinding_whenProjectDoesNotExist_returnsEmpty() {
    ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    when(projectAlmSettingDto.getProjectUuid()).thenReturn("non-existent-uuid");

    when(dbClient.projectDao().selectByUuid(dbSession, "non-existent-uuid")).thenReturn(Optional.empty());

    Optional<ProjectDto> result = underTest.findProjectFromBinding(projectAlmSettingDto);

    assertThat(result).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "not-a-valid-url"})
  void findProjectBindingsByGitUrl_whenUrlIsInvalid_returnsEmptyResults(@Nullable String url) {
    SearchResults<ProjectBindingInformation> result = underTest.findProjectBindingsByGitUrl(url);

    assertThat(result.searchResults()).isEmpty();
    assertThat(result.total()).isZero();
  }

  @Test
  void findProjectBindingsByGitUrl_whenUrlIsValid_returnsResults() {
    ProjectAlmSettingDto githubSetting = mockProjectAlmSettingDto("1");
    ProjectAlmSettingDto azureSetting = mockProjectAlmSettingDto("2");

    when(dbClient.projectAlmSettingDao().selectProjectAlmSettings(eq(dbSession), any(), eq(1), eq(Integer.MAX_VALUE)))
      .thenReturn(List.of(githubSetting))
      .thenReturn(List.of(azureSetting))
      .thenReturn(List.of())
      .thenReturn(List.of());

    ProjectDto projectDto1 = mockProjectDto("1");
    ProjectDto projectDto2 = mockProjectDto("2");
    when(dbClient.projectDao().selectByUuids(dbSession, Set.of("projectUuid_1", "projectUuid_2")))
      .thenReturn(List.of(projectDto1, projectDto2));

    SearchResults<ProjectBindingInformation> result = underTest.findProjectBindingsByGitUrl("https://github.com/org/repo");

    assertThat(result.searchResults()).hasSize(2);
    assertThat(result.total()).isEqualTo(2);
    assertThat(result.searchResults()).extracting(ProjectBindingInformation::id)
      .containsExactlyInAnyOrder("uuid_1", "uuid_2");
  }

  @Test
  void findProjectBindingsByGitUrl_whenDuplicateResults_removesThemAndReturnsDistinct() {
    ProjectAlmSettingDto duplicatedSetting = mockProjectAlmSettingDto("1");

    when(dbClient.projectAlmSettingDao().selectProjectAlmSettings(eq(dbSession), any(), eq(1), eq(Integer.MAX_VALUE)))
      .thenReturn(List.of(duplicatedSetting))
      .thenReturn(List.of(duplicatedSetting))
      .thenReturn(List.of())
      .thenReturn(List.of());

    ProjectDto projectDto = mockProjectDto("1");
    when(dbClient.projectDao().selectByUuids(dbSession, Set.of("projectUuid_1")))
      .thenReturn(List.of(projectDto));

    SearchResults<ProjectBindingInformation> result = underTest.findProjectBindingsByGitUrl("https://github.com/org/repo");

    assertThat(result.searchResults()).hasSize(1);
    assertThat(result.total()).isEqualTo(1);
    assertThat(result.searchResults().get(0).id()).isEqualTo("uuid_1");
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
    return new ProjectBindingInformation("uuid_" + i, "almSettingUuid_" + i, "projectUuid_" + i, "projectKey_" + i, "almRepo_" + i, "almSlug_" + i);
  }

}
