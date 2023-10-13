/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.qualityprofile.QProfileRulesImpl;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.rule.ws.RuleQueryFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_QPROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RESET;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SEVERITY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TARGET_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TARGET_SEVERITY;

public class QProfilesWsMediumIT {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().logIn();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester dbTester = DbTester.create();

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();
  private final RuleIndex ruleIndex = new RuleIndex(es.client(), System2.INSTANCE);
  private final RuleIndexer ruleIndexer = new RuleIndexer(es.client(), dbClient);
  private final ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(dbClient, es.client());
  private final TypeValidations typeValidations = new TypeValidations(emptyList());
  private final QualityProfileChangeEventService qualityProfileChangeEventService = mock(QualityProfileChangeEventService.class);
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
  private final RuleActivator ruleActivator = new RuleActivator(System2.INSTANCE, dbClient, typeValidations, userSessionRule, mock(Configuration.class), sonarQubeVersion);
  private final QProfileRules qProfileRules = new QProfileRulesImpl(dbClient, ruleActivator, ruleIndex, activeRuleIndexer, qualityProfileChangeEventService);
  private final QProfileWsSupport qProfileWsSupport = new QProfileWsSupport(dbClient, userSessionRule);
  private final RuleQueryFactory ruleQueryFactory = new RuleQueryFactory(dbClient);

  private final WsActionTester wsDeactivateRule = new WsActionTester(new DeactivateRuleAction(dbClient, qProfileRules, userSessionRule, qProfileWsSupport));
  private final WsActionTester wsDeactivateRules = new WsActionTester(new DeactivateRulesAction(ruleQueryFactory, userSessionRule, qProfileRules, qProfileWsSupport, dbClient));
  private final WsActionTester wsActivateRule = new WsActionTester(new ActivateRuleAction(dbClient, qProfileRules, userSessionRule, qProfileWsSupport));
  private final WsActionTester wsActivateRules = new WsActionTester(new ActivateRulesAction(ruleQueryFactory, userSessionRule, qProfileRules, qProfileWsSupport, dbClient));

  @Before
  public void before(){
    userSessionRule.logIn().setSystemAdministrator();
    userSessionRule.addPermission(GlobalPermission.ADMINISTER);
    userSessionRule.addPermission(GlobalPermission.ADMINISTER_QUALITY_PROFILES);
  }

  @Test
  public void deactivate_rule() {
    QProfileDto profile = createProfile("java");
    RuleDto rule = createRule(profile.getLanguage(), "toto");
    createActiveRule(rule, profile);
    ruleIndexer.commitAndIndex(dbSession, rule.getUuid());
    activeRuleIndexer.indexAll();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(1);

    // 1. Deactivate Rule
    wsDeactivateRule.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, profile.getKee())
      .setParam(PARAM_RULE, rule.getKey().toString())
      .execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();
  }

  @Test
  public void bulk_deactivate_rule() {
    QProfileDto profile = createProfile("java");
    RuleDto rule0 = createRule(profile.getLanguage(), "toto1");
    RuleDto rule1 = createRule(profile.getLanguage(), "toto2");
    RuleDto rule2 = createRule(profile.getLanguage(), "toto3");
    RuleDto rule3 = createRule(profile.getLanguage(), "toto4");
    createActiveRule(rule0, profile);
    createActiveRule(rule2, profile);
    createActiveRule(rule3, profile);
    createActiveRule(rule1, profile);
    dbSession.commit();
    activeRuleIndexer.indexAll();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(4);

    // 1. Deactivate Rule
    wsDeactivateRules.newRequest().setMethod("POST")
      .setParam(PARAM_TARGET_KEY, profile.getKee())
      .execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();
  }

  @Test
  public void bulk_deactivate_rule_not_all() {
    QProfileDto profile = createProfile("java");
    QProfileDto php = createProfile("php");
    RuleDto rule0 = createRule(profile.getLanguage(), "toto1");
    RuleDto rule1 = createRule(profile.getLanguage(), "toto2");
    createActiveRule(rule0, profile);
    createActiveRule(rule1, profile);
    createActiveRule(rule0, php);
    createActiveRule(rule1, php);
    dbSession.commit();
    activeRuleIndexer.indexAll();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(2);

    // 1. Deactivate Rule
    wsDeactivateRules.newRequest().setMethod("POST")
      .setParam(PARAM_TARGET_KEY, profile.getKee())
      .execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, php.getKee())).hasSize(2);
  }

  @Test
  public void bulk_deactivate_rule_by_profile() {
    QProfileDto profile = createProfile("java");
    RuleDto rule0 = createRule(profile.getLanguage(), "hello");
    RuleDto rule1 = createRule(profile.getLanguage(), "world");
    createActiveRule(rule0, profile);
    createActiveRule(rule1, profile);
    dbSession.commit();
    activeRuleIndexer.indexAll();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(2);

    // 1. Deactivate Rule
    wsDeactivateRules.newRequest().setMethod("POST")
      .setParam(PARAM_TARGET_KEY, profile.getKee())
      .setParam(Param.TEXT_QUERY, "hello")
      .execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(1);
  }

  @Test
  public void activate_rule() {
    QProfileDto profile = createProfile("java");
    RuleDto rule = createRule(profile.getLanguage(), "toto");
    ruleIndexer.commitAndIndex(dbSession, rule.getUuid());

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 1. Activate Rule
    wsActivateRule.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, profile.getKee())
      .setParam(PARAM_RULE, rule.getKey().toString())
      .execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(1);
  }

  @Test
  public void activate_rule_diff_languages() {
    QProfileDto profile = createProfile("java");
    RuleDto rule = createRule("php", "toto");
    ruleIndexer.commitAndIndex(dbSession, rule.getUuid());

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    try {
      // 1. Activate Rule
      wsActivateRule.newRequest().setMethod("POST")
        .setParam(PARAM_KEY, profile.getKee())
        .setParam(PARAM_RULE, rule.getKey().toString())
        .execute();
      dbSession.clearCache();
      fail();
    } catch (BadRequestException e) {
      assertThat(e.getMessage()).isEqualTo("php rule blah:toto cannot be activated on java profile Pjava");
    }
  }

  @Test
  public void activate_rule_override_severity() {
    QProfileDto profile = createProfile("java");
    RuleDto rule = createRule(profile.getLanguage(), "toto");
    ruleIndexer.commitAndIndex(dbSession, rule.getUuid());

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 1. Activate Rule
    wsActivateRule.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, profile.getKee())
      .setParam(PARAM_RULE, rule.getKey().toString())
      .setParam(PARAM_SEVERITY, "MINOR")
      .execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profile, rule.getKey());

    Optional<ActiveRuleDto> activeRuleDto = dbClient.activeRuleDao().selectByKey(dbSession, activeRuleKey);
    assertThat(activeRuleDto).isPresent();
    assertThat(activeRuleDto.get().getSeverityString()).isEqualTo(Severity.MINOR);
  }

  @Test
  public void bulk_activate_rule() {
    QProfileDto profile = createProfile("java");
    createRule(profile.getLanguage(), "toto");
    createRule(profile.getLanguage(), "tata");
    createRule(profile.getLanguage(), "hello");
    createRule(profile.getLanguage(), "world");
    dbSession.commit();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 1. Activate Rule
    wsActivateRules.newRequest().setMethod("POST")
      .setParam(PARAM_TARGET_KEY, profile.getKee())
      .setParam(PARAM_LANGUAGES, "java")
      .execute()
      .assertJson(getClass(), "bulk_activate_rule.json");
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(4);
  }

  @Test
  public void bulk_activate_rule_not_all() {
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
    wsActivateRules.newRequest().setMethod("POST")
      .setParam(PARAM_TARGET_KEY, php.getKee())
      .setParam(PARAM_LANGUAGES, "php")
      .execute()
      .assertJson(getClass(), "bulk_activate_rule_not_all.json");
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, php.getKee())).hasSize(2);
  }

  @Test
  public void bulk_activate_rule_by_query() {
    QProfileDto profile = createProfile("java");
    createRule(profile.getLanguage(), "toto");
    createRule(profile.getLanguage(), "tata");
    createRule(profile.getLanguage(), "hello");
    createRule(profile.getLanguage(), "world");
    dbSession.commit();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 1. Activate Rule with query returning 0 hits
    wsActivateRules.newRequest().setMethod("POST")
      .setParam(PARAM_TARGET_KEY, profile.getKee())
      .setParam(Param.TEXT_QUERY, "php")
      .execute();
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 1. Activate Rule with query returning 1 hits
    wsActivateRules.newRequest().setMethod("POST")
      .setParam(PARAM_TARGET_KEY, profile.getKee())
      .setParam(Param.TEXT_QUERY, "world")
      .execute();
    dbSession.commit();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).hasSize(1);
  }

  @Test
  public void bulk_activate_rule_by_query_with_severity() {
    QProfileDto profile = createProfile("java");
    RuleDto rule0 = createRule(profile.getLanguage(), "toto");
    RuleDto rule1 = createRule(profile.getLanguage(), "tata");
    dbSession.commit();

    // 0. Assert No Active Rule for profile
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, profile.getKee())).isEmpty();

    // 2. Assert ActiveRule with BLOCKER severity
    assertThat(ruleIndex.search(
      new RuleQuery().setSeverities(ImmutableSet.of("BLOCKER")),
      new SearchOptions()).getUuids()).hasSize(2);

    // 1. Activate Rule with query returning 2 hits
    wsActivateRules.newRequest().setMethod("POST")
      .setParam(PARAM_TARGET_KEY, profile.getKee())
      .setParam(PARAM_TARGET_SEVERITY, "MINOR")
      .execute();
    dbSession.commit();

    // 2. Assert ActiveRule with MINOR severity
    assertThat(dbClient.activeRuleDao().selectByOrgRuleUuid(dbSession, rule0.getUuid()).get(0).getSeverityString()).isEqualTo("MINOR");
    assertThat(ruleIndex.searchAll(new RuleQuery()
      .setQProfile(profile)
      .setKey(rule0.getKey().toString())
      .setActiveSeverities(Collections.singleton("MINOR"))
      .setActivation(true))).toIterable().hasSize(1);
  }

  @Test
  public void does_not_return_warnings_when_bulk_activate_on_profile_and_rules_exist_on_another_language_than_profile() {
    QProfileDto javaProfile = createProfile("java");
    createRule(javaProfile.getLanguage(), "toto");
    createRule(javaProfile.getLanguage(), "tata");
    QProfileDto phpProfile = createProfile("php");
    createRule(phpProfile.getLanguage(), "hello");
    createRule(phpProfile.getLanguage(), "world");
    dbSession.commit();

    // 1. Activate Rule
    wsActivateRules.newRequest().setMethod("POST")
      .setParam(PARAM_TARGET_KEY, javaProfile.getKee())
      .setParam(PARAM_QPROFILE, javaProfile.getKee())
      .setParam("activation", "false")
      .execute()
      .assertJson(getClass(), "does_not_return_warnings_when_bulk_activate_on_profile_and_rules_exist_on_another_language_than_profile.json");
    dbSession.clearCache();

    // 2. Assert ActiveRule in DAO
    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, javaProfile.getKee())).hasSize(2);
  }

  @Test
  public void reset() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto().setLanguage("java");
    QProfileDto childProfile = QualityProfileTesting.newQualityProfileDto().setParentKee(profile.getKee()).setLanguage("java");
    dbClient.qualityProfileDao().insert(dbSession, profile, childProfile);

    RuleDto rule = createRule(profile.getLanguage(), "rule");
    ActiveRuleDto active1 = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    ActiveRuleDto active2 = ActiveRuleDto.createFor(childProfile, rule)
      .setSeverity("MINOR");
    dbClient.activeRuleDao().insert(dbSession, active1);
    dbClient.activeRuleDao().insert(dbSession, active2);

    dbSession.commit();
    activeRuleIndexer.indexAll();

    // 0. assert rule child rule is minor
    Optional<ActiveRuleDto> activeRuleDto = dbClient.activeRuleDao().selectByKey(dbSession, active2.getKey());
    assertThat(activeRuleDto).isPresent();
    assertThat(activeRuleDto.get().getSeverityString()).isEqualTo(Severity.MINOR);

    // 1. reset child rule
    wsActivateRule.newRequest().setMethod("POST")
      .setParam(PARAM_KEY, childProfile.getKee())
      .setParam(PARAM_RULE, rule.getKey().toString())
      .setParam(PARAM_RESET, "true")
      .execute();
    dbSession.clearCache();

    // 2. assert rule child rule is NOT minor
    activeRuleDto = dbClient.activeRuleDao().selectByKey(dbSession, active2.getKey());
    assertThat(activeRuleDto).isPresent();
    assertThat(activeRuleDto.get().getSeverityString()).isNotEqualTo(Severity.MINOR);
  }

  private QProfileDto createProfile(String lang) {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto().setName("P" + lang).setLanguage(lang);
    dbClient.qualityProfileDao().insert(dbSession, profile);
    return profile;
  }

  private RuleDto createRule(String lang, String id) {
    RuleDto rule = RuleTesting.newRule(RuleKey.of("blah", id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY);
    dbClient.ruleDao().insert(dbSession, rule);
    ruleIndexer.commitAndIndex(dbSession, rule.getUuid());
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDto rule, QProfileDto profile) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    return activeRule;
  }
}
