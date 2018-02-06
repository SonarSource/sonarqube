/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class RuleRepositoryImplTest {

  private static final RuleDto AB_RULE = createABRuleDto()
    .setId(9688);
  private static final RuleKey AB_RULE_DEPRECATED_KEY_1 = RuleKey.of("old_a", "old_b");
  private static final RuleKey AB_RULE_DEPRECATED_KEY_2 = RuleKey.of(AB_RULE.getRepositoryKey(), "old_b");
  private static final RuleKey DEPRECATED_KEY_OF_NON_EXITING_RULE = RuleKey.of("some_rep", "some_key");
  private static final RuleKey AC_RULE_KEY = RuleKey.of("a", "c");
  private static final int AC_RULE_ID = 684;
  private static final String ORGANIZATION_UUID = "org-1";
  private static final String QUALITY_GATE_UUID = "QUALITY_GATE_UUID";

  @org.junit.Rule
  public ExpectedException expectedException = ExpectedException.none();
  @org.junit.Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setOrganizationUuid(ORGANIZATION_UUID, QUALITY_GATE_UUID);

  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);
  private RuleDao ruleDao = mock(RuleDao.class);

  private RuleRepositoryImpl underTest = new RuleRepositoryImpl(dbClient, analysisMetadataHolder);

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(anyBoolean())).thenReturn(dbSession);
    when(dbClient.ruleDao()).thenReturn(ruleDao);
    when(ruleDao.selectAll(any(DbSession.class), eq(ORGANIZATION_UUID))).thenReturn(ImmutableList.of(AB_RULE));
    DeprecatedRuleKeyDto abDeprecatedRuleKey1 = deprecatedRuleKeyOf(AB_RULE, AB_RULE_DEPRECATED_KEY_1);
    DeprecatedRuleKeyDto abDeprecatedRuleKey2 = deprecatedRuleKeyOf(AB_RULE, AB_RULE_DEPRECATED_KEY_2);
    DeprecatedRuleKeyDto deprecatedRuleOfNonExistingRule = deprecatedRuleKeyOf(77777, DEPRECATED_KEY_OF_NON_EXITING_RULE);
    when(ruleDao.selectAllDeprecatedRuleKeys(any(DbSession.class))).thenReturn(ImmutableSet.of(
      abDeprecatedRuleKey1, abDeprecatedRuleKey2, deprecatedRuleOfNonExistingRule));
  }

  private static DeprecatedRuleKeyDto deprecatedRuleKeyOf(RuleDto ruleDto, RuleKey deprecatedRuleKey) {
    return deprecatedRuleKeyOf(ruleDto.getId(), deprecatedRuleKey);
  }

  private static DeprecatedRuleKeyDto deprecatedRuleKeyOf(int ruleId, RuleKey deprecatedRuleKey) {
    return new DeprecatedRuleKeyDto().setRuleId(ruleId)
      .setOldRepositoryKey(deprecatedRuleKey.repository())
      .setOldRuleKey(deprecatedRuleKey.rule());
  }

  @Test
  public void constructor_does_not_query_DB_to_retrieve_rules() {
    verifyNoMoreInteractions(dbClient);
  }

  @Test
  public void first_call_to_getByKey_triggers_call_to_db_and_any_subsequent_get_or_find_call_does_not() {
    underTest.getByKey(AB_RULE.getKey());

    verify(ruleDao, times(1)).selectAll(any(DbSession.class), eq(ORGANIZATION_UUID));

    verifyNoMethodCallTriggersCallToDB();
  }

  @Test
  public void first_call_to_findByKey_triggers_call_to_db_and_any_subsequent_get_or_find_call_does_not() {
    underTest.findByKey(AB_RULE.getKey());

    verify(ruleDao, times(1)).selectAll(any(DbSession.class), eq(ORGANIZATION_UUID));

    verifyNoMethodCallTriggersCallToDB();
  }

  @Test
  public void first_call_to_getById_triggers_call_to_db_and_any_subsequent_get_or_find_call_does_not() {
    underTest.getById(AB_RULE.getId());

    verify(ruleDao, times(1)).selectAll(any(DbSession.class), eq(ORGANIZATION_UUID));

    verifyNoMethodCallTriggersCallToDB();
  }

  @Test
  public void first_call_to_findById_triggers_call_to_db_and_any_subsequent_get_or_find_call_does_not() {
    underTest.findById(AB_RULE.getId());

    verify(ruleDao, times(1)).selectAll(any(DbSession.class), eq(ORGANIZATION_UUID));

    verifyNoMethodCallTriggersCallToDB();
  }

  @Test
  public void getByKey_throws_NPE_if_key_argument_is_null() {
    expectNullRuleKeyNPE();

    underTest.getByKey(null);
  }

  @Test
  public void getByKey_does_not_call_DB_if_key_argument_is_null() {
    try {
      underTest.getByKey(null);
    } catch (NullPointerException e) {
      assertNoCallToDb();
    }
  }

  @Test
  public void getByKey_returns_Rule_if_it_exists_in_DB() {
    Rule rule = underTest.getByKey(AB_RULE.getKey());

    assertIsABRule(rule);
  }

  @Test
  public void getByKey_returns_Rule_if_argument_is_deprecated_key_in_DB_of_rule_in_DB() {
    Rule rule = underTest.getByKey(AB_RULE_DEPRECATED_KEY_1);

    assertIsABRule(rule);
  }

  @Test
  public void getByKey_throws_IAE_if_rules_does_not_exist_in_DB() {
    expectIAERuleNotFound(AC_RULE_KEY);

    underTest.getByKey(AC_RULE_KEY);
  }

  @Test
  public void getByKey_throws_IAE_if_argument_is_deprecated_key_in_DB_of_non_existing_rule() {
    expectIAERuleNotFound(DEPRECATED_KEY_OF_NON_EXITING_RULE);

    underTest.getByKey(DEPRECATED_KEY_OF_NON_EXITING_RULE);
  }

  private void expectIAERuleNotFound(RuleKey ruleKey) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Can not find rule for key " + ruleKey.toString() + ". This rule does not exist in DB");
  }

  @Test
  public void findByKey_throws_NPE_if_key_argument_is_null() {
    expectNullRuleKeyNPE();

    underTest.findByKey(null);
  }

  @Test
  public void findByKey_does_not_call_DB_if_key_argument_is_null() {
    try {
      underTest.findByKey(null);
    } catch (NullPointerException e) {
      assertNoCallToDb();
    }
  }

  @Test
  public void findByKey_returns_absent_if_rule_does_not_exist_in_DB() {
    Optional<Rule> rule = underTest.findByKey(AC_RULE_KEY);

    assertThat(rule).isEmpty();
  }

  @Test
  public void findByKey_returns_Rule_if_it_exists_in_DB() {
    Optional<Rule> rule = underTest.findByKey(AB_RULE.getKey());

    assertIsABRule(rule.get());
  }

  @Test
  public void findByKey_returns_Rule_if_argument_is_deprecated_key_in_DB_of_rule_in_DB() {
    Optional<Rule> rule = underTest.findByKey(AB_RULE_DEPRECATED_KEY_1);

    assertIsABRule(rule.get());
  }

  @Test
  public void findByKey_returns_empty_if_argument_is_deprecated_key_in_DB_of_rule_in_DB() {
    Optional<Rule> rule = underTest.findByKey(DEPRECATED_KEY_OF_NON_EXITING_RULE);

    assertThat(rule).isEmpty();
  }

  @Test
  public void getById_returns_Rule_if_it_exists_in_DB() {
    Rule rule = underTest.getById(AB_RULE.getId());

    assertIsABRule(rule);
  }

  @Test
  public void getById_throws_IAE_if_rules_does_not_exist_in_DB() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Can not find rule for id " + AC_RULE_ID + ". This rule does not exist in DB");

    underTest.getById(AC_RULE_ID);
  }

  @Test
  public void findById_returns_absent_if_rule_does_not_exist_in_DB() {
    Optional<Rule> rule = underTest.findById(AC_RULE_ID);

    assertThat(rule).isEmpty();
  }

  @Test
  public void findById_returns_Rule_if_it_exists_in_DB() {
    Optional<Rule> rule = underTest.findById(AB_RULE.getId());

    assertIsABRule(rule.get());
  }

  private void expectNullRuleKeyNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("RuleKey can not be null");
  }

  private void verifyNoMethodCallTriggersCallToDB() {
    reset(ruleDao);
    underTest.getByKey(AB_RULE.getKey());
    assertNoCallToDb();
    reset(ruleDao);
    underTest.findByKey(AB_RULE.getKey());
    assertNoCallToDb();
    reset(ruleDao);
    underTest.getById(AB_RULE.getId());
    assertNoCallToDb();
    reset(ruleDao);
    underTest.findById(AB_RULE.getId());
    assertNoCallToDb();
  }

  private void assertNoCallToDb() {
    verifyNoMoreInteractions(ruleDao);
  }

  private void assertIsABRule(Rule rule) {
    assertThat(rule).isNotNull();
    assertThat(rule.getId()).isEqualTo(AB_RULE.getId());
    assertThat(rule.getKey()).isEqualTo(AB_RULE.getKey());
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.REMOVED);
  }

  private static RuleDto createABRuleDto() {
    RuleKey ruleKey = RuleKey.of("a", "b");
    return new RuleDto()
      .setId(ruleKey.hashCode())
      .setRepositoryKey(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setStatus(RuleStatus.REMOVED)
      .setType(RuleType.CODE_SMELL);
  }

}
