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
//  public void ignore_parameters_when_activating_custom_rule() {
//    // initial activation
//    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(XOO_P1_KEY, CUSTOM_RULE_KEY);
//    RuleActivation activation = new RuleActivation(CUSTOM_RULE_KEY);
//    activate(activation, XOO_P1_KEY);
//
//    // update
//    RuleActivation update = new RuleActivation(CUSTOM_RULE_KEY)
//      .setParameter("format", "xls");
//    activate(update, XOO_P1_KEY);
//
//    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(1);
//    verifyHasActiveRuleInDb(activeRuleKey, MINOR, null, ImmutableMap.of("format", "txt"));
//  }
//
//
//  @Test
//  public void deactivation_fails_if_profile_not_found() {
//    ActiveRuleKey key = ActiveRuleKey.of("unknown", XOO_X1);
//    try {
//      ruleActivator.deactivateAndUpdateIndex(dbSession, key);
//      fail();
//    } catch (BadRequestException e) {
//      assertThat(e).hasMessage("Quality profile not found: unknown");
//    }
//  }
//
//
//  @Test
//  public void bulk_activation() {
//    // Generate more rules than the search's max limit
//    int bulkSize = SearchOptions.MAX_LIMIT + 10;
//    List<RuleKey> keys = new ArrayList<>();
//    for (int i = 0; i < bulkSize; i++) {
//      RuleDefinitionDto ruleDefinitionDto = newDto(RuleKey.of("bulk", "r_" + i)).setLanguage("xoo").getDefinition();
//      db.ruleDao().insert(dbSession, ruleDefinitionDto);
//      keys.add(ruleDefinitionDto.getKey());
//    }
//    dbSession.commit();
//    ruleIndexer.indexRuleDefinitions(keys);
//
//    // 0. No active rules so far (base case) and plenty rules available
//    verifyZeroActiveRules(XOO_P1_KEY);
//    assertThat(tester.get(RuleIndex.class)
//      .search(new RuleQuery().setRepositories(singletonList("bulk")), new SearchOptions()).getTotal())
//        .isEqualTo(bulkSize);
//
//    // 1. bulk activate all the rules
//    RuleQuery ruleQuery = new RuleQuery().setRepositories(singletonList("bulk"));
//    BulkChangeResult result = ruleActivator.bulkActivate(dbSession, ruleQuery, selectProfile(XOO_P1_KEY), "MINOR");
//
//    // 2. assert that all activation has been commit to DB and ES
//    dbSession.commit();
//    assertThat(db.activeRuleDao().selectByProfileUuid(dbSession, XOO_P1_KEY)).hasSize(bulkSize);
//    assertThat(result.countSucceeded()).isEqualTo(bulkSize);
//    assertThat(result.countFailed()).isEqualTo(0);
//  }
//
//  private QProfileDto selectProfile(String uuid) {
//    return db.qualityProfileDao().selectByUuid(dbSession, uuid);
//  }
//
//  @Test
//  public void bulk_activation_ignores_errors() {
//    // 1. bulk activate all the rules, even non xoo-rules and xoo templates
//    BulkChangeResult result = ruleActivator.bulkActivate(dbSession, new RuleQuery(), selectProfile(XOO_P1_KEY), "MINOR");
//
//    // 2. assert that all activations have been commit to DB and ES
//    // -> xoo rules x1, x2 and custom1
//    dbSession.commit();
//    assertThat(db.activeRuleDao().selectByProfileUuid(dbSession, XOO_P1_KEY)).hasSize(3);
//    assertThat(result.countSucceeded()).isEqualTo(3);
//    assertThat(result.countFailed()).isGreaterThan(0);
//  }
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
//  @Test
//  public void bulk_deactivate() {
//    activate(new RuleActivation(XOO_X1), XOO_P1_KEY);
//    activate(new RuleActivation(XOO_X2), XOO_P1_KEY);
//    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(2);
//
//    BulkChangeResult result = ruleActivator.bulkDeactivate(new RuleQuery().setActivation(true).setQProfileKey(XOO_P1_KEY), XOO_P1_KEY);
//
//    dbSession.clearCache();
//    assertThat(countActiveRules(XOO_P1_KEY)).isEqualTo(0);
//    assertThat(result.countFailed()).isEqualTo(0);
//    assertThat(result.countSucceeded()).isEqualTo(2);
//    assertThat(result.getChanges()).hasSize(2);
//  }
//
//  @Test
//  public void bulk_deactivation_ignores_errors() {
//    // activate on parent profile P1
//    createChildProfiles();
//    activate(new RuleActivation(XOO_X1), XOO_P1_KEY);
//    assertThat(countActiveRules(XOO_P2_KEY)).isEqualTo(1);
//
//    // bulk deactivate on child profile P2 -> not possible
//    BulkChangeResult result = ruleActivator.bulkDeactivate(new RuleQuery().setActivation(true).setQProfileKey(XOO_P2_KEY), XOO_P2_KEY);
//
//    dbSession.clearCache();
//    assertThat(countActiveRules(XOO_P2_KEY)).isEqualTo(1);
//    assertThat(result.countFailed()).isEqualTo(1);
//    assertThat(result.countSucceeded()).isEqualTo(0);
//    assertThat(result.getChanges()).hasSize(0);
//  }
//
//  @Test
//  public void bulk_change_severity() {
//    createChildProfiles();
//
//    // activate two rules on root profile P1 (propagated to P2 and P3)
//    RuleActivation activation = new RuleActivation(XOO_X1).setSeverity(INFO).setParameter("max", "7");
//    activate(activation, XOO_P1_KEY);
//    activation = new RuleActivation(XOO_X2).setSeverity(INFO);
//    activate(activation, XOO_P1_KEY);
//
//    // bulk change severity to BLOCKER. Parameters are not set.
//    RuleQuery query = new RuleQuery().setActivation(true).setQProfileKey(XOO_P1_KEY);
//    BulkChangeResult result = ruleActivator.bulkActivate(dbSession, query, selectProfile(XOO_P1_KEY), "BLOCKER");
//    dbSession.commit();
//    assertThat(result.countSucceeded()).isEqualTo(2);
//
//    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P1_KEY, XOO_X1), BLOCKER, null, ImmutableMap.of("max", "7"));
//    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P1_KEY, XOO_X2), BLOCKER, null, Collections.<String, String>emptyMap());
//    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P2_KEY, XOO_X1), BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
//    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P2_KEY, XOO_X2), BLOCKER, INHERITED, Collections.<String, String>emptyMap());
//    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P3_KEY, XOO_X1), BLOCKER, INHERITED, ImmutableMap.of("max", "7"));
//    verifyHasActiveRuleInDbAndIndex(ActiveRuleKey.of(XOO_P3_KEY, XOO_X2), BLOCKER, INHERITED, Collections.<String, String>emptyMap());
//  }
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
