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
package org.sonar.server.pushapi.qualityprofile;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.pushevent.PushEventDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.qualityprofile.ActiveRuleChange;

import static java.util.List.of;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.db.rule.RuleTesting.newTemplateRule;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;

class QualityProfileChangeEventServiceImplTest {

  @RegisterExtension
  private final DbTester db = DbTester.create();
  private final QualityProfileChangeEventServiceImpl underTest = new QualityProfileChangeEventServiceImpl(db.getDbClient());

  @Test
  void distributeRuleChangeEvent() {
    QProfileDto qualityProfileDto = QualityProfileTesting.newQualityProfileDto();

    RuleDto templateRule = newTemplateRule(RuleKey.of("xoo", "template-key"));
    db.rules().insert(templateRule);

    RuleDto rule1 = newCustomRule(templateRule, "<div>line1\nline2</div>")
      .setLanguage("xoo")
      .setRepositoryKey("repo")
      .setRuleKey("ruleKey")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN);
    db.rules().insert(rule1);

    ActiveRuleChange activeRuleChange = changeActiveRule(qualityProfileDto, rule1, "paramChangeKey", "paramChangeValue");
    activeRuleChange.setImpactSeverities(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.LOW));

    Collection<QProfileDto> profiles = Collections.singleton(qualityProfileDto);

    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.qualityProfiles().associateWithProject(project, qualityProfileDto);

    underTest.distributeRuleChangeEvent(profiles, of(activeRuleChange), "xoo");

    Deque<PushEventDto> events = getProjectEvents(project);

    assertThat(events).isNotEmpty().hasSize(1);
    assertThat(events.getFirst())
      .extracting(PushEventDto::getName, PushEventDto::getLanguage)
      .contains("RuleSetChanged", "xoo");

    String ruleSetChangedEvent = new String(events.getFirst().getPayload(), StandardCharsets.UTF_8);

    assertThat(ruleSetChangedEvent)
      .contains("\"activatedRules\":[{\"key\":\"repo:ruleKey\"," +
                "\"language\":\"xoo\"," +
                "\"templateKey\":\"xoo:template-key\"," +
                "\"params\":[{\"key\":\"paramChangeKey\",\"value\":\"paramChangeValue\"}]," +
                "\"impacts\":[{\"softwareQuality\":\"MAINTAINABILITY\",\"severity\":\"LOW\"}]}]," +
                "\"deactivatedRules\":[]");
  }

  @Test
  void distributeRuleChangeEvent_when_project_has_only_default_quality_profiles() {
    String language = "xoo";
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    RuleDto templateRule = insertTemplateRule();
    QProfileDto defaultQualityProfile = insertDefaultQualityProfile(language);
    RuleDto rule = insertCustomRule(templateRule, language, "<div>line1\nline2</div>");
    ActiveRuleChange activeRuleChange = changeActiveRule(defaultQualityProfile, rule, "paramChangeKey", "paramChangeValue")
      .setImpactSeverities(Map.of(SoftwareQuality.RELIABILITY, Severity.MEDIUM));
    db.measures().insertMeasure(mainBranch, m -> m.addValue(NCLOC_LANGUAGE_DISTRIBUTION_KEY, language + "=100"));

    db.getSession().commit();

    underTest.distributeRuleChangeEvent(List.of(defaultQualityProfile), of(activeRuleChange), language);

    Deque<PushEventDto> events = getProjectEvents(projectData.getProjectDto());

    assertThat(events)
      .hasSize(1);

    assertThat(events.getFirst())
      .extracting(PushEventDto::getName, PushEventDto::getLanguage)
      .contains("RuleSetChanged", "xoo");

    String ruleSetChangedEvent = new String(events.getFirst().getPayload(), StandardCharsets.UTF_8);

    assertThat(ruleSetChangedEvent)
      .contains("\"activatedRules\":[{\"key\":\"repo:ruleKey\"," +
                "\"language\":\"xoo\"," +
                "\"templateKey\":\"xoo:template-key\"," +
                "\"params\":[{\"key\":\"paramChangeKey\",\"value\":\"paramChangeValue\"}]," +
                "\"impacts\":[{\"softwareQuality\":\"RELIABILITY\",\"severity\":\"MEDIUM\"}]}]," +
                "\"deactivatedRules\":[]");
  }

  private Deque<PushEventDto> getProjectEvents(ProjectDto projectDto) {
    return db.getDbClient()
      .pushEventDao()
      .selectChunkByProjectUuids(db.getSession(), Set.of(projectDto.getUuid()), 1L, null, 1);
  }

  private ActiveRuleChange changeActiveRule(QProfileDto defaultQualityProfile, RuleDto rule, String changeKey, String changeValue) {
    ActiveRuleDto activeRuleDto = ActiveRuleDto.createFor(defaultQualityProfile, rule);
    ActiveRuleChange activeRuleChange = new ActiveRuleChange(ACTIVATED, activeRuleDto, rule);
    return activeRuleChange.setParameter(changeKey, changeValue);
  }

  private RuleDto insertCustomRule(RuleDto templateRule, String language, String description) {
    RuleDto rule = newCustomRule(templateRule, description)
      .setLanguage(language)
      .setRepositoryKey("repo")
      .setRuleKey("ruleKey")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN);

    db.rules().insert(rule);
    return rule;
  }

  private QProfileDto insertDefaultQualityProfile(String language) {
    Consumer<QProfileDto> configureQualityProfile = profile -> profile
      .setIsBuiltIn(true)
      .setLanguage(language);

    QProfileDto defaultQualityProfile = db.qualityProfiles().insert(configureQualityProfile);
    db.qualityProfiles().setAsDefault(defaultQualityProfile);

    return defaultQualityProfile;
  }

  private RuleDto insertTemplateRule() {
    RuleKey key = RuleKey.of("xoo", "template-key");
    RuleDto templateRule = newTemplateRule(key);
    db.rules().insert(templateRule);
    return templateRule;
  }

  @Test
  void publishRuleActivationToSonarLintClients() {
    ProjectDto projectDto = new ProjectDto().setUuid("project-uuid");
    QProfileDto activatedQualityProfile = QualityProfileTesting.newQualityProfileDto();
    activatedQualityProfile.setLanguage("xoo");
    db.qualityProfiles().insert(activatedQualityProfile);
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage("xoo").setRepositoryKey("repo").setRuleKey("ruleKey"));
    RuleParamDto rule1Param = db.rules().insertRuleParam(rule1);

    ActiveRuleDto activeRule1 = db.qualityProfiles().activateRule(activatedQualityProfile, rule1, ar -> ar.setImpacts(Map.of(SoftwareQuality.SECURITY, Severity.BLOCKER)));
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param).setValue(secure().nextAlphanumeric(20));
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRule1, activeRuleParam1);
    db.getSession().commit();

    QProfileDto deactivatedQualityProfile = QualityProfileTesting.newQualityProfileDto();
    db.qualityProfiles().insert(deactivatedQualityProfile);
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage("xoo").setRepositoryKey("repo2").setRuleKey("ruleKey2"));
    RuleParamDto rule2Param = db.rules().insertRuleParam(rule2);

    ActiveRuleDto activeRule2 = db.qualityProfiles().activateRule(deactivatedQualityProfile, rule2);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule2Param).setValue(secure().nextAlphanumeric(20));
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRule2, activeRuleParam2);
    db.getSession().commit();

    underTest.publishRuleActivationToSonarLintClients(projectDto, activatedQualityProfile, deactivatedQualityProfile);

    Deque<PushEventDto> events = getProjectEvents(projectDto);

    assertThat(events).isNotEmpty().hasSize(1);
    assertThat(events.getFirst())
      .extracting(PushEventDto::getName, PushEventDto::getLanguage)
      .contains("RuleSetChanged", "xoo");

    String ruleSetChangedEvent = new String(events.getFirst().getPayload(), StandardCharsets.UTF_8);

    assertThat(ruleSetChangedEvent)
      .contains("\"activatedRules\":[{\"key\":\"repo:ruleKey\"," +
                "\"language\":\"xoo\",\"severity\":\"" + activeRule1.getSeverityString() + "\"," +
                "\"params\":[{\"key\":\"" + activeRuleParam1.getKey() + "\",\"value\":\"" + activeRuleParam1.getValue() + "\"}]," +
                "\"impacts\":[{\"softwareQuality\":\"SECURITY\",\"severity\":\"BLOCKER\"}]}]," +
                "\"deactivatedRules\":[\"repo2:ruleKey2\"]");
  }
}
