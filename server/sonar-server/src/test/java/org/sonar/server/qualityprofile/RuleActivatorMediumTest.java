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
package org.sonar.server.qualityprofile;

// TODO Replace ServerTester by EsTester and DbTester
public class RuleActivatorMediumTest {

//  static final RuleKey TEMPLATE_RULE_KEY = RuleKey.of("xoo", "template1");
//  static final RuleKey CUSTOM_RULE_KEY = RuleKey.of("xoo", "custom1");
//
//  @Before
//  public void before() {
//    tester.clearDbAndIndexes();
//    db = tester.get(DbClient.class);
//    dbSession = db.openSession(false);
//    ruleActivator = tester.get(RuleActivator.class);
//    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
//    ruleIndexer = tester.get(RuleIndexer.class);
//    String defaultOrganizationUuid = tester.get(DefaultOrganizationProvider.class).get().getUuid();
//    organization = db.organizationDao().selectByUuid(dbSession, defaultOrganizationUuid).orElseThrow(() -> new IllegalStateException(String.format("Cannot find default organization '%s'", defaultOrganizationUuid)));
//
//    // create pre-defined rules
//    RuleDto javaRule = newDto(RuleKey.of("squid", "j1"))
//      .setSeverity("MAJOR").setLanguage("java");
//    RuleDto xooRule1 = newXooX1().setSeverity("MINOR");
//    RuleDto xooRule2 = newXooX2().setSeverity("INFO");
//    RuleDto xooTemplateRule1 = newTemplateRule(TEMPLATE_RULE_KEY)
//      .setSeverity("MINOR").setLanguage("xoo");
//
//    // store pre-defined rules in database
//    asList(javaRule, xooRule1, xooRule2, xooTemplateRule1).stream()
//      .map(RuleDto::getDefinition)
//      .forEach(definition -> db.ruleDao().insert(dbSession, definition));
//    db.ruleDao().insertRuleParam(dbSession, xooRule1.getDefinition(), RuleParamDto.createFor(xooRule1.getDefinition())
//      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));
//    db.ruleDao().insertRuleParam(dbSession, xooRule1.getDefinition(), RuleParamDto.createFor(xooRule1.getDefinition())
//      .setName("min").setType(RuleParamType.INTEGER.type()));
//    db.ruleDao().insertRuleParam(dbSession, xooTemplateRule1.getDefinition(), RuleParamDto.createFor(xooTemplateRule1.getDefinition())
//      .setName("format").setType(RuleParamType.STRING.type()));
//
//    // create custom rule
//    RuleDto xooCustomRule1 = newCustomRule(xooTemplateRule1).setRuleKey(CUSTOM_RULE_KEY.rule())
//      .setSeverity("MINOR").setLanguage("xoo");
//
//    // store custom rule in database
//    db.ruleDao().insert(dbSession, xooCustomRule1.getDefinition());
//    db.ruleDao().insertRuleParam(dbSession, xooCustomRule1.getDefinition(), RuleParamDto.createFor(xooTemplateRule1.getDefinition())
//      .setName("format").setDefaultValue("txt").setType(RuleParamType.STRING.type()));
//
//    // create pre-defined profile P1
//    profileDto = QProfileTesting.newXooP1(organization);
//    db.qualityProfileDao().insert(dbSession, profileDto);
//
//    // index all rules
//    dbSession.commit();
//    ruleIndexer.indexRuleDefinitions(Stream.of(javaRule, xooRule1, xooRule2, xooTemplateRule1, xooCustomRule1).map(RuleDto::getKey).collect(Collectors.toList()));
//  }
//
//  @After
//  public void after() {
//    dbSession.close();
//  }
//
//
//
//
//
//
//
//
//
//  @Test
//  public void ignore_activation_errors_when_setting_parent() {
//    // x1 and x2 are activated on the "future parent" P1
//    RuleActivation activation = new RuleActivation(XOO_X1).setSeverity("MAJOR");
//    activate(activation, XOO_P1_KEY);
//    activation = new RuleActivation(XOO_X2).setSeverity("MAJOR");
//    activate(activation, XOO_P1_KEY);
//
//    // create profile P2
//    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP2("org-123"));
//
//    // mark rule x1 as REMOVED
//    RuleDefinitionDto rule = db.ruleDao().selectOrFailDefinitionByKey(dbSession, XOO_X1);
//    rule.setStatus(RuleStatus.REMOVED);
//    db.ruleDao().update(dbSession, rule);
//    dbSession.commit();
//    dbSession.clearCache();
//
//    // set parent -> child profile inherits x2 but not x1
//    ruleActivator.setParent(dbSession, selectProfile(XOO_P2_KEY), selectProfile(XOO_P1_KEY));
//    dbSession.clearCache();
//
//    assertThat(db.qualityProfileDao().selectByUuid(dbSession, XOO_P2_KEY).getParentKee()).isEqualTo(XOO_P1_KEY);
//    assertThat(countActiveRules(XOO_P2_KEY)).isEqualTo(1);
//    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P2_KEY, XOO_X2), MAJOR, INHERITED, Collections.emptyMap());
//  }
//
//
//  private int countActiveRules(String profileKey) {
//    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().selectByProfileUuid(dbSession, profileKey);
//    return activeRuleDtos.size();
//  }
//
//  private void verifyOneActiveRuleInDb(String profileKey, RuleKey ruleKey, String expectedSeverity,
//    @Nullable String expectedInheritance, Map<String, String> expectedParams) {
//    assertThat(countActiveRules(profileKey)).isEqualTo(1);
//    verifyHasActiveRuleInDb(ActiveRuleKey.of(profileKey, ruleKey), expectedSeverity, expectedInheritance, expectedParams);
//  }
//
//  private void verifyOneActiveRuleInDbAndIndex(String profileKey, RuleKey ruleKey, String expectedSeverity,
//    @Nullable String expectedInheritance, Map<String, String> expectedParams) {
//    assertThat(countActiveRules(profileKey)).isEqualTo(1);
//    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(profileKey, ruleKey), expectedSeverity, expectedInheritance, expectedParams);
//  }
//
//  private void verifyHasActiveRuleInDb(ActiveRuleKey activeRuleKey, String expectedSeverity,
//    @Nullable String expectedInheritance, Map<String, String> expectedParams) {
//    // verify db
//    boolean found = false;
//    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().selectByProfileUuid(dbSession, activeRuleKey.getRuleProfileUuid());
//    for (ActiveRuleDto activeRuleDto : activeRuleDtos) {
//      if (activeRuleDto.getKey().equals(activeRuleKey)) {
//        found = true;
//        assertThat(activeRuleDto.getSeverityString()).isEqualTo(expectedSeverity);
//        assertThat(activeRuleDto.getInheritance()).isEqualTo(expectedInheritance);
//        // Dates should be set
//        assertThat(activeRuleDto.getCreatedAt()).isNotNull();
//        assertThat(activeRuleDto.getUpdatedAt()).isNotNull();
//
//        List<ActiveRuleParamDto> paramDtos = db.activeRuleDao().selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
//        assertThat(paramDtos).hasSize(expectedParams.size());
//        for (Map.Entry<String, String> entry : expectedParams.entrySet()) {
//          ActiveRuleParamDto paramDto = db.activeRuleDao().selectParamByKeyAndName(activeRuleDto.getKey(), entry.getKey(), dbSession);
//          assertThat(paramDto).isNotNull();
//          assertThat(paramDto.getValue()).isEqualTo(entry.getValue());
//        }
//      }
//    }
//    assertThat(found).as("Rule is not activated in db").isTrue();
//  }
//
//  private void verifyHasActiveRuleInIndex(ActiveRuleKey activeRuleKey, String expectedSeverity, @Nullable String expectedInheritance) {
//    // verify es
//    List<RuleKey> ruleKeys = newArrayList(tester.get(RuleIndex.class).searchAll(
//      new RuleQuery()
//        .setKey(activeRuleKey.getRuleKey().toString())
//        .setQProfileKey(activeRuleKey.getRuleProfileUuid())
//        .setActivation(true)
//        .setInheritance(singleton(expectedInheritance == null ? ActiveRule.Inheritance.NONE.name() : ActiveRule.Inheritance.valueOf(expectedInheritance).name()))
//        .setActiveSeverities(singleton(expectedSeverity))));
//    assertThat(ruleKeys).as("Rule is not activated in index").hasSize(1);
//  }
//
//  private void verifyHasActiveRuleInDbAndIndex(ActiveRuleKey activeRuleKey, String expectedSeverity,
//    @Nullable String expectedInheritance, Map<String, String> expectedParams) {
//    verifyHasActiveRuleInDb(activeRuleKey, expectedSeverity, expectedInheritance, expectedParams);
//    verifyHasActiveRuleInIndex(activeRuleKey, expectedSeverity, expectedInheritance);
//  }
//
//  private void createChildProfiles() {
//    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP2("org-123").setParentKee(XOO_P1_KEY));
//    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP3("org-123").setParentKee(XOO_P2_KEY));
//    dbSession.commit();
//  }
//
//  private List<ActiveRuleChange> activate(RuleActivation activation, String profileKey) {
//    List<ActiveRuleChange> changes = ruleActivator.activate(dbSession, activation, profileKey);
//    dbSession.commit();
//    dbSession.clearCache();
//    activeRuleIndexer.index(changes);
//    return changes;
//  }
//
//  private void verifyZeroActiveRules(String key) {
//    // verify db
//    dbSession.clearCache();
//    List<ActiveRuleDto> activeRuleDtos = db.activeRuleDao().selectByProfileUuid(dbSession, key);
//    assertThat(activeRuleDtos).isEmpty();
//
//    // verify es
//    assertThat(tester.get(RuleIndex.class).searchAll(
//      new RuleQuery()
//        .setQProfileKey(key)
//        .setActivation(true))).isEmpty();
//  }
//
//  private void assertProfileHasBeenUpdatedManually(String profileKey) {
//    QProfileDto profile = db.qualityProfileDao().selectByUuid(dbSession, profileKey);
//    assertThat(profile.getRulesUpdatedAt()).isNotEmpty();
//    assertThat(profile.getUserUpdatedAt()).isNotNull();
//  }
//
//  private void assertProfileHasBeenUpdatedAutomatically(String profileKey) {
//    QProfileDto profile = db.qualityProfileDao().selectByUuid(dbSession, profileKey);
//    assertThat(profile.getRulesUpdatedAt()).isNotEmpty();
//    assertThat(profile.getUserUpdatedAt()).isNull();
//  }
}
