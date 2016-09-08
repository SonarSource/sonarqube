/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfileFactory;
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
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_QPROFILE;

public class QProfilesWsMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester)
    .login("gandalf").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

  QProfilesWs ws;
  DbClient db;
  DbSession session;
  WsTester wsTester;

  RuleIndexer ruIndexer = tester.get(RuleIndexer.class);
  ActiveRuleIndexer activeRuIndexer = tester.get(ActiveRuleIndexer.class);

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    ws = tester.get(QProfilesWs.class);
    wsTester = tester.get(WsTester.class);
    session = db.openSession(false);

    ruIndexer = tester.get(RuleIndexer.class);
    activeRuIndexer = tester.get(ActiveRuleIndexer.class);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void deactivate_rule() throws Exception {
    QualityProfileDto profile = createProfile("java");
    RuleDto rule = createRule(profile.getLanguage(), "toto");
    createActiveRule(rule, profile);
    session.commit();
    ruIndexer.index();
    activeRuIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(1);

    // 1. Deactivate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.DEACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey());
    request.setParam(RuleActivationActions.RULE_KEY, rule.getKey().toString());
    request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();
  }

  @Test
  public void bulk_deactivate_rule() throws Exception {
    QualityProfileDto profile = createProfile("java");
    RuleDto rule0 = createRule(profile.getLanguage(), "toto1");
    RuleDto rule1 = createRule(profile.getLanguage(), "toto2");
    RuleDto rule2 = createRule(profile.getLanguage(), "toto3");
    RuleDto rule3 = createRule(profile.getLanguage(), "toto4");
    createActiveRule(rule0, profile);
    createActiveRule(rule2, profile);
    createActiveRule(rule3, profile);
    createActiveRule(rule1, profile);
    session.commit();
    ruIndexer.index();
    activeRuIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(4);

    // 1. Deactivate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_DEACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey());
    WsTester.Result result = request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();
  }

  @Test
  public void bulk_deactivate_rule_not_all() throws Exception {
    QualityProfileDto profile = createProfile("java");
    QualityProfileDto php = createProfile("php");
    RuleDto rule0 = createRule(profile.getLanguage(), "toto1");
    RuleDto rule1 = createRule(profile.getLanguage(), "toto2");
    createActiveRule(rule0, profile);
    createActiveRule(rule1, profile);
    createActiveRule(rule0, php);
    createActiveRule(rule1, php);
    session.commit();
    ruIndexer.index();
    activeRuIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(2);

    // 1. Deactivate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_DEACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey());
    WsTester.Result result = request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(0);
    assertThat(db.activeRuleDao().selectByProfileKey(session, php.getKey())).hasSize(2);
  }

  @Test
  public void bulk_deactivate_rule_by_profile() throws Exception {
    QualityProfileDto profile = createProfile("java");
    RuleDto rule0 = createRule(profile.getLanguage(), "hello");
    RuleDto rule1 = createRule(profile.getLanguage(), "world");
    createActiveRule(rule0, profile);
    createActiveRule(rule1, profile);
    session.commit();
    ruIndexer.index();
    activeRuIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(2);

    // 1. Deactivate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_DEACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey());
    request.setParam(WebService.Param.TEXT_QUERY, "hello");
    WsTester.Result result = request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(1);
  }

  @Test
  public void activate_rule() throws Exception {
    QualityProfileDto profile = createProfile("java");
    RuleDto rule = createRule(profile.getLanguage(), "toto");
    session.commit();
    ruIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey());
    request.setParam(RuleActivationActions.RULE_KEY, rule.getKey().toString());
    WsTester.Result result = request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(1);
  }

  @Test
  public void activate_rule_diff_languages() throws Exception {
    QualityProfileDto profile = createProfile("java");
    RuleDto rule = createRule("php", "toto");
    session.commit();
    ruIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    try {
      // 1. Activate Rule
      WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.ACTIVATE_ACTION);
      request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey());
      request.setParam(RuleActivationActions.RULE_KEY, rule.getKey().toString());
      request.execute();
      session.clearCache();
      fail();
    } catch (BadRequestException e) {
      assertThat(e.getMessage()).isEqualTo("Rule blah:toto and profile pjava have different languages");
    }
  }

  @Test
  public void activate_rule_override_severity() throws Exception {
    QualityProfileDto profile = createProfile("java");
    RuleDto rule = createRule(profile.getLanguage(), "toto");
    session.commit();
    ruIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey());
    request.setParam(RuleActivationActions.RULE_KEY, rule.getKey().toString());
    request.setParam(RuleActivationActions.SEVERITY, "MINOR");
    WsTester.Result result = request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), rule.getKey());

    assertThat(db.activeRuleDao().selectOrFailByKey(session, activeRuleKey).getSeverityString())
      .isEqualTo("MINOR");
  }

  @Test
  public void bulk_activate_rule() throws Exception {
    QualityProfileDto profile = createProfile("java");
    createRule(profile.getLanguage(), "toto");
    createRule(profile.getLanguage(), "tata");
    createRule(profile.getLanguage(), "hello");
    createRule(profile.getLanguage(), "world");
    session.commit();
    ruIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey());
    request.setParam(PARAM_LANGUAGES, "java");
    request.execute().assertJson(getClass(), "bulk_activate_rule.json");
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(4);
  }

  @Test
  public void bulk_activate_rule_not_all() throws Exception {
    QualityProfileDto java = createProfile("java");
    QualityProfileDto php = createProfile("php");
    createRule(java.getLanguage(), "toto");
    createRule(java.getLanguage(), "tata");
    createRule(php.getLanguage(), "hello");
    createRule(php.getLanguage(), "world");
    session.commit();
    ruIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, php.getKey())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, php.getKey());
    request.setParam(PARAM_LANGUAGES, "php");
    request.execute().assertJson(getClass(), "bulk_activate_rule_not_all.json");
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, php.getKey())).hasSize(2);
  }

  @Test
  public void bulk_activate_rule_by_query() throws Exception {
    QualityProfileDto profile = createProfile("java");
    createRule(profile.getLanguage(), "toto");
    createRule(profile.getLanguage(), "tata");
    createRule(profile.getLanguage(), "hello");
    createRule(profile.getLanguage(), "world");
    session.commit();
    ruIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    // 1. Activate Rule with query returning 0 hits
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey());
    request.setParam(WebService.Param.TEXT_QUERY, "php");
    request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(0);

    // 1. Activate Rule with query returning 1 hits
    request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey());
    request.setParam(WebService.Param.TEXT_QUERY, "world");
    request.execute();
    session.commit();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(1);
  }

  @Test
  public void bulk_activate_rule_by_query_with_severity() throws Exception {
    QualityProfileDto profile = createProfile("java");
    RuleDto rule0 = createRule(profile.getLanguage(), "toto");
    RuleDto rule1 = createRule(profile.getLanguage(), "tata");
    session.commit();
    ruIndexer.index();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    // 2. Assert ActiveRule with BLOCKER severity
    assertThat(tester.get(RuleIndex.class).search(
      new RuleQuery().setSeverities(ImmutableSet.of("BLOCKER")),
      new SearchOptions()).getIds()).hasSize(2);

    // 1. Activate Rule with query returning 2 hits
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(BulkRuleActivationActions.PROFILE_KEY, profile.getKey());
    request.setParam(BulkRuleActivationActions.SEVERITY, "MINOR");
    request.execute();
    session.commit();

    // 2. Assert ActiveRule with MINOR severity
    assertThat(tester.get(ActiveRuleDao.class).selectByRuleId(session, rule0.getId()).get(0).getSeverityString()).isEqualTo("MINOR");
    assertThat(tester.get(RuleIndex.class).searchAll(new RuleQuery()
      .setQProfileKey(profile.getKey())
      .setKey(rule0.getKey().toString())
      .setActiveSeverities(Collections.singleton("MINOR"))
      .setActivation(true))).hasSize(1);
  }

  @Test
  public void does_not_return_warnings_when_bulk_activate_on_profile_and_rules_exist_on_another_language_than_profile() throws Exception {
    QualityProfileDto javaProfile = createProfile("java");
    createRule(javaProfile.getLanguage(), "toto");
    createRule(javaProfile.getLanguage(), "tata");
    QualityProfileDto phpProfile = createProfile("php");
    createRule(phpProfile.getLanguage(), "hello");
    createRule(phpProfile.getLanguage(), "world");
    session.commit();
    ruIndexer.index();

    // 1. Activate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, javaProfile.getKey());
    request.setParam(PARAM_QPROFILE, javaProfile.getKey());
    request.setParam("activation", "false");
    request.execute().assertJson(getClass(), "does_not_return_warnings_when_bulk_activate_on_profile_and_rules_exist_on_another_language_than_profile.json");
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, javaProfile.getKey())).hasSize(2);
  }

  @Test
  public void reset() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    QualityProfileDto subProfile = QProfileTesting.newXooP2().setParentKee(QProfileTesting.XOO_P1_KEY);
    db.qualityProfileDao().insert(session, profile, subProfile);

    RuleDto rule = createRule(profile.getLanguage(), "rule");
    ActiveRuleDto active1 = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    ActiveRuleDto active2 = ActiveRuleDto.createFor(subProfile, rule)
      .setSeverity("MINOR");
    db.activeRuleDao().insert(session, active1);
    db.activeRuleDao().insert(session, active2);

    session.commit();
    ruIndexer.index();
    activeRuIndexer.index();

    // 0. assert rule child rule is minor
    assertThat(db.activeRuleDao().selectOrFailByKey(session, active2.getKey()).getSeverityString()).isEqualTo("MINOR");

    // 1. reset child rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.ACTIVATE_ACTION);
    request.setParam("profile_key", subProfile.getKey());
    request.setParam("rule_key", rule.getKey().toString());
    request.setParam("reset", "true");
    request.execute();
    session.clearCache();

    // 2. assert rule child rule is NOT minor
    assertThat(db.activeRuleDao().selectOrFailByKey(session, active2.getKey()).getSeverityString()).isNotEqualTo("MINOR");
  }

  @Test
  public void add_project_with_key_and_uuid() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setId(1L);
    db.componentDao().insert(session, project);
    QualityProfileDto profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(session, profile);

    session.commit();

    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "add_project")
      .setParam("profileKey", profile.getKee()).setParam("projectUuid", project.uuid())
      .execute().assertNoContent();
    assertThat(tester.get(QProfileFactory.class).getByProjectAndLanguage(session, project.getKey(), "xoo").getKee()).isEqualTo(profile.getKee());

    // Second call must not fail, do nothing
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "add_project")
      .setParam("profileKey", profile.getKee()).setParam("projectUuid", project.uuid())
      .execute().assertNoContent();
    assertThat(tester.get(QProfileFactory.class).getByProjectAndLanguage(session, project.getKey(), "xoo").getKee()).isEqualTo(profile.getKee());
  }

  @Test
  public void change_project_association_with_key_and_uuid() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setId(1L);
    db.componentDao().insert(session, project);
    QualityProfileDto profile1 = QProfileTesting.newXooP1();
    QualityProfileDto profile2 = QProfileTesting.newXooP2();
    db.qualityProfileDao().insert(session, profile1, profile2);
    db.qualityProfileDao().insertProjectProfileAssociation(project.uuid(), profile1.getKey(), session);

    session.commit();

    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "add_project")
      .setParam("profileKey", profile2.getKee()).setParam("projectUuid", project.uuid())
      .execute().assertNoContent();
    assertThat(tester.get(QProfileFactory.class).getByProjectAndLanguage(session, project.getKey(), "xoo").getKee()).isEqualTo(profile2.getKee());
  }

  @Test
  public void add_project_with_name_language_and_key() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setId(1L);
    db.componentDao().insert(session, project);
    QualityProfileDto profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(session, profile);

    session.commit();

    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "add_project")
      .setParam("language", "xoo").setParam("profileName", profile.getName()).setParam("projectKey", project.getKey())
      .execute().assertNoContent();
    assertThat(tester.get(QProfileFactory.class).getByProjectAndLanguage(session, project.getKey(), "xoo").getKee()).isEqualTo(profile.getKee());
  }

  @Test(expected = IllegalArgumentException.class)
  public void add_project_missing_language() throws Exception {
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "add_project")
      .setParam("profileName", "polop").setParam("projectKey", "palap")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void add_project_missing_name() throws Exception {
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "add_project")
      .setParam("language", "xoo").setParam("projectKey", "palap")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void add_project_too_many_profile_parameters() throws Exception {
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "add_project")
      .setParam("profileKey", "plouf").setParam("language", "xoo").setParam("profileName", "polop").setParam("projectUuid", "palap")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void add_project_missing_project() throws Exception {
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "add_project")
      .setParam("profileKey", "plouf")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void add_project_too_many_project_parameters() throws Exception {
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "add_project")
      .setParam("profileKey", "plouf").setParam("projectUuid", "polop").setParam("projectKey", "palap")
      .execute();
  }

  @Test(expected = NotFoundException.class)
  public void add_project_unknown_profile() throws Exception {
    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "add_project")
      .setParam("projectUuid", "plouf").setParam("profileName", "polop").setParam("language", "xoo")
      .execute();
  }

  @Test
  public void remove_project_with_key_and_uuid() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setId(1L);
    db.componentDao().insert(session, project);
    QualityProfileDto profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(session, profile);
    db.qualityProfileDao().insertProjectProfileAssociation(project.uuid(), profile.getKee(), session);

    session.commit();

    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "remove_project")
      .setParam("profileKey", profile.getKee()).setParam("projectUuid", project.uuid())
      .execute().assertNoContent();
    assertThat(tester.get(QProfileFactory.class).getByProjectAndLanguage(session, project.getKey(), "xoo")).isNull();
  }

  @Test
  public void remove_project_with_name_language_and_key() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setId(1L);
    db.componentDao().insert(session, project);
    QualityProfileDto profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(session, profile);
    db.qualityProfileDao().insertProjectProfileAssociation(project.uuid(), profile.getKee(), session);

    session.commit();

    wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, "remove_project")
      .setParam("language", "xoo").setParam("profileName", profile.getName()).setParam("projectKey", project.getKey())
      .execute().assertNoContent();
    assertThat(tester.get(QProfileFactory.class).getByProjectAndLanguage(session, project.getKey(), "xoo")).isNull();
  }

  private QualityProfileDto createProfile(String lang) {
    QualityProfileDto profile = QProfileTesting.newQProfileDto(new QProfileName(lang, "P" + lang), "p" + lang);
    db.qualityProfileDao().insert(session, profile);
    return profile;
  }

  private RuleDto createRule(String lang, String id) {
    RuleDto rule = RuleTesting.newDto(RuleKey.of("blah", id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY);
    db.ruleDao().insert(session, rule);
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDto rule, QualityProfileDto profile) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    db.activeRuleDao().insert(session, activeRule);
    return activeRule;
  }
}
