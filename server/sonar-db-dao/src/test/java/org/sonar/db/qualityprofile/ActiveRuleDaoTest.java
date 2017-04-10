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
package org.sonar.db.qualityprofile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.After;
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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
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

  private static final long NOW = 10_000_000L;

  private OrganizationDto organization = OrganizationTesting.newOrganizationDto();

  private QualityProfileDto profile1 = QualityProfileDto.createFor("qp1").setOrganizationUuid(organization.getUuid()).setName("QProfile1");
  private QualityProfileDto profile2 = QualityProfileDto.createFor("qp2").setOrganizationUuid(organization.getUuid()).setName("QProfile2");

  private RuleDefinitionDto rule1 = RuleTesting.newRule(RuleTesting.XOO_X1);
  private RuleDefinitionDto rule2 = RuleTesting.newRule(RuleTesting.XOO_X2);
  private RuleDefinitionDto rule3 = RuleTesting.newRule(RuleTesting.XOO_X3);

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
    dbTester.rules().insert(rule1);
    dbTester.rules().insert(rule2);
    dbTester.rules().insert(rule3);

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

  @After
  public void tearDown() {
    // minor optimization, no need to commit pending operations
    dbSession.rollback();
  }

  @Test
  public void select_by_key() {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule);

    assertThat(underTest.selectByKey(dbSession, activeRule.getKey())).isPresent();
    assertThat(underTest.selectByKey(dbSession, ActiveRuleKey.of(profile2.getKey(), rule2.getKey()))).isAbsent();
  }

  @Test
  public void select_or_fail_by_key() {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule);

    assertThat(underTest.selectOrFailByKey(dbSession, activeRule.getKey())).isNotNull();

    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Active rule with key 'qp2:xoo:x2' does not exist");
    underTest.selectOrFailByKey(dbSession, ActiveRuleKey.of(profile2.getKey(), rule2.getKey()));
  }

  @Test
  public void select_by_rule() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    ActiveRuleDto activeRule2 = createFor(profile2, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    underTest.insert(dbTester.getSession(), activeRule2);
    dbSession.commit();

    assertThat(underTest.selectByRuleId(dbSession, rule1.getId())).extracting("key").containsOnly(activeRule1.getKey(), activeRule2.getKey());
    assertThat(underTest.selectByRuleId(dbSession, rule3.getId())).isEmpty();
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
  public void select_by_profile() {
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
    RuleDefinitionDto removedRule = RuleTesting.newRule(RuleKey.of("removed", "rule")).setStatus(RuleStatus.REMOVED);
    dbTester.rules().insert(removedRule);
    ActiveRuleDto activeRule = createFor(profile1, removedRule).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    dbSession.commit();

    assertThat(underTest.selectByProfileKey(dbSession, profile1.getKey())).isEmpty();
  }

  @Test
  public void insert() {
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
  public void fail_to_insert_when_profile_id_is_null() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Quality profile is not persisted (missing id)");

    underTest.insert(dbTester.getSession(), createFor(profile1, rule1).setProfileId(null));
  }

  @Test
  public void fail_to_insert_when_rule_id_is_null() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Rule is not persisted");

    underTest.insert(dbTester.getSession(), createFor(profile1, rule1).setRuleId(null));
  }

  @Test
  public void fail_to_insert_when_id_is_not_null() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("ActiveRule is already persisted");

    underTest.insert(dbTester.getSession(), createFor(profile1, rule1).setId(100));
  }

  @Test
  public void update() {
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
  public void fail_to_update_when_profile_id_is_null() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Quality profile is not persisted (missing id)");

    underTest.update(dbTester.getSession(), createFor(profile1, rule1).setId(100).setProfileId(null));
  }

  @Test
  public void fail_to_update_when_rule_id_is_null() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Rule is not persisted");

    underTest.update(dbTester.getSession(), createFor(profile1, rule1).setId(100).setRuleId(null));
  }

  @Test
  public void fail_to_update_when_id_is_null() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("ActiveRule is not persisted");

    underTest.update(dbTester.getSession(), createFor(profile1, rule1).setId(null));
  }

  @Test
  public void delete() {
    ActiveRuleDto activeRule = createFor(profile1, rule1)
      .setSeverity(BLOCKER)
      .setInheritance(INHERITED)
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);
    underTest.insert(dbSession, activeRule);

    underTest.delete(dbSession, activeRule.getKey());

    assertThat(underTest.selectByKey(dbSession, ActiveRuleKey.of(profile1.getKey(), rule1.getKey()))).isAbsent();
  }

  @Test
  public void delete_does_not_fail_when_active_rule_does_not_exist() {
    underTest.delete(dbSession, ActiveRuleKey.of(profile1.getKey(), rule1.getKey()));
  }

  @Test
  public void deleteByKeys_deletes_rows_from_table() {
    underTest.insert(dbSession, newRow(profile1, rule1));
    underTest.insert(dbSession, newRow(profile1, rule2));
    underTest.insert(dbSession, newRow(profile2, rule1));

    underTest.deleteByProfileKeys(dbSession, asList(profile1.getKey()));

    assertThat(dbTester.countRowsOfTable(dbSession, "active_rules")).isEqualTo(1);
    assertThat(underTest.selectByKey(dbSession, ActiveRuleKey.of(profile2.getKey(), rule1.getKey()))).isPresent();
  }

  @Test
  public void deleteByKeys_does_not_fail_when_profile_with_specified_key_does_not_exist() {
    underTest.insert(dbSession, newRow(profile1, rule1));

    underTest.deleteByProfileKeys(dbSession, asList("does_not_exist"));

    assertThat(dbTester.countRowsOfTable(dbSession, "active_rules")).isEqualTo(1);
  }

  private static ActiveRuleDto newRow(QualityProfileDto profile, RuleDefinitionDto rule) {
    return createFor(profile, rule).setSeverity(BLOCKER);
  }

  @Test
  public void select_params_by_active_rule_id() {
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
  public void select_params_by_active_rule_ids() {
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
  public void select_param_by_key_and_name() {
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
  public void select_all_params() {
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
  public void insert_param() {
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
  public void fail_to_insert_param_when_active_rule_id_is_null() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("ActiveRule is not persisted");

    underTest.insertParam(dbTester.getSession(),
      createFor(profile1, rule1).setId(null),
      ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1"));
  }

  @Test
  public void fail_to_insert_param_when_active_rule_param_id_is_null() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("ActiveRuleParam is already persisted");

    underTest.insertParam(dbTester.getSession(),
      createFor(profile1, rule1).setId(100),
      ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1").setId(100));
  }

  @Test
  public void fail_to_insert_param_when_id_is_not_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Rule param is not persisted");

    underTest.insertParam(dbTester.getSession(),
      createFor(profile1, rule1).setId(100),
      ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1").setRulesParameterId(null));
  }

  @Test
  public void update_param() {
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
  public void deleteParam_deletes_rows_by_id() {
    ActiveRuleDto activeRule = newRow(profile1, rule1);
    underTest.insert(dbSession, activeRule);
    ActiveRuleParamDto param = ActiveRuleParamDto.createFor(rule1Param1).setValue("foo");
    underTest.insertParam(dbSession, activeRule, param);

    underTest.deleteParam(dbSession, activeRule, param);

    assertThat(underTest.selectParamByKeyAndName(activeRule.getKey(), param.getKey(), dbSession)).isNull();
  }

  @Test
  public void deleteParametersByProfileKeys_deletes_rows_by_profile_keys() {
    ActiveRuleDto activeRuleInProfile1 = newRow(profile1, rule1);
    underTest.insert(dbSession, activeRuleInProfile1);
    ActiveRuleParamDto param1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("foo");
    underTest.insertParam(dbSession, activeRuleInProfile1, param1);
    ActiveRuleDto activeRuleInProfile2 = newRow(profile2, rule1);
    underTest.insert(dbSession, activeRuleInProfile2);
    ActiveRuleParamDto param2 = ActiveRuleParamDto.createFor(rule1Param1).setValue("bar");
    underTest.insertParam(dbSession, activeRuleInProfile2, param2);

    underTest.deleteParametersByProfileKeys(dbSession, asList(profile1.getKey(), "does_not_exist"));

    List<ActiveRuleParamDto> params = underTest.selectAllParams(dbSession);
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getActiveRuleId()).isEqualTo(activeRuleInProfile2.getId());
  }

  @Test
  public void deleteParametersByProfileKeys_does_nothing_if_keys_are_empty() {
    ActiveRuleDto activeRuleInProfile1 = newRow(profile1, rule1);
    underTest.insert(dbSession, activeRuleInProfile1);
    ActiveRuleParamDto param1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("foo");
    underTest.insertParam(dbSession, activeRuleInProfile1, param1);

    underTest.deleteParametersByProfileKeys(dbSession, emptyList());

    List<ActiveRuleParamDto> params = underTest.selectAllParams(dbSession);
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getActiveRuleId()).isEqualTo(activeRuleInProfile1.getId());
  }

  @Test
  public void deleteParamByKeyAndName_deletes_rows_by_key_and_name() {
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
  public void does_not_fail_to_delete_param_by_key_and_name_when_active_rule_does_not_exist() {
    underTest.deleteParamByKeyAndName(dbSession, ActiveRuleKey.of(profile1.getKey(), rule1.getKey()), rule1Param1.getName());
  }

  @Test
  public void does_not_fail_to_delete_param_by_key_and_name_when_active_rule_param_does_not_exist() {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1");
    underTest.insertParam(dbSession, activeRule, activeRuleParam1);
    dbSession.commit();

    underTest.deleteParamByKeyAndName(dbSession, activeRule.getKey(), "unknown");
  }

  @Test
  public void delete_param_by_rule_param() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1");
    underTest.insertParam(dbSession, activeRule1, activeRuleParam1);

    ActiveRuleDto activeRule2 = createFor(profile2, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule2);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue2");
    underTest.insertParam(dbSession, activeRule2, activeRuleParam2);

    dbSession.commit();

    underTest.deleteParamsByRuleParam(dbSession, rule1.getId(), rule1Param1.getName());
    dbSession.commit();

    assertThat(underTest.selectParamByKeyAndName(activeRule1.getKey(), activeRuleParam1.getKey(), dbSession)).isNull();
    assertThat(underTest.selectParamByKeyAndName(activeRule2.getKey(), activeRuleParam2.getKey(), dbSession)).isNull();
  }

  @Test
  public void does_not_fail_to_delete_param_by_rule_param_when_active_param_name_not_found() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule1);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1");
    underTest.insertParam(dbSession, activeRule1, activeRuleParam1);

    ActiveRuleDto activeRule2 = createFor(profile2, rule1).setSeverity(BLOCKER);
    underTest.insert(dbTester.getSession(), activeRule2);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue2");
    underTest.insertParam(dbSession, activeRule2, activeRuleParam2);

    dbSession.commit();

    underTest.deleteParamsByRuleParam(dbSession, rule1.getId(), "unknown");
  }

  @Test
  public void test_countActiveRulesByProfileKey_for_a_specified_organization() {
    dbTester.qualityProfiles().activateRule(profile1, rule1);
    dbTester.qualityProfiles().activateRule(profile1, rule2);
    dbTester.qualityProfiles().activateRule(profile2, rule1);

    Map<String, Long> counts = underTest.countActiveRulesByProfileKey(dbSession, organization);

    assertThat(counts).containsOnly(
      entry(profile1.getKey(), 2L),
      entry(profile2.getKey(), 1L));
  }

  @Test
  public void countActiveRulesByProfileKey_returns_empty_map_if_organization_does_not_exist() {
    Map<String, Long> counts = underTest.countActiveRulesByProfileKey(dbSession, OrganizationTesting.newOrganizationDto());

    assertThat(counts).isEmpty();
  }

  @Test
  public void countActiveRulesByProfileKey_returns_empty_map_if_profile_does_not_have_active_rules() {
    Map<String, Long> counts = underTest.countActiveRulesByProfileKey(dbSession, organization);

    assertThat(counts).isEmpty();
  }

  @Test
  public void test_countActiveRulesForRuleStatusByProfileKey_for_a_specified_organization() {
    RuleDefinitionDto betaRule1 = dbTester.rules().insertRule(RuleTesting.newRuleDto().setStatus(RuleStatus.BETA)).getDefinition();
    RuleDefinitionDto betaRule2 = dbTester.rules().insertRule(RuleTesting.newRuleDto().setStatus(RuleStatus.BETA)).getDefinition();
    dbTester.qualityProfiles().activateRule(profile1, rule1);
    dbTester.qualityProfiles().activateRule(profile2, betaRule1);
    dbTester.qualityProfiles().activateRule(profile2, betaRule2);

    Map<String, Long> counts = underTest.countActiveRulesForRuleStatusByProfileKey(dbSession, organization, RuleStatus.BETA);

    assertThat(counts).containsOnly(entry(profile2.getKey(), 2L));
  }

  @Test
  public void countActiveRulesForRuleStatusByProfileKey_returns_empty_map_if_organization_does_not_exist() {
    Map<String, Long> counts = underTest.countActiveRulesForRuleStatusByProfileKey(dbSession, OrganizationTesting.newOrganizationDto(), RuleStatus.READY);

    assertThat(counts).isEmpty();
  }

  @Test
  public void countActiveRulesForRuleStatusByProfileKey_returns_empty_map_if_profile_does_not_have_rules_with_specified_status() {
    Map<String, Long> counts = underTest.countActiveRulesForRuleStatusByProfileKey(dbSession, organization, RuleStatus.DEPRECATED);

    assertThat(counts).isEmpty();
  }

  @Test
  public void test_countActiveRulesForInheritanceByProfileKey_for_a_specified_organization() {
    dbTester.qualityProfiles().activateRule(profile1, rule1);
    dbTester.qualityProfiles().activateRule(profile2, rule1, ar -> ar.setInheritance(ActiveRuleDto.OVERRIDES));
    dbTester.qualityProfiles().activateRule(profile2, rule2, ar -> ar.setInheritance(ActiveRuleDto.INHERITED));

    Map<String, Long> counts = underTest.countActiveRulesForInheritanceByProfileKey(dbSession, organization, ActiveRuleDto.OVERRIDES);

    assertThat(counts).containsOnly(entry(profile2.getKey(), 1L));
  }

  @Test
  public void countActiveRulesForInheritanceByProfileKey_returns_empty_map_if_organization_does_not_exist() {
    Map<String, Long> counts = underTest.countActiveRulesForInheritanceByProfileKey(dbSession, OrganizationTesting.newOrganizationDto(), ActiveRuleDto.OVERRIDES);

    assertThat(counts).isEmpty();
  }

  @Test
  public void countActiveRulesForInheritanceByProfileKey_returns_empty_map_if_profile_does_not_have_rules_with_specified_status() {
    Map<String, Long> counts = underTest.countActiveRulesForInheritanceByProfileKey(dbSession, organization, ActiveRuleDto.OVERRIDES);

    assertThat(counts).isEmpty();
  }
}
