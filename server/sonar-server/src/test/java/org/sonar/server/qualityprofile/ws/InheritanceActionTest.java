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

import java.util.ArrayList;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.RuleActivatorContextFactory;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;

public class InheritanceActionTest {

  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings()));
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient;
  private DbSession dbSession;
  private EsClient esClient;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;
  private InheritanceAction underTest;
  private WsActionTester wsActionTester;
  private RuleActivator ruleActivator;
  private OrganizationDto organization;

  @Before
  public void setUp() {
    dbClient = dbTester.getDbClient();
    dbSession = dbTester.getSession();
    esClient = esTester.client();
    ruleIndexer = new RuleIndexer(esClient, dbClient);
    activeRuleIndexer = new ActiveRuleIndexer(System2.INSTANCE, dbClient, esClient);
    TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
    underTest = new InheritanceAction(
      dbClient,
      new QProfileLookup(dbClient),
      new QProfileWsSupport(dbClient, userSession, defaultOrganizationProvider),
      new Languages());
    wsActionTester = new WsActionTester(underTest);
    ruleActivator = new RuleActivator(
      System2.INSTANCE,
      dbClient,
      new RuleIndex(esClient),
      new RuleActivatorContextFactory(dbClient),
      new TypeValidations(new ArrayList<>()),
      activeRuleIndexer,
      userSession);
    organization = dbTester.organizations().insert();
  }

  @Test
  public void inheritance_nominal() throws Exception {
    RuleDefinitionDto rule1 = createRule("xoo", "rule1");
    RuleDefinitionDto rule2 = createRule("xoo", "rule2");
    RuleDefinitionDto rule3 = createRule("xoo", "rule3");

    /*
     * groupWide (2) <- companyWide (2) <- buWide (2, 1 overriding) <- (forProject1 (2), forProject2 (2))
     */
    QualityProfileDto groupWide = createProfile("xoo", "My Group Profile", "xoo-my-group-profile-01234");
    createActiveRule(rule1, groupWide);
    createActiveRule(rule2, groupWide);

    dbSession.commit();
    activeRuleIndexer.index();

    QualityProfileDto companyWide = createProfile("xoo", "My Company Profile", "xoo-my-company-profile-12345");
    setParent(groupWide, companyWide);

    QualityProfileDto buWide = createProfile("xoo", "My BU Profile", "xoo-my-bu-profile-23456");
    setParent(companyWide, buWide);
    overrideActiveRuleSeverity(rule1, buWide, Severity.CRITICAL);

    QualityProfileDto forProject1 = createProfile("xoo", "For Project One", "xoo-for-project-one-34567");
    setParent(buWide, forProject1);
    createActiveRule(rule3, forProject1);
    dbSession.commit();
    activeRuleIndexer.index();

    QualityProfileDto forProject2 = createProfile("xoo", "For Project Two", "xoo-for-project-two-45678");
    setParent(buWide, forProject2);
    overrideActiveRuleSeverity(rule2, forProject2, Severity.CRITICAL);

    String response = wsActionTester.newRequest()
      .setMethod("GET")
      .setParam("profileKey", buWide.getKee())
      .execute()
      .getInput();

    JsonAssert.assertJson(response).isSimilarTo(getClass().getResource("InheritanceActionTest/inheritance-buWide.json"));
  }

  @Test
  public void inheritance_no_family() throws Exception {
    // Simple profile, no parent, no child
    QualityProfileDto remi = createProfile("xoo", "Nobodys Boy", "xoo-nobody-s-boy-01234");

    String response = wsActionTester.newRequest()
      .setMethod("GET")
      .setParam("profileKey", remi.getKee())
      .execute()
      .getInput();

    JsonAssert.assertJson(response).isSimilarTo(getClass().getResource("InheritanceActionTest/inheritance-simple.json"));
  }

  @Test(expected = NotFoundException.class)
  public void fail_if_not_found() throws Exception {
    wsActionTester.newRequest()
      .setMethod("GET").setParam("profileKey", "polop").execute();
  }

  private QualityProfileDto createProfile(String lang, String name, String key) {
    QualityProfileDto profile = QProfileTesting.newQProfileDto(organization, new QProfileName(lang, name), key);
    dbClient.qualityProfileDao().insert(dbSession, profile);
    dbSession.commit();
    return profile;
  }

  private void setParent(QualityProfileDto profile, QualityProfileDto parent) {
    ruleActivator.setParent(dbSession, parent.getKey(), profile.getKey());
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
    dbSession.commit();
    ruleIndexer.indexRuleDefinition(rule.getKey());
    return rule;
  }

  private ActiveRuleDto createActiveRule(RuleDefinitionDto rule, QualityProfileDto profile) {
    long now = new Date().getTime();
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
      .setSeverity(rule.getSeverityString())
      .setUpdatedAt(now)
      .setCreatedAt(now);
    dbClient.activeRuleDao().insert(dbSession, activeRule);
    return activeRule;
  }

  private void overrideActiveRuleSeverity(RuleDefinitionDto rule, QualityProfileDto profile, String severity) {
    ruleActivator.activate(dbSession, new RuleActivation(rule.getKey()).setSeverity(severity), profile.getKey());
    dbSession.commit();
    activeRuleIndexer.index();
  }
}
