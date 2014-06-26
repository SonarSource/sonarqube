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
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.qualityprofile.index.ActiveRuleNormalizer;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.search.FacetValue;
import org.sonar.server.tester.ServerTester;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class ActiveRuleBackendMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession dbSession;
  ActiveRuleIndex index;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    index = tester.get(ActiveRuleIndex.class);
  }

  @Test
  public void synchronize_index() throws Exception {
    QualityProfileDto profile1 = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profile1);

    RuleDto rule1 = RuleDto.createFor(RuleTesting.XOO_X1).setSeverity(Severity.MAJOR);
    db.ruleDao().insert(dbSession, rule1);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profile1, rule1).setSeverity("BLOCKER");
    db.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();

    tester.clearIndexes();

    // 0. Assert that we have no rules in Is.
    assertThat(index.getByKey(activeRule.getKey())).isNull();

    // 1. Synchronize since 0
    db.activeRuleDao().synchronizeAfter(dbSession, new Date());

    // 2. Assert that we have the rule in Index
    assertThat(index.getByKey(activeRule.getKey())).isNotNull();

  }

  @Test
  public void insert_and_index_active_rule() {
    QualityProfileDto profileDto = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profileDto);
    RuleKey ruleKey = RuleTesting.XOO_X1;
    RuleDto ruleDto = newRuleDto(ruleKey);
    db.ruleDao().insert(dbSession, ruleDto);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profileDto, ruleDto)
      .setInheritance(ActiveRule.Inheritance.INHERITED.name())
      .setSeverity(Severity.BLOCKER);
    db.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();

    // verify db
    assertThat(db.activeRuleDao().getByKey(dbSession, activeRule.getKey())).isNotNull();
    List<ActiveRuleDto> persistedDtos = db.activeRuleDao().findByRule(dbSession, ruleDto);
    assertThat(persistedDtos).hasSize(1);

    // verify es
    ActiveRule hit = index.getByKey(activeRule.getKey());
    assertThat(hit).isNotNull();
    assertThat(hit.key()).isEqualTo(activeRule.getKey());
    assertThat(hit.inheritance().name()).isEqualTo(activeRule.getInheritance());
    assertThat(hit.parentKey()).isNull();
    assertThat(hit.severity()).isEqualTo(activeRule.getSeverityString());
  }

  @Test
  public void insert_and_index_active_rule_param() throws InterruptedException {
    // insert and index
    QualityProfileDto profileDto = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profileDto);
    RuleKey ruleKey = RuleTesting.XOO_X1;
    RuleDto ruleDto = newRuleDto(ruleKey);
    db.ruleDao().insert(dbSession, ruleDto);

    RuleParamDto minParam = new RuleParamDto()
      .setName("min")
      .setType("STRING");
    db.ruleDao().addRuleParam(dbSession, ruleDto, minParam);

    RuleParamDto maxParam = new RuleParamDto()
      .setName("max")
      .setType("STRING");
    db.ruleDao().addRuleParam(dbSession, ruleDto, maxParam);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profileDto, ruleDto)
      .setInheritance(ActiveRule.Inheritance.INHERITED.name())
      .setSeverity(Severity.BLOCKER);
    db.activeRuleDao().insert(dbSession, activeRule);

    ActiveRuleParamDto activeRuleMinParam = ActiveRuleParamDto.createFor(minParam)
      .setValue("minimum");
    db.activeRuleDao().addParam(dbSession, activeRule, activeRuleMinParam);

    ActiveRuleParamDto activeRuleMaxParam = ActiveRuleParamDto.createFor(maxParam)
      .setValue("maximum");
    db.activeRuleDao().addParam(dbSession, activeRule, activeRuleMaxParam);

    dbSession.commit();

    // verify db
    List<ActiveRuleParamDto> persistedDtos =
      db.activeRuleDao().findParamsByActiveRuleKey(dbSession, activeRule.getKey());
    assertThat(persistedDtos).hasSize(2);

    // verify es
    ActiveRule rule = index.getByKey(activeRule.getKey());
    assertThat(rule).isNotNull();
    assertThat(rule.params()).hasSize(2);
    assertThat(rule.params().keySet()).containsOnly("min", "max");
    assertThat(rule.params().values()).containsOnly("minimum", "maximum");
    assertThat(rule.params().get("min")).isEqualTo("minimum");
  }

  @Test
  public void find_active_rules() throws Exception {
    QualityProfileDto profile1 = QProfileTesting.newXooP1();
    QualityProfileDto profile2 = QProfileTesting.newXooP2();
    db.qualityProfileDao().insert(dbSession, profile1, profile2);

    RuleDto rule1 = RuleTesting.newXooX1().setSeverity(Severity.MAJOR);
    RuleDto rule2 = RuleTesting.newXooX2().setSeverity(Severity.MAJOR);
    db.ruleDao().insert(dbSession, rule1, rule2);

    db.activeRuleDao().insert(dbSession, ActiveRuleDto.createFor(profile1, rule1).setSeverity(Severity.MINOR));
    db.activeRuleDao().insert(dbSession, ActiveRuleDto.createFor(profile1, rule2).setSeverity(Severity.BLOCKER));
    db.activeRuleDao().insert(dbSession, ActiveRuleDto.createFor(profile2, rule2).setSeverity(Severity.CRITICAL));
    dbSession.commit();

    // 1. find by rule key

    // in db
    dbSession.clearCache();
    assertThat(db.activeRuleDao().findByRule(dbSession, rule1)).hasSize(1);
    assertThat(db.activeRuleDao().findByRule(dbSession, rule2)).hasSize(2);

    // in es
    List<ActiveRule> activeRules = index.findByRule(RuleTesting.XOO_X1);
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).key().ruleKey()).isEqualTo(RuleTesting.XOO_X1);

    activeRules = index.findByRule(RuleTesting.XOO_X2);
    assertThat(activeRules).hasSize(2);
    assertThat(activeRules.get(0).key().ruleKey()).isEqualTo(RuleTesting.XOO_X2);

    activeRules = index.findByRule(RuleTesting.XOO_X3);
    assertThat(activeRules).isEmpty();

    // 2. find by profile
    activeRules = index.findByProfile(profile1.getKey());
    assertThat(activeRules).hasSize(2);
    assertThat(activeRules.get(0).key().qProfile()).isEqualTo(profile1.getKey());
    assertThat(activeRules.get(1).key().qProfile()).isEqualTo(profile1.getKey());

    activeRules = index.findByProfile(profile2.getKey());
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).key().qProfile()).isEqualTo(profile2.getKey());

    activeRules = index.findByProfile("unknown");
    assertThat(activeRules).isEmpty();
  }

  @Test
  public void find_many_active_rules_by_profile() {
    // insert and index
    QualityProfileDto profileDto = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profileDto);
    for (int i = 0; i < 100; i++) {
      RuleDto rule = newRuleDto(RuleKey.of("xoo", "S00" + i));
      db.ruleDao().insert(dbSession, rule);

      ActiveRuleDto activeRule = ActiveRuleDto.createFor(profileDto, rule).setSeverity(Severity.MAJOR);
      db.activeRuleDao().insert(dbSession, activeRule);
    }
    dbSession.commit();
    dbSession.clearCache();

    // verify index
    Collection<ActiveRule> activeRules = index.findByProfile(profileDto.getKey());
    assertThat(activeRules).hasSize(100);
  }

//  @Test
//  public void count_by_profile() {
//    QualityProfileDto profileDto1 = QProfileTesting.newXooP1();
//    QualityProfileDto profileDto2 = QProfileTesting.newXooP2();
//    db.qualityProfileDao().insert(dbSession, profileDto1, profileDto2);
//
//    RuleKey ruleKey = RuleTesting.XOO_X1;
//    RuleDto ruleDto = newRuleDto(ruleKey);
//    db.ruleDao().insert(dbSession, ruleDto);
//
//    ActiveRuleDto activeRule1 = ActiveRuleDto.createFor(profileDto1, ruleDto).setSeverity(Severity.MAJOR);
//    ActiveRuleDto activeRule2 = ActiveRuleDto.createFor(profileDto2, ruleDto).setSeverity(Severity.MAJOR);
//    db.activeRuleDao().insert(dbSession, activeRule1, activeRule2);
//    dbSession.commit();
//
//    // 0. Test base case
//    assertThat(index.countAll()).isEqualTo(2);
//
//    // 1. Assert by profileKey
//    assertThat(index.countByQualityProfileKey(profileDto1.getKey())).isEqualTo(1);
//
//    // 2. Assert by term aggregation;
//    Map<String, Long> counts = index.countByField(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY);
//    assertThat(counts).hasSize(2);
//    assertThat(counts.values()).containsOnly(1L, 1L);
//    assertThat(counts.keySet()).containsOnly(profileDto1.getKey().toString(), profileDto2.getKey().toString());
//  }

  @Test
  public void count_all_by_index_field() {
    QualityProfileDto profileDto1 = QProfileTesting.newXooP1();
    QualityProfileDto profileDto2 = QProfileTesting.newXooP2();
    db.qualityProfileDao().insert(dbSession, profileDto1, profileDto2);

    RuleKey ruleKey = RuleTesting.XOO_X1;
    RuleDto ruleDto = newRuleDto(ruleKey);
    db.ruleDao().insert(dbSession, ruleDto);

    ActiveRuleDto activeRule1 = ActiveRuleDto.createFor(profileDto1, ruleDto).setSeverity(Severity.MAJOR);
    ActiveRuleDto activeRule2 = ActiveRuleDto.createFor(profileDto2, ruleDto).setSeverity(Severity.MAJOR);
    db.activeRuleDao().insert(dbSession, activeRule1, activeRule2);
    dbSession.commit();

    // 0. Test base case
    assertThat(index.countAll()).isEqualTo(2);

    // 1. Assert by term aggregation;
    Map<String, Long> counts = index.countByField(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY);
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
    db.ruleDao().insert(dbSession, ruleDto1, ruleDto2);

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
        .setSeverity(Severity.BLOCKER)
    );
    dbSession.commit();
    dbSession.clearCache();

    // 0. Test base case
    assertThat(index.countAll()).isEqualTo(4);

    // 1. Assert by term aggregation;
    Map<String, Multimap<String, FacetValue>> stats = index.getStatsByProfileKeys(
      ImmutableList.of(profileDto1.getKey(),
        profileDto2.getKey()));

    assertThat(stats).hasSize(2);
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
}
