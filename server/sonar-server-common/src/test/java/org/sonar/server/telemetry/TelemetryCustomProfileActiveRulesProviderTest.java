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
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryCustomProfileActiveRulesProviderTest {

  @Mock
  private DbClient dbClient;
  @Mock
  private DbSession dbSession;
  @Mock
  private QualityProfileDao qualityProfileDao;
  @Mock
  private ActiveRuleDao activeRuleDao;

  private TelemetryCustomProfileActiveRulesProvider underTest;

  @BeforeEach
  void setUp() {
    lenient().when(dbClient.openSession(false)).thenReturn(dbSession);
    lenient().when(dbClient.qualityProfileDao()).thenReturn(qualityProfileDao);
    lenient().when(dbClient.activeRuleDao()).thenReturn(activeRuleDao);
    lenient().when(qualityProfileDao.selectAllDefaultProfiles(dbSession)).thenReturn(List.of());
    lenient().when(qualityProfileDao.countProjectsByProfiles(eq(dbSession), anyList())).thenReturn(Map.of());
    lenient().when(activeRuleDao.selectByProfileUuids(eq(dbSession), anyCollection())).thenReturn(List.of());
    underTest = new TelemetryCustomProfileActiveRulesProvider(dbClient);
  }

  @Test
  void getValues_includesCustomProfileThatIsDefault() {
    QProfileDto customProfile = newProfile("custom-uuid", false);
    when(qualityProfileDao.selectAll(dbSession)).thenReturn(List.of(customProfile));
    when(qualityProfileDao.selectAllDefaultProfiles(dbSession)).thenReturn(List.of(customProfile));

    OrgActiveRuleDto activeRule = newActiveRule(customProfile, "custom-uuid", "java", "S1234", "MAJOR");
    when(activeRuleDao.selectByProfileUuids(dbSession, List.of("custom-uuid"))).thenReturn(List.of(activeRule));

    assertThat(underTest.getValues()).containsExactlyInAnyOrderEntriesOf(Map.of("custom-uuid|java:S1234", "MAJOR"));
  }

  @Test
  void getValues_includesCustomProfileThatHasProjectAssociations() {
    QProfileDto customProfile = newProfile("custom-uuid", false);
    when(qualityProfileDao.selectAll(dbSession)).thenReturn(List.of(customProfile));
    when(qualityProfileDao.countProjectsByProfiles(eq(dbSession), anyList())).thenReturn(Map.of("custom-uuid", 3L));

    OrgActiveRuleDto activeRule = newActiveRule(customProfile, "custom-uuid", "java", "S1234", "MAJOR");
    when(activeRuleDao.selectByProfileUuids(dbSession, List.of("custom-uuid"))).thenReturn(List.of(activeRule));

    assertThat(underTest.getValues()).containsExactlyInAnyOrderEntriesOf(Map.of("custom-uuid|java:S1234", "MAJOR"));
  }

  @Test
  void getValues_excludesCustomProfileThatIsNeitherDefaultNorAssociatedToProjects() {
    QProfileDto customProfile = newProfile("custom-uuid", false);
    when(qualityProfileDao.selectAll(dbSession)).thenReturn(List.of(customProfile));

    assertThat(underTest.getValues()).isEmpty();
    verify(activeRuleDao, never()).selectByProfileUuids(eq(dbSession), anyCollection());
  }

  @Test
  void getValues_excludesBuiltInProfileEvenIfDefault() {
    QProfileDto builtInProfile = newProfile("builtin-uuid", true);
    when(qualityProfileDao.selectAll(dbSession)).thenReturn(List.of(builtInProfile));
    when(qualityProfileDao.selectAllDefaultProfiles(dbSession)).thenReturn(List.of(builtInProfile));

    assertThat(underTest.getValues()).isEmpty();
    verify(activeRuleDao, never()).selectByProfileUuids(eq(dbSession), anyCollection());
  }

  @Test
  void getValues_whenMultipleRelevantCustomProfiles_returnsAllEntries() {
    QProfileDto defaultCustomProfile = newProfile("custom-uuid-1", false);
    QProfileDto associatedCustomProfile = newProfile("custom-uuid-2", false);
    when(qualityProfileDao.selectAll(dbSession)).thenReturn(List.of(defaultCustomProfile, associatedCustomProfile));
    when(qualityProfileDao.selectAllDefaultProfiles(dbSession)).thenReturn(List.of(defaultCustomProfile));
    when(qualityProfileDao.countProjectsByProfiles(eq(dbSession), anyList())).thenReturn(Map.of("custom-uuid-2", 1L));

    OrgActiveRuleDto activeRule1 = newActiveRule(defaultCustomProfile, "custom-uuid-1", "java", "S1234", "MAJOR");
    OrgActiveRuleDto activeRule2 = newActiveRule(associatedCustomProfile, "custom-uuid-2", "java", "S5678", "BLOCKER");
    when(activeRuleDao.selectByProfileUuids(dbSession, List.of("custom-uuid-1", "custom-uuid-2")))
      .thenReturn(List.of(activeRule1, activeRule2));

    assertThat(underTest.getValues()).containsExactlyInAnyOrderEntriesOf(Map.of(
      "custom-uuid-1|java:S1234", "MAJOR",
      "custom-uuid-2|java:S5678", "BLOCKER"));
  }

  @Test
  void getValues_excludesActiveRulesBasedOnACustomRuleTemplate() {
    QProfileDto customProfile = newProfile("custom-uuid", false);
    when(qualityProfileDao.selectAll(dbSession)).thenReturn(List.of(customProfile));
    when(qualityProfileDao.selectAllDefaultProfiles(dbSession)).thenReturn(List.of(customProfile));

    OrgActiveRuleDto regularRule = newActiveRule(customProfile, "custom-uuid", "java", "S1234", "MAJOR");
    OrgActiveRuleDto customRule = newActiveRule(customProfile, "custom-uuid", "java", "S5678", "BLOCKER");
    customRule.setTemplateUuid("template-uuid");
    when(activeRuleDao.selectByProfileUuids(dbSession, List.of("custom-uuid"))).thenReturn(List.of(regularRule, customRule));

    assertThat(underTest.getValues()).containsExactlyInAnyOrderEntriesOf(Map.of("custom-uuid|java:S1234", "MAJOR"));
  }

  @Test
  void getValues_limitsNumberOfCustomProfilesTo100() {
    List<QProfileDto> profiles = IntStream.range(0, 101)
      .mapToObj(i -> newProfile("custom-uuid-" + i, false))
      .toList();
    when(qualityProfileDao.selectAll(dbSession)).thenReturn(profiles);
    when(qualityProfileDao.selectAllDefaultProfiles(dbSession)).thenReturn(profiles);

    // DatabaseUtils.executeLargeInputs de-duplicates and naturally sorts uuids before querying
    List<String> keptUuids = IntStream.range(0, 100).mapToObj(i -> "custom-uuid-" + i).sorted().toList();
    List<OrgActiveRuleDto> activeRules = IntStream.range(0, 100)
      .mapToObj(i -> newActiveRule(profiles.get(i), "custom-uuid-" + i, "java", "S" + i, "MAJOR"))
      .toList();
    when(activeRuleDao.selectByProfileUuids(dbSession, keptUuids)).thenReturn(activeRules);

    assertThat(underTest.getValues()).hasSize(100);
  }

  @Test
  void getValues_whenNoCustomProfiles_returnsEmptyMap() {
    QProfileDto builtInProfile = newProfile("builtin-uuid", true);
    when(qualityProfileDao.selectAll(dbSession)).thenReturn(List.of(builtInProfile));

    assertThat(underTest.getValues()).isEmpty();
    verify(activeRuleDao, never()).selectByProfileUuids(eq(dbSession), anyCollection());
  }

  @Test
  void getMetricKey_returnsExpectedKey() {
    assertThat(underTest.getMetricKey()).isEqualTo("custom_profile_active_rule");
  }

  @Test
  void getDimension_returnsInstallation() {
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
  }

  @Test
  void getGranularity_returnsWeekly() {
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.WEEKLY);
  }

  @Test
  void getType_returnsString() {
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.STRING);
  }

  private static QProfileDto newProfile(String uuid, boolean builtIn) {
    return new QProfileDto()
      .setKee(uuid)
      .setRulesProfileUuid(uuid)
      .setName(uuid)
      .setLanguage("java")
      .setIsBuiltIn(builtIn);
  }

  private static OrgActiveRuleDto newActiveRule(QProfileDto profile, String orgProfileUuid, String repository, String rule, String severity) {
    return (OrgActiveRuleDto) new OrgActiveRuleDto()
      .setOrgProfileUuid(orgProfileUuid)
      .setKey(ActiveRuleKey.of(profile, RuleKey.of(repository, rule)))
      .setSeverity(severity);
  }
}
