/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.rule.RuleStatus.BETA;
import static org.sonar.api.rule.RuleStatus.READY;
import static org.sonar.api.rule.RuleStatus.REMOVED;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.db.qualityprofile.ActiveRuleDto.INHERITED;
import static org.sonar.db.qualityprofile.ActiveRuleDto.OVERRIDES;
import static org.sonar.db.qualityprofile.ActiveRuleDto.createFor;
import static org.sonar.db.rule.RuleDto.deserializeSecurityStandardsString;

class ActiveRuleDaoIT {

  private static final long NOW = 10_000_000L;

  static {
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> map = new LinkedHashMap<>();
    map.put(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.INFO);
    map.put(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.HIGH);
    IMPACTS = map;
  }
  public static final Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> IMPACTS;

  private QProfileDto profile1;
  private QProfileDto profile2;
  private RuleDto rule1;
  private RuleDto rule2;
  private RuleDto rule3;
  private RuleDto removedRule;
  private RuleParamDto rule1Param1;
  private RuleParamDto rule1Param2;
  private RuleParamDto rule2Param1;
  private final System2 system = new TestSystem2().setNow(NOW);

  @RegisterExtension
  DbTester db = DbTester.create(system);

  private final DbSession dbSession = db.getSession();

  private final ActiveRuleDao underTest = db.getDbClient().activeRuleDao();

  @BeforeEach
  void setUp() {
    profile1 = db.qualityProfiles().insert();
    profile2 = db.qualityProfiles().insert();
    rule1 = db.rules().insert(r -> r.replaceAllDefaultImpacts(
      List.of(new ImpactDto(SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER), new ImpactDto(RELIABILITY, LOW))));
    rule2 = db.rules().insert();
    rule3 = db.rules().insert();
    removedRule = db.rules().insert(r -> r.setStatus(REMOVED));

    rule1Param1 = new RuleParamDto()
      .setName("param1")
      .setDefaultValue("value1")
      .setType(RuleParamType.STRING.toString());
    rule1Param1 = db.rules().insertRuleParam(rule1);
    rule1Param2 = db.rules().insertRuleParam(rule1);
    rule2Param1 = db.rules().insertRuleParam(rule2);
  }

  @Test
  void selectAllParamsByProfileUuids() {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1);
    underTest.insertParam(dbSession, activeRule, activeRuleParam1);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule1Param2);
    underTest.insertParam(dbSession, activeRule, activeRuleParam2);
    dbSession.commit();

    assertThat(underTest.selectAllParamsByProfileUuids(dbSession, List.of(profile1.getKee()))).hasSize(2);
  }

  @Test
  void selectByKey() {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule);

    assertThat(underTest.selectByKey(dbSession, activeRule.getKey())).isPresent();
    assertThat(underTest.selectByKey(dbSession, ActiveRuleKey.of(profile2, rule2.getKey()))).isEmpty();
  }

  @Test
  void selectByRuleUuid() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    ActiveRuleDto activeRule2 = createFor(profile2, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule1);
    underTest.insert(dbSession, activeRule2);
    dbSession.commit();

    assertThat(underTest.selectByOrgRuleUuid(dbSession, rule1.getUuid())).extracting("key")
      .containsOnly(activeRule1.getKey(), activeRule2.getKey());
    assertThat(underTest.selectByOrgRuleUuid(dbSession, rule3.getUuid())).isEmpty();
  }

  @Test
  void selectByRuleIds() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER)
      .setImpacts(Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW));
    ActiveRuleDto activeRule2 = createFor(profile1, rule2).setSeverity(BLOCKER)
      .setImpacts(Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW));
    ActiveRuleDto activeRule3 = createFor(profile2, rule1).setSeverity(BLOCKER)
      .setImpacts(Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW));
    underTest.insert(dbSession, activeRule1);
    underTest.insert(dbSession, activeRule2);
    underTest.insert(dbSession, activeRule3);
    dbSession.commit();

    assertThat(underTest.selectByRuleUuids(dbSession, singletonList(rule1.getUuid())))
      .extracting("key").containsOnly(activeRule1.getKey(), activeRule3.getKey());
    assertThat(underTest.selectByRuleUuids(dbSession, newArrayList(rule1.getUuid(), rule2.getUuid())))
      .extracting("key").containsOnly(activeRule1.getKey(), activeRule2.getKey(), activeRule3.getKey());
  }

  @Test
  void selectByProfile() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    ActiveRuleDto activeRule2 = createFor(profile1, rule2).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule1);
    underTest.insert(dbSession, activeRule2);

    List<OrgActiveRuleDto> result = underTest.selectByProfile(dbSession, profile1);
    assertThat(result)
      .hasSize(2)
      .extracting(OrgActiveRuleDto::getOrgProfileUuid, OrgActiveRuleDto::getProfileUuid)
      .containsOnly(tuple(profile1.getKee(), profile1.getRulesProfileUuid()));

    assertThat(underTest.selectByProfile(dbSession, profile2)).isEmpty();
  }

  @Test
  void selectPrioritizedRules() {
    db.qualityProfiles().activateRule(profile1, rule1, r -> r.setPrioritizedRule(true));
    assertThat(underTest.selectPrioritizedRules(dbSession, Set.of(profile1.getKee()))).contains(rule1.getKey());
  }

  @Test
  void selectByProfileUuid_ignores_removed_rules() {
    ActiveRuleDto activeRule = createFor(profile1, removedRule).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule);

    assertThat(underTest.selectByProfile(dbSession, profile1)).isEmpty();
  }

  @Test
  void selectByProfileUuids_ignores_removed_rules() {
    ActiveRuleDto activeRule = createFor(profile1, removedRule).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule);

    assertThat(underTest.selectByProfileUuids(dbSession, List.of(profile1.getKee()))).isEmpty();
  }

  @Test
  void selectByProfileUuids_exclude_other_profiles() {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule);
    ActiveRuleDto activeRule2 = createFor(profile2, rule2).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule2);

    assertThat(underTest.selectByProfileUuids(dbSession, List.of(profile1.getKee()))).hasSize(1);
  }

  @Test
  void selectByProfileUuids_returns_all_fields() {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule);

    List<OrgActiveRuleDto> actualActiveRule = underTest.selectByProfileUuids(dbSession, List.of(profile1.getKee()));
    // verify data from the "rules" table
    assertThat(actualActiveRule)
      .extracting(OrgActiveRuleDto::getRuleKey, org -> deserializeSecurityStandardsString(org.getSecurityStandards()), OrgActiveRuleDto::isExternal, OrgActiveRuleDto::getName,
        OrgActiveRuleDto::getConfigKey, OrgActiveRuleDto::getTemplateUuid, OrgActiveRuleDto::getLanguage)
      .containsExactly(
        tuple(rule1.getKey(), rule1.getSecurityStandards(), rule1.isExternal(), rule1.getName(), rule1.getConfigKey(), rule1.getTemplateUuid(), rule1.getLanguage()));

    // verify data from "active_rules" table
    assertThat(actualActiveRule)
      .extracting(OrgActiveRuleDto::getUuid, OrgActiveRuleDto::getProfileUuid, OrgActiveRuleDto::getRuleUuid, OrgActiveRuleDto::getSeverity, OrgActiveRuleDto::getInheritance,
        OrgActiveRuleDto::isPrioritizedRule, OrgActiveRuleDto::getCreatedAt, OrgActiveRuleDto::getUpdatedAt)
      .containsExactly(tuple(activeRule.getUuid(), activeRule.getProfileUuid(), activeRule.getRuleUuid(), activeRule.getSeverity(), activeRule.getInheritance(),
        activeRule.isPrioritizedRule(), activeRule.getCreatedAt(), activeRule.getUpdatedAt()));

    // verify data from "rules_profiles" and "org_qprofiles"
    assertThat(actualActiveRule)
      .extracting(o -> o.getKey().getRuleProfileUuid(), OrgActiveRuleDto::getOrgProfileUuid)
      .containsExactly(tuple(activeRule.getKey().getRuleProfileUuid(), profile1.getKee()));
  }

  @Test
  void selectByTypeAndProfileUuids() {
    RuleDto rule1 = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY.getDbConstant()));
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);

    underTest.insert(dbSession, activeRule1);

    assertThat(underTest.selectByTypeAndProfileUuids(dbSession, singletonList(RuleType.VULNERABILITY.getDbConstant()),
      singletonList(profile1.getKee())))
      .extracting(OrgActiveRuleDto::getOrgProfileUuid, OrgActiveRuleDto::getRuleUuid)
      .contains(tuple(profile1.getKee(), rule1.getUuid()));
  }

  @Test
  void selectByTypeAndProfileUuids_ignores_rules_in_other_profiles() {
    RuleDto rule1 = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY.getDbConstant()));
    ActiveRuleDto activeRule1 = createFor(profile2, rule1).setSeverity(BLOCKER);

    underTest.insert(dbSession, activeRule1);

    assertThat(underTest.selectByTypeAndProfileUuids(dbSession, singletonList(RuleType.VULNERABILITY.getDbConstant()),
      singletonList(profile1.getKee())))
      .isEmpty();
  }

  @Test
  void selectByTypeAndProfileUuids_ignores_rules_with_another_rule_type() {
    RuleDto rule1 = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY.getDbConstant()));
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);

    underTest.insert(dbSession, activeRule1);

    assertThat(
      underTest.selectByTypeAndProfileUuids(dbSession,
        singletonList(RuleType.VULNERABILITY.getDbConstant()),
        singletonList(profile1.getKee())))
      .extracting(OrgActiveRuleDto::getOrgProfileUuid, OrgActiveRuleDto::getRuleUuid)
      .contains(tuple(profile1.getKee(), rule1.getUuid()));

    assertThat(
      underTest.selectByTypeAndProfileUuids(dbSession,
        asList(RuleType.CODE_SMELL.getDbConstant(), RuleType.SECURITY_HOTSPOT.getDbConstant(), RuleType.BUG.getDbConstant()),
        singletonList(profile1.getKee())))
      .isEmpty();
  }

  @Test
  void selectByRuleProfile() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    ActiveRuleDto activeRule2 = createFor(profile1, rule2).setSeverity(MAJOR);
    underTest.insert(dbSession, activeRule1);
    underTest.insert(dbSession, activeRule2);

    List<ActiveRuleDto> result = underTest.selectByRuleProfile(dbSession, RulesProfileDto.from(profile1));
    assertThat(result)
      .hasSize(2)
      .extracting(ActiveRuleDto::getProfileUuid, ActiveRuleDto::getRuleKey, ActiveRuleDto::getSeverityString)
      .containsOnly(tuple(profile1.getRulesProfileUuid(), rule1.getKey(), BLOCKER), tuple(profile1.getRulesProfileUuid(),
        rule2.getKey(),
        MAJOR));

    assertThat(underTest.selectByProfile(dbSession, profile2)).isEmpty();
  }

  @Test
  void selectByRulesAndRuleProfileUuids() {
    ActiveRuleDto rule1P1 = createFor(profile1, rule1).setSeverity(MAJOR);
    ActiveRuleDto rule2P1 = createFor(profile1, rule2).setSeverity(MAJOR);
    ActiveRuleDto rule1P2 = createFor(profile2, rule1).setSeverity(MAJOR);
    underTest.insert(dbSession, rule1P1);
    underTest.insert(dbSession, rule2P1);
    underTest.insert(dbSession, rule1P2);

    // empty rules
    Collection<ActiveRuleDto> result = underTest.selectByRulesAndRuleProfileUuids(dbSession, emptyList(),
      singletonList(profile1.getRulesProfileUuid()));
    assertThat(result).isEmpty();

    // empty profiles
    result = underTest.selectByRulesAndRuleProfileUuids(dbSession, singletonList(rule1.getUuid()), emptyList());
    assertThat(result).isEmpty();

    // match
    result = underTest.selectByRulesAndRuleProfileUuids(dbSession, singletonList(rule1.getUuid()), asList(profile1.getRulesProfileUuid(),
      profile2.getRulesProfileUuid()));
    assertThat(result)
      .extracting(ActiveRuleDto::getUuid)
      .containsExactlyInAnyOrder(rule1P1.getUuid(), rule1P2.getUuid());

    result = underTest.selectByRulesAndRuleProfileUuids(dbSession, asList(rule1.getUuid(), rule2.getUuid()),
      asList(profile1.getRulesProfileUuid(), profile2.getRulesProfileUuid()));
    assertThat(result)
      .extracting(ActiveRuleDto::getUuid)
      .containsExactlyInAnyOrder(rule1P1.getUuid(), rule1P2.getUuid(), rule2P1.getUuid());

    // do not match
    result = underTest.selectByRulesAndRuleProfileUuids(dbSession, singletonList(rule3.getUuid()), asList(profile1.getRulesProfileUuid(),
      profile2.getRulesProfileUuid()));
    assertThat(result).isEmpty();

    result = underTest.selectByRulesAndRuleProfileUuids(dbSession, singletonList(rule1.getUuid()), singletonList("unknown"));
    assertThat(result).isEmpty();
  }

  @Test
  void insert() {
    ActiveRuleDto activeRule = createFor(profile1, rule1)
      .setSeverity(BLOCKER)
      .setInheritance(INHERITED)
      .setImpacts(IMPACTS)
      .setIsExternal(false)
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);
    underTest.insert(dbSession, activeRule);
    dbSession.commit();

    ActiveRuleDto result = underTest.selectByKey(dbSession, activeRule.getKey()).get();
    assertThat(result.getUuid()).isEqualTo(activeRule.getUuid());
    assertThat(result.getKey()).isEqualTo(ActiveRuleKey.of(profile1, rule1.getKey()));
    assertThat(result.getRuleUuid()).isEqualTo(rule1.getUuid());
    assertThat(result.getProfileUuid()).isEqualTo(profile1.getRulesProfileUuid());
    assertThat(result.getSeverityString()).isEqualTo(BLOCKER);
    assertThat(result.getInheritance()).isEqualTo(INHERITED);
    assertThat(result.getImpacts()).isEqualTo(IMPACTS);
    assertThat(result.isExternal()).isFalse();
    assertThat(result.getCreatedAt()).isEqualTo(1000L);
    assertThat(result.getUpdatedAt()).isEqualTo(2000L);
  }

  @Test
  void fail_to_insert_when_profile_id_is_null() {
    assertThatThrownBy(() -> underTest.insert(dbSession, createFor(profile1, rule1).setProfileUuid(null)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Quality profile is not persisted (missing id)");
  }

  @Test
  void fail_to_insert_when_rule_uuid_is_null() {
    assertThatThrownBy(() -> underTest.insert(dbSession, createFor(profile1, rule1).setRuleUuid(null)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Rule is not persisted");
  }

  @Test
  void fail_to_insert_when_uuid_is_not_null() {
    assertThatThrownBy(() -> underTest.insert(dbSession, createFor(profile1, rule1).setUuid("uuid")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("ActiveRule is already persisted");
  }

  @Test
  void update() {
    ActiveRuleDto activeRule = createFor(profile1, rule1)
      .setSeverity(BLOCKER)
      .setInheritance(INHERITED)
      .setImpactsString("{\"RELIABILITY\":\"INFO\"}")
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);
    underTest.insert(dbSession, activeRule);
    dbSession.commit();

    ActiveRuleDto activeRuleUpdated = activeRule
      .setSeverity(MAJOR)
      .setInheritance(OVERRIDES)
      .setImpacts(IMPACTS)
      // created at should not be updated
      .setCreatedAt(3000L)
      .setUpdatedAt(4000L);
    underTest.update(dbSession, activeRuleUpdated);
    dbSession.commit();

    ActiveRuleDto result = underTest.selectByKey(dbSession, ActiveRuleKey.of(profile1, rule1.getKey())).get();
    assertThat(result.getUuid()).isEqualTo(activeRule.getUuid());
    assertThat(result.getKey()).isEqualTo(ActiveRuleKey.of(profile1, rule1.getKey()));
    assertThat(result.getRuleUuid()).isEqualTo(rule1.getUuid());
    assertThat(result.getProfileUuid()).isEqualTo(profile1.getRulesProfileUuid());
    assertThat(result.getSeverityString()).isEqualTo(MAJOR);
    assertThat(result.getInheritance()).isEqualTo(OVERRIDES);
    assertThat(result.getImpacts()).isEqualTo(IMPACTS);
    assertThat(result.getCreatedAt()).isEqualTo(1000L);
    assertThat(result.getUpdatedAt()).isEqualTo(4000L);
  }

  @Test
  void fail_to_update_when_profile_id_is_null() {
    ActiveRuleDto ruleDto = createFor(profile1, rule1).setUuid("uuid").setProfileUuid(null);
    assertThatThrownBy(() -> underTest.update(dbSession, ruleDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Quality profile is not persisted (missing id)");
  }

  @Test
  void fail_to_update_when_rule_id_is_null() {
    ActiveRuleDto ruleDto = createFor(profile1, rule1).setUuid("uuid").setRuleUuid(null);
    assertThatThrownBy(() -> underTest.update(dbSession, ruleDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Rule is not persisted");
  }

  @Test
  void fail_to_update_when_id_is_null() {
    ActiveRuleDto ruleDto = createFor(profile1, rule1).setUuid(null);
    assertThatThrownBy(() -> underTest.update(dbSession, ruleDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("ActiveRule is not persisted");
  }

  @Test
  void delete() {
    ActiveRuleDto activeRule = createFor(profile1, rule1)
      .setSeverity(BLOCKER)
      .setInheritance(INHERITED)
      .setCreatedAt(1000L)
      .setUpdatedAt(2000L);
    underTest.insert(dbSession, activeRule);

    underTest.delete(dbSession, activeRule.getKey());

    assertThat(underTest.selectByKey(dbSession, ActiveRuleKey.of(profile1, rule1.getKey()))).isEmpty();
  }

  @Test
  void delete_does_not_fail_when_active_rule_does_not_exist() {
    assertThatNoException().isThrownBy(() -> underTest.delete(dbSession, ActiveRuleKey.of(profile1, rule1.getKey())));
  }

  @Test
  void deleteByRuleProfileUuids_deletes_rows_from_table() {
    underTest.insert(dbSession, newRow(profile1, rule1));
    underTest.insert(dbSession, newRow(profile1, rule2));
    underTest.insert(dbSession, newRow(profile2, rule1));

    underTest.deleteByRuleProfileUuids(dbSession, singletonList(profile1.getRulesProfileUuid()));

    assertThat(db.countRowsOfTable(dbSession, "active_rules")).isOne();
    assertThat(underTest.selectByKey(dbSession, ActiveRuleKey.of(profile2, rule1.getKey()))).isPresent();
  }

  @Test
  void deleteByRuleProfileUuids_does_not_fail_when_rules_profile_with_specified_key_does_not_exist() {
    underTest.insert(dbSession, newRow(profile1, rule1));

    underTest.deleteByRuleProfileUuids(dbSession, singletonList("does_not_exist"));

    assertThat(db.countRowsOfTable(dbSession, "active_rules")).isOne();
  }

  @Test
  void deleteByIds() {
    ActiveRuleDto ar1 = underTest.insert(dbSession, newRow(profile1, rule1));
    ActiveRuleDto ar2 = underTest.insert(dbSession, newRow(profile1, rule2));
    ActiveRuleDto ar3 = underTest.insert(dbSession, newRow(profile2, rule1));

    underTest.deleteByUuids(dbSession, asList(ar1.getUuid(), ar3.getUuid()));

    assertThat(db.countRowsOfTable(dbSession, "active_rules")).isOne();
    assertThat(underTest.selectByProfile(dbSession, profile1))
      .extracting(ActiveRuleDto::getUuid)
      .containsExactly(ar2.getUuid());
  }

  @Test
  void deleteByIds_does_nothing_if_empty_list_of_ids() {
    underTest.insert(dbSession, newRow(profile1, rule1));

    underTest.deleteByUuids(dbSession, emptyList());

    assertThat(db.countRowsOfTable(dbSession, "active_rules")).isOne();
  }

  private static ActiveRuleDto newRow(QProfileDto profile, RuleDto rule) {
    return createFor(profile, rule).setSeverity(BLOCKER);
  }

  @Test
  void select_params_by_active_rule_id() {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1);
    underTest.insertParam(dbSession, activeRule, activeRuleParam1);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule1Param2);
    underTest.insertParam(dbSession, activeRule, activeRuleParam2);
    dbSession.commit();

    assertThat(underTest.selectParamsByActiveRuleUuid(dbSession, activeRule.getUuid())).hasSize(2);
  }

  @Test
  void select_params_by_active_rule_ids() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule1);
    underTest.insertParam(dbSession, activeRule1, ActiveRuleParamDto.createFor(rule1Param1));
    underTest.insertParam(dbSession, activeRule1, ActiveRuleParamDto.createFor(rule1Param2));

    ActiveRuleDto activeRule2 = createFor(profile1, rule2).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule2);
    underTest.insertParam(dbSession, activeRule2, ActiveRuleParamDto.createFor(rule2Param1));
    dbSession.commit();

    assertThat(underTest.selectParamsByActiveRuleUuids(dbSession, asList(activeRule1.getUuid(), activeRule2.getUuid()))).hasSize(3);
  }

  @Test
  void insertParam() {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(Severity.CRITICAL);
    underTest.insert(dbSession, activeRule);

    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(rule1Param1).setValue("foo");
    underTest.insertParam(dbSession, activeRule, activeRuleParam);

    List<ActiveRuleParamDto> reloaded = underTest.selectParamsByActiveRuleUuid(dbSession, activeRule.getUuid());
    assertThat(reloaded).hasSize(1);
    assertThat(reloaded.get(0))
      .matches(p -> Objects.equals(p.getUuid(), activeRuleParam.getUuid()))
      .matches(p -> p.getKey().equals(activeRuleParam.getKey()))
      .matches(p -> p.getActiveRuleUuid().equals(activeRule.getUuid()))
      .matches(p -> p.getRulesParameterUuid().equals(rule1Param1.getUuid()))
      .matches(p -> p.getValue().equals("foo"));
  }

  @Test
  void insertParam_fails_when_active_rule_id_is_null() {
    assertThatThrownBy(() -> {
      underTest.insertParam(dbSession,
        createFor(profile1, rule1).setUuid(null),
        ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1"));
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("ActiveRule is not persisted");
  }

  @Test
  void insertParam_fails_when_active_rule_param_id_is_null() {
    assertThatThrownBy(() -> {
      underTest.insertParam(dbSession,
        createFor(profile1, rule1).setUuid("uuid"),
        ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1").setUuid("uuid-1"));
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("ActiveRuleParam is already persisted");
  }

  @Test
  void insertParam_fails_when_uuid_is_not_null() {
    assertThatThrownBy(() -> {
      underTest.insertParam(dbSession,
        createFor(profile1, rule1).setUuid("uuid"),
        ActiveRuleParamDto.createFor(rule1Param1).setValue("activeValue1").setRulesParameterUuid(null));
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Rule param is not persisted");
  }

  @Test
  void updateParam() {
    ActiveRuleDto activeRule = createFor(profile1, rule1).setSeverity(Severity.CRITICAL);
    underTest.insert(dbSession, activeRule);
    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(rule1Param1).setValue("foo");
    underTest.insertParam(dbSession, activeRule, activeRuleParam);

    underTest.updateParam(dbSession, activeRuleParam.setValue("bar"));

    List<ActiveRuleParamDto> reloaded = underTest.selectParamsByActiveRuleUuid(dbSession, activeRule.getUuid());
    assertThat(reloaded).hasSize(1);
    assertThat(reloaded.get(0))
      .matches(p -> Objects.equals(p.getUuid(), activeRuleParam.getUuid()))
      .matches(p -> p.getKey().equals(activeRuleParam.getKey()))
      .matches(p -> p.getActiveRuleUuid().equals(activeRule.getUuid()))
      .matches(p -> p.getRulesParameterUuid().equals(rule1Param1.getUuid()))
      .matches(p -> p.getValue().equals("bar"));
  }

  @Test
  void deleteParam_deletes_rows_by_id() {
    ActiveRuleDto activeRule = newRow(profile1, rule1);
    underTest.insert(dbSession, activeRule);
    ActiveRuleParamDto param = ActiveRuleParamDto.createFor(rule1Param1).setValue("foo");
    underTest.insertParam(dbSession, activeRule, param);

    underTest.deleteParam(dbSession, param);

    assertThat(underTest.selectParamsByActiveRuleUuid(dbSession, activeRule.getUuid())).isEmpty();
  }

  @Test
  void deleteParametersByRuleProfileUuids_deletes_rows_by_rule_profile_uuids() {
    ActiveRuleDto activeRuleInProfile1 = newRow(profile1, rule1);
    underTest.insert(dbSession, activeRuleInProfile1);
    ActiveRuleParamDto param1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("foo");
    underTest.insertParam(dbSession, activeRuleInProfile1, param1);
    ActiveRuleDto activeRuleInProfile2 = newRow(profile2, rule1);
    underTest.insert(dbSession, activeRuleInProfile2);
    ActiveRuleParamDto param2 = ActiveRuleParamDto.createFor(rule1Param1).setValue("bar");
    underTest.insertParam(dbSession, activeRuleInProfile2, param2);

    underTest.deleteParametersByRuleProfileUuids(dbSession, asList(profile1.getRulesProfileUuid(), "does_not_exist"));

    assertThat(underTest.selectParamsByActiveRuleUuid(dbSession, activeRuleInProfile1.getUuid())).isEmpty();
    assertThat(underTest.selectParamsByActiveRuleUuid(dbSession, activeRuleInProfile2.getUuid()))
      .extracting(ActiveRuleParamDto::getKey, ActiveRuleParamDto::getValue)
      .containsExactly(tuple(rule1Param1.getName(), "bar"));
  }

  @Test
  void deleteParametersByRuleProfileUuids_does_nothing_if_keys_are_empty() {
    ActiveRuleDto activeRuleInProfile1 = newRow(profile1, rule1);
    underTest.insert(dbSession, activeRuleInProfile1);
    ActiveRuleParamDto param1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("foo");
    underTest.insertParam(dbSession, activeRuleInProfile1, param1);

    underTest.deleteParametersByRuleProfileUuids(dbSession, emptyList());

    assertThat(underTest.selectParamsByActiveRuleUuid(dbSession, activeRuleInProfile1.getUuid()))
      .hasSize(1);
  }

  @Test
  void deleteParamsByRuleParam() {
    ActiveRuleDto activeRule1 = createFor(profile1, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule1);
    ActiveRuleParamDto activeRuleParam1 = ActiveRuleParamDto.createFor(rule1Param1).setValue("foo");
    underTest.insertParam(dbSession, activeRule1, activeRuleParam1);

    ActiveRuleDto activeRule2 = createFor(profile2, rule1).setSeverity(BLOCKER);
    underTest.insert(dbSession, activeRule2);
    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(rule1Param1).setValue("bar");
    underTest.insertParam(dbSession, activeRule2, activeRuleParam2);

    List<String> activeRuleUuids = asList(activeRule1.getUuid(), activeRule2.getUuid());
    assertThat(underTest.selectParamsByActiveRuleUuids(dbSession, activeRuleUuids)).hasSize(2);

    underTest.deleteParamsByRuleParam(dbSession, rule1Param1);

    assertThat(underTest.selectParamsByActiveRuleUuids(dbSession, activeRuleUuids)).isEmpty();
  }

  @Test
  void deleteParamsByActiveRuleUuids() {
    ActiveRuleDto ar1 = underTest.insert(dbSession, newRow(profile1, rule1));
    ActiveRuleParamDto param = ActiveRuleParamDto.createFor(rule1Param1).setValue("foo");
    underTest.insertParam(dbSession, ar1, param);

    ActiveRuleDto ar2 = underTest.insert(dbSession, newRow(profile1, rule2));
    ActiveRuleParamDto param2 = ActiveRuleParamDto.createFor(rule2Param1).setValue("bar");
    underTest.insertParam(dbSession, ar2, param2);

    underTest.deleteParamsByActiveRuleUuids(dbSession, singletonList(ar1.getUuid()));

    assertThat(underTest.selectParamsByActiveRuleUuid(dbSession, ar1.getUuid())).isEmpty();
    assertThat(underTest.selectParamsByActiveRuleUuid(dbSession, ar2.getUuid())).hasSize(1);
  }

  @Test
  void countActiveRulesByQuery_filter_by_profiles() {
    db.qualityProfiles().activateRule(profile1, rule1);
    db.qualityProfiles().activateRule(profile1, rule2);
    db.qualityProfiles().activateRule(profile1, removedRule);
    db.qualityProfiles().activateRule(profile2, rule1);
    QProfileDto profileWithoutActiveRule = db.qualityProfiles().insert();

    ActiveRuleCountQuery.Builder builder = ActiveRuleCountQuery.builder();
    assertThat(underTest.countActiveRulesByQuery(dbSession, builder.setProfiles(asList(profile1, profile2)).build()))
      .containsOnly(entry(profile1.getKee(), 2L), entry(profile2.getKee(), 1L));
    assertThat(underTest.countActiveRulesByQuery(dbSession, builder.setProfiles(singletonList(profileWithoutActiveRule)).build())).isEmpty();
    assertThat(underTest.countActiveRulesByQuery(dbSession,
      builder.setProfiles(asList(profile1, profile2, profileWithoutActiveRule)).build())).containsOnly(
        entry(profile1.getKee(), 2L),
        entry(profile2.getKee(), 1L));
    assertThat(underTest.countActiveRulesByQuery(dbSession, builder.setProfiles(emptyList()).build())).isEmpty();
  }

  @Test
  void countActiveRulesByQuery_filter_by_rule_status() {
    RuleDto betaRule = db.rules().insert(r -> r.setStatus(BETA));
    db.qualityProfiles().activateRule(profile1, rule1);
    db.qualityProfiles().activateRule(profile1, rule2);
    db.qualityProfiles().activateRule(profile1, betaRule);
    db.qualityProfiles().activateRule(profile1, removedRule);
    db.qualityProfiles().activateRule(profile2, rule1);
    db.qualityProfiles().activateRule(profile2, betaRule);

    ActiveRuleCountQuery.Builder builder = ActiveRuleCountQuery.builder();
    assertThat(underTest.countActiveRulesByQuery(dbSession, builder.setProfiles(asList(profile1, profile2)).setRuleStatus(BETA).build()))
      .containsOnly(entry(profile1.getKee(), 1L), entry(profile2.getKee(), 1L));
    assertThat(underTest.countActiveRulesByQuery(dbSession, builder.setProfiles(singletonList(profile1)).setRuleStatus(READY).build()))
      .containsOnly(entry(profile1.getKee(), 2L));
    assertThat(underTest.countActiveRulesByQuery(dbSession, builder.setProfiles(singletonList(profile1)).setRuleStatus(REMOVED).build()))
      .containsOnly(entry(profile1.getKee(), 1L));
  }

  @Test
  void countActiveRulesByQuery_filter_by_inheritance() {
    db.qualityProfiles().activateRule(profile1, rule1);
    db.qualityProfiles().activateRule(profile1, rule2, ar -> ar.setInheritance(OVERRIDES));
    db.qualityProfiles().activateRule(profile1, removedRule, ar -> ar.setInheritance(OVERRIDES));
    db.qualityProfiles().activateRule(profile2, rule1, ar -> ar.setInheritance(OVERRIDES));
    db.qualityProfiles().activateRule(profile2, rule2, ar -> ar.setInheritance(INHERITED));

    ActiveRuleCountQuery.Builder builder = ActiveRuleCountQuery.builder();
    assertThat(underTest.countActiveRulesByQuery(dbSession,
      builder.setProfiles(asList(profile1, profile2)).setInheritance(OVERRIDES).build()))
      .containsOnly(entry(profile1.getKee(), 1L), entry(profile2.getKee(), 1L));
    assertThat(underTest.countActiveRulesByQuery(dbSession,
      builder.setProfiles(asList(profile1, profile2)).setInheritance(INHERITED).build()))
      .containsOnly(entry(profile2.getKee(), 1L));
  }

  @Test
  void scrollAllForIndexing_empty_table() {
    Accumulator accumulator = new Accumulator();
    underTest.scrollAllForIndexing(dbSession, accumulator);
    assertThat(accumulator.list).isEmpty();
  }

  @Test
  void scrollAllForIndexing() {
    ActiveRuleDto ar1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto ar2 = db.qualityProfiles().activateRule(profile2, rule1);
    ActiveRuleDto ar3 = db.qualityProfiles().activateRule(profile2, rule2);

    Accumulator accumulator = new Accumulator();
    underTest.scrollAllForIndexing(dbSession, accumulator);
    assertThat(accumulator.list)
      .extracting(IndexedActiveRuleDto::getUuid,
        IndexedActiveRuleDto::getRuleUuid, IndexedActiveRuleDto::getRepository, IndexedActiveRuleDto::getKey,
        IndexedActiveRuleDto::getRuleProfileUuid,
        IndexedActiveRuleDto::getSeverity, IndexedActiveRuleDto::getInheritance, IndexedActiveRuleDto::getImpacts)
      .containsExactlyInAnyOrder(
        tuple(ar1.getUuid(), rule1.getUuid(), ar1.getRuleKey().repository(), ar1.getRuleKey().rule(), profile1.getRulesProfileUuid(),
          ar1.getSeverity(), ar1.getInheritance(), ar1.getImpacts()),
        tuple(ar2.getUuid(), rule1.getUuid(), ar2.getRuleKey().repository(), ar2.getRuleKey().rule(), profile2.getRulesProfileUuid(),
          ar2.getSeverity(), ar2.getInheritance(), ar2.getImpacts()),
        tuple(ar3.getUuid(), rule2.getUuid(), ar3.getRuleKey().repository(), ar3.getRuleKey().rule(), profile2.getRulesProfileUuid(),
          ar3.getSeverity(), ar3.getInheritance(), ar3.getImpacts()));
  }

  @Test
  void scrollByIdsForIndexing() {
    ActiveRuleDto ar1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto ar2 = db.qualityProfiles().activateRule(profile2, rule1);
    ActiveRuleDto ar3 = db.qualityProfiles().activateRule(profile2, rule2);

    Accumulator accumulator = new Accumulator();
    underTest.scrollByUuidsForIndexing(dbSession, asList(ar1.getUuid(), ar2.getUuid()), accumulator);
    assertThat(accumulator.list)
      .extracting(IndexedActiveRuleDto::getUuid,
        IndexedActiveRuleDto::getRuleUuid, IndexedActiveRuleDto::getRepository, IndexedActiveRuleDto::getKey,
        IndexedActiveRuleDto::getRuleProfileUuid, IndexedActiveRuleDto::getSeverity, IndexedActiveRuleDto::getImpacts)
      .containsExactlyInAnyOrder(
        tuple(ar1.getUuid(), rule1.getUuid(), ar1.getRuleKey().repository(), ar1.getRuleKey().rule(), profile1.getRulesProfileUuid(),
          ar1.getSeverity(), ar1.getImpacts()),
        tuple(ar2.getUuid(), rule1.getUuid(), ar2.getRuleKey().repository(), ar2.getRuleKey().rule(), profile2.getRulesProfileUuid(),
          ar2.getSeverity(), ar2.getImpacts()));
  }

  @Test
  void scrollByRuleProfileForIndexing() {
    ActiveRuleDto ar1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto ar2 = db.qualityProfiles().activateRule(profile2, rule1);
    ActiveRuleDto ar3 = db.qualityProfiles().activateRule(profile2, rule2);

    Accumulator accumulator = new Accumulator();
    underTest.scrollByRuleProfileForIndexing(dbSession, profile2.getRulesProfileUuid(), accumulator);
    assertThat(accumulator.list)
      .extracting(IndexedActiveRuleDto::getUuid, IndexedActiveRuleDto::getRepository, IndexedActiveRuleDto::getKey,
        IndexedActiveRuleDto::getRuleProfileUuid, IndexedActiveRuleDto::getSeverity, IndexedActiveRuleDto::getImpacts)
      .containsExactlyInAnyOrder(
        tuple(ar2.getUuid(), ar2.getRuleKey().repository(), ar2.getRuleKey().rule(), profile2.getRulesProfileUuid(), ar2.getSeverity(),
          ar2.getImpacts()),
        tuple(ar3.getUuid(), ar3.getRuleKey().repository(), ar3.getRuleKey().rule(), profile2.getRulesProfileUuid(), ar3.getSeverity(),
          ar3.getImpacts()));
  }

  @Test
  void countMissingRules() {
    db.qualityProfiles().activateRule(profile1, rule1);
    db.qualityProfiles().activateRule(profile1, rule3);
    db.qualityProfiles().activateRule(profile2, rule1);
    db.qualityProfiles().activateRule(profile2, rule2);

    int result = underTest.countMissingRules(dbSession, profile1.getRulesProfileUuid(), profile2.getRulesProfileUuid());

    assertThat(result).isOne();
  }

  @Test
  void countMissingRules_whenNoRulesInCommon_shouldReturnNumberOfRulesInComparedToProfile() {
    db.qualityProfiles().activateRule(profile1, rule1);
    db.qualityProfiles().activateRule(profile2, rule2);
    db.qualityProfiles().activateRule(profile2, rule3);

    int result = underTest.countMissingRules(dbSession, profile1.getRulesProfileUuid(), profile2.getRulesProfileUuid());

    assertThat(result).isEqualTo(2);
  }

  @Test
  void countMissingRules_whenSomeRulesRemoved_shouldNotCountRemovedRules() {
    db.qualityProfiles().activateRule(profile1, rule1);
    db.qualityProfiles().activateRule(profile2, rule2);
    db.qualityProfiles().activateRule(profile2, rule3);
    db.qualityProfiles().activateRule(profile2, removedRule);

    int result = underTest.countMissingRules(dbSession, profile1.getRulesProfileUuid(), profile2.getRulesProfileUuid());

    assertThat(result).isEqualTo(2);
  }

  private static class Accumulator implements Consumer<IndexedActiveRuleDto> {
    private final List<IndexedActiveRuleDto> list = new ArrayList<>();

    @Override
    public void accept(IndexedActiveRuleDto dto) {
      list.add(dto);
    }
  }
}
