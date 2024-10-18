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
package org.sonar.server.rule.registration;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileChangeDao;
import org.sonar.db.rule.RuleChangeDao;
import org.sonar.db.rule.RuleChangeDto;
import org.sonar.db.rule.RuleImpactChangeDto;
import org.sonar.server.rule.PluginRuleUpdate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QualityProfileChangesUpdaterTest {

  public static final String RULE_UUID = "ruleUuid";
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final RuleChangeDao ruleChangeDao = mock();
  private final QProfileChangeDao qualityProfileChangeDao = mock();
  private final ActiveRuleDao activeRuleDao = mock();
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));

  private final QualityProfileChangesUpdater underTest = new QualityProfileChangesUpdater(dbClient, UuidFactoryImpl.INSTANCE, sonarQubeVersion);

  @BeforeEach
  public void before() {
    when(dbClient.ruleChangeDao()).thenReturn(ruleChangeDao);
    when(dbClient.qProfileChangeDao()).thenReturn(qualityProfileChangeDao);
    when(dbClient.activeRuleDao()).thenReturn(activeRuleDao);
  }

  @Test
  void updateWithoutCommit_whenNoRuleChanges_thenDontInteractWithDatabase() {
    underTest.createQprofileChangesForRuleUpdates(mock(), Set.of());

    verifyNoInteractions(dbClient);
  }

  @Test
  void updateWithoutCommit_whenOneRuleChangedItsAttribute_thenInsertRuleChangeButNotImpactChange() {
    PluginRuleUpdate pluginRuleUpdate = new PluginRuleUpdate();
    pluginRuleUpdate.setNewCleanCodeAttribute(CleanCodeAttribute.CLEAR);
    pluginRuleUpdate.setOldCleanCodeAttribute(CleanCodeAttribute.TESTED);
    pluginRuleUpdate.setRuleUuid(RULE_UUID);

    underTest.createQprofileChangesForRuleUpdates(dbSession, Set.of(pluginRuleUpdate));

    verify(ruleChangeDao).insert(argThat(dbSession::equals), argThat(ruleChangeDto -> ruleChangeDto.getNewCleanCodeAttribute() == CleanCodeAttribute.CLEAR
      && ruleChangeDto.getOldCleanCodeAttribute() == CleanCodeAttribute.TESTED
      && ruleChangeDto.getRuleUuid().equals(RULE_UUID)
      && ruleChangeDto.getRuleImpactChanges().isEmpty()));
  }

  @Test
  void updateWithoutCommit_whenTwoRulesChangedTheirImpactsAndAttributes_thenInsertRuleChangeAndImpactChange() {
    PluginRuleUpdate pluginRuleUpdate = new PluginRuleUpdate();
    pluginRuleUpdate.setNewCleanCodeAttribute(CleanCodeAttribute.CLEAR);
    pluginRuleUpdate.setOldCleanCodeAttribute(CleanCodeAttribute.TESTED);
    pluginRuleUpdate.setRuleUuid(RULE_UUID);

    // testing here detecting the change with 2 the same software qualities
    pluginRuleUpdate.addNewImpact(SoftwareQuality.RELIABILITY, Severity.LOW);
    pluginRuleUpdate.addOldImpact(SoftwareQuality.RELIABILITY, Severity.MEDIUM);

    PluginRuleUpdate pluginRuleUpdate2 = new PluginRuleUpdate();
    pluginRuleUpdate2.setNewCleanCodeAttribute(CleanCodeAttribute.EFFICIENT);
    pluginRuleUpdate2.setOldCleanCodeAttribute(CleanCodeAttribute.DISTINCT);
    pluginRuleUpdate2.setRuleUuid("ruleUuid2");

    // testing here detecting the change with 2 the different software qualities
    pluginRuleUpdate2.addNewImpact(SoftwareQuality.SECURITY, Severity.HIGH);
    pluginRuleUpdate2.addOldImpact(SoftwareQuality.RELIABILITY, Severity.MEDIUM);

    underTest.createQprofileChangesForRuleUpdates(dbSession, Set.of(pluginRuleUpdate, pluginRuleUpdate2));

    ArgumentCaptor<RuleChangeDto> captor = ArgumentCaptor.forClass(RuleChangeDto.class);
    verify(ruleChangeDao, times(2)).insert(argThat(dbSession::equals), captor.capture());

    RuleChangeDto firstChange = captor.getAllValues().stream().filter(change -> change.getRuleUuid().equals(RULE_UUID)).findFirst().get();
    RuleChangeDto secondChange = captor.getAllValues().stream().filter(change -> change.getRuleUuid().equals("ruleUuid2")).findFirst().get();

    assertThat(firstChange.getNewCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CLEAR);
    assertThat(firstChange.getOldCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.TESTED);
    assertThat(firstChange.getRuleUuid()).isEqualTo(RULE_UUID);
    assertThat(firstChange.getRuleImpactChanges()).isEmpty();

    assertThat(secondChange.getNewCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.EFFICIENT);
    assertThat(secondChange.getOldCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.DISTINCT);
    assertThat(secondChange.getRuleUuid()).isEqualTo("ruleUuid2");
    assertThat(secondChange.getRuleImpactChanges()).hasSize(1);
    assertThat(secondChange.getRuleImpactChanges()).extracting(RuleImpactChangeDto::getNewSoftwareQuality,
      RuleImpactChangeDto::getOldSoftwareQuality, RuleImpactChangeDto::getOldSeverity, RuleImpactChangeDto::getNewSeverity)
      .containsExactly(tuple(SoftwareQuality.SECURITY, SoftwareQuality.RELIABILITY, Severity.MEDIUM, Severity.HIGH));
  }

  @Test
  void updateWithoutCommit_whenImpactsSeverityIsChanged_shouldNotCreateRuleChange() {
    PluginRuleUpdate pluginRuleUpdate = new PluginRuleUpdate();
    pluginRuleUpdate.setRuleUuid(RULE_UUID);

    // testing here detecting the change with 2 the same software qualities
    pluginRuleUpdate.addNewImpact(SoftwareQuality.RELIABILITY, Severity.LOW);
    pluginRuleUpdate.addOldImpact(SoftwareQuality.RELIABILITY, Severity.MEDIUM);

    underTest.createQprofileChangesForRuleUpdates(dbSession, Set.of(pluginRuleUpdate));

    verifyNoInteractions(ruleChangeDao);
  }

  @Test
  void updateWithoutCommit_whenOneRuleBelongingToTwoQualityProfilesChanged_thenInsertOneRuleChangeAndTwoQualityProfileChanges() {
    List<ActiveRuleDto> activeRuleDtos = List.of(
      new ActiveRuleDto().setProfileUuid("profileUuid1").setRuleUuid(RULE_UUID),
      new ActiveRuleDto().setProfileUuid("profileUuid2").setRuleUuid(RULE_UUID));
    when(activeRuleDao.selectByRuleUuid(any(), any())).thenReturn(activeRuleDtos);

    PluginRuleUpdate pluginRuleUpdate = new PluginRuleUpdate();
    pluginRuleUpdate.setNewCleanCodeAttribute(CleanCodeAttribute.CLEAR);
    pluginRuleUpdate.setOldCleanCodeAttribute(CleanCodeAttribute.TESTED);
    pluginRuleUpdate.setRuleUuid(RULE_UUID);

    underTest.createQprofileChangesForRuleUpdates(dbSession, Set.of(pluginRuleUpdate));

    verify(qualityProfileChangeDao, times(1)).bulkInsert(argThat(dbSession::equals),
      argThat(qProfileChangeDtos -> qProfileChangeDtos.stream()
        .allMatch(dto -> "UPDATED".equals(dto.getChangeType()) && dto.getRuleChange() != null)));
  }
}
