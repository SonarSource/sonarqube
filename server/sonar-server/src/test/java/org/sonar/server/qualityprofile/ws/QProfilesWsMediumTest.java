/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.server.qualityprofile.ws.QProfilesWs.API_ENDPOINT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ACTIVATE_RULES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DEACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DEACTIVATE_RULES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RESET;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SEVERITY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TARGET_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TARGET_SEVERITY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_QPROFILE;

public class QProfilesWsMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester)
    .logIn().setRoot();

  private DbClient dbClient;
  private DbSession dbSession;
  private RuleIndexer ruleIndexer = tester.get(RuleIndexer.class);
  private ActiveRuleIndexer activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
  private OrganizationDto organization;

  private WsTester ws;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    dbClient = tester.get(DbClient.class);
    ws = tester.get(WsTester.class);
    dbSession = dbClient.openSession(false);

    ruleIndexer = tester.get(RuleIndexer.class);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
    organization = OrganizationTesting.newOrganizationDto().setKey("org-123");
    dbClient.organizationDao().insert(dbSession, organization, false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void deactivate_rule() throws Exception {
    QProfileDto profile = createProfile("java");
    RuleDefinitionDto rule = createRule(profile.getLanguage(), "toto");
    createActiveRule(rule, profile);
    ruleIndexer.commitAndIndex(dbSession, rule.getKey());
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(1);

    // 1. Deactivate Rule
    WsTester.TestRequest request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_DEACTIVATE_RULE);
    request.setParam(PARAM_PROFILE, profile.getKee());
    request.setParam(PARAM_RULE, rule.getKey().toString());
    request.execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();
  }

  @Test
  public void bulk_deactivate_rule() throws Exception {
    QProfileDto profile = createProfile("java");
    RuleDefinitionDto rule0 = createRule(profile.getLanguage(), "toto1");
    RuleDefinitionDto rule1 = createRule(profile.getLanguage(), "toto2");
    RuleDefinitionDto rule2 = createRule(profile.getLanguage(), "toto3");
    RuleDefinitionDto rule3 = createRule(profile.getLanguage(), "toto4");
    createActiveRule(rule0, profile);
    createActiveRule(rule2, profile);
    createActiveRule(rule3, profile);
    createActiveRule(rule1, profile);
    dbSession.commit();
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(4);

    // 1. Deactivate Rule
    WsTester.TestRequest request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_DEACTIVATE_RULES);
    request.setParam(PARAM_TARGET_PROFILE, profile.getKee());
    WsTester.Result result = request.execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();
  }

  @Test
  public void bulk_deactivate_rule_not_all() throws Exception {
    QProfileDto profile = createProfile("java");
    QProfileDto php = createProfile("php");
    RuleDefinitionDto rule0 = createRule(profile.getLanguage(), "toto1");
    RuleDefinitionDto rule1 = createRule(profile.getLanguage(), "toto2");
    createActiveRule(rule0, profile);
    createActiveRule(rule1, profile);
    createActiveRule(rule0, php);
    createActiveRule(rule1, php);
    dbSession.commit();
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(2);

    // 1. Deactivate Rule
    WsTester.TestRequest request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_DEACTIVATE_RULES);
    request.setParam(PARAM_TARGET_PROFILE, profile.getKee());
    WsTester.Result result = request.execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(0);
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, php.getKee())).hasSize(2);
  }

  @Test
  public void bulk_deactivate_rule_by_profile() throws Exception {
    QProfileDto profile = createProfile("java");
    RuleDefinitionDto rule0 = createRule(profile.getLanguage(), "hello");
    RuleDefinitionDto rule1 = createRule(profile.getLanguage(), "world");
    createActiveRule(rule0, profile);
    createActiveRule(rule1, profile);
    dbSession.commit();
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(2);

    // 1. Deactivate Rule
    WsTester.TestRequest request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_DEACTIVATE_RULES);
    request.setParam(PARAM_TARGET_PROFILE, profile.getKee());
    request.setParam(Param.TEXT_QUERY, "hello");
    WsTester.Result result = request.execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(1);
  }

  @Test
  public void activate_rule() throws Exception {
    QProfileDto profile = createProfile("java");
    RuleDefinitionDto rule = createRule(profile.getLanguage(), "toto");
    ruleIndexer.commitAndIndex(dbSession, rule.getKey());

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_ACTIVATE_RULE);
    request.setParam(PARAM_PROFILE, profile.getKee());
    request.setParam(PARAM_RULE, rule.getKey().toString());
    WsTester.Result result = request.execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(1);
  }

  @Test
  public void activate_rule_diff_languages() throws Exception {
    QProfileDto profile = createProfile("java");
    RuleDefinitionDto rule = createRule("php", "toto");
    ruleIndexer.commitAndIndex(dbSession, rule.getKey());

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    try {
      // 1. Activate Rule
      WsTester.TestRequest request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_ACTIVATE_RULE);
      request.setParam(PARAM_PROFILE, profile.getKee());
      request.setParam(PARAM_RULE, rule.getKey().toString());
      request.execute();
      dbSession.clearCache();
      fail();
    } catch (BadRequestException e) {
      assertThat(e.getMessage()).isEqualTo("Rule blah:toto and profile pjava have different languages");
    }
  }

  @Test
  public void activate_rule_override_severity() throws Exception {
    QProfileDto profile = createProfile("java");
    RuleDefinitionDto rule = createRule(profile.getLanguage(), "toto");
    ruleIndexer.commitAndIndex(dbSession, rule.getKey());

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_ACTIVATE_RULE);
    request.setParam(PARAM_PROFILE, profile.getKee());
    request.setParam(PARAM_RULE, rule.getKey().toString());
    request.setParam(PARAM_SEVERITY, "MINOR");
    WsTester.Result result = request.execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile, rule.getKey());

    Optional<ActiveRuleDto> activeRuleDto = dbClient.activeRuleDao().selectByKey(dbSession, activeRuleKey);
    assertThat(activeRuleDto.isPresent()).isTrue();
    assertThat(activeRuleDto.get().getSeverityString()).isEqualTo(Severity.MINOR);
  }

  @Test
  public void bulk_activate_rule() throws Exception {
    QProfileDto profile = createProfile("java");
    createRule(profile.getLanguage(), "toto");
    createRule(profile.getLanguage(), "tata");
    createRule(profile.getLanguage(), "hello");
    createRule(profile.getLanguage(), "world");
    dbSession.commit();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_ACTIVATE_RULES);
    request.setParam(PARAM_TARGET_PROFILE, profile.getKee());
    request.setParam(PARAM_LANGUAGES, "java");
    request.execute().assertJson(getClass(), "bulk_activate_rule.json");
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(4);
  }

  @Test
  public void bulk_activate_rule_not_all() throws Exception {
    QProfileDto java = createProfile("java");
    QProfileDto php = createProfile("php");
    createRule(java.getLanguage(), "toto");
    createRule(java.getLanguage(), "tata");
    createRule(php.getLanguage(), "hello");
    createRule(php.getLanguage(), "world");
    dbSession.commit();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, php.getKee())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_ACTIVATE_RULES);
    request.setParam(PARAM_TARGET_PROFILE, php.getKee());
    request.setParam(PARAM_LANGUAGES, "php");
    request.execute().assertJson(getClass(), "bulk_activate_rule_not_all.json");
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, php.getKee())).hasSize(2);
  }

  @Test
  public void bulk_activate_rule_by_query() throws Exception {
    QProfileDto profile = createProfile("java");
    createRule(profile.getLanguage(), "toto");
    createRule(profile.getLanguage(), "tata");
    createRule(profile.getLanguage(), "hello");
    createRule(profile.getLanguage(), "world");
    dbSession.commit();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 1. Activate Rule with query returning 0 hits
    WsTester.TestRequest request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_ACTIVATE_RULES);
    request.setParam(PARAM_TARGET_PROFILE, profile.getKee());
    request.setParam(Param.TEXT_QUERY, "php");
    request.execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(0);

    // 1. Activate Rule with query returning 1 hits
    request = ws.newPostRequest(QProfilesWs.API_ENDPOINT, ACTION_ACTIVATE_RULES);
    request.setParam(PARAM_TARGET_PROFILE, profile.getKee());
    request.setParam(Param.TEXT_QUERY, "world");
    request.execute();
    dbSession.commit();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(1);
  }

  @Test
  public void bulk_activate_rule_by_query_with_severity() throws Exception {
    QProfileDto profile = createProfile("java");
    RuleDefinitionDto rule0 = createRule(profile.getLanguage(), "toto");
    RuleDefinitionDto rule1 = createRule(profile.getLanguage(), "tata");
    dbSession.commit();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 2. Assert ActiveRule with BLOCKER severity
    assertThat(tester.get(RuleIndex.class).search(
      new RuleQuery().setSeverities(ImmutableSet.of("BLOCKER")),
      new SearchOptions()).getIds()).hasSize(2);

    // 1. Activate Rule with query returning 2 hits
    WsTester.TestRequest request = ws.newPostRequest(API_ENDPOINT, ACTION_ACTIVATE_RULES);
    request.setParam(PARAM_TARGET_PROFILE, profile.getKee());
    request.setParam(PARAM_TARGET_SEVERITY, "MINOR");
    request.execute();
    dbSession.commit();

    // 2. Assert ActiveRule with MINOR severity
    assertThat(tester.get(ActiveRuleDao.class).selectByRuleId(dbSession, organization, rule0.getId()).get(0).getSeverityString()).isEqualTo("MINOR");
    assertThat(tester.get(RuleIndex.class).searchAll(new RuleQuery()
      .setQProfile(profile)
      .setKey(rule0.getKey().toString())
      .setActiveSeverities(Collections.singleton("MINOR"))
      .setActivation(true))).hasSize(1);
  }

  @Test
  public void does_not_return_warnings_when_bulk_activate_on_profile_and_rules_exist_on_another_language_than_profile() throws Exception {
    QProfileDto javaProfile = createProfile("java");
    createRule(javaProfile.getLanguage(), "toto");
    createRule(javaProfile.getLanguage(), "tata");
    QProfileDto phpProfile = createProfile("php");
    createRule(phpProfile.getLanguage(), "hello");
    createRule(phpProfile.getLanguage(), "world");
    dbSession.commit();

    // 1. Activate Rule
    WsTester.TestRequest request = ws.newPostRequest(API_ENDPOINT, ACTION_ACTIVATE_RULES);
    request.setParam(PARAM_TARGET_PROFILE, javaProfile.getKee());
    request.setParam(PARAM_QPROFILE, javaProfile.getKee());
    request.setParam("activation", "false");
    request.execute().assertJson(getClass(), "does_not_return_warnings_when_bulk_activate_on_profile_and_rules_exist_on_another_language_than_profile.json");
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, javaProfile.getKee())).hasSize(2);
  }

  @Test
  public void reset() throws Exception {
    QProfileDto profile = QProfileTesting.newXooP1(organization);
    QProfileDto subProfile = QProfileTesting.newXooP2(organization).setParentKee(QProfileTesting.XOO_P1_KEY);
    dbClient.qualityProfileDao().insert(dbSession, profile, subProfile);

    RuleDefinitionDto rule = createRule(profile.getLanguage(), "rule");
    ActiveRuleDto active1 = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    ActiveRuleDto active2 = ActiveRuleDto.createFor(subProfile, rule)
      .setSeverity("MINOR");
    dbClient.activeRuleDao().insert(dbSession, active1);
    dbClient.activeRuleDao().insert(dbSession, active2);

    dbSession.commit();
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    // 0. assert rule child rule is minor
    Optional<ActiveRuleDto> activeRuleDto = dbClient.activeRuleDao().selectByKey(dbSession, active2.getKey());
    assertThat(activeRuleDto.isPresent()).isTrue();
    assertThat(activeRuleDto.get().getSeverityString()).isEqualTo(Severity.MINOR);

    // 1. reset child rule
    WsTester.TestRequest request = ws.newPostRequest(API_ENDPOINT, ACTION_ACTIVATE_RULE);
    request.setParam(PARAM_PROFILE, subProfile.getKee());
    request.setParam(PARAM_RULE, rule.getKey().toString());
    request.setParam(PARAM_RESET, "true");
    request.execute();
    dbSession.clearCache();

    // 2. assert rule child rule is NOT minor
    activeRuleDto = dbClient.activeRuleDao().selectByKey(dbSession, active2.getKey());
    assertThat(activeRuleDto.isPresent()).isTrue();
    assertThat(activeRuleDto.get().getSeverityString()).isNotEqualTo(Severity.MINOR);
  }

  private QProfileDto createProfile(String lang) {
    QProfileDto profile = QProfileTesting.newQProfileDto(organization, new QProfileName(lang, "P" + lang), "p" + lang);
    dbClient.qualityProfileDao().insert(dbSession, profile);
    return profile;
  }

  private RuleDefinitionDto createRule(String lang, String id) {
    RuleDefinitionDto rule = RuleTesting.newRule(RuleKey.of("blah", id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY);
    dbClient.ruleDao().insert(dbSession, rule);
    ruleIndexer.commitAndIndex(dbSession, rule.getKey());
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDefinitionDto rule, QProfileDto profile) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    return activeRule;
  }
}
