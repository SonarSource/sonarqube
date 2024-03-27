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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingQuery;
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
  @Mock
  private DbClient dbClient;

  @InjectMocks
  private ProjectBindingsService underTest;

  @Captor
  private ArgumentCaptor<ProjectAlmSettingQuery> daoQueryCaptor;

  @BeforeEach
  void setup() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.projectAlmSettingDao()).thenReturn(mock(ProjectAlmSettingDao.class));
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
    ProjectAlmSettingDto dto1 = mock();
    ProjectAlmSettingDto dto2 = mock();
    List<ProjectAlmSettingDto> expectedResults = List.of(dto1, dto2);

    when(dbClient.projectAlmSettingDao().selectProjectAlmSettings(eq(dbSession), daoQueryCaptor.capture(), eq(12), eq(42)))
      .thenReturn(expectedResults);
    when(dbClient.projectAlmSettingDao().countProjectAlmSettings(eq(dbSession), any()))
      .thenReturn(expectedResults.size());

    ProjectBindingsSearchRequest request = new ProjectBindingsSearchRequest(REPO_QUERY, ALM_SETTING_UUID_QUERY, 12, 42);
    SearchResults<ProjectAlmSettingDto> actualResults = underTest.findProjectBindingsByRequest(request);

    assertThat(daoQueryCaptor.getValue().repository()).isEqualTo(REPO_QUERY);
    assertThat(daoQueryCaptor.getValue().almSettingUuid()).isEqualTo(ALM_SETTING_UUID_QUERY);
    assertThat(actualResults.total()).isEqualTo(expectedResults.size());
    assertThat(actualResults.searchResults()).containsExactlyInAnyOrderElementsOf(expectedResults);
  }

  @Test
  void findProjectBindingsByRequest_whenPageSize0_returnsOnlyTotal() {
    when(dbClient.projectAlmSettingDao().countProjectAlmSettings(eq(dbSession), any()))
      .thenReturn(12);

    ProjectBindingsSearchRequest request = new ProjectBindingsSearchRequest(null, null, 42, 0);
    SearchResults<ProjectAlmSettingDto> actualResults = underTest.findProjectBindingsByRequest(request);

    assertThat(actualResults.total()).isEqualTo(12);
    assertThat(actualResults.searchResults()).isEmpty();

    verify(dbClient.projectAlmSettingDao(), never()).selectProjectAlmSettings(eq(dbSession), any(), anyInt(), anyInt());
  }

}
