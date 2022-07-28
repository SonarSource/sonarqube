/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.pushapi.qualityprofile;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.pushevent.PushEventDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonarqube.ws.Common;

import static java.util.List.of;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.db.rule.RuleTesting.newTemplateRule;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;

public class QualityProfileChangeEventServiceImplTest {

  @Rule
  public DbTester db = DbTester.create();

  public final QualityProfileChangeEventServiceImpl underTest = new QualityProfileChangeEventServiceImpl(db.getDbClient());

  @Test
  public void distributeRuleChangeEvent() {
    QProfileDto qualityProfileDto = QualityProfileTesting.newQualityProfileDto();

    RuleDto templateRule = newTemplateRule(RuleKey.of("xoo", "template-key"));
    db.rules().insert(templateRule);

    RuleDto rule1 = newCustomRule(templateRule, "<div>line1\nline2</div>")
      .setLanguage("xoo")
      .setRepositoryKey("repo")
      .setRuleKey("ruleKey")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN);
    db.rules().insert(rule1);

    ActiveRuleDto activeRuleDto = ActiveRuleDto.createFor(qualityProfileDto, rule1);

    ActiveRuleChange activeRuleChange = new ActiveRuleChange(ACTIVATED, activeRuleDto, rule1);
    activeRuleChange.setParameter("paramChangeKey", "paramChangeValue");

    Collection<QProfileDto> profiles = Collections.singleton(qualityProfileDto);

    ProjectDto project = db.components().insertPrivateProjectDto();
    db.qualityProfiles().associateWithProject(project, qualityProfileDto);

    underTest.distributeRuleChangeEvent(profiles, of(activeRuleChange), "xoo");

    Deque<PushEventDto> events = db.getDbClient().pushEventDao()
      .selectChunkByProjectUuids(db.getSession(), Set.of(project.getUuid()), 1l, null, 1);

    assertThat(events).isNotEmpty().hasSize(1);
    assertThat(events.getFirst())
      .extracting(PushEventDto::getName, PushEventDto::getLanguage)
      .contains("RuleSetChanged", "xoo");

    String ruleSetChangedEvent = new String(events.getFirst().getPayload(), StandardCharsets.UTF_8);

    assertThat(ruleSetChangedEvent)
      .contains("\"activatedRules\":[{\"key\":\"repo:ruleKey\"," +
        "\"language\":\"xoo\"," +
        "\"templateKey\":\"xoo:template-key\"," +
        "\"params\":[{\"key\":\"paramChangeKey\",\"value\":\"paramChangeValue\"}]}]," +
        "\"deactivatedRules\":[]");
  }

  @Test
  public void publishRuleActivationToSonarLintClients() {
    ProjectDto projectDao = new ProjectDto().setUuid("project-uuid");
    QProfileDto activatedQualityProfile = QualityProfileTesting.newQualityProfileDto();
    activatedQualityProfile.setLanguage("xoo");
    db.qualityProfiles().insert(activatedQualityProfile);
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("xoo").setRepositoryKey("repo").setRuleKey("ruleKey"));
    RuleParamDto rule1Param = db.rules().insertRuleParam(rule1);

    ActiveRuleDto activeRule1 = db.qualityProfiles().activateRule(activatedQualityProfile, rule1);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param).setValue(randomAlphanumeric(20));
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRule1, activeRuleParam1);
    db.getSession().commit();

    QProfileDto deactivatedQualityProfile = QualityProfileTesting.newQualityProfileDto();
    db.qualityProfiles().insert(deactivatedQualityProfile);
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("xoo").setRepositoryKey("repo2").setRuleKey("ruleKey2"));
    RuleParamDto rule2Param = db.rules().insertRuleParam(rule2);

    ActiveRuleDto activeRule2 = db.qualityProfiles().activateRule(deactivatedQualityProfile, rule2);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule2Param).setValue(randomAlphanumeric(20));
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRule2, activeRuleParam2);
    db.getSession().commit();

    underTest.publishRuleActivationToSonarLintClients(projectDao, activatedQualityProfile, deactivatedQualityProfile);

    Deque<PushEventDto> events = db.getDbClient().pushEventDao()
      .selectChunkByProjectUuids(db.getSession(), Set.of(projectDao.getUuid()), 1l, null, 1);

    assertThat(events).isNotEmpty().hasSize(1);
    assertThat(events.getFirst())
      .extracting(PushEventDto::getName, PushEventDto::getLanguage)
      .contains("RuleSetChanged", "xoo");

    String ruleSetChangedEvent = new String(events.getFirst().getPayload(), StandardCharsets.UTF_8);

    assertThat(ruleSetChangedEvent)
      .contains("\"activatedRules\":[{\"key\":\"repo:ruleKey\"," +
        "\"language\":\"xoo\",\"severity\":\"" + Common.Severity.forNumber(rule1.getSeverity()).name() + "\"," +
        "\"params\":[{\"key\":\"" + activeRuleParam1.getKey() + "\",\"value\":\"" + activeRuleParam1.getValue() + "\"}]}]," +
        "\"deactivatedRules\":[\"repo2:ruleKey2\"]");
  }

}
