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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
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
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.qualityprofile.QProfileRulesImpl;
import org.sonar.server.qualityprofile.QProfileTree;
import org.sonar.server.qualityprofile.QProfileTreeImpl;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualityprofiles.InheritanceWsResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.qualityprofile.QProfileTesting.newQProfileDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;

public class InheritanceActionTest {

  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public EsTester es = new EsTester(new RuleIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient;
  private DbSession dbSession;
  private EsClient esClient;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;
  private QProfileRules qProfileRules;
  private QProfileTree qProfileTree;
  private OrganizationDto organization;

  private InheritanceAction underTest;
  private WsActionTester ws;

  @Before
  public void setUp() {
    dbClient = dbTester.getDbClient();
    dbSession = dbTester.getSession();
    esClient = es.client();
    ruleIndexer = new RuleIndexer(esClient, dbClient);
    activeRuleIndexer = new ActiveRuleIndexer(dbClient, esClient);
    TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
    underTest = new InheritanceAction(
      dbClient,
      new QProfileWsSupport(dbClient, userSession, defaultOrganizationProvider),
      new Languages());

    ws = new WsActionTester(underTest);
    RuleIndex ruleIndex = new RuleIndex(esClient, System2.INSTANCE);
    RuleActivator ruleActivator = new RuleActivator(System2.INSTANCE, dbClient, new TypeValidations(new ArrayList<>()), userSession);
    qProfileRules = new QProfileRulesImpl(dbClient, ruleActivator, ruleIndex, activeRuleIndexer);
    qProfileTree = new QProfileTreeImpl(dbClient, ruleActivator, System2.INSTANCE, activeRuleIndexer);
    organization = dbTester.organizations().insert();
  }

  @Test
  public void inheritance_nominal() {
    RuleDefinitionDto rule1 = createRule("xoo", "rule1");
    RuleDefinitionDto rule2 = createRule("xoo", "rule2");
    RuleDefinitionDto rule3 = createRule("xoo", "rule3");

    /*
     * sonar way (2) <- companyWide (2) <- buWide (2, 1 overriding) <- (forProject1 (2), forProject2 (2))
     */
    QProfileDto sonarway = dbTester.qualityProfiles().insert(organization, p -> p.setKee("xoo-sonar-way").setLanguage("xoo").setName("Sonar way").setIsBuiltIn(true));
    ActiveRuleDto activeRule1 = createActiveRule(rule1, sonarway);
    ActiveRuleDto activeRule2 = createActiveRule(rule2, sonarway);

    dbSession.commit();
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    QProfileDto companyWide = createProfile("xoo", "My Company Profile", "xoo-my-company-profile-12345");
    setParent(sonarway, companyWide);

    QProfileDto buWide = createProfile("xoo", "My BU Profile", "xoo-my-bu-profile-23456");
    setParent(companyWide, buWide);
    overrideActiveRuleSeverity(rule1, buWide, Severity.CRITICAL);

    QProfileDto forProject1 = createProfile("xoo", "For Project One", "xoo-for-project-one-34567");
    setParent(buWide, forProject1);
    createActiveRule(rule3, forProject1);
    dbSession.commit();
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    QProfileDto forProject2 = createProfile("xoo", "For Project Two", "xoo-for-project-two-45678");
    setParent(buWide, forProject2);
    overrideActiveRuleSeverity(rule2, forProject2, Severity.CRITICAL);

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, buWide.getKee())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("InheritanceActionTest/inheritance-buWide.json"));
  }

  @Test
  public void inheritance_parent_child() throws Exception {
    RuleDefinitionDto rule1 = dbTester.rules().insert();
    RuleDefinitionDto rule2 = dbTester.rules().insert();
    RuleDefinitionDto rule3 = dbTester.rules().insert();
    ruleIndexer.commitAndIndex(dbTester.getSession(), asList(rule1.getId(), rule2.getId(), rule3.getId()));

    QProfileDto parent = dbTester.qualityProfiles().insert(organization);
    dbTester.qualityProfiles().activateRule(parent, rule1);
    dbTester.qualityProfiles().activateRule(parent, rule2);
    long parentRules = 2;

    QProfileDto child = dbTester.qualityProfiles().insert(organization, q -> q.setParentKee(parent.getKee()));
    dbTester.qualityProfiles().activateRule(child, rule3);
    long childRules = 1;

    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    InputStream response = ws.newRequest()
      .setMethod("GET")
      .setMediaType(PROTOBUF)
      .setParam(PARAM_KEY, child.getKee())
      .execute()
      .getInputStream();

    InheritanceWsResponse result = InheritanceWsResponse.parseFrom(response);

    assertThat(result.getProfile().getKey()).isEqualTo(child.getKee());
    assertThat(result.getProfile().getActiveRuleCount()).isEqualTo(childRules);

    assertThat(result.getAncestorsList()).extracting(InheritanceWsResponse.QualityProfile::getKey).containsExactly(parent.getKee());
    assertThat(result.getAncestorsList()).extracting(InheritanceWsResponse.QualityProfile::getActiveRuleCount).containsExactly(parentRules);
  }

  @Test
  public void inheritance_ignores_removed_rules() throws Exception {
    RuleDefinitionDto rule = dbTester.rules().insert(r -> r.setStatus(RuleStatus.REMOVED));
    ruleIndexer.commitAndIndex(dbTester.getSession(), rule.getId());

    QProfileDto profile = dbTester.qualityProfiles().insert(organization);
    dbTester.qualityProfiles().activateRule(profile, rule);
    long activeRules = 0;

    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());

    InputStream response = ws.newRequest()
      .setMethod("GET")
      .setMediaType(PROTOBUF)
      .setParam(PARAM_KEY, profile.getKee())
      .execute()
      .getInputStream();

    InheritanceWsResponse result = InheritanceWsResponse.parseFrom(response);
    assertThat(result.getProfile().getKey()).isEqualTo(profile.getKee());
    assertThat(result.getProfile().getActiveRuleCount()).isEqualTo(activeRules);
  }

  @Test
  public void inheritance_no_family() {
    // Simple profile, no parent, no child
    QProfileDto remi = createProfile("xoo", "Nobodys Boy", "xoo-nobody-s-boy-01234");

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_KEY, remi.getKee())
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("InheritanceActionTest/inheritance-simple.json"));
  }

  @Test(expected = NotFoundException.class)
  public void fail_if_not_found() {
    ws.newRequest()
      .setMethod("GET").setParam(PARAM_KEY, "polop").execute();
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("inheritance");
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("key", "language", "qualityProfile", "organization");
    Param key = definition.param("key");
    assertThat(key.deprecatedKey()).isEqualTo("profileKey");
    assertThat(key.deprecatedSince()).isEqualTo("6.6");
    Param profileName = definition.param("qualityProfile");
    assertThat(profileName.deprecatedSince()).isNullOrEmpty();
    Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isNullOrEmpty();
  }

  private QProfileDto createProfile(String lang, String name, String key) {
    QProfileDto profile = newQProfileDto(organization, new QProfileName(lang, name), key);
    dbClient.qualityProfileDao().insert(dbSession, profile);
    dbSession.commit();
    return profile;
  }

  private void setParent(QProfileDto profile, QProfileDto parent) {
    qProfileTree.setParentAndCommit(dbSession, parent, profile);
  }

  private RuleDefinitionDto createRule(String lang, String id) {
    long now = new Date().getTime();
    RuleDefinitionDto rule = RuleTesting.newRule(RuleKey.of("blah", id))
      .setLanguage(lang)
      .setSeverity(Severity.BLOCKER)
      .setStatus(RuleStatus.READY)
      .setUpdatedAt(now)
      .setCreatedAt(now);
    dbClient.ruleDao().insert(dbSession, rule);
    ruleIndexer.commitAndIndex(dbSession, rule.getId());
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDefinitionDto rule, QProfileDto profile) {
    long now = new Date().getTime();
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString())
      .setUpdatedAt(now)
      .setCreatedAt(now);
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    return activeRule;
  }

  private void overrideActiveRuleSeverity(RuleDefinitionDto rule, QProfileDto profile, String severity) {
    qProfileRules.activateAndCommit(dbSession, profile, singleton(RuleActivation.create(rule.getId(), severity, null)));
//    dbSession.commit();
//    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());
  }
}
