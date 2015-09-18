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
package org.sonar.server.qualityprofile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.platform.Platform;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.qualityprofile.index.ActiveRuleNormalizer;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class ActiveRuleTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  RuleDao dao;
  IndexClient index;
  DbSession dbSession;

  @Before
  public void before() {
    dao = tester.get(RuleDao.class);
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    index = tester.get(IndexClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    if (dbSession != null) {
      dbSession.close();
    }
  }

  @Test
  public void synchronize_index() {
    Date beginning = new Date();

    QualityProfileDto profile1 = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profile1);

    RuleDto rule1 = RuleDto.createFor(RuleTesting.XOO_X1).setSeverity(Severity.MAJOR);
    db.deprecatedRuleDao().insert(dbSession, rule1);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile1, rule1).setSeverity("BLOCKER");
    db.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();

    // 1. Synchronize since 0
    tester.clearIndexes();
    assertThat(index.get(ActiveRuleIndex.class).getNullableByKey(activeRule.getKey())).isNull();
    db.activeRuleDao().synchronizeAfter(dbSession, new Date(0L));
    dbSession.commit();
    assertThat(index.get(ActiveRuleIndex.class).getNullableByKey(activeRule.getKey())).isNotNull();

    // 2. Synchronize since beginning
    tester.clearIndexes();
    assertThat(index.get(ActiveRuleIndex.class).getNullableByKey(activeRule.getKey())).isNull();
    db.activeRuleDao().synchronizeAfter(dbSession, beginning);
    dbSession.commit();
    assertThat(index.get(ActiveRuleIndex.class).getNullableByKey(activeRule.getKey())).isNotNull();

    // 3. Assert startup picks it up
    tester.clearIndexes();
    assertThat(index.get(ActiveRuleIndex.class).getLastSynchronization()).isNull();
    tester.get(Platform.class).executeStartupTasks();
    assertThat(index.get(ActiveRuleIndex.class).getNullableByKey(activeRule.getKey())).isNotNull();
    assertThat(index.get(ActiveRuleIndex.class).getLastSynchronization()).isNotNull();
  }

  /**
   * SONAR-6540
   */
  @Test
  public void active_rule_linked_to_not_existing_rule_should_be_ignored() throws SQLException {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profile);

    RuleDto rule = RuleDto.createFor(RuleTesting.XOO_X1).setSeverity(Severity.MAJOR);
    db.deprecatedRuleDao().insert(dbSession, rule);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule).setSeverity("BLOCKER");
    db.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();

    // Remove rule -> Active rule is now linked to a not existing rule
    executeSql(String.format("DELETE FROM rules WHERE id=%s", rule.getId()));
    dbSession.commit();

    // Synchronize index from start
    tester.clearIndexes();
    db.activeRuleDao().synchronizeAfter(dbSession, new Date(0L));
    dbSession.commit();

    // Active does not exist in the index
    assertThat(index.get(ActiveRuleIndex.class).getNullableByKey(activeRule.getKey())).isNull();
  }

  /**
   * SONAR-6540
   */
  @Test
  public void active_rule_linked_to_not_existing_profile_should_be_ignored() throws SQLException {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profile);

    RuleDto rule = RuleDto.createFor(RuleTesting.XOO_X1).setSeverity(Severity.MAJOR);
    db.deprecatedRuleDao().insert(dbSession, rule);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule).setSeverity("BLOCKER");
    db.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();

    // Remove quality profile -> active rule is now linked to a not existing quality profile
    executeSql(String.format("DELETE FROM rules_profiles WHERE id=%s", profile.getId()));
    dbSession.commit();

    // Synchronize index from start
    tester.clearIndexes();
    db.activeRuleDao().synchronizeAfter(dbSession, new Date(0L));
    dbSession.commit();

    // Active does not exist in the index
    assertThat(index.get(ActiveRuleIndex.class).getNullableByKey(activeRule.getKey())).isNull();
  }

  @Test
  public void insert_and_index_active_rule() {
    QualityProfileDto profileDto = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profileDto);
    RuleKey ruleKey = RuleTesting.XOO_X1;
    RuleDto ruleDto = newRuleDto(ruleKey);
    db.deprecatedRuleDao().insert(dbSession, ruleDto);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profileDto, ruleDto)
      .setInheritance(ActiveRule.Inheritance.INHERITED.name())
      .setSeverity(Severity.BLOCKER);
    db.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();

    // verify db
    assertThat(db.activeRuleDao().getByKey(dbSession, activeRule.getKey())).isNotNull();
    List<ActiveRuleDto> persistedDtos = db.activeRuleDao().selectByRule(dbSession, ruleDto);
    assertThat(persistedDtos).hasSize(1);

    // verify es
    ActiveRule hit = index.get(ActiveRuleIndex.class).getByKey(activeRule.getKey());
    assertThat(hit).isNotNull();
    assertThat(hit.key()).isEqualTo(activeRule.getKey());
    assertThat(hit.inheritance().name()).isEqualTo(activeRule.getInheritance());
    assertThat(hit.parentKey()).isNull();
    assertThat(hit.severity()).isEqualTo(activeRule.getSeverityString());
  }

  @Test
  public void insert_and_index_active_rule_param() {
    // insert and index
    QualityProfileDto profileDto = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profileDto);
    RuleKey ruleKey = RuleTesting.XOO_X1;
    RuleDto ruleDto = newRuleDto(ruleKey);
    db.deprecatedRuleDao().insert(dbSession, ruleDto);

    RuleParamDto minParam = new RuleParamDto()
      .setName("min")
      .setType("STRING");
    db.deprecatedRuleDao().insertRuleParam(dbSession, ruleDto, minParam);

    RuleParamDto maxParam = new RuleParamDto()
      .setName("max")
      .setType("STRING");
    db.deprecatedRuleDao().insertRuleParam(dbSession, ruleDto, maxParam);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profileDto, ruleDto)
      .setInheritance(ActiveRule.Inheritance.INHERITED.name())
      .setSeverity(Severity.BLOCKER);
    db.activeRuleDao().insert(dbSession, activeRule);

    ActiveRuleParamDto activeRuleMinParam = ActiveRuleParamDto.createFor(minParam)
      .setValue("minimum");
    db.activeRuleDao().insertParam(dbSession, activeRule, activeRuleMinParam);

    ActiveRuleParamDto activeRuleMaxParam = ActiveRuleParamDto.createFor(maxParam)
      .setValue("maximum");
    db.activeRuleDao().insertParam(dbSession, activeRule, activeRuleMaxParam);

    dbSession.commit();

    // verify db
    List<ActiveRuleParamDto> persistedDtos = db.activeRuleDao().selectParamsByActiveRuleKey(dbSession, activeRule.getKey());
    assertThat(persistedDtos).hasSize(2);

    // verify es
    ActiveRule rule = index.get(ActiveRuleIndex.class).getByKey(activeRule.getKey());
    assertThat(rule).isNotNull();
    assertThat(rule.params()).hasSize(2);
    assertThat(rule.params().keySet()).containsOnly("min", "max");
    assertThat(rule.params().values()).containsOnly("minimum", "maximum");
    assertThat(rule.params().get("min")).isEqualTo("minimum");
  }

  @Test
  public void find_active_rules() {
    QualityProfileDto profile1 = QProfileTesting.newXooP1();
    QualityProfileDto profile2 = QProfileTesting.newXooP2();
    db.qualityProfileDao().insert(dbSession, profile1, profile2);

    RuleDto rule1 = RuleTesting.newXooX1().setSeverity(Severity.MAJOR);
    RuleDto rule2 = RuleTesting.newXooX2().setSeverity(Severity.MAJOR);
    RuleDto removedRule = RuleTesting.newDto(RuleKey.of("xoo", "removed")).setSeverity(Severity.MAJOR).setStatus(RuleStatus.REMOVED);
    db.deprecatedRuleDao().insert(dbSession, rule1, rule2, removedRule);

    db.activeRuleDao().insert(dbSession, ActiveRuleDto.createFor(profile1, rule1).setSeverity(Severity.MINOR));
    db.activeRuleDao().insert(dbSession, ActiveRuleDto.createFor(profile1, rule2).setSeverity(Severity.BLOCKER));
    db.activeRuleDao().insert(dbSession, ActiveRuleDto.createFor(profile2, rule2).setSeverity(Severity.CRITICAL));
    // Removed rule can still be activated for instance when removing the checkstyle plugin, active rules related on checkstyle are not
    // removed
    // because if the plugin is re-install, quality profiles using these rule are not changed.
    db.activeRuleDao().insert(dbSession, ActiveRuleDto.createFor(profile2, removedRule).setSeverity(Severity.MAJOR));
    dbSession.commit();

    // 1. find by rule key

    // in db
    dbSession.clearCache();
    assertThat(db.activeRuleDao().selectByRule(dbSession, rule1)).hasSize(1);
    assertThat(db.activeRuleDao().selectByRule(dbSession, rule2)).hasSize(2);

    // in es
    List<ActiveRule> activeRules = index.get(ActiveRuleIndex.class).findByRule(RuleTesting.XOO_X1);
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).key().ruleKey()).isEqualTo(RuleTesting.XOO_X1);

    activeRules = index.get(ActiveRuleIndex.class).findByRule(RuleTesting.XOO_X2);
    assertThat(activeRules).hasSize(2);
    assertThat(activeRules.get(0).key().ruleKey()).isEqualTo(RuleTesting.XOO_X2);

    activeRules = index.get(ActiveRuleIndex.class).findByRule(RuleTesting.XOO_X3);
    assertThat(activeRules).isEmpty();

    // 2. find by profile
    activeRules = Lists.newArrayList(index.get(ActiveRuleIndex.class).findByProfile(profile1.getKey()));
    assertThat(activeRules).hasSize(2);
    assertThat(activeRules.get(0).key().qProfile()).isEqualTo(profile1.getKey());
    assertThat(activeRules.get(1).key().qProfile()).isEqualTo(profile1.getKey());

    activeRules = Lists.newArrayList(index.get(ActiveRuleIndex.class).findByProfile(profile2.getKey()));
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).key().qProfile()).isEqualTo(profile2.getKey());

    activeRules = Lists.newArrayList(index.get(ActiveRuleIndex.class).findByProfile("unknown"));
    assertThat(activeRules).isEmpty();
  }

  @Test
  public void find_many_active_rules_by_profile() {
    // insert and index
    QualityProfileDto profileDto = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profileDto);
    int nb = 100;
    for (int i = 0; i < nb; i++) {
      RuleDto rule = newRuleDto(RuleKey.of("xoo", "S00" + i));
      db.deprecatedRuleDao().insert(dbSession, rule);

      ActiveRuleDto activeRule = ActiveRuleDto.createFor(profileDto, rule).setSeverity(Severity.MAJOR);
      db.activeRuleDao().insert(dbSession, activeRule);
    }
    dbSession.commit();
    dbSession.clearCache();

    // verify index
    Collection<ActiveRule> activeRules = Lists.newArrayList(index.get(ActiveRuleIndex.class).findByProfile(profileDto.getKey()));
    assertThat(activeRules).hasSize(nb);
  }

  @Test
  public void count_by_profile() {
    QualityProfileDto profileDto1 = QProfileTesting.newXooP1();
    QualityProfileDto profileDto2 = QProfileTesting.newXooP2();
    db.qualityProfileDao().insert(dbSession, profileDto1, profileDto2);

    RuleKey ruleKey = RuleTesting.XOO_X1;
    RuleDto ruleDto = newRuleDto(ruleKey);
    db.deprecatedRuleDao().insert(dbSession, ruleDto);

    ActiveRuleDto activeRule1 = ActiveRuleDto.createFor(profileDto1, ruleDto).setSeverity(Severity.MAJOR);
    ActiveRuleDto activeRule2 = ActiveRuleDto.createFor(profileDto2, ruleDto).setSeverity(Severity.MAJOR);
    db.activeRuleDao().insert(dbSession, activeRule1, activeRule2);
    dbSession.commit();

    // 0. Test base case
    assertThat(index.get(ActiveRuleIndex.class).countAll()).isEqualTo(2);

    // 1. Assert by profileKey
    assertThat(index.get(ActiveRuleIndex.class).countByQualityProfileKey(profileDto1.getKey())).isEqualTo(1);

    // 2. Assert by term aggregation;
    Map<String, Long> counts = index.get(ActiveRuleIndex.class).countByField(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY);
    assertThat(counts).hasSize(2);
    assertThat(counts.values()).containsOnly(1L, 1L);
    assertThat(counts.keySet()).containsOnly(profileDto1.getKey().toString(), profileDto2.getKey().toString());
  }

  @Test
  public void count_all_by_index_field() {
    QualityProfileDto profileDto1 = QProfileTesting.newXooP1();
    QualityProfileDto profileDto2 = QProfileTesting.newXooP2();
    db.qualityProfileDao().insert(dbSession, profileDto1, profileDto2);

    RuleKey ruleKey = RuleTesting.XOO_X1;
    RuleDto ruleDto = newRuleDto(ruleKey);
    db.deprecatedRuleDao().insert(dbSession, ruleDto);

    ActiveRuleDto activeRule1 = ActiveRuleDto.createFor(profileDto1, ruleDto).setSeverity(Severity.MAJOR);
    ActiveRuleDto activeRule2 = ActiveRuleDto.createFor(profileDto2, ruleDto).setSeverity(Severity.MAJOR);
    db.activeRuleDao().insert(dbSession, activeRule1, activeRule2);
    dbSession.commit();

    // 0. Test base case
    assertThat(index.get(ActiveRuleIndex.class).countAll()).isEqualTo(2);

    // 1. Assert by term aggregation;
    Map<String, Long> counts = index.get(ActiveRuleIndex.class).countByField(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY);
    assertThat(counts).hasSize(2);
    assertThat(counts.values()).containsOnly(1L, 1L);
    assertThat(counts.keySet()).containsOnly(profileDto1.getKey(), profileDto2.getKey());
  }

  @Test
  public void stats_for_all() {
    QualityProfileDto profileDto1 = QProfileTesting.newXooP1();
    QualityProfileDto profileDto2 = QProfileTesting.newXooP2();
    db.qualityProfileDao().insert(dbSession, profileDto1, profileDto2);

    RuleDto ruleDto1 = newRuleDto(RuleTesting.XOO_X1);
    RuleDto ruleDto2 = newRuleDto(RuleTesting.XOO_X2);
    db.deprecatedRuleDao().insert(dbSession, ruleDto1, ruleDto2);

    db.activeRuleDao().insert(dbSession,
      ActiveRuleDto.createFor(profileDto1, ruleDto1)
        .setInheritance(ActiveRule.Inheritance.INHERITED.name())
        .setSeverity(Severity.BLOCKER),
      ActiveRuleDto.createFor(profileDto2, ruleDto1)
        .setInheritance(ActiveRule.Inheritance.INHERITED.name())
        .setSeverity(Severity.MINOR),
      ActiveRuleDto.createFor(profileDto1, ruleDto2)
        .setInheritance(ActiveRule.Inheritance.OVERRIDES.name())
        .setSeverity(Severity.MAJOR),
      ActiveRuleDto.createFor(profileDto2, ruleDto2)
        .setInheritance(ActiveRule.Inheritance.INHERITED.name())
        .setSeverity(Severity.BLOCKER));
    dbSession.commit();
    dbSession.clearCache();

    // 0. Test base case
    assertThat(index.get(ActiveRuleIndex.class).countAll()).isEqualTo(4);

    // 1. Assert by term aggregation;
    Map<String, Multimap<String, FacetValue>> stats = index.get(ActiveRuleIndex.class).getStatsByProfileKeys(
      ImmutableList.of(profileDto1.getKey(),
        profileDto2.getKey()));

    assertThat(stats).hasSize(2);
  }

  /**
   * SONAR-5844
   */
  @Test
  public void stats_for_all_with_lof_of_profiles() {
    RuleDto ruleDto1 = newRuleDto(RuleTesting.XOO_X1);
    RuleDto ruleDto2 = newRuleDto(RuleTesting.XOO_X2);
    db.deprecatedRuleDao().insert(dbSession, ruleDto1, ruleDto2);

    List<String> profileKeys = newArrayList();
    for (int i = 0; i < 30; i++) {
      QualityProfileDto profileDto = QProfileTesting.newQProfileDto(QProfileName.createFor("xoo", "profile-" + i), "profile-" + i);
      profileKeys.add(profileDto.getKey());
      db.qualityProfileDao().insert(dbSession, profileDto);

      db.activeRuleDao().insert(dbSession,
        ActiveRuleDto.createFor(profileDto, ruleDto1)
          .setSeverity(Severity.BLOCKER),
        ActiveRuleDto.createFor(profileDto, ruleDto2)
          .setSeverity(Severity.MAJOR));
    }
    dbSession.commit();
    dbSession.clearCache();

    Map<String, Multimap<String, FacetValue>> stats = index.get(ActiveRuleIndex.class).getStatsByProfileKeys(profileKeys);
    assertThat(stats).hasSize(30);
  }

  @Test
  public void select_by_id() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profile);

    RuleDto rule = RuleDto.createFor(RuleTesting.XOO_X1).setSeverity(Severity.MAJOR);
    db.deprecatedRuleDao().insert(dbSession, rule);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule).setSeverity("BLOCKER");
    db.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();

    assertThat(db.activeRuleDao().selectById(dbSession, activeRule.getId()).getId()).isEqualTo(activeRule.getId());
  }

  @Test
  public void select_by_id_return_nothing_when_rule_does_not_exist() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profile);

    RuleDto rule = RuleDto.createFor(RuleTesting.XOO_X1).setSeverity(Severity.MAJOR);
    db.deprecatedRuleDao().insert(dbSession, rule);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule).setSeverity("BLOCKER");
    db.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();

    // Remove rule -> Active rule is now linked to a not existing rule
    executeSql(String.format("DELETE FROM rules WHERE id=%s", rule.getId()));
    dbSession.commit();

    assertThat(db.activeRuleDao().selectById(dbSession, activeRule.getId())).isNull();
  }

  @Test
  public void select_by_id_return_nothing_when_profile_does_not_exist() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profile);

    RuleDto rule = RuleDto.createFor(RuleTesting.XOO_X1).setSeverity(Severity.MAJOR);
    db.deprecatedRuleDao().insert(dbSession, rule);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile, rule).setSeverity("BLOCKER");
    db.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();

    // Remove quality profile -> active rule is now linked to a not existing quality profile
    executeSql(String.format("DELETE FROM rules_profiles WHERE id=%s", profile.getId()));
    dbSession.commit();

    assertThat(db.activeRuleDao().selectById(dbSession, activeRule.getId())).isNull();
  }

  private RuleDto newRuleDto(RuleKey ruleKey) {
    return new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setStatus(RuleStatus.READY)
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setIsTemplate(false)
      .setLanguage("js")
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix");
  }

  private void executeSql(String sql) throws SQLException {
    PreparedStatement stmt = db.getDatabase().getDataSource().getConnection().prepareStatement(sql);
    stmt.executeUpdate();
  }

}
