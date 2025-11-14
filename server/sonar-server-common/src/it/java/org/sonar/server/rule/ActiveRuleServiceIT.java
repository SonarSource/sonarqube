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
package org.sonar.server.rule;

import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.rule.ActiveRuleRestReponse.ActiveRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;

class ActiveRuleServiceIT {

  public static final Language XOO_LANGUAGE = mock(Language.class);
  public static final Language XOO2_LANGUAGE = mock(Language.class);

  public static final String XOO_LANGUAGE_KEY = "xoo";
  public static final String XOO2_LANGUAGE_KEY = "xoo2";

  static {
    when(XOO_LANGUAGE.getKey()).thenReturn(XOO_LANGUAGE_KEY);
    when(XOO2_LANGUAGE.getKey()).thenReturn(XOO2_LANGUAGE_KEY);
  }

  @RegisterExtension
  DbTester db = DbTester.create(System2.INSTANCE);

  private final ActiveRuleService activeRuleService = new ActiveRuleService(db.getDbClient(), new Languages(XOO_LANGUAGE, XOO2_LANGUAGE));

  @Test
  void buildDefaultActiveRules() {
    QProfileDto qProfileDto1 = db.qualityProfiles().insert(qp -> qp.setLanguage(XOO_LANGUAGE_KEY));
    db.qualityProfiles().setAsDefault(qProfileDto1);
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage(XOO_LANGUAGE_KEY), r -> r.setName("rule1"));
    ActiveRuleDto activatedRule1 = db.qualityProfiles().activateRule(qProfileDto1, rule1);
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage(XOO_LANGUAGE_KEY), r -> r.setName("rule2"));
    ActiveRuleDto activatedRule2 = db.qualityProfiles().activateRule(qProfileDto1, rule2);

    QProfileDto qProfileDto2 = db.qualityProfiles().insert(qp -> qp.setLanguage(XOO2_LANGUAGE_KEY));
    db.qualityProfiles().setAsDefault(qProfileDto2);
    RuleDto rule3 = db.rules().insert(r -> r.setLanguage(XOO2_LANGUAGE_KEY), r -> r.setName("rule3"));
    ActiveRuleDto activatedRule3 = db.qualityProfiles().activateRule(qProfileDto2, rule3);

    List<ActiveRule> activeRules = activeRuleService.buildDefaultActiveRules();
    assertThat(activeRules).isNotNull().hasSize(3)
      .extracting(ActiveRule::ruleKey, ActiveRule::name, ActiveRule::severity,
        ActiveRule::createdAt, ActiveRule::updatedAt, ActiveRule::internalKey,
        ActiveRule::language, ActiveRule::templateRuleKey, ActiveRule::qProfileKey,
        ActiveRule::deprecatedKeys, ActiveRule::params)
      .containsExactlyInAnyOrder(
        toTuple(activatedRule1, XOO_LANGUAGE_KEY, qProfileDto1, List.of()),
        toTuple(activatedRule2, XOO_LANGUAGE_KEY, qProfileDto1, List.of()),
        toTuple(activatedRule3, XOO2_LANGUAGE_KEY, qProfileDto2, List.of()));
  }

  @Test
  void buildActiveRule_with_default_profile() {
    QProfileDto qProfileDto1 = db.qualityProfiles().insert(qp -> qp.setLanguage(XOO_LANGUAGE_KEY));
    db.qualityProfiles().setAsDefault(qProfileDto1);
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage(XOO_LANGUAGE_KEY), r -> r.setName("rule1"));
    ActiveRuleDto activatedRule1 = db.qualityProfiles().activateRule(qProfileDto1, rule1);
    RuleDto rule2 = db.rules().insert(r -> r.setLanguage(XOO_LANGUAGE_KEY), r -> r.setName("rule2"));
    ActiveRuleDto activatedRule2 = db.qualityProfiles().activateRule(qProfileDto1, rule2);

    QProfileDto qProfileDto2 = db.qualityProfiles().insert(qp -> qp.setLanguage(XOO2_LANGUAGE_KEY));
    db.qualityProfiles().setAsDefault(qProfileDto2);
    RuleDto rule3 = db.rules().insert(r -> r.setLanguage(XOO2_LANGUAGE_KEY), r -> r.setName("rule3"));
    ActiveRuleDto activatedRule3 = db.qualityProfiles().activateRule(qProfileDto2, rule3);

    var removedLanguageKey = "removed";
    QProfileDto qProfileDtoRemovedLanguage = db.qualityProfiles().insert(qp -> qp.setLanguage(removedLanguageKey));
    db.qualityProfiles().setAsDefault(qProfileDtoRemovedLanguage);
    RuleDto ruleRemovedLanguage = db.rules().insert(r -> r.setLanguage(removedLanguageKey), r -> r.setName("ruleRemovedLanguage"));
    db.qualityProfiles().activateRule(qProfileDtoRemovedLanguage, ruleRemovedLanguage);

    List<ActiveRule> activeRules = activeRuleService.buildActiveRules("myProjectUuid");
    assertThat(activeRules).isNotNull().hasSize(3)
      .extracting(ActiveRule::ruleKey, ActiveRule::name, ActiveRule::severity,
        ActiveRule::createdAt, ActiveRule::updatedAt, ActiveRule::internalKey,
        ActiveRule::language, ActiveRule::templateRuleKey, ActiveRule::qProfileKey,
        ActiveRule::deprecatedKeys, ActiveRule::params)
      .containsExactlyInAnyOrder(
        toTuple(activatedRule1, XOO_LANGUAGE_KEY, qProfileDto1, List.of()),
        toTuple(activatedRule2, XOO_LANGUAGE_KEY, qProfileDto1, List.of()),
        toTuple(activatedRule3, XOO2_LANGUAGE_KEY, qProfileDto2, List.of()));
  }

  @Test
  void projectProfile_overrides_defaultProfile() {
    QProfileDto qProfileDto1 = db.qualityProfiles().insert(qp -> qp.setLanguage(XOO_LANGUAGE_KEY));
    db.qualityProfiles().setAsDefault(qProfileDto1);
    RuleDto rule1 = db.rules().insert(r -> r.setLanguage(XOO_LANGUAGE_KEY), r -> r.setName("rule1"));
    db.qualityProfiles().activateRule(qProfileDto1, rule1);

    QProfileDto qProfileDto2 = db.qualityProfiles().insert(qp -> qp.setLanguage(XOO_LANGUAGE_KEY));
    ProjectData project = db.components().insertPrivateProject();
    db.qualityProfiles().associateWithProject(project.getProjectDto(), qProfileDto2);
    ActiveRuleDto activatedRule2 = db.qualityProfiles().activateRule(qProfileDto2, rule1);

    List<ActiveRule> activeRules = activeRuleService.buildActiveRules(project.projectUuid());
    assertThat(activeRules).hasSize(1)
      .extracting(ActiveRule::ruleKey, ActiveRule::name, ActiveRule::severity,
        ActiveRule::createdAt, ActiveRule::updatedAt, ActiveRule::internalKey,
        ActiveRule::language, ActiveRule::templateRuleKey, ActiveRule::qProfileKey,
        ActiveRule::deprecatedKeys, ActiveRule::params)
      .containsExactlyInAnyOrder(
        toTuple(activatedRule2, XOO_LANGUAGE_KEY, qProfileDto2, List.of()));
  }

  @Test
  void customRuleParam_overrides_defaultRuleParamValue() {
    RuleDto rule = db.rules().insert(r -> r.setLanguage(XOO_LANGUAGE_KEY), r -> r.setName("rule"));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setDefaultValue("defaultValue"));
    QProfileDto defaultQualityProfile = db.qualityProfiles().insert(qp -> qp.setLanguage(XOO_LANGUAGE_KEY));
    db.qualityProfiles().setAsDefault(defaultQualityProfile);
    db.qualityProfiles().activateRule(defaultQualityProfile, rule);

    QProfileDto qProfileDto2 = db.qualityProfiles().insert(qp -> qp.setLanguage(XOO_LANGUAGE_KEY));
    ProjectData project = db.components().insertPrivateProject();
    db.qualityProfiles().associateWithProject(project.getProjectDto(), qProfileDto2);
    ActiveRuleDto activatedRule2 = db.qualityProfiles().activateRule(qProfileDto2, rule);
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activatedRule2, new ActiveRuleParamDto()
      .setRulesParameterUuid(ruleParam.getUuid())
      .setKey(ruleParam.getName())
      .setValue("overriddenValue"));

    db.commit();

    List<ActiveRule> activeRules = activeRuleService.buildActiveRules(project.projectUuid());
    assertThat(activeRules).hasSize(1)
      .extracting(ActiveRule::ruleKey, ActiveRule::name, ActiveRule::severity,
        ActiveRule::createdAt, ActiveRule::updatedAt, ActiveRule::internalKey,
        ActiveRule::language, ActiveRule::templateRuleKey, ActiveRule::qProfileKey,
        ActiveRule::deprecatedKeys, ActiveRule::params)
      .containsExactlyInAnyOrder(
        toTuple(activatedRule2, XOO_LANGUAGE_KEY, qProfileDto2, List.of(new ActiveRuleRestReponse.Param(ruleParam.getName(), "overriddenValue"))));
  }

  @Test
  void returnsRuleTemplate() {
    RuleDto templateRule = db.rules().insert(r -> r.setLanguage(XOO_LANGUAGE_KEY)
      .setIsTemplate(true));
    RuleDto rule = db.rules().insert(r -> r.setLanguage(XOO_LANGUAGE_KEY)
      .setTemplateUuid(templateRule.getUuid()));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule);
    QProfileDto defaultQualityProfile = db.qualityProfiles().insert(qp -> qp.setLanguage(XOO_LANGUAGE_KEY));
    db.qualityProfiles().setAsDefault(defaultQualityProfile);
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(defaultQualityProfile, rule);
    ProjectData project = db.components().insertPrivateProject();

    List<ActiveRule> activeRules = activeRuleService.buildActiveRules(project.projectUuid());
    assertThat(activeRules).hasSize(1)
      .extracting(ActiveRule::ruleKey, ActiveRule::name, ActiveRule::severity,
        ActiveRule::createdAt, ActiveRule::updatedAt, ActiveRule::internalKey,
        ActiveRule::language, ActiveRule::templateRuleKey, ActiveRule::qProfileKey,
        ActiveRule::deprecatedKeys, ActiveRule::params)
      .containsExactlyInAnyOrder(
        tuple(toRuleKey(activeRule.getKey()), activeRule.getName(), activeRule.getSeverityString(), formatDateTime(activeRule.getCreatedAt()),
          formatDateTime(activeRule.getUpdatedAt()), activeRule.getConfigKey(), XOO_LANGUAGE_KEY, templateRule.getKey().toString(), defaultQualityProfile.getKee(), List.of(),
          List.of(new ActiveRuleRestReponse.Param(ruleParam.getName(), ruleParam.getDefaultValue()))));

  }

  private Tuple toTuple(ActiveRuleDto activatedRule, String language, QProfileDto qProfileDto, List<ActiveRuleRestReponse.Param> expectedParams) {
    return tuple(toRuleKey(activatedRule.getKey()), activatedRule.getName(), activatedRule.getSeverityString(), formatDateTime(activatedRule.getCreatedAt()),
      formatDateTime(activatedRule.getUpdatedAt()), activatedRule.getConfigKey(), language, null, qProfileDto.getKee(), List.of(), expectedParams);
  }

  private ActiveRuleRestReponse.RuleKey toRuleKey(ActiveRuleKey key) {
    return new ActiveRuleRestReponse.RuleKey(key.getRuleKey().repository(), key.getRuleKey().rule());
  }

}
