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
package org.sonar.ce.task.projectanalysis.qualityprofile;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.qualityprofile.QProfileChangeDao;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.rule.RuleChangeDto;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.DEACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.UPDATED;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  QualityProfileRuleChangeResolverTest.TextResolutionTest.class,
  QualityProfileRuleChangeResolverTest.ExceptionTest.class
})
public class QualityProfileRuleChangeResolverTest {
  private static final String COMPONENT_UUID = "123";

  @RunWith(Parameterized.class)
  public static class TextResolutionTest {
    private final DbClient dbClient = mock(DbClient.class);
    private final DbSession dbSession = mock(DbSession.class);
    private final QProfileChangeDao qProfileChangeDao = mock(QProfileChangeDao.class);
    private final SnapshotDao snapshotDao = mock(SnapshotDao.class);
    private final QualityProfile qualityProfile = mock(QualityProfile.class);

    private final QualityProfileRuleChangeResolver underTest = new QualityProfileRuleChangeResolver(dbClient);

    private final List<QProfileChangeDto> changes;
    private final Map<ActiveRuleChange.Type, Long> expectedMap;


    public TextResolutionTest(List<QProfileChangeDto> changes, Map<ActiveRuleChange.Type, Long> expectedMap) {
      this.changes = changes;
      this.expectedMap = expectedMap;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][]{
        {
          List.of(
            createChange(ACTIVATED, "ruleUuid1", 123L),
            createChange(DEACTIVATED, "ruleUuid2", 124L),
            createChange(UPDATED, "ruleUuid3", 125L)
          ),
          Map.ofEntries(
            Map.entry(ACTIVATED, 1L),
            Map.entry(DEACTIVATED, 1L),
            Map.entry(UPDATED, 1L)
          )
        },
        {
          List.of(
            createChange(ACTIVATED, "ruleUuid1", 123L),
            createChange(DEACTIVATED, "ruleUuid1", 124L), // should cancel previous change
            createChange(UPDATED, "ruleUuid2", 125L)
          ),
          Map.ofEntries(
            Map.entry(UPDATED, 1L)
          )
        },
        {
          List.of(
            createChange(ACTIVATED, "ruleUuid1", 123L),
            createChange(DEACTIVATED, "ruleUuid1", 124L), // should cancel previous change
            createChange(ACTIVATED, "ruleUuid1", 125L),
            createChange(UPDATED, "ruleUuid2", 126L)
          ),
          Map.ofEntries(
            Map.entry(ACTIVATED, 1L),
            Map.entry(UPDATED, 1L)
          )
        },
        {
          List.of(
            createChange(ACTIVATED, "ruleUuid1", 123L),
            createCCTUpdate("ruleUuid1", 130L), // should overwrite previous change
            createChange(DEACTIVATED, "ruleUuid2", 124L),
            createChange(DEACTIVATED, "ruleUuid3", 125L),
            createChange(UPDATED, "ruleUuid4", 126L)
          ),
          Map.ofEntries(
            Map.entry(DEACTIVATED, 2L),
            Map.entry(UPDATED, 2L)
          )
        },
        {
          List.of(
            createChange(ACTIVATED, "ruleUuid1", 123L),
            createChange(UPDATED, "ruleUuid1", 130L),
            createChange(DEACTIVATED, "ruleUuid1", 131L), // should overwrite update and cancel out the activation resulting to no change
            createCCTUpdate("ruleUuid2", 126L)
          ),
          Map.ofEntries(
            Map.entry(UPDATED, 1L)
          )
        },
        {
          List.of(
            createChange(DEACTIVATED, "ruleUuid1", 123L),
            createChange(UPDATED, "ruleUuid1", 130L),
            createChange(UPDATED, "ruleUuid2", 126L)
          ),
          Map.ofEntries(
            Map.entry(UPDATED, 2L)
          )
        },
        {
          // single CCT change
          List.of(
            createCCTUpdate("ruleUuid1", 123L)
          ),
          Map.ofEntries(
            Map.entry(UPDATED, 1L)
          )
        },
        {
          // multiple CCT changes
          List.of(
            createCCTUpdate("ruleUuid1", 123L),
            createCCTUpdate("ruleUuid2", 124L)
          ),
          Map.ofEntries(
            Map.entry(UPDATED, 2L)
          )
        },
        {
          // mixed CCT and old taxonomy changes
          List.of(
            createCCTUpdate("ruleUuid1", 123L),
            createChange(ACTIVATED, "ruleUuid2", 124L),
            createCCTUpdate("ruleUuid3", 125L),
            createChange(DEACTIVATED, "ruleUuid4", 126L),
            createChange(UPDATED, "ruleUuid3", 127L)
          ),
          Map.ofEntries(
            Map.entry(ACTIVATED, 1L),
            Map.entry(DEACTIVATED, 1L),
            Map.entry(UPDATED, 2L)
          )
        },
        {
          List.of(
            createChange(ACTIVATED, "ruleUuid1", 123L),
            createChange(DEACTIVATED, "ruleUuid1", 124L) // should cancel previous change
          ),
          Map.ofEntries()
        }
      });
    }

    @Before
    public void setUp() {
      when(dbClient.openSession(false)).thenReturn(dbSession);
      doReturn(qProfileChangeDao).when(dbClient).qProfileChangeDao();

      SnapshotDto snapshotDto = new SnapshotDto()
        .setAnalysisDate(123L);
      doReturn(Optional.of(snapshotDto)).when(snapshotDao).selectLastAnalysisByComponentUuid(dbSession, COMPONENT_UUID);
      doReturn(snapshotDao).when(dbClient).snapshotDao();

      doReturn("profileUuid").when(qualityProfile).getQpKey();
      doReturn("profileName").when(qualityProfile).getQpName();
    }

    @Test
    public void givenQPChanges_whenResolveText_thenResolvedTextContainsAll() {
      // given
      doReturn(changes).when(qProfileChangeDao).selectByQuery(eq(dbSession), any(QProfileChangeQuery.class));

      // when
      Map<ActiveRuleChange.Type, Long> changeToNumberOfRules = underTest.mapChangeToNumberOfRules(qualityProfile, COMPONENT_UUID);

      // then
      assertThat(changeToNumberOfRules).isEqualTo(expectedMap);
    }
  }

  public static class ExceptionTest {

    private final DbClient dbClient = mock(DbClient.class);
    private final DbSession dbSession = mock(DbSession.class);
    private final QProfileChangeDao qProfileChangeDao = mock(QProfileChangeDao.class);
    private final SnapshotDao snapshotDao = mock(SnapshotDao.class);

    private final QualityProfile qualityProfile = mock(QualityProfile.class);

    private final QualityProfileRuleChangeResolver underTest = new QualityProfileRuleChangeResolver(dbClient);


    @Before
    public void setUp() {
      when(dbClient.openSession(false)).thenReturn(dbSession);
      doReturn(qProfileChangeDao).when(dbClient).qProfileChangeDao();

      SnapshotDto snapshotDto = new SnapshotDto()
        .setAnalysisDate(123L);
      doReturn(Optional.of(snapshotDto)).when(snapshotDao).selectLastAnalysisByComponentUuid(dbSession, COMPONENT_UUID);
      doReturn(snapshotDao).when(dbClient).snapshotDao();

      doReturn("profileUuid").when(qualityProfile).getQpKey();
      doReturn("profileName").when(qualityProfile).getQpName();
    }

    @Test
    public void givenNoQPChanges_whenResolveText_thenThrows() {
      // given
      doReturn(List.of()).when(qProfileChangeDao).selectByQuery(eq(dbSession), any(QProfileChangeQuery.class));

      // when then
      assertThatThrownBy(() -> underTest.mapChangeToNumberOfRules(qualityProfile, COMPONENT_UUID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No profile changes found for profileName");
    }

    @Test
    public void givenNoSnapshotFound_whenResolveText_thenThrows() {
      // given
      doReturn(Optional.empty()).when(snapshotDao).selectLastAnalysisByComponentUuid(dbSession, COMPONENT_UUID);

      // when then
      assertThatThrownBy(() -> underTest.mapChangeToNumberOfRules(qualityProfile, COMPONENT_UUID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No snapshot found for 123");
    }
  }

  private static QProfileChangeDto createChange(ActiveRuleChange.Type type, String ruleUuid, Long createdAt) {
    return new QProfileChangeDto()
      .setUuid("uuid")
      .setCreatedAt(createdAt)
      .setRulesProfileUuid("ruleProfileUuid")
      .setChangeType(type.name())
      .setData(KeyValueFormat.parse("ruleUuid=" + ruleUuid));
  }

  private static QProfileChangeDto createCCTUpdate(String ruleUuid, Long createdAt) {
    RuleChangeDto ruleChangeDto = new RuleChangeDto();
    ruleChangeDto.setOldCleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL);
    ruleChangeDto.setNewCleanCodeAttribute(CleanCodeAttribute.CLEAR);
    ruleChangeDto.setRuleUuid(ruleUuid);
    return new QProfileChangeDto()
      .setUuid("uuid")
      .setCreatedAt(createdAt)
      .setRulesProfileUuid("ruleProfileUuid")
      .setChangeType(UPDATED.name())
      .setRuleChange(ruleChangeDto);
  }

}