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

import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.RuleActivatorContextFactory;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualityprofile.ws.QProfileReference.DEFAULT_PARAM_LANGUAGE;
import static org.sonar.server.qualityprofile.ws.QProfileReference.DEFAULT_PARAM_PROFILE_KEY;
import static org.sonar.server.qualityprofile.ws.QProfileReference.DEFAULT_PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;

public class ChangeParentActionTest {

  @Rule
  public DbTester dbTester = new DbTester(System2.INSTANCE, null);
  @Rule
  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DbClient dbClient;
  private DbSession dbSession;
  private RuleIndex ruleIndex;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;
  private WsActionTester wsActionTester;
  private OrganizationDto organization;
  private RuleActivator ruleActivator;

  @Before
  public void setUp() {
    dbClient = dbTester.getDbClient();
    dbSession = dbTester.getSession();
    EsClient esClient = esTester.client();
    ruleIndex = new RuleIndex(esClient);
    ruleIndexer = new RuleIndexer(
      System2.INSTANCE,
      dbClient,
      esClient
    );
    activeRuleIndexer = new ActiveRuleIndexer(
      System2.INSTANCE,
      dbClient,
      esClient
    );
    RuleActivatorContextFactory ruleActivatorContextFactory = new RuleActivatorContextFactory(dbClient);
    TypeValidations typeValidations = new TypeValidations(Collections.emptyList());
    ruleActivator = new RuleActivator(
      System2.INSTANCE,
      dbClient,
      ruleIndex,
      ruleActivatorContextFactory,
      typeValidations,
      activeRuleIndexer,
      userSessionRule
    );
    ChangeParentAction underTest = new ChangeParentAction(
      dbClient,
      new RuleActivator(
        System2.INSTANCE,
        dbClient,
        ruleIndex,
        ruleActivatorContextFactory,
        typeValidations,
        activeRuleIndexer,
        userSessionRule
      ),
      new QProfileFactory(
        dbClient,
        UuidFactoryFast.getInstance(),
        System2.INSTANCE
      ),
      new Languages(),
      new QProfileWsSupport(
        dbClient,
        userSessionRule,
        TestDefaultOrganizationProvider.from(dbTester)
      )
    );
    wsActionTester = new WsActionTester(underTest);
    organization = dbTester.getDefaultOrganization();
    userSessionRule.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization.getUuid());
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void change_parent_with_no_parent_before() throws Exception {
    QualityProfileDto parent1 = createProfile("xoo", "Parent 1");
    QualityProfileDto child = createProfile("xoo", "Child");

    RuleDto rule1 = createRule("xoo", "rule1");
    createActiveRule(rule1, parent1);
    dbSession.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    assertThat(dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey())).isEmpty();

    // Set parent
    wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(DEFAULT_PARAM_PROFILE_KEY, child.getKey())
      .setParam("parentKey", parent1.getKey())
      .execute();
    dbSession.clearCache();

    // Check rule 1 enabled
    List<ActiveRuleDto> activeRules1 = dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey());
    assertThat(activeRules1).hasSize(1);
    assertThat(activeRules1.get(0).getKey().ruleKey().rule()).isEqualTo("rule1");

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).hasSize(1);
  }

  @Test
  public void replace_existing_parent() throws Exception {
    QualityProfileDto parent1 = createProfile("xoo", "Parent 1");
    QualityProfileDto parent2 = createProfile("xoo", "Parent 2");
    QualityProfileDto child = createProfile("xoo", "Child");

    RuleDto rule1 = createRule("xoo", "rule1");
    RuleDto rule2 = createRule("xoo", "rule2");
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    dbSession.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    // Set parent 1
    ruleActivator.setParent(dbSession, child.getKey(), parent1.getKey());
    dbSession.clearCache();

    // Set parent 2 through WS
    wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(DEFAULT_PARAM_PROFILE_KEY, child.getKey())
      .setParam("parentKey", parent2.getKey())
      .execute();
    dbSession.clearCache();

    // Check rule 2 enabled
    List<ActiveRuleDto> activeRules2 = dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey());
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().ruleKey().rule()).isEqualTo("rule2");

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).hasSize(1);
  }

  @Test
  public void remove_parent() throws Exception {
    QualityProfileDto parent = createProfile("xoo", "Parent 1");
    QualityProfileDto child = createProfile("xoo", "Child");

    RuleDto rule1 = createRule("xoo", "rule1");
    createActiveRule(rule1, parent);
    dbSession.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    // Set parent
    ruleActivator.setParent(dbSession, child.getKey(), parent.getKey());
    dbSession.clearCache();

    // Remove parent through WS
    wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(DEFAULT_PARAM_PROFILE_KEY, child.getKey())
      .execute();
    dbSession.clearCache();

    // Check no rule enabled
    List<ActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey());
    assertThat(activeRules).isEmpty();

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void change_parent_with_names() throws Exception {
    QualityProfileDto parent1 = createProfile("xoo", "Parent 1");
    QualityProfileDto parent2 = createProfile("xoo", "Parent 2");
    QualityProfileDto child = createProfile("xoo", "Child");

    RuleDto rule1 = createRule("xoo", "rule1");
    RuleDto rule2 = createRule("xoo", "rule2");
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    dbSession.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    assertThat(dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey())).isEmpty();

    // 1. Set parent 1
    wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(DEFAULT_PARAM_LANGUAGE, "xoo")
      .setParam(DEFAULT_PARAM_PROFILE_NAME, child.getName())
      .setParam("parentName", parent1.getName())
      .execute();
    dbSession.clearCache();

    // 1. check rule 1 enabled
    List<ActiveRuleDto> activeRules1 = dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey());
    assertThat(activeRules1).hasSize(1);
    assertThat(activeRules1.get(0).getKey().ruleKey().rule()).isEqualTo("rule1");
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).hasSize(1);

    // 2. Set parent 2
    wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(DEFAULT_PARAM_LANGUAGE, "xoo")
      .setParam(DEFAULT_PARAM_PROFILE_NAME, child.getName())
      .setParam("parentName", parent2.getName())
      .execute();
    dbSession.clearCache();

    // 2. check rule 2 enabled
    List<ActiveRuleDto> activeRules2 = dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey());
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().ruleKey().rule()).isEqualTo("rule2");

    // 3. Remove parent
    wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(DEFAULT_PARAM_LANGUAGE, "xoo")
      .setParam(DEFAULT_PARAM_PROFILE_NAME, child.getName())
      .setParam("parentName", "")
      .execute();
    dbSession.clearCache();

    // 3. check no rule enabled
    List<ActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey());
    assertThat(activeRules).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void remove_parent_with_empty_key() throws Exception {
    QualityProfileDto parent = createProfile("xoo", "Parent 1");
    QualityProfileDto child = createProfile("xoo", "Child");

    RuleDto rule1 = createRule("xoo", "rule1");
    createActiveRule(rule1, parent);
    dbSession.commit();
    ruleIndexer.index();
    activeRuleIndexer.index();

    assertThat(dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey())).isEmpty();

    // Set parent
    ruleActivator.setParent(dbSession, child.getKey(), parent.getKey());
    dbSession.clearCache();

    // Remove parent
    wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(DEFAULT_PARAM_PROFILE_KEY, child.getKey())
      .setParam("parentKey", "")
      .execute();
    dbSession.clearCache();

    // Check no rule enabled
    List<ActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey());
    assertThat(activeRules).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void fail_if_parent_key_and_name_both_set() throws Exception {
    QualityProfileDto child = createProfile("xoo", "Child");
    dbSession.commit();

    assertThat(dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).isEmpty();

    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(DEFAULT_PARAM_PROFILE_KEY, child.getKee())
      .setParam("parentName", "polop")
      .setParam("parentKey", "palap");
    thrown.expect(IllegalArgumentException.class);
    request
      .execute();
  }

  @Test
  public void fail_if_profile_key_and_name_both_set() throws Exception {
    QualityProfileDto child = createProfile("xoo", "Child");
    dbSession.commit();

    assertThat(dbClient.activeRuleDao().selectByProfileKey(dbSession, child.getKey())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfileKey(child.getKey()), new SearchOptions()).getIds()).isEmpty();

    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(DEFAULT_PARAM_PROFILE_KEY, child.getKee())
      .setParam(DEFAULT_PARAM_PROFILE_NAME, child.getName())
      .setParam(PARAM_ORGANIZATION, child.getOrganizationUuid())
      .setParam("parentKey", "palap");

    thrown.expect(IllegalArgumentException.class);
    request.execute();
  }

  @Test
  public void fail_if_missing_permission() throws Exception {
    userSessionRule.logIn("anakin");
    TestRequest request = wsActionTester.newRequest()
      .setMethod("POST")
      .setParam(DEFAULT_PARAM_PROFILE_KEY, "polop")
      .setParam("parentKey", "pulup");

    thrown.expect(ForbiddenException.class);
    request.execute();
  }

  private QualityProfileDto createProfile(String lang, String name) {
    QualityProfileDto profile = QProfileTesting.newQProfileDto(organization, new QProfileName(lang, name), "p" + lang + "-" + name.toLowerCase());
    dbClient.qualityProfileDao().insert(dbSession, profile);
    return profile;
  }

  private RuleDto createRule(String lang, String id) {
    RuleDto rule = RuleTesting.newDto(RuleKey.of("blah", id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY);
    dbClient.ruleDao().insert(dbSession, rule);
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDto rule, QualityProfileDto profile) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    return activeRule;
  }
}
