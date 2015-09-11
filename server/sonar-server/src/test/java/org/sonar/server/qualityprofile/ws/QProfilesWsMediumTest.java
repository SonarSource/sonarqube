/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.rule.ws.SearchAction;
import org.sonar.server.search.QueryContext;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class QProfilesWsMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester)
      .login("gandalf").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

  QProfilesWs ws;
  DbClient db;
  DbSession session;
  WsTester wsTester;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    ws = tester.get(QProfilesWs.class);
    wsTester = tester.get(WsTester.class);
    session = db.openSession(false);
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

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(1);

    // 1. Deactivate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.DEACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
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

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(4);

    // 1. Deactivate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_DEACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
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

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(2);

    // 1. Deactivate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_DEACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
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

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(2);

    // 1. Deactivate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_DEACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
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

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
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

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    try {
      // 1. Activate Rule
      WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.ACTIVATE_ACTION);
      request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
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

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
    request.setParam(RuleActivationActions.RULE_KEY, rule.getKey().toString());
    request.setParam(RuleActivationActions.SEVERITY, "MINOR");
    WsTester.Result result = request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile.getKey(), rule.getKey());

    assertThat(db.activeRuleDao().getNullableByKey(session, activeRuleKey).getSeverityString())
      .isEqualTo("MINOR");
  }

  @Test
  public void bulk_activate_rule() throws Exception {
    QualityProfileDto profile = createProfile("java");
    RuleDto rule0 = createRule(profile.getLanguage(), "toto");
    RuleDto rule1 = createRule(profile.getLanguage(), "tata");
    RuleDto rule2 = createRule(profile.getLanguage(), "hello");
    RuleDto rule3 = createRule(profile.getLanguage(), "world");
    session.commit();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
    request.setParam(SearchAction.PARAM_LANGUAGES, "java");
    WsTester.Result result = request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(4);
  }

  @Test
  public void bulk_activate_rule_not_all() throws Exception {
    QualityProfileDto java = createProfile("java");
    QualityProfileDto php = createProfile("php");
    RuleDto rule0 = createRule(java.getLanguage(), "toto");
    RuleDto rule1 = createRule(java.getLanguage(), "tata");
    RuleDto rule2 = createRule(php.getLanguage(), "hello");
    RuleDto rule3 = createRule(php.getLanguage(), "world");
    session.commit();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, php.getKey())).isEmpty();

    // 1. Activate Rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, php.getKey().toString());
    request.setParam(SearchAction.PARAM_LANGUAGES, "php");
    WsTester.Result result = request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, php.getKey())).hasSize(2);
  }

  @Test
  public void bulk_activate_rule_by_query() throws Exception {
    QualityProfileDto profile = createProfile("java");
    RuleDto rule0 = createRule(profile.getLanguage(), "toto");
    RuleDto rule1 = createRule(profile.getLanguage(), "tata");
    RuleDto rule2 = createRule(profile.getLanguage(), "hello");
    RuleDto rule3 = createRule(profile.getLanguage(), "world");
    session.commit();

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();

    // 1. Activate Rule with query returning 0 hits
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
    request.setParam(WebService.Param.TEXT_QUERY, "php");
    WsTester.Result result = request.execute();
    session.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(0);

    // 1. Activate Rule with query returning 1 hits
    request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(RuleActivationActions.PROFILE_KEY, profile.getKey().toString());
    request.setParam(WebService.Param.TEXT_QUERY, "world");
    result = request.execute();
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

    // 0. Assert No Active Rule for profile
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).isEmpty();
    assertThat(db.activeRuleDao().selectByProfileKey(session, profile.getKey())).hasSize(0);

    // 2. Assert ActiveRule with BLOCKER severity
    assertThat(tester.get(RuleIndex.class).search(
      new RuleQuery().setSeverities(ImmutableSet.of("BLOCKER")),
      new QueryContext(userSessionRule)).getHits()).hasSize(2);

    // 1. Activate Rule with query returning 2 hits
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    request.setParam(BulkRuleActivationActions.PROFILE_KEY, profile.getKey().toString());
    request.setParam(BulkRuleActivationActions.SEVERITY, "MINOR");
    request.execute();
    session.commit();

    // 2. Assert ActiveRule with MINOR severity
    assertThat(tester.get(ActiveRuleIndex.class).findByRule(rule0.getKey()).get(0).severity()).isEqualTo("MINOR");

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
    db.activeRuleDao().insert(session, active1, active2);

    session.commit();

    // 0. assert rule child rule is minor
    assertThat(db.activeRuleDao().getByKey(session, active2.getKey()).getSeverityString()).isEqualTo("MINOR");

    // 1. reset child rule
    WsTester.TestRequest request = wsTester.newPostRequest(QProfilesWs.API_ENDPOINT, RuleActivationActions.ACTIVATE_ACTION);
    request.setParam("profile_key", subProfile.getKey());
    request.setParam("rule_key", rule.getKey().toString());
    request.setParam("reset", "true");
    request.execute();
    session.clearCache();

    // 2. assert rule child rule is NOT minor
    assertThat(db.activeRuleDao().getByKey(session, active2.getKey()).getSeverityString()).isNotEqualTo("MINOR");
  }

  @Test
  public void add_project_with_key_and_uuid() throws Exception {
    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setUuid("ABCD")
      .setKey("org.codehaus.sonar:sonar")
      .setName("SonarQube")
      .setLongName("SonarQube")
      .setQualifier("TRK")
      .setScope("TRK")
      .setEnabled(true);
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
    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setUuid("ABCD")
      .setKey("org.codehaus.sonar:sonar")
      .setName("SonarQube")
      .setLongName("SonarQube")
      .setQualifier("TRK")
      .setScope("TRK")
      .setEnabled(true);
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
    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setUuid("ABCD")
      .setKey("org.codehaus.sonar:sonar")
      .setName("SonarQube")
      .setLongName("SonarQube")
      .setQualifier("TRK")
      .setScope("TRK")
      .setEnabled(true);
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
    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setUuid("ABCD")
      .setKey("org.codehaus.sonar:sonar")
      .setName("SonarQube")
      .setLongName("SonarQube")
      .setQualifier("TRK")
      .setScope("TRK")
      .setEnabled(true);
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
    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setUuid("ABCD")
      .setKey("org.codehaus.sonar:sonar")
      .setName("SonarQube")
      .setLongName("SonarQube")
      .setQualifier("TRK")
      .setScope("TRK")
      .setEnabled(true);
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
    RuleDto rule = RuleDto.createFor(RuleKey.of("blah", id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY);
    db.deprecatedRuleDao().insert(session, rule);
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDto rule, QualityProfileDto profile) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    db.activeRuleDao().insert(session, activeRule);
    return activeRule;
  }
}
