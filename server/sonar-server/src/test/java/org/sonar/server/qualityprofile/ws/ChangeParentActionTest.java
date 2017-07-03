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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARENT_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARENT_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;

public class ChangeParentActionTest {

  @Rule
  public DbTester dbTester = new DbTester(System2.INSTANCE, null);
  @Rule
  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DbClient dbClient;
  private DbSession dbSession;
  private RuleIndex ruleIndex;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;
  private WsActionTester ws;
  private OrganizationDto organization;
  private RuleActivator ruleActivator;
  private Language language = LanguageTesting.newLanguage(randomAlphanumeric(20));
  private String ruleRepository = randomAlphanumeric(5);

  @Before
  public void setUp() {
    dbClient = dbTester.getDbClient();
    dbSession = dbTester.getSession();
    EsClient esClient = esTester.client();
    ruleIndex = new RuleIndex(esClient);
    ruleIndexer = new RuleIndexer(esClient, dbClient);
    activeRuleIndexer = new ActiveRuleIndexer(dbClient, esClient);
    RuleActivatorContextFactory ruleActivatorContextFactory = new RuleActivatorContextFactory(dbClient);
    TypeValidations typeValidations = new TypeValidations(Collections.emptyList());
    ruleActivator = new RuleActivator(System2.INSTANCE, dbClient, ruleIndex, ruleActivatorContextFactory, typeValidations, activeRuleIndexer, userSessionRule);

    ChangeParentAction underTest = new ChangeParentAction(
      dbClient,
      new RuleActivator(
        System2.INSTANCE,
        dbClient,
        ruleIndex,
        ruleActivatorContextFactory,
        typeValidations,
        activeRuleIndexer,
        userSessionRule),
      new Languages(),
      new QProfileWsSupport(
        dbClient,
        userSessionRule,
        TestDefaultOrganizationProvider.from(dbTester)),
      userSessionRule);

    ws = new WsActionTester(underTest);
    organization = dbTester.organizations().insert();
    userSessionRule.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization.getUuid());
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting("key").containsExactlyInAnyOrder(
      "organization", "profile", "profileName", "language", "parentProfile", "parentName");
    assertThat(definition.param("organization").since()).isEqualTo("6.4");
    WebService.Param profile = definition.param("profile");
    assertThat(profile.deprecatedKey()).isEqualTo("profileKey");
    WebService.Param parentProfile = definition.param("parentProfile");
    assertThat(parentProfile.deprecatedKey()).isEqualTo("parentKey");
    WebService.Param profileName = definition.param("profileName");
    assertThat(profileName.deprecatedSince()).isEqualTo("6.5");
    WebService.Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isEqualTo("6.5");
    WebService.Param parentName = definition.param("parentName");
    assertThat(parentName.deprecatedSince()).isEqualTo("6.5");
  }

  @Test
  public void change_parent_with_no_parent_before() throws Exception {
    QProfileDto parent1 = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    createActiveRule(rule1, parent1);
    ruleIndexer.commitAndIndex(dbSession, rule1.getKey());
    activeRuleIndexer.indexOnStartup(emptySet());

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();

    // Set parent
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROFILE, child.getKee())
      .setParam(PARAM_PARENT_PROFILE, parent1.getKee())
      .execute();

    // Check rule 1 enabled
    List<OrgActiveRuleDto> activeRules1 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules1).hasSize(1);
    assertThat(activeRules1.get(0).getKey().getRuleKey().rule()).isEqualTo(rule1.getRuleKey());

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).hasSize(1);
  }

  @Test
  public void replace_existing_parent() throws Exception {
    QProfileDto parent1 = createProfile();
    QProfileDto parent2 = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    RuleDefinitionDto rule2 = createRule();
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    ruleIndexer.commitAndIndex(dbSession, asList(rule1.getKey(), rule2.getKey()));
    activeRuleIndexer.indexOnStartup(emptySet());

    // Set parent 1
    ruleActivator.setParentAndCommit(dbSession, child, parent1);

    // Set parent 2 through WS
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROFILE, child.getKee())
      .setParam(PARAM_PARENT_PROFILE, parent2.getKee())
      .execute();

    // Check rule 2 enabled
    List<OrgActiveRuleDto> activeRules2 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().getRuleKey().rule()).isEqualTo(rule2.getRuleKey());

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).hasSize(1);
  }

  @Test
  public void remove_parent() throws Exception {
    QProfileDto parent = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    createActiveRule(rule1, parent);
    ruleIndexer.commitAndIndex(dbSession, rule1.getKey());
    activeRuleIndexer.indexOnStartup(emptySet());

    // Set parent
    ruleActivator.setParentAndCommit(dbSession, child, parent);

    // Remove parent through WS
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROFILE, child.getKee())
      .execute();

    // Check no rule enabled
    assertThat(dbClient.activeRuleDao().selectByProfile(dbSession, child)).isEmpty();

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void change_parent_with_names() throws Exception {
    QProfileDto parent1 = createProfile();
    QProfileDto parent2 = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    RuleDefinitionDto rule2 = createRule();
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    ruleIndexer.commitAndIndex(dbSession, rule1.getKey());
    activeRuleIndexer.indexOnStartup(emptySet());

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();

    System.out.println("org uuid: " + organization.getUuid());
    System.out.println("org key: " + organization.getKey());

    // 1. Set parent 1
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_PROFILE_NAME, child.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PARENT_NAME, parent1.getName())
      .execute();

    // 1. check rule 1 enabled
    List<OrgActiveRuleDto> activeRules1 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules1).hasSize(1);
    assertThat(activeRules1.get(0).getKey().getRuleKey().rule()).isEqualTo(rule1.getRuleKey());
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).hasSize(1);

    // 2. Set parent 2
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_PROFILE_NAME, child.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PARENT_NAME, parent2.getName())
      .execute();

    // 2. check rule 2 enabled
    List<OrgActiveRuleDto> activeRules2 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().getRuleKey().rule()).isEqualTo(rule2.getRuleKey());

    // 3. Remove parent
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_PROFILE_NAME, child.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PARENT_NAME, "")
      .execute();

    // 3. check no rule enabled
    List<OrgActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void remove_parent_with_empty_key() throws Exception {
    QProfileDto parent = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    createActiveRule(rule1, parent);
    ruleIndexer.commitAndIndex(dbSession, rule1.getKey());
    activeRuleIndexer.indexOnStartup(emptySet());

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();

    // Set parent
    ruleActivator.setParentAndCommit(dbSession, child, parent);

    // Remove parent
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROFILE, child.getKee())
      .setParam(PARAM_PARENT_PROFILE, "")
      .execute();

    // Check no rule enabled
    assertThat(dbClient.activeRuleDao().selectByProfile(dbSession, child)).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void fail_if_built_in_profile() {
    QProfileDto child = dbTester.qualityProfiles().insert(organization, p -> p
      .setLanguage(language.getKey())
      .setIsBuiltIn(true));

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROFILE, child.getKee())
      .setParam(PARAM_PARENT_PROFILE, "palap");

    thrown.expect(BadRequestException.class);

    request.execute();
  }

  @Test
  public void fail_if_parent_key_and_name_both_set() throws Exception {
    QProfileDto child = createProfile();

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROFILE, child.getKee())
      .setParam(PARAM_PARENT_NAME, "polop")
      .setParam(PARAM_PARENT_PROFILE, "palap");
    thrown.expect(IllegalArgumentException.class);
    request
      .execute();
  }

  @Test
  public void fail_if_profile_key_and_name_both_set() throws Exception {
    QProfileDto child = createProfile();

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROFILE, child.getKee())
      .setParam(PARAM_PROFILE_NAME, child.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PARENT_PROFILE, "palap");

    thrown.expect(IllegalArgumentException.class);
    request.execute();
  }

  @Test
  public void fail_if_missing_permission() throws Exception {
    userSessionRule.logIn();

    QProfileDto child = createProfile();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROFILE, child.getKee());

    thrown.expect(ForbiddenException.class);
    thrown.expectMessage("Insufficient privileges");
    request.execute();
  }

  @Test
  public void fail_if_missing_permission_for_this_organization() throws Exception {
    OrganizationDto organization2 = dbTester.organizations().insert();
    userSessionRule.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization2.getUuid());

    QProfileDto child = createProfile();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_PROFILE, child.getKee());

    thrown.expect(ForbiddenException.class);
    thrown.expectMessage("Insufficient privileges");
    request.execute();
  }

  private QProfileDto createProfile() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid())
      .setLanguage(language.getKey());
    dbClient.qualityProfileDao().insert(dbSession, profile);
    dbSession.commit();
    return profile;
  }

  private RuleDefinitionDto createRule() {
    RuleDefinitionDto rule = RuleTesting.newRule(RuleKey.of(ruleRepository, randomAlphanumeric(5)))
      .setLanguage(language.getKey())
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY);
    dbClient.ruleDao().insert(dbSession, rule);
    dbSession.commit();
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDefinitionDto rule, QProfileDto profile) {
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString());
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();
    return activeRule;
  }
}
