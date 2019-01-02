/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
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
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileTreeImpl;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARENT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARENT_QUALITY_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class ChangeParentActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient;
  private DbSession dbSession;
  private RuleIndex ruleIndex;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;
  private WsActionTester ws;
  private OrganizationDto organization;
  private Language language = LanguageTesting.newLanguage(randomAlphanumeric(20));
  private String ruleRepository = randomAlphanumeric(5);
  private QProfileTreeImpl qProfileTree;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();
    EsClient esClient = es.client();
    ruleIndex = new RuleIndex(esClient, System2.INSTANCE);
    ruleIndexer = new RuleIndexer(esClient, dbClient);
    activeRuleIndexer = new ActiveRuleIndexer(dbClient, esClient);
    TypeValidations typeValidations = new TypeValidations(Collections.emptyList());
    RuleActivator ruleActivator = new RuleActivator(System2.INSTANCE, dbClient, typeValidations, userSession);
    qProfileTree = new QProfileTreeImpl(dbClient, ruleActivator, System2.INSTANCE, activeRuleIndexer);
    ChangeParentAction underTest = new ChangeParentAction(
      dbClient,
      qProfileTree,
      new Languages(),
      new QProfileWsSupport(
        dbClient,
        userSession,
        TestDefaultOrganizationProvider.from(db)),
      userSession);

    ws = new WsActionTester(underTest);
    organization = db.organizations().insert();
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization.getUuid());
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder(
      "organization", "key", "qualityProfile", "language", "parentKey", "parentQualityProfile");
    assertThat(definition.param("organization").since()).isEqualTo("6.4");
    Param profile = definition.param("key");
    assertThat(profile.deprecatedKey()).isEqualTo("profileKey");
    assertThat(profile.deprecatedSince()).isEqualTo("6.6");
    Param parentProfile = definition.param("parentKey");
    assertThat(parentProfile.deprecatedKey()).isNullOrEmpty();
  }

  @Test
  public void change_parent_with_no_parent_before() {
    QProfileDto parent1 = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    createActiveRule(rule1, parent1);
    ruleIndexer.commitAndIndex(dbSession, rule1.getId());
    activeRuleIndexer.indexOnStartup(emptySet());

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();

    // Set parent
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, child.getKee())
      .setParam(PARAM_PARENT_KEY, parent1.getKee())
      .execute();

    // Check rule 1 enabled
    List<OrgActiveRuleDto> activeRules1 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules1).hasSize(1);
    assertThat(activeRules1.get(0).getKey().getRuleKey().rule()).isEqualTo(rule1.getRuleKey());

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).hasSize(1);
  }

  @Test
  public void replace_existing_parent() {
    QProfileDto parent1 = createProfile();
    QProfileDto parent2 = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    RuleDefinitionDto rule2 = createRule();
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    ruleIndexer.commitAndIndex(dbSession, asList(rule1.getId(), rule2.getId()));
    activeRuleIndexer.indexOnStartup(emptySet());

    // Set parent 1
    qProfileTree.setParentAndCommit(dbSession, child, parent1);

    // Set parent 2 through WS
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, child.getKee())
      .setParam(PARAM_PARENT_KEY, parent2.getKee())
      .execute();

    // Check rule 2 enabled
    List<OrgActiveRuleDto> activeRules2 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().getRuleKey().rule()).isEqualTo(rule2.getRuleKey());

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).hasSize(1);
  }

  @Test
  public void remove_parent() {
    QProfileDto parent = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    createActiveRule(rule1, parent);
    ruleIndexer.commitAndIndex(dbSession, rule1.getId());
    activeRuleIndexer.indexOnStartup(emptySet());

    // Set parent
    qProfileTree.setParentAndCommit(dbSession, child, parent);

    // Remove parent through WS
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, child.getKee())
      .execute();

    // Check no rule enabled
    assertThat(dbClient.activeRuleDao().selectByProfile(dbSession, child)).isEmpty();

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void change_parent_with_names() {
    QProfileDto parent1 = createProfile();
    QProfileDto parent2 = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    RuleDefinitionDto rule2 = createRule();
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    ruleIndexer.commitAndIndex(dbSession, rule1.getId());
    activeRuleIndexer.indexOnStartup(emptySet());

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();

    System.out.println("org uuid: " + organization.getUuid());
    System.out.println("org key: " + organization.getKey());

    // 1. Set parent 1
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PARENT_QUALITY_PROFILE, parent1.getName())
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
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(QualityProfileWsParameters.PARAM_PARENT_QUALITY_PROFILE, parent2.getName())
      .execute();

    // 2. check rule 2 enabled
    List<OrgActiveRuleDto> activeRules2 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().getRuleKey().rule()).isEqualTo(rule2.getRuleKey());

    // 3. Remove parent
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_LANGUAGE, child.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(QualityProfileWsParameters.PARAM_PARENT_QUALITY_PROFILE, "")
      .execute();

    // 3. check no rule enabled
    List<OrgActiveRuleDto> activeRules = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void remove_parent_with_empty_key() {
    QProfileDto parent = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    createActiveRule(rule1, parent);
    ruleIndexer.commitAndIndex(dbSession, rule1.getId());
    activeRuleIndexer.indexOnStartup(emptySet());

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();

    // Set parent
    qProfileTree.setParentAndCommit(dbSession, child, parent);

    // Remove parent
    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, child.getKee())
      .setParam(PARAM_PARENT_KEY, "")
      .execute();

    // Check no rule enabled
    assertThat(dbClient.activeRuleDao().selectByProfile(dbSession, child)).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void as_qprofile_editor() {
    QProfileDto parent1 = createProfile();
    QProfileDto parent2 = createProfile();
    QProfileDto child = createProfile();

    RuleDefinitionDto rule1 = createRule();
    RuleDefinitionDto rule2 = createRule();
    createActiveRule(rule1, parent1);
    createActiveRule(rule2, parent2);
    ruleIndexer.commitAndIndex(dbSession, asList(rule1.getId(), rule2.getId()));
    activeRuleIndexer.indexOnStartup(emptySet());
    // Set parent 1
    qProfileTree.setParentAndCommit(dbSession, child, parent1);
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(child, user);
    userSession.logIn(user);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, child.getKee())
      .setParam(PARAM_PARENT_KEY, parent2.getKee())
      .execute();

    List<OrgActiveRuleDto> activeRules2 = dbClient.activeRuleDao().selectByProfile(dbSession, child);
    assertThat(activeRules2).hasSize(1);
    assertThat(activeRules2.get(0).getKey().getRuleKey().rule()).isEqualTo(rule2.getRuleKey());

    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).hasSize(1);
  }

  @Test
  public void fail_if_built_in_profile() {
    QProfileDto child = db.qualityProfiles().insert(organization, p -> p
      .setLanguage(language.getKey())
      .setIsBuiltIn(true));

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, child.getKee())
      .setParam(PARAM_PARENT_KEY, "palap");

    expectedException.expect(BadRequestException.class);

    request.execute();
  }

  @Test
  public void fail_if_parent_key_and_name_both_set() {
    QProfileDto child = createProfile();

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, child.getKee())
      .setParam(PARAM_PARENT_QUALITY_PROFILE, "polop")
      .setParam(PARAM_PARENT_KEY, "palap");
    expectedException.expect(IllegalArgumentException.class);
    request
      .execute();
  }

  @Test
  public void fail_if_profile_key_and_name_both_set() {
    QProfileDto child = createProfile();

    assertThat(dbClient.activeRuleDao().selectByProfileUuid(dbSession, child.getKee())).isEmpty();
    assertThat(ruleIndex.search(new RuleQuery().setActivation(true).setQProfile(child), new SearchOptions()).getIds()).isEmpty();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, child.getKee())
      .setParam(PARAM_QUALITY_PROFILE, child.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_PARENT_KEY, "palap");

    expectedException.expect(IllegalArgumentException.class);
    request.execute();
  }

  @Test
  public void fail_if_missing_permission() {
    userSession.logIn(db.users().insertUser());

    QProfileDto child = createProfile();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, child.getKee());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");
    request.execute();
  }

  @Test
  public void fail_if_missing_permission_for_this_organization() {
    OrganizationDto organization2 = db.organizations().insert();
    userSession.logIn(db.users().insertUser()).addPermission(ADMINISTER_QUALITY_PROFILES, organization2.getUuid());

    QProfileDto child = createProfile();

    TestRequest request = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_KEY, child.getKee());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");
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
