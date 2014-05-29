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
package org.sonar.server.qualityprofile.index;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.tester.ServerTester;

import java.util.Collection;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class ActiveRuleIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db = tester.get(DbClient.class);
  ActiveRuleIndex index = tester.get(ActiveRuleIndex.class);
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndEs();
    dbSession = db.openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void insert_and_index_active_rule() throws InterruptedException {
    QualityProfileDto profileDto = QualityProfileDto.createFor("myprofile", "java");
    db.qualityProfileDao().insert(dbSession, profileDto);
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDto ruleDto = newRuleDto(ruleKey);
    db.ruleDao().insert(dbSession, ruleDto);

    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profileDto, ruleDto)
      .setInheritance(ActiveRule.Inheritance.INHERIT.name())
      .setSeverity(Severity.BLOCKER);
    db.activeRuleDao().insert(dbSession, activeRule);
    dbSession.commit();

    // verify db
    List<ActiveRuleDto> persistedDtos = db.activeRuleDao().findByRule(dbSession, ruleDto);
    assertThat(persistedDtos).hasSize(1);

    // verify es
    ActiveRule hit = index.getByKey(activeRule.getKey());
    assertThat(hit).isNotNull();
    assertThat(hit.key()).isEqualTo(activeRule.getKey());
    assertThat(hit.inheritance().name()).isEqualTo(activeRule.getInheritance());
    assertThat(hit.parentKey()).isEqualTo(activeRule.getParentId());
    assertThat(hit.severity()).isEqualTo(activeRule.getSeverityString());
  }

  @Test
  public void insert_and_index_active_rule_param() throws InterruptedException {
    // insert and index
    QualityProfileDto profileDto = QualityProfileDto.createFor("myprofile", "java");
    db.qualityProfileDao().insert(dbSession, profileDto);
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
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
      .setInheritance(ActiveRule.Inheritance.INHERIT.name())
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
    List<ActiveRuleParamDto> persistedDtos = db.activeRuleDao().findParamsByActiveRule(dbSession, activeRule);
    assertThat(persistedDtos).hasSize(2);

    // verify es
    ActiveRule rule = index.getByKey(activeRule.getKey());
    assertThat(rule.params()).hasSize(2);
    assertThat(rule.params().keySet()).containsOnly("min", "max");
    assertThat(rule.params().values()).containsOnly("minimum", "maximum");
    assertThat(rule.params().get("min")).isEqualTo("minimum");
  }

  @Test
  public void find_active_rules() throws Exception {
    QualityProfileDto profile1 = QualityProfileDto.createFor("p1", "java");
    QualityProfileDto profile2 = QualityProfileDto.createFor("p2", "java");
    db.qualityProfileDao().insert(dbSession, profile1, profile2);

    RuleDto rule1 = RuleTesting.newDto(RuleKey.of("java", "r1")).setSeverity(Severity.MAJOR);
    RuleDto rule2 = RuleTesting.newDto(RuleKey.of("java", "r2")).setSeverity(Severity.MAJOR);
    db.ruleDao().insert(dbSession, rule1);
    db.ruleDao().insert(dbSession, rule2);

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
    List<ActiveRule> activeRules = index.findByRule(RuleKey.of("java", "r1"));
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).key().ruleKey()).isEqualTo(RuleKey.of("java", "r1"));

    activeRules = index.findByRule(RuleKey.of("java", "r2"));
    assertThat(activeRules).hasSize(2);
    assertThat(activeRules.get(0).key().ruleKey()).isEqualTo(RuleKey.of("java", "r2"));

    activeRules = index.findByRule(RuleKey.of("java", "r3"));
    assertThat(activeRules).isEmpty();

    // 2. find by profile
    activeRules = index.findByProfile(profile1.getKey());
    assertThat(activeRules).hasSize(2);
    assertThat(activeRules.get(0).key().qProfile()).isEqualTo(profile1.getKey());
    assertThat(activeRules.get(1).key().qProfile()).isEqualTo(profile1.getKey());

    activeRules = index.findByProfile(profile2.getKey());
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).key().qProfile()).isEqualTo(profile2.getKey());

    activeRules = index.findByProfile(QualityProfileKey.of("unknown", "unknown"));
    assertThat(activeRules).isEmpty();
  }

  @Test
  public void find_many_active_rules_by_profile() throws InterruptedException {
    // insert and index
    QualityProfileDto profileDto = QualityProfileDto.createFor("P1", "java");
    db.qualityProfileDao().insert(dbSession, profileDto);
    for (int i = 0; i < 100; i++) {
      RuleDto rule = newRuleDto(RuleKey.of("javascript", "S00" + i));
      db.ruleDao().insert(dbSession, rule);

      ActiveRuleDto activeRule = ActiveRuleDto.createFor(profileDto, rule).setSeverity(Severity.MAJOR);
      db.activeRuleDao().insert(dbSession, activeRule);
    }
    dbSession.commit();

    // verify index
    Collection<ActiveRule> activeRules = index.findByProfile(profileDto.getKey());
    assertThat(activeRules).hasSize(100);
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
      .setCardinality(Cardinality.SINGLE)
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
