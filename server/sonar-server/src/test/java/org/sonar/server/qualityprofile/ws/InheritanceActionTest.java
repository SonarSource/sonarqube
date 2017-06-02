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

public class InheritanceActionTest {

//  @Rule
//  public DbTester dbTester = DbTester.create();
//  @Rule
//  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings()));
//  @Rule
//  public UserSessionRule userSession = UserSessionRule.standalone();
//
//  private DbClient dbClient;
//  private DbSession dbSession;
//  private EsClient esClient;
//  private RuleIndexer ruleIndexer;
//  private ActiveRuleIndexer activeRuleIndexer;
//  private InheritanceAction underTest;
//  private WsActionTester wsActionTester;
//  private RuleActivator ruleActivator;
//  private OrganizationDto organization;
//
//  @Before
//  public void setUp() {
//    dbClient = dbTester.getDbClient();
//    dbSession = dbTester.getSession();
//    esClient = esTester.client();
//    ruleIndexer = new RuleIndexer(esClient, dbClient);
//    activeRuleIndexer = new ActiveRuleIndexer(dbClient, esClient, , new ActiveRuleIteratorFactory(dbClient));
//    TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
//    underTest = new InheritanceAction(
//      dbClient,
//      new QProfileLookup(dbClient),
//      new QProfileWsSupport(dbClient, userSession, defaultOrganizationProvider),
//      new Languages());
//    wsActionTester = new WsActionTester(underTest);
//    ruleActivator = new RuleActivator(
//      System2.INSTANCE,
//      dbClient,
//      new RuleIndex(esClient),
//      new RuleActivatorContextFactory(dbClient),
//      new TypeValidations(new ArrayList<>()),
//      activeRuleIndexer,
//      userSession);
//    organization = dbTester.organizations().insert();
//  }
//
//  @Test
//  public void inheritance_nominal() throws Exception {
//    RuleDefinitionDto rule1 = createRule("xoo", "rule1");
//    RuleDefinitionDto rule2 = createRule("xoo", "rule2");
//    RuleDefinitionDto rule3 = createRule("xoo", "rule3");
//
//    /*
//     * sonar way (2) <- companyWide (2) <- buWide (2, 1 overriding) <- (forProject1 (2), forProject2 (2))
//     */
//    QProfileDto sonarway = dbTester.qualityProfiles().insert(organization, p ->
//      p.setKee("xoo-sonar-way").setLanguage("xoo").setName("Sonar way").setIsBuiltIn(true));
//    createActiveRule(rule1, sonarway);
//    createActiveRule(rule2, sonarway);
//
//    dbSession.commit();
//    activeRuleIndexer.index();
//
//    QProfileDto companyWide = createProfile("xoo", "My Company Profile", "xoo-my-company-profile-12345");
//    setParent(sonarway, companyWide);
//
//    QProfileDto buWide = createProfile("xoo", "My BU Profile", "xoo-my-bu-profile-23456");
//    setParent(companyWide, buWide);
//    overrideActiveRuleSeverity(rule1, buWide, Severity.CRITICAL);
//
//    QProfileDto forProject1 = createProfile("xoo", "For Project One", "xoo-for-project-one-34567");
//    setParent(buWide, forProject1);
//    createActiveRule(rule3, forProject1);
//    dbSession.commit();
//    activeRuleIndexer.index();
//
//    QProfileDto forProject2 = createProfile("xoo", "For Project Two", "xoo-for-project-two-45678");
//    setParent(buWide, forProject2);
//    overrideActiveRuleSeverity(rule2, forProject2, Severity.CRITICAL);
//
//    String response = wsActionTester.newRequest()
//      .setMethod("GET")
//      .setParam("profileKey", buWide.getKee())
//      .execute()
//      .getInput();
//
//    JsonAssert.assertJson(response).isSimilarTo(getClass().getResource("InheritanceActionTest/inheritance-buWide.json"));
//  }
//
//  @Test
//  public void inheritance_parent_child() throws Exception {
//    RuleDefinitionDto rule1 = dbTester.rules().insert();
//    ruleIndexer.indexRuleDefinition(rule1.getKey());
//
//    RuleDefinitionDto rule2 = dbTester.rules().insert();
//    ruleIndexer.indexRuleDefinition(rule1.getKey());
//
//    RuleDefinitionDto rule3 = dbTester.rules().insert();
//    ruleIndexer.indexRuleDefinition(rule1.getKey());
//
//    QProfileDto parent = dbTester.qualityProfiles().insert(organization);
//    dbTester.qualityProfiles().activateRule(parent, rule1);
//    dbTester.qualityProfiles().activateRule(parent, rule2);
//    long parentRules = 2;
//
//    QProfileDto child = dbTester.qualityProfiles().insert(organization, q -> q.setParentKee(parent.getKee()));
//    dbTester.qualityProfiles().activateRule(child, rule3);
//    long childRules = 1;
//
//    activeRuleIndexer.index();
//
//    InputStream response = wsActionTester.newRequest()
//      .setMethod("GET")
//      .setMediaType(PROTOBUF)
//      .setParam("profileKey", child.getKee())
//      .execute()
//      .getInputStream();
//
//    InheritanceWsResponse result = InheritanceWsResponse.parseFrom(response);
//
//    assertThat(result.getProfile().getKey()).isEqualTo(child.getKee());
//    assertThat(result.getProfile().getActiveRuleCount()).isEqualTo(childRules);
//
//    assertThat(result.getAncestorsList()).extracting(InheritanceWsResponse.QualityProfile::getKey).containsExactly(parent.getKee());
//    assertThat(result.getAncestorsList()).extracting(InheritanceWsResponse.QualityProfile::getActiveRuleCount).containsExactly(parentRules);
//  }
//
//  @Test
//  public void inheritance_ignores_removed_rules() throws Exception {
//    RuleDefinitionDto rule = dbTester.rules().insert(r -> r.setStatus(RuleStatus.REMOVED));
//    ruleIndexer.indexRuleDefinition(rule.getKey());
//
//    QProfileDto profile = dbTester.qualityProfiles().insert(organization);
//    dbTester.qualityProfiles().activateRule(profile, rule);
//    long activeRules = 0;
//
//    activeRuleIndexer.index();
//
//    InputStream response = wsActionTester.newRequest()
//      .setMethod("GET")
//      .setMediaType(PROTOBUF)
//      .setParam("profileKey", profile.getKee())
//      .execute()
//      .getInputStream();
//
//    InheritanceWsResponse result = InheritanceWsResponse.parseFrom(response);
//    assertThat(result.getProfile().getKey()).isEqualTo(profile.getKee());
//    assertThat(result.getProfile().getActiveRuleCount()).isEqualTo(activeRules);
//  }
//
//  @Test
//  public void inheritance_no_family() throws Exception {
//    // Simple profile, no parent, no child
//    QProfileDto remi = createProfile("xoo", "Nobodys Boy", "xoo-nobody-s-boy-01234");
//
//    String response = wsActionTester.newRequest()
//      .setMethod("GET")
//      .setParam("profileKey", remi.getKee())
//      .execute()
//      .getInput();
//
//    JsonAssert.assertJson(response).isSimilarTo(getClass().getResource("InheritanceActionTest/inheritance-simple.json"));
//  }
//
//  @Test(expected = NotFoundException.class)
//  public void fail_if_not_found() throws Exception {
//    wsActionTester.newRequest()
//      .setMethod("GET").setParam("profileKey", "polop").execute();
//  }
//
//  private QProfileDto createProfile(String lang, String name, String key) {
//    QProfileDto profile = newQProfileDto(organization, new QProfileName(lang, name), key);
//    dbClient.qualityProfileDao().insert(dbSession, profile);
//    dbSession.commit();
//    return profile;
//  }
//
//  private void setParent(QProfileDto profile, QProfileDto parent) {
//    ruleActivator.setParent(dbSession, parent, profile);
//  }
//
//  private RuleDefinitionDto createRule(String lang, String id) {
//    long now = new Date().getTime();
//    RuleDefinitionDto rule = RuleTesting.newRule(RuleKey.of("blah", id))
//      .setLanguage(lang)
//      .setSeverity(Severity.BLOCKER)
//      .setStatus(RuleStatus.READY)
//      .setUpdatedAt(now)
//      .setCreatedAt(now);
//    dbClient.ruleDao().insert(dbSession, rule);
//    dbSession.commit();
//    ruleIndexer.indexRuleDefinition(rule.getKey());
//    return rule;
//  }
//
//  private ActiveRuleDto createActiveRule(RuleDefinitionDto rule, QProfileDto profile) {
//    long now = new Date().getTime();
//    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule)
//      .setSeverity(rule.getSeverityString())
//      .setUpdatedAt(now)
//      .setCreatedAt(now);
//    dbClient.activeRuleDao().insert(dbSession, activeRule);
//    return activeRule;
//  }
//
//  private void overrideActiveRuleSeverity(RuleDefinitionDto rule, QProfileDto profile, String severity) {
//    ruleActivator.activate(dbSession, new RuleActivation(rule.getKey()).setSeverity(severity), profile.getKee());
//    dbSession.commit();
//    activeRuleIndexer.index();
//  }
}
