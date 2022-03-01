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
package org.sonar.server.qualityprofile.builtin;

import java.util.Collection;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.util.ParamChange;
import org.sonar.core.util.RuleChange;
import org.sonar.core.util.RuleSetChangedEvent;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventServiceImpl;
import org.sonar.server.pushapi.qualityprofile.RuleActivatorEventsDistributor;
import org.sonar.server.qualityprofile.ActiveRuleChange;

import static java.util.List.of;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.rule.RuleDto.Format.MARKDOWN;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.db.rule.RuleTesting.newTemplateRule;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;

public class QualityProfileChangeEventServiceImplTest {

  @Rule
  public DbTester db = DbTester.create();

  RuleActivatorEventsDistributor eventsDistributor = mock(RuleActivatorEventsDistributor.class);

  public final QualityProfileChangeEventServiceImpl underTest = new QualityProfileChangeEventServiceImpl(db.getDbClient(), eventsDistributor);

  @Test
  public void distributeRuleChangeEvent() {
    QProfileDto qualityProfileDto = QualityProfileTesting.newQualityProfileDto();

    // Template rule
    RuleDto templateRule = newTemplateRule(RuleKey.of("xoo", "template-key"));
    db.rules().insert(templateRule.getDefinition());
    // Custom rule
    RuleDefinitionDto rule1 = newCustomRule(templateRule.getDefinition())
      .setLanguage("xoo")
      .setRepositoryKey("repo")
      .setRuleKey("ruleKey")
      .setDescription("<div>line1\nline2</div>")
      .setDescriptionFormat(MARKDOWN);
    db.rules().insert(rule1);

    ActiveRuleDto activeRuleDto = ActiveRuleDto.createFor(qualityProfileDto, rule1);

    ActiveRuleChange activeRuleChange = new ActiveRuleChange(ACTIVATED, activeRuleDto, rule1);
    activeRuleChange.setParameter("paramChangeKey", "paramChangeValue");

    Collection<QProfileDto> profiles = Collections.singleton(qualityProfileDto);

    ProjectDto project = db.components().insertPrivateProjectDto();
    db.qualityProfiles().associateWithProject(project, qualityProfileDto);

    underTest.distributeRuleChangeEvent(profiles, of(activeRuleChange), "xoo");

    ArgumentCaptor<RuleSetChangedEvent> eventCaptor = ArgumentCaptor.forClass(RuleSetChangedEvent.class);
    verify(eventsDistributor).pushEvent(eventCaptor.capture());

    RuleSetChangedEvent ruleSetChangedEvent = eventCaptor.getValue();
    assertThat(ruleSetChangedEvent).isNotNull();
    assertThat(ruleSetChangedEvent).extracting(RuleSetChangedEvent::getEvent,
        RuleSetChangedEvent::getLanguage, RuleSetChangedEvent::getProjects)
      .containsExactly("RuleSetChanged", "xoo", new String[]{project.getKey()});

    assertThat(ruleSetChangedEvent.getActivatedRules())
      .extracting(RuleChange::getKey, RuleChange::getLanguage,
        RuleChange::getSeverity, RuleChange::getTemplateKey)
      .containsExactly(tuple("repo:ruleKey", "xoo", null, "xoo:template-key"));

    assertThat(ruleSetChangedEvent.getActivatedRules()[0].getParams()).hasSize(1);
    ParamChange actualParamChange = ruleSetChangedEvent.getActivatedRules()[0].getParams()[0];
    assertThat(actualParamChange)
      .extracting(ParamChange::getKey, ParamChange::getValue)
      .containsExactly("paramChangeKey", "paramChangeValue");

    assertThat(ruleSetChangedEvent.getDeactivatedRules()).isEmpty();

  }

  @Test
  public void publishRuleActivationToSonarLintClients() {
    ProjectDto projectDao = new ProjectDto();
    QProfileDto activatedQualityProfile = QualityProfileTesting.newQualityProfileDto();
    activatedQualityProfile.setLanguage("xoo");
    db.qualityProfiles().insert(activatedQualityProfile);
    RuleDefinitionDto rule1 = db.rules().insert(r -> r.setLanguage("xoo").setRepositoryKey("repo").setRuleKey("ruleKey"));
    RuleParamDto rule1Param = db.rules().insertRuleParam(rule1);

    ActiveRuleDto activeRule1 = db.qualityProfiles().activateRule(activatedQualityProfile, rule1);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param).setValue(randomAlphanumeric(20));
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRule1, activeRuleParam1);
    db.getSession().commit();

    QProfileDto deactivatedQualityProfile = QualityProfileTesting.newQualityProfileDto();
    db.qualityProfiles().insert(deactivatedQualityProfile);
    RuleDefinitionDto rule2 = db.rules().insert(r -> r.setLanguage("xoo").setRepositoryKey("repo2").setRuleKey("ruleKey2"));
    RuleParamDto rule2Param = db.rules().insertRuleParam(rule2);

    ActiveRuleDto activeRule2 = db.qualityProfiles().activateRule(deactivatedQualityProfile, rule2);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule2Param).setValue(randomAlphanumeric(20));
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRule2, activeRuleParam2);
    db.getSession().commit();

    underTest.publishRuleActivationToSonarLintClients(projectDao, activatedQualityProfile, deactivatedQualityProfile);

    ArgumentCaptor<RuleSetChangedEvent> eventCaptor = ArgumentCaptor.forClass(RuleSetChangedEvent.class);
    verify(eventsDistributor).pushEvent(eventCaptor.capture());

    RuleSetChangedEvent ruleSetChangedEvent = eventCaptor.getValue();
    assertThat(ruleSetChangedEvent).isNotNull();
    assertThat(ruleSetChangedEvent).extracting(RuleSetChangedEvent::getEvent,
        RuleSetChangedEvent::getLanguage, RuleSetChangedEvent::getProjects)
      .containsExactly("RuleSetChanged", "xoo", new String[]{null});

    // activated rule
    assertThat(ruleSetChangedEvent.getActivatedRules())
      .extracting(RuleChange::getKey, RuleChange::getLanguage,
        RuleChange::getSeverity, RuleChange::getTemplateKey)
      .containsExactly(tuple("repo:ruleKey", "xoo", rule1.getSeverityString(), null));

    assertThat(ruleSetChangedEvent.getActivatedRules()[0].getParams()).hasSize(1);
    ParamChange actualParamChange = ruleSetChangedEvent.getActivatedRules()[0].getParams()[0];
    assertThat(actualParamChange)
      .extracting(ParamChange::getKey, ParamChange::getValue)
      .containsExactly(activeRuleParam1.getKey(), activeRuleParam1.getValue());

    // deactivated rule
    assertThat(ruleSetChangedEvent.getDeactivatedRules())
      .containsExactly("repo2:ruleKey2");
  }

}
