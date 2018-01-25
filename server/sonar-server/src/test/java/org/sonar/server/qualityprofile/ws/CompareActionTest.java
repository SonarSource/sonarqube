/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileComparison;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.util.Arrays.asList;

public class CompareActionTest {

  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private WsActionTester wsTester;
  private CompareAction underTest;
  private DbClient db = dbTester.getDbClient();
  private DbSession session = dbTester.getSession();

  @Before
  public void before() {
    underTest = new CompareAction(dbTester.getDbClient(), new QProfileComparison(dbTester.getDbClient()), new Languages(LanguageTesting.newLanguage("xoo", "Xoo")));
    wsTester = new WsActionTester(underTest);
  }

  @Test
  public void should_not_allow_to_compare_quality_profiles_from_different_organizations() {
    QProfileDto left = QualityProfileTesting.newQualityProfileDto();
    QProfileDto right = QualityProfileTesting.newQualityProfileDto();
    dbTester.qualityProfiles().insert(left, right);

    TestRequest request = wsTester.newRequest().setMethod("POST")
      .setParam("leftKey", left.getKee())
      .setParam("rightKey", right.getKee());

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Cannot compare quality profiles of different organizations.");
    request.execute();
  }

  @Test
  public void compare_nominal() throws Exception {
    createRepository("blah", "xoo", "Blah");

    RuleDefinitionDto rule1 = createRule("xoo", "rule1");
    RuleDefinitionDto rule2 = createRule("xoo", "rule2");
    RuleDefinitionDto rule3 = createRule("xoo", "rule3");
    RuleDefinitionDto rule4 = createRuleWithParam("xoo", "rule4");
    RuleDefinitionDto rule5 = createRule("xoo", "rule5");

    /*
     * Profile 1:
     * - rule 1 active (on both profiles) => "same"
     * - rule 2 active (only in this profile) => "inLeft"
     * - rule 4 active with different parameters => "modified"
     * - rule 5 active with different severity => "modified"
     */
    QProfileDto profile1 = createProfile("xoo", "Profile 1", "xoo-profile-1-01234");
    createActiveRule(rule1, profile1);
    createActiveRule(rule2, profile1);
    createActiveRuleWithParam(rule4, profile1, "polop");
    createActiveRuleWithSeverity(rule5, profile1, Severity.MINOR);
    session.commit();

    /*
     * Profile 1:
     * - rule 1 active (on both profiles) => "same"
     * - rule 3 active (only in this profile) => "inRight"
     * - rule 4 active with different parameters => "modified"
     */
    QProfileDto profile2 = createProfile("xoo", "Profile 2", "xoo-profile-2-12345");
    createActiveRule(rule1, profile2);
    createActiveRule(rule3, profile2);
    createActiveRuleWithParam(rule4, profile2, "palap");
    createActiveRuleWithSeverity(rule5, profile2, Severity.MAJOR);
    session.commit();

    wsTester.newRequest()
      .setParam("leftKey", profile1.getKee())
      .setParam("rightKey", profile2.getKee())
      .execute().assertJson(this.getClass(), "compare_nominal.json");
  }

  @Test
  public void compare_param_on_left() throws Exception {
    RuleDefinitionDto rule1 = createRuleWithParam("xoo", "rule1");
    createRepository("blah", "xoo", "Blah");
    QProfileDto profile1 = createProfile("xoo", "Profile 1", "xoo-profile-1-01234");
    createActiveRuleWithParam(rule1, profile1, "polop");
    QProfileDto profile2 = createProfile("xoo", "Profile 2", "xoo-profile-2-12345");
    createActiveRule(rule1, profile2);
    session.commit();

    wsTester.newRequest()
      .setParam("leftKey", profile1.getKee())
      .setParam("rightKey", profile2.getKee())
      .execute().assertJson(this.getClass(), "compare_param_on_left.json");
  }

  @Test
  public void compare_param_on_right() throws Exception {
    RuleDefinitionDto rule1 = createRuleWithParam("xoo", "rule1");
    createRepository("blah", "xoo", "Blah");
    QProfileDto profile1 = createProfile("xoo", "Profile 1", "xoo-profile-1-01234");
    createActiveRule(rule1, profile1);
    QProfileDto profile2 = createProfile("xoo", "Profile 2", "xoo-profile-2-12345");
    createActiveRuleWithParam(rule1, profile2, "polop");
    session.commit();

    wsTester.newRequest()
      .setParam("leftKey", profile1.getKee())
      .setParam("rightKey", profile2.getKee())
      .execute().assertJson(this.getClass(), "compare_param_on_right.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_left_param() {
    wsTester.newRequest()
      .setParam("rightKey", "polop")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_right_param() {
    wsTester.newRequest()
      .setParam("leftKey", "polop")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_left_profile_not_found() {
    createProfile("xoo", "Right", "xoo-right-12345");
    wsTester.newRequest()
      .setParam("leftKey", "polop")
      .setParam("rightKey", "xoo-right-12345")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_right_profile_not_found() {
    createProfile("xoo", "Left", "xoo-left-12345");
    wsTester.newRequest()
      .setParam("leftKey", "xoo-left-12345")
      .setParam("rightKey", "polop")
      .execute();
  }

  private QProfileDto createProfile(String lang, String name, String key) {
    QProfileDto profile = QProfileTesting.newQProfileDto("org-123", new QProfileName(lang, name), key);
    db.qualityProfileDao().insert(session, profile);
    session.commit();
    return profile;
  }

  private RuleDefinitionDto createRule(String lang, String id) {
    RuleDto rule = RuleDto.createFor(RuleKey.of("blah", id))
      .setName(StringUtils.capitalize(id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setScope(Scope.MAIN)
      .setStatus(RuleStatus.READY);
    RuleDefinitionDto ruleDefinition = rule.getDefinition();
    db.ruleDao().insert(session, ruleDefinition);
    RuleParamDto param = RuleParamDto.createFor(ruleDefinition).setName("param_" + id).setType(RuleParamType.STRING.toString());
    db.ruleDao().insertRuleParam(session, ruleDefinition, param);
    return ruleDefinition;
  }

  private RuleDefinitionDto createRuleWithParam(String lang, String id) {
    RuleDefinitionDto rule = createRule(lang, id);
    RuleParamDto param = RuleParamDto.createFor(rule)
      .setName("param_" + id)
      .setType(RuleParamType.STRING.toString());
    db.ruleDao().insertRuleParam(session, rule, param);
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDefinitionDto rule, QProfileDto profile) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    db.activeRuleDao().insert(session, activeRule);
    return activeRule;
  }

  private ActiveRuleDto createActiveRuleWithParam(RuleDefinitionDto rule, QProfileDto profile, String value) {
    ActiveRuleDto activeRule = createActiveRule(rule, profile);
    RuleParamDto paramDto = db.ruleDao().selectRuleParamsByRuleKey(session, rule.getKey()).get(0);
    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(paramDto).setValue(value);
    db.activeRuleDao().insertParam(session, activeRule, activeRuleParam);
    return activeRule;
  }

  private ActiveRuleDto createActiveRuleWithSeverity(RuleDefinitionDto rule, QProfileDto profile, String severity) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(severity);
    db.activeRuleDao().insert(session, activeRule);
    return activeRule;
  }

  private void createRepository(String repositoryKey, String repositoryLanguage, String repositoryName) {
    RuleRepositoryDto dto = new RuleRepositoryDto(repositoryKey, repositoryLanguage, repositoryName);
    db.ruleRepositoryDao().insert(session, asList(dto));
    session.commit();
  }
}
