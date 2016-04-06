/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.qualityprofile;

import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.db.qualityprofile.ActiveRuleDto.INHERITED;
import static org.sonar.db.qualityprofile.ActiveRuleDto.OVERRIDES;
import static org.sonar.db.qualityprofile.ActiveRuleDto.createFor;

public class ActiveRuleDaoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final long NOW = 10000000L;

  private QualityProfileDto profile1 = QualityProfileDto.createFor("qp1").setName("QProile1");
  private QualityProfileDto profile2 = QualityProfileDto.createFor("qp2").setName("QProile2");

  private RuleDto rule1 = RuleTesting.newDto(RuleTesting.XOO_X1);
  private RuleDto rule2 = RuleTesting.newDto(RuleTesting.XOO_X2);
  private RuleDto rule3 = RuleTesting.newDto(RuleTesting.XOO_X3);

  private RuleParamDto rule1Param1;
  private RuleParamDto rule1Param2;
  private RuleParamDto rule2Param1;

  private System2 system = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();

  private ActiveRuleDao underTest = dbTester.getDbClient().activeRuleDao();

  @Before
  public void setUp() {
    when(system.now()).thenReturn(NOW);

    dbClient.qualityProfileDao().insert(dbSession, profile1);
    dbClient.qualityProfileDao().insert(dbSession, profile2);
    dbClient.ruleDao().insert(dbSession, rule1);
    dbClient.ruleDao().insert(dbSession, rule2);
    dbClient.ruleDao().insert(dbSession, rule3);

    rule1Param1 = new RuleParamDto()
      .setName("param1")
      .setDefaultValue("value1")
      .setType(RuleParamType.STRING.toString());
    dbClient.ruleDao().insertRuleParam(dbSession, rule1, rule1Param1);

    rule1Param2 = new RuleParamDto()
      .setRuleId(rule1.getId())
      .setName("param2")
      .setDefaultValue("2")
      .setType(RuleParamType.INTEGER.toString());
    dbClient.ruleDao().insertRuleParam(dbSession, rule1, rule1Param2);

    rule2Param1 = new RuleParamDto()
      .setRuleId(rule2.getId())
      .setName("param1")
      .setDefaultValue("1")
      .setType(RuleParamType.INTEGER.toString());
    dbClient.ruleDao().insertRuleParam(dbSession, rule2, rule2Param1);

    dbSession.commit();
  }

  @Test
  public void select_by_key() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession, activeRule.getKey())).isPresent();
    assertThat(underTest.selectByKey(dbSession, ActiveRuleKey.of(profile2.getKey(), rule2.getKey()))).isAbsent();
  }

  @Test
  public void select_or_fail_by_key() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    dbSession.commit();

    assertThat(underTest.selectOrFailByKey(dbSession, activeRule.getKey())).isNotNull();

    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Active rule with key 'qp2:xoo:x2' does not exist");
    underTest.selectOrFailByKey(dbSession, ActiveRuleKey.of(profile2.getKey(), rule2.getKey()));
  }

  @Test
  public void select_by_keys() throws Exception {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    ActiveRuleDto activeRule2 = createFor(profile1, rule2).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    underTest.insert(dbTester.getSession(), activeRule2);
    dbSession.commit();

    assertThat(underTest.selectByKeys(dbSession, asList(activeRule1.getKey(), activeRule2.getKey()))).hasSize(2);
    assertThat(underTest.selectByKeys(dbSession, asList(activeRule1.getKey()))).hasSize(1);
    assertThat(underTest.selectByKeys(dbSession, asList(ActiveRuleKey.of(profile2.getKey(), rule1.getKey())))).isEmpty();
  }

  @Test
  public void select_by_rule() throws Exception {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    ActiveRuleDto activeRule2 = createFor(profile2, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    underTest.insert(dbTester.getSession(), activeRule2);
    dbSession.commit();

    assertThat(underTest.selectByRule(dbSession, rule1)).extracting("key").containsOnly(activeRule1.getKey(), activeRule2.getKey());
    assertThat(underTest.selectByRule(dbSession, rule3)).isEmpty();
  }

  @Test
  public void select_by_rule_ids() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    ActiveRuleDto activeRule2 = createFor(profile1, rule2).setSeverity(BLOCKER);
    ActiveRuleDto activeRule3 = createFor(profile2, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule1);
    underTest.insert(dbSession, activeRule2);
    underTest.insert(dbSession, activeRule3);
    dbSession.commit();

    assertThat(underTest.selectByRuleIds(dbSession, Collections.singletonList(rule1.getId())))
      .extracting("key").containsOnly(activeRule1.getKey(), activeRule3.getKey());
    assertThat(underTest.selectByRuleIds(dbSession, newArrayList(rule1.getId(), rule2.getId())))
      .extracting("key").containsOnly(activeRule1.getKey(), activeRule2.getKey(), activeRule3.getKey());
  }

  @Test
  public void select_all() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    ActiveRuleDto activeRule2 = createFor(profile1, rule2).setSeverity(BLOCKER);
    ActiveRuleDto activeRule3 = createFor(profile2, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule1);
    underTest.insert(dbSession, activeRule2);
    underTest.insert(dbSession, activeRule3);
    dbSession.commit();

    assertThat(underTest.selectAll(dbSession)).hasSize(3);
  }

  @Test
  public void select_by_profile() throws Exception {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    ActiveRuleDto activeRule2 = createFor(profile1, rule2).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    underTest.insert(dbTester.getSession(), activeRule2);
    dbSession.commit();

    assertThat(underTest.selectByProfileKey(dbSession, profile1.getKey())).hasSize(2);
    assertThat(underTest.selectByProfileKey(dbSession, profile2.getKey())).isEmpty();
  }

  @Test
  public void select_by_profile_ignore_removed_rules() throws Exception {
    RuleDto removedRule = RuleTesting.newDto(RuleKey.of("removed", "rule")).setStatus(RuleStatus.REMOVED);
    dbClient.ruleDao().insert(dbTester.getSession(), removedRule);
    ActiveRuleDto activeRule = createFor(profile1, removedRule).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    dbSession.commit();

    assertThat(underTest.selectByProfileKey(dbSession, profile1.getKey())).isEmpty();
  }

  @Test
  public void insert() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1)
      .setSeverity(BLOCKER)
      .setInheritance(INHERITED)
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);
    underTest.insert(dbTester.getSession(), activeRule);
    dbSession.commit();

    ActiveRuleDto result = underTest.selectOrFailByKey(dbSession, activeRule.getKey());
    assertThat(result.getId()).isEqualTo(activeRule.getId());
    assertThat(result.getKey()).isEqualTo(ActiveRuleKey.of(profile1.getKey(), rule1.getKey()));
    assertThat(result.getRuleId()).isEqualTo(rule1.getId());
    assertThat(result.getProfileId()).isEqualTo(profile1.getId());
    assertThat(result.getSeverityString()).isEqualTo(BLOCKER);
    assertThat(result.getInheritance()).isEqualTo(INHERITED);
    assertThat(result.getCreatedAt()).isEqualTo(1000L);
    assertThat(result.getUpdatedAt()).isEqualTo(2000L);
  }

  @Test
  public void fail_to_insert_when_profile_id_is_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Quality profile is not persisted (missing id)");

    underTest.insert(dbTester.getSession(), createFor(profile1, rule1).setProfileId(null));
  }

  @Test
  public void fail_to_insert_when_rule_id_is_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Rule is not persisted");

    underTest.insert(dbTester.getSession(), createFor(profile1, rule1).setRuleId(null));
  }

  @Test
  public void fail_to_insert_when_id_is_not_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("ActiveRule is already persisted");

    underTest.insert(dbTester.getSession(), createFor(profile1, rule1).setId(100));
  }

  @Test
  public void update() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1)
      .setSeverity(BLOCKER)
      .setInheritance(INHERITED)
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);
    underTest.insert(dbTester.getSession(), activeRule);
    dbSession.commit();

    ActiveRuleDto activeRuleUpdated = activeRule
      .setSeverity(MAJOR)
      .setInheritance(OVERRIDES)
      // created at should not be updated
      .setCreatedAt(3000L)
      .setUpdatedAt(4000L);
    underTest.update(dbTester.getSession(), activeRuleUpdated);
    dbSession.commit();

    ActiveRuleDto result = underTest.selectOrFailByKey(dbSession, ActiveRuleKey.of(profile1.getKey(), rule1.getKey()));
    assertThat(result.getId()).isEqualTo(activeRule.getId());
    assertThat(result.getKey()).isEqualTo(ActiveRuleKey.of(profile1.getKey(), rule1.getKey()));
    assertThat(result.getRuleId()).isEqualTo(rule1.getId());
    assertThat(result.getProfileId()).isEqualTo(profile1.getId());
    assertThat(result.getSeverityString()).isEqualTo(MAJOR);
    assertThat(result.getInheritance()).isEqualTo(OVERRIDES);
    assertThat(result.getCreatedAt()).isEqualTo(1000L);
    assertThat(result.getUpdatedAt()).isEqualTo(4000L);
  }

  @Test
  public void fail_to_update_when_profile_id_is_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Quality profile is not persisted (missing id)");

    underTest.update(dbTester.getSession(), createFor(profile1, rule1).setId(100).setProfileId(null));
  }

  @Test
  public void fail_to_update_when_rule_id_is_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Rule is not persisted");

    underTest.update(dbTester.getSession(), createFor(profile1, rule1).setId(100).setRuleId(null));
  }

  @Test
  public void fail_to_update_when_id_is_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("ActiveRule is not persisted");

    underTest.update(dbTester.getSession(), createFor(profile1, rule1).setId(null));
  }

  @Test
  public void delete() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1)
      .setSeverity(BLOCKER)
      .setInheritance(INHERITED)
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);
    underTest.insert(dbTester.getSession(), activeRule);
    dbSession.commit();

    underTest.delete(dbSession, activeRule.getKey());
    dbSession.commit();

    assertThat(underTest.selectByKey(dbSession,  ActiveRuleKey.of(profile1.getKey(), rule1.getKey()))).isAbsent();
  }

  @Test
  public void does_not_fail_when_active_rule_does_not_exist() throws Exception {
    underTest.delete(dbSession, ActiveRuleKey.of(profile1.getKey(), rule1.getKey()));
  }

  @Test
  public void select_params_by_active_rule_id() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1);
    underTest.insertParam(dbSession, activeRule, activeRuleParam1);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule1Param2);
    underTest.insertParam(dbSession, activeRule, activeRuleParam2);
    dbSession.commit();

    assertThat(underTest.selectParamsByActiveRuleId(dbSession, activeRule.getId())).hasSize(2);
  }

  @Test
  public void select_params_by_active_rule_ids() throws Exception {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    underTest.insertParam(dbSession, activeRule1, ActiveRuleParamDto.createFor(rule1Param1));
    underTest.insertParam(dbSession, activeRule1, ActiveRuleParamDto.createFor(rule1Param2));

    ActiveRuleDto activeRule2 = createFor(profile1, rule2).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule2);
    underTest.insertParam(dbSession, activeRule2, ActiveRuleParamDto.createFor(rule2Param1));
    dbSession.commit();

    assertThat(underTest.selectParamsByActiveRuleIds(dbSession, asList(activeRule1.getId(), activeRule2.getId()))).hasSize(3);
  }

  @Test
  public void select_param_by_key_and_name() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1");
    underTest.insertParam(dbSession, activeRule, activeRuleParam1);
    underTest.insertParam(dbSession, activeRule, ActiveRuleParamDto.createFor(rule1Param2));
    dbSession.commit();

    assertThat(underTest.selectParamByKeyAndName(activeRule.getKey(), activeRuleParam1.getKey(), dbSession)).isNotNull();

    assertThat(underTest.selectParamByKeyAndName(activeRule.getKey(), "unknown", dbSession)).isNull();
    assertThat(underTest.selectParamByKeyAndName(ActiveRuleKey.of(profile2.getKey(), rule1.getKey()), "unknown", dbSession)).isNull();
  }

  @Test
  public void select_all_params() throws Exception {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    underTest.insertParam(dbSession, activeRule1, ActiveRuleParamDto.createFor(rule1Param1));
    underTest.insertParam(dbSession, activeRule1, ActiveRuleParamDto.createFor(rule1Param2));

    ActiveRuleDto activeRule2 = createFor(profile1, rule2).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule2);
    underTest.insertParam(dbSession, activeRule2, ActiveRuleParamDto.createFor(rule2Param1));
    dbSession.commit();

    assertThat(underTest.selectAllParams(dbSession)).hasSize(3);
  }

  @Test
  public void insert_param() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1");
    underTest.insertParam(dbSession, activeRule, activeRuleParam1);
    dbSession.commit();

    ActiveRuleParamDto result = underTest.selectParamByKeyAndName(activeRule.getKey(), activeRuleParam1.getKey(), dbSession);
    assertThat(result).isNotNull();

    assertThat(result.getId()).isEqualTo(activeRuleParam1.getId());
    assertThat(result.getKey()).isEqualTo(activeRuleParam1.getKey());
    assertThat(result.getActiveRuleId()).isEqualTo(activeRule.getId());
    assertThat(result.getRulesParameterId()).isEqualTo(rule1Param1.getId());
    assertThat(result.getValue()).isEqualTo("activeValue1");
  }

  @Test
  public void fail_to_insert_param_when_active_rule_id_is_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("ActiveRule is not persisted");

    underTest.insertParam(dbTester.getSession(),
      createFor(profile1, rule1).setId(null),
      ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1"));
  }

  @Test
  public void fail_to_insert_param_when_active_rule_param_id_is_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("ActiveRuleParam is already persisted");

    underTest.insertParam(dbTester.getSession(),
      createFor(profile1, rule1).setId(100),
      ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1").setId(100));
  }

  @Test
  public void fail_to_insert_param_when_id_is_not_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Rule param is not persisted");

    underTest.insertParam(dbTester.getSession(),
      createFor(profile1, rule1).setId(100),
      ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1").setRulesParameterId(null));
  }

  @Test
  public void update_param() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue");
    underTest.insertParam(dbSession, activeRule, activeRuleParam1);
    dbSession.commit();

    underTest.updateParam(dbSession, activeRule, activeRuleParam1.setValue("updatedActiveValue"));
    dbSession.commit();

    ActiveRuleParamDto result = underTest.selectParamByKeyAndName(activeRule.getKey(), activeRuleParam1.getKey(), dbSession);
    assertThat(result.getId()).isEqualTo(activeRuleParam1.getId());
    assertThat(result.getKey()).isEqualTo(activeRuleParam1.getKey());
    assertThat(result.getActiveRuleId()).isEqualTo(activeRule.getId());
    assertThat(result.getRulesParameterId()).isEqualTo(rule1Param1.getId());
    assertThat(result.getValue()).isEqualTo("updatedActiveValue");
  }

  @Test
  public void delete_param() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1");
    underTest.insertParam(dbSession, activeRule, activeRuleParam1);
    dbSession.commit();

    underTest.deleteParam(dbSession, activeRule, activeRuleParam1);
    dbSession.commit();

    assertThat(underTest.selectParamByKeyAndName(activeRule.getKey(), activeRuleParam1.getKey(), dbSession)).isNull();
  }

  @Test
  public void delete_param_by_key_and_name() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1");
    underTest.insertParam(dbSession, activeRule, activeRuleParam1);
    dbSession.commit();

    underTest.deleteParamByKeyAndName(dbSession, activeRule.getKey(), rule1Param1.getName());
    dbSession.commit();

    assertThat(underTest.selectParamByKeyAndName(activeRule.getKey(), activeRuleParam1.getKey(), dbSession)).isNull();
  }

  @Test
  public void does_not_fail_to_delete_param_by_key_and_name_when_active_rule_does_not_exist() throws Exception {
    underTest.deleteParamByKeyAndName(dbSession, ActiveRuleKey.of(profile1.getKey(), rule1.getKey()), rule1Param1.getName());
  }

  @Test
  public void does_not_fail_to_delete_param_by_key_and_name_when_active_rule_param_does_not_exist() throws Exception {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1");
    underTest.insertParam(dbSession, activeRule, activeRuleParam1);
    dbSession.commit();

    underTest.deleteParamByKeyAndName(dbSession, activeRule.getKey(), "unknown");
  }

  @Test
  public void delete_param_by_rule_param() throws Exception {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1");
    underTest.insertParam(dbSession, activeRule1, activeRuleParam1);

    ActiveRuleDto activeRule2 = createFor(profile2, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule2);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue2");
    underTest.insertParam(dbSession, activeRule2, activeRuleParam2);

    dbSession.commit();

    underTest.deleteParamsByRuleParam(dbSession, rule1, rule1Param1.getName());
    dbSession.commit();

    assertThat(underTest.selectParamByKeyAndName(activeRule1.getKey(), activeRuleParam1.getKey(), dbSession)).isNull();
    assertThat(underTest.selectParamByKeyAndName(activeRule2.getKey(), activeRuleParam2.getKey(), dbSession)).isNull();
  }

  @Test
  public void does_not_fail_to_delete_param_by_rule_param_when_active_param_name_not_found() throws Exception {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1");
    underTest.insertParam(dbSession, activeRule1, activeRuleParam1);

    ActiveRuleDto activeRule2 = createFor(profile2, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule2);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue2");
    underTest.insertParam(dbSession, activeRule2, activeRuleParam2);

    dbSession.commit();

    underTest.deleteParamsByRuleParam(dbSession, rule1, "unknown");
  }
}
