/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.telemetry;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDao; 
import org.sonar.db.qualityprofile.ProjectQProfileLanguageAssociationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileDao;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.sonar.server.telemetry.TelemetryAgenticQPAdoptionProvider.AGENTIC_QUALITY_PROFILE_NAME;

@ExtendWith(MockitoExtension.class)
class AgenticQPProjectResolverTest {

  private static final String AGENTIC_PROFILE_UUID = "agentic-profile-uuid";
  private static final String OTHER_PROFILE_UUID = "other-profile-uuid";
  private static final String PROJECT_UUID_1 = "project-uuid-1";
  private static final String PROJECT_UUID_2 = "project-uuid-2";
  private static final String JAVA = "java";
  private static final String PYTHON = "python";

  @Mock
  private DbClient dbClient;
  @Mock
  private DbSession dbSession;
  @Mock
  private QualityProfileDao qualityProfileDao;
  @Mock
  private ProjectDao projectDao;

  private AgenticQPProjectResolver underTest;

  @BeforeEach
  void setUp() {
    lenient().when(dbClient.qualityProfileDao()).thenReturn(qualityProfileDao);
    lenient().when(dbClient.projectDao()).thenReturn(projectDao);
    underTest = new AgenticQPProjectResolver(dbClient);
  }

  @Test
  void resolveAgenticProjectUuidsByLanguage_whenProfileNotFound_returnsEmpty() {
    when(qualityProfileDao.selectBuiltInByName(dbSession, AGENTIC_QUALITY_PROFILE_NAME)).thenReturn(emptyList());

    assertThat(underTest.resolveAgenticProjectUuidsByLanguage(dbSession)).isEmpty();
  }

  @Test
  void resolveAgenticProjectUuidsByLanguage_whenNoProjectsUseAgenticProfile_returnsEmpty() {
    when(qualityProfileDao.selectBuiltInByName(dbSession, AGENTIC_QUALITY_PROFILE_NAME)).thenReturn(List.of(createAgenticProfile(AGENTIC_PROFILE_UUID, JAVA)));
    when(qualityProfileDao.selectAllProjectAssociations(dbSession)).thenReturn(emptyList());
    when(qualityProfileDao.selectAllDefaultProfiles(dbSession)).thenReturn(emptyList());

    assertThat(underTest.resolveAgenticProjectUuidsByLanguage(dbSession)).isEmpty();
  }

  @Test
  void resolveAgenticProjectUuidsByLanguage_whenProjectsHaveExplicitAgenticProfile_returnsThem() {
    when(qualityProfileDao.selectBuiltInByName(dbSession, AGENTIC_QUALITY_PROFILE_NAME)).thenReturn(List.of(createAgenticProfile(AGENTIC_PROFILE_UUID, JAVA)));
    when(qualityProfileDao.selectAllProjectAssociations(dbSession)).thenReturn(List.of(
      new ProjectQProfileLanguageAssociationDto(PROJECT_UUID_1, AGENTIC_PROFILE_UUID, JAVA),
      new ProjectQProfileLanguageAssociationDto(PROJECT_UUID_2, OTHER_PROFILE_UUID, JAVA)));
    when(qualityProfileDao.selectAllDefaultProfiles(dbSession)).thenReturn(emptyList());

    assertThat(underTest.resolveAgenticProjectUuidsByLanguage(dbSession))
      .containsOnlyKeys(JAVA)
      .satisfies(m -> assertThat(m.get(JAVA)).containsExactly(PROJECT_UUID_1));
  }

  @Test
  void resolveAgenticProjectUuidsByLanguage_whenAgenticProfileIsDefault_includesAnalyzedProjectsWithoutExplicitOverride() {
    when(qualityProfileDao.selectBuiltInByName(dbSession, AGENTIC_QUALITY_PROFILE_NAME)).thenReturn(List.of(createAgenticProfile(AGENTIC_PROFILE_UUID, JAVA)));
    when(qualityProfileDao.selectAllProjectAssociations(dbSession)).thenReturn(List.of(
      new ProjectQProfileLanguageAssociationDto(PROJECT_UUID_1, OTHER_PROFILE_UUID, PYTHON),
      new ProjectQProfileLanguageAssociationDto(PROJECT_UUID_2, OTHER_PROFILE_UUID, JAVA)));
    when(qualityProfileDao.selectAllDefaultProfiles(dbSession)).thenReturn(List.of(createAgenticProfile(AGENTIC_PROFILE_UUID, JAVA)));

    assertThat(underTest.resolveAgenticProjectUuidsByLanguage(dbSession))
      .containsOnlyKeys(JAVA)
      .satisfies(m -> assertThat(m.get(JAVA)).containsExactly(PROJECT_UUID_1));
  }

  @Test
  void resolveAgenticProjectUuidsByLanguage_withMultipleLanguages_returnsEntryPerLanguage() {
    QProfileDto javaProfile = createAgenticProfile(AGENTIC_PROFILE_UUID, JAVA);
    QProfileDto pythonProfile = createAgenticProfile("agentic-python-uuid", PYTHON);
    when(qualityProfileDao.selectBuiltInByName(dbSession, AGENTIC_QUALITY_PROFILE_NAME)).thenReturn(List.of(javaProfile, pythonProfile));
    when(qualityProfileDao.selectAllProjectAssociations(dbSession)).thenReturn(List.of(
      new ProjectQProfileLanguageAssociationDto(PROJECT_UUID_1, AGENTIC_PROFILE_UUID, JAVA),
      new ProjectQProfileLanguageAssociationDto(PROJECT_UUID_1, "agentic-python-uuid", PYTHON),
      new ProjectQProfileLanguageAssociationDto(PROJECT_UUID_2, AGENTIC_PROFILE_UUID, JAVA)));
    when(qualityProfileDao.selectAllDefaultProfiles(dbSession)).thenReturn(emptyList());

    assertThat(underTest.resolveAgenticProjectUuidsByLanguage(dbSession))
      .containsOnlyKeys(JAVA, PYTHON)
      .satisfies(m -> {
        assertThat(m.get(JAVA)).containsExactlyInAnyOrder(PROJECT_UUID_1, PROJECT_UUID_2);
        assertThat(m.get(PYTHON)).containsExactly(PROJECT_UUID_1);
      });
  }

  private static QProfileDto createAgenticProfile(String uuid, String language) {
    return new QProfileDto().setKee(uuid).setRulesProfileUuid(uuid).setName(AGENTIC_QUALITY_PROFILE_NAME).setLanguage(language);
  }
}
