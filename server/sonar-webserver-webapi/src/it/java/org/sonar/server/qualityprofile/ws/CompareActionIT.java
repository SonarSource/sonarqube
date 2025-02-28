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
package org.sonar.server.qualityprofile.ws;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileComparison;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;

class CompareActionIT {

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession session = db.getSession();

  private final WsActionTester ws = new WsActionTester(
    new CompareAction(db.getDbClient(), new QProfileComparison(db.getDbClient()), new Languages(LanguageTesting.newLanguage("xoo", "Xoo"))));

  @Test
  void compare_nominal() {
    createRepository("blah", "xoo", "Blah");

    RuleDto rule1 = createRule("xoo", "rule1");
    RuleDto rule2 = createRule("xoo", "rule2");
    RuleDto rule3 = createRule("xoo", "rule3");
    RuleDto rule4 = createRuleWithParam("xoo", "rule4");
    RuleDto rule5 = createRule("xoo", "rule5");
    RuleDto rule6 = createRuleWithImpacts("xoo", "rule6", List.of(new ImpactDto(SECURITY, BLOCKER)));

    /*
     * Profile 1:
     * - rule 1 active (on both profiles) => "same"
     * - rule 2 active (only in this profile) => "inLeft"
     * - rule 4 active with different parameters => "modified"
     * - rule 5 active with different severity => "modified"
     * - rule 6 active with different impacts => "modified"
     */
    QProfileDto profile1 = createProfile("xoo", "Profile 1", "xoo-profile-1-01234");
    createActiveRule(rule1, profile1);
    createActiveRule(rule2, profile1);
    createActiveRuleWithParam(rule4, profile1, "polop");
    createActiveRuleWithSeverity(rule5, profile1, Severity.MINOR);
    createActiveRuleWithImpacts(rule6, profile1, Map.of(SECURITY, BLOCKER));
    session.commit();

    /*
     * Profile 1:
     * - rule 1 active (on both profiles) => "same"
     * - rule 3 active (only in this profile) => "inRight"
     * - rule 4 active with different parameters => "modified"
     * - rule 6 active with different impacts => "modified"
     */
    QProfileDto profile2 = createProfile("xoo", "Profile 2", "xoo-profile-2-12345");
    createActiveRule(rule1, profile2);
    createActiveRule(rule3, profile2);
    createActiveRuleWithParam(rule4, profile2, "palap");
    createActiveRuleWithSeverity(rule5, profile2, Severity.MAJOR);
    createActiveRuleWithImpacts(rule6, profile2, Map.of(SECURITY, HIGH));
    session.commit();

    ws.newRequest()
      .setParam("leftKey", profile1.getKee())
      .setParam("rightKey", profile2.getKee())
      .execute().assertJson(this.getClass(), "compare_nominal.json");
  }

  @Test
  void compare_whenSecurityHotspot_shouldReturnEmptyCleanCodeInformation() {
    createRepository("blah", "xoo", "Blah");

    RuleDto rule1 = createSecurityHotspot("xoo", "rule1");
    RuleDto rule2 = createSecurityHotspot("xoo", "rule2");

    QProfileDto profile1 = createProfile("xoo", "Profile 1", "xoo-profile-1-01234");
    createActiveRule(rule1, profile1);

    QProfileDto profile2 = createProfile("xoo", "Profile 2", "xoo-profile-2-12345");
    createActiveRule(rule2, profile2);
    session.commit();

    ws.newRequest()
      .setParam("leftKey", profile1.getKee())
      .setParam("rightKey", profile2.getKee())
      .execute().assertJson(this.getClass(), "compare_hotspot.json");
  }

  @Test
  void compare_param_on_left() {
    RuleDto rule1 = createRuleWithParam("xoo", "rule1");
    createRepository("blah", "xoo", "Blah");
    QProfileDto profile1 = createProfile("xoo", "Profile 1", "xoo-profile-1-01234");
    createActiveRuleWithParam(rule1, profile1, "polop");
    QProfileDto profile2 = createProfile("xoo", "Profile 2", "xoo-profile-2-12345");
    createActiveRule(rule1, profile2);
    session.commit();

    ws.newRequest()
      .setParam("leftKey", profile1.getKee())
      .setParam("rightKey", profile2.getKee())
      .execute().assertJson(this.getClass(), "compare_param_on_left.json");
  }

  @Test
  void compare_param_on_right() {
    RuleDto rule1 = createRuleWithParam("xoo", "rule1");
    createRepository("blah", "xoo", "Blah");
    QProfileDto profile1 = createProfile("xoo", "Profile 1", "xoo-profile-1-01234");
    createActiveRule(rule1, profile1);
    QProfileDto profile2 = createProfile("xoo", "Profile 2", "xoo-profile-2-12345");
    createActiveRuleWithParam(rule1, profile2, "polop");
    session.commit();

    ws.newRequest()
      .setParam("leftKey", profile1.getKee())
      .setParam("rightKey", profile2.getKee())
      .execute().assertJson(this.getClass(), "compare_param_on_right.json");
  }

  @Test
  void compare_impacts() {
    RuleDto rule1 = createRuleWithImpacts("xoo", "impactRule", List.of(new ImpactDto(SECURITY, MEDIUM)));
    createRepository("blah", "xoo", "Blah");
    QProfileDto profile1 = createProfile("xoo", "Profile 1", "xoo-profile-1-01234");
    createActiveRuleWithImpacts(rule1, profile1, Map.of(SECURITY, MEDIUM, RELIABILITY, HIGH));
    QProfileDto profile2 = createProfile("xoo", "Profile 2", "xoo-profile-2-12345");
    createActiveRuleWithImpacts(rule1, profile2, Map.of(SECURITY, BLOCKER));
    session.commit();

    ws.newRequest()
      .setParam("leftKey", profile1.getKee())
      .setParam("rightKey", profile2.getKee())
      .execute().assertJson(this.getClass(), "compare_impacts.json");
  }

  @Test
  void compare_impacts_left_only_should_return_impacts_based_on_rule_impacts() {
    RuleDto rule1 = createRuleWithImpacts("xoo", "impactRule", List.of(new ImpactDto(SECURITY, MEDIUM)));
    createRepository("blah", "xoo", "Blah");
    QProfileDto profile1 = createProfile("xoo", "Profile 1", "xoo-profile-1-01234");
    createActiveRuleWithImpacts(rule1, profile1, Map.of(SECURITY, LOW, RELIABILITY, HIGH));
    QProfileDto profile2 = createProfile("xoo", "Profile 2", "xoo-profile-2-12345");
    session.commit();

    ws.newRequest()
      .setParam("leftKey", profile1.getKee())
      .setParam("rightKey", profile2.getKee())
      .execute().assertJson(this.getClass(), "compare_impacts_left_only.json");
  }

  @Test
  void fail_on_missing_left_param() {
    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("rightKey", "polop")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fail_on_missing_right_param() {
    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("leftKey", "polop")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fail_on_left_profile_not_found() {
    createProfile("xoo", "Right", "xoo-right-12345");

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("leftKey", "polop")
        .setParam("rightKey", "xoo-right-12345")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fail_on_right_profile_not_found() {
    createProfile("xoo", "Left", "xoo-left-12345");

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("leftKey", "xoo-left-12345")
        .setParam("rightKey", "polop")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition).isNotNull();
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.params()).hasSize(2).extracting("key").containsOnly(
      "leftKey", "rightKey");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
  }

  private QProfileDto createProfile(String lang, String name, String key) {
    return db.qualityProfiles().insert(p -> p.setKee(key).setName(name).setLanguage(lang));
  }

  private RuleDto createRule(String lang, String id) {
    RuleDto rule = createFor(RuleKey.of("blah", id))
      .setUuid(Uuids.createFast())
      .setName(StringUtils.capitalize(id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setScope(Scope.MAIN)
      .setStatus(RuleStatus.READY)
      .setCleanCodeAttribute(CleanCodeAttribute.EFFICIENT)
      .addDefaultImpact(new ImpactDto(SoftwareQuality.RELIABILITY, HIGH));
    RuleDto ruleDto = rule;
    dbClient.ruleDao().insert(session, ruleDto);
    return ruleDto;
  }

  private RuleDto createRuleWithImpacts(String lang, String id, Collection<ImpactDto> impacts) {
    RuleDto rule = createFor(RuleKey.of("blah", id))
      .setUuid(Uuids.createFast())
      .setName(StringUtils.capitalize(id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setScope(Scope.MAIN)
      .setStatus(RuleStatus.READY)
      .setCleanCodeAttribute(CleanCodeAttribute.EFFICIENT)
      .replaceAllDefaultImpacts(impacts);
    RuleDto ruleDto = rule;
    dbClient.ruleDao().insert(session, ruleDto);
    return ruleDto;
  }

  private RuleDto createSecurityHotspot(String lang, String id) {
    RuleDto rule = createFor(RuleKey.of("blah", id))
      .setUuid(Uuids.createFast())
      .setName(StringUtils.capitalize(id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setScope(Scope.MAIN)
      .setStatus(RuleStatus.READY)
      .setType(RuleType.SECURITY_HOTSPOT);
    dbClient.ruleDao().insert(session, rule);
    return rule;
  }

  private static RuleDto createFor(RuleKey key) {
    return new RuleDto()
      .setRepositoryKey(key.repository())
      .setRuleKey(key.rule());
  }

  private RuleDto createRuleWithParam(String lang, String id) {
    RuleDto rule = createRule(lang, id);
    RuleParamDto param = RuleParamDto.createFor(rule)
      .setName("param_" + id)
      .setType(RuleParamType.STRING.toString());
    dbClient.ruleDao().insertRuleParam(session, rule, param);
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDto rule, QProfileDto profile) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString())
      .setImpacts(rule.getDefaultImpactsMap());
    dbClient.activeRuleDao().insert(session, activeRule);
    return activeRule;
  }

  private ActiveRuleDto createActiveRuleWithImpacts(RuleDto rule, QProfileDto profile, Map<SoftwareQuality,
    org.sonar.api.issue.impact.Severity> impacts) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString())
      .setImpacts(impacts);
    dbClient.activeRuleDao().insert(session, activeRule);
    return activeRule;
  }

  private ActiveRuleDto createActiveRuleWithParam(RuleDto rule, QProfileDto profile, String value) {
    ActiveRuleDto activeRule = createActiveRule(rule, profile);
    RuleParamDto paramDto = dbClient.ruleDao().selectRuleParamsByRuleKey(session, rule.getKey()).get(0);
    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(paramDto).setValue(value);
    dbClient.activeRuleDao().insertParam(session, activeRule, activeRuleParam);
    return activeRule;
  }

  private ActiveRuleDto createActiveRuleWithSeverity(RuleDto rule, QProfileDto profile, String severity) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(severity)
      .setImpacts(rule.getDefaultImpactsMap());
    dbClient.activeRuleDao().insert(session, activeRule);
    return activeRule;
  }

  private void createRepository(String repositoryKey, String repositoryLanguage, String repositoryName) {
    RuleRepositoryDto dto = new RuleRepositoryDto(repositoryKey, repositoryLanguage, repositoryName);
    dbClient.ruleRepositoryDao().insert(session, singletonList(dto));
    session.commit();
  }
}
