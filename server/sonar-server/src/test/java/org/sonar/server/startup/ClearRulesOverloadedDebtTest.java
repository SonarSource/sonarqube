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

package org.sonar.server.startup;

import java.util.Date;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.ServerTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.loadedtemplate.LoadedTemplateDto.ONE_SHOT_TASK_TYPE;

public class ClearRulesOverloadedDebtTest {

  static final int SUB_CHARACTERISTIC_ID = 1;

  private static final RuleKey RULE_KEY_1 = RuleTesting.XOO_X1;
  private static final RuleKey RULE_KEY_2 = RuleTesting.XOO_X2;
  private static final RuleKey RULE_KEY_3 = RuleTesting.XOO_X3;

  @ClassRule
  public static ServerTester tester = new ServerTester();

  @org.junit.Rule
  public LogTester logTester = new LogTester();

  RuleDao ruleDao = tester.get(RuleDao.class);
  RuleIndex ruleIndex = tester.get(RuleIndex.class);
  DbClient dbClient = tester.get(DbClient.class);
  DbSession dbSession = tester.get(DbClient.class).openSession(false);

  ClearRulesOverloadedDebt underTest = new ClearRulesOverloadedDebt(dbClient);

  @Before
  public void before() {
    tester.clearDbAndIndexes();
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void remove_overridden_debt() throws Exception {
    // Characteristic and remediation function is overridden
    insertRuleDto(RULE_KEY_1, SUB_CHARACTERISTIC_ID, "LINEAR", null, "1d");
    // Only characteristic is overridden
    insertRuleDto(RULE_KEY_2, SUB_CHARACTERISTIC_ID, null, null, null);
    // Only remediation function is overridden
    insertRuleDto(RULE_KEY_3, null, "CONSTANT_ISSUE", "5min", null);

    underTest.start();

    verifyRuleHasNotOverriddenDebt(RULE_KEY_1);
    verifyRuleHasNotOverriddenDebt(RULE_KEY_2);
    verifyRuleHasNotOverriddenDebt(RULE_KEY_3);
    verifyTaskRegistered();
    verifyLog(3);
  }

  @Test
  public void not_update_rule_debt_not_overridden() throws Exception {
    RuleDto rule = insertRuleDto(RULE_KEY_1, null, null, null, null);
    Date updateAt = rule.getUpdatedAt();

    underTest.start();

    RuleDto reloaded = ruleDao.getByKey(dbSession, RULE_KEY_1);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(updateAt);
    verifyRuleHasNotOverriddenDebt(RULE_KEY_1);

    verifyTaskRegistered();
    verifyEmptyLog();
  }

  @Test
  public void not_update_rule_debt_when_sqale_is_installed() throws Exception {
    insertSqaleProperty();
    RuleDto rule = insertRuleDto(RULE_KEY_1, SUB_CHARACTERISTIC_ID, "LINEAR", null, "1d");
    Date updateAt = rule.getUpdatedAt();

    underTest.start();

    RuleDto reloaded = ruleDao.getByKey(dbSession, RULE_KEY_1);
    assertThat(reloaded.getUpdatedAt()).isEqualTo(updateAt);

    Rule ruleEs = ruleIndex.getByKey(RULE_KEY_1);
    assertThat(ruleEs.debtOverloaded()).isTrue();

    verifyTaskRegistered();
    verifyEmptyLog();
  }

  @Test
  public void not_execute_task_when_already_executed() throws Exception {
    insertRuleDto(RULE_KEY_1, SUB_CHARACTERISTIC_ID, "LINEAR", null, "1d");
    underTest.start();
    verifyLog(1);
    verifyTaskRegistered();

    logTester.clear();
    underTest.start();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
    verifyEmptyLog();
  }

  private void verifyRuleHasNotOverriddenDebt(RuleKey ruleKey) {
    // Refresh session
    dbSession.commit(true);

    RuleDto ruleDto = ruleDao.getByKey(dbSession, ruleKey);
    assertThat(ruleDto.getSubCharacteristicId()).isNull();
    assertThat(ruleDto.getRemediationFunction()).isNull();
    assertThat(ruleDto.getRemediationCoefficient()).isNull();
    assertThat(ruleDto.getRemediationOffset()).isNull();

    Rule rule = ruleIndex.getByKey(ruleKey);
    assertThat(rule.debtOverloaded()).isFalse();
  }

  private RuleDto insertRuleDto(RuleKey ruleKey, @Nullable Integer subCharId, @Nullable String function, @Nullable String coeff, @Nullable String offset) {
    RuleDto ruleDto = RuleTesting.newDto(ruleKey).setSubCharacteristicId(subCharId).setRemediationFunction(function).setRemediationOffset(offset).setRemediationCoefficient(coeff);
    ruleDao.insert(dbSession,
      ruleDto
      );
    dbSession.commit();
    return ruleDto;
  }

  private void insertSqaleProperty() {
    dbClient.propertiesDao().insertProperty(dbSession, new PropertyDto().setKey("sonar.sqale.licenseHash.secured").setValue("ABCD"));
    dbSession.commit();
  }

  private void verifyTaskRegistered() {
    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(ONE_SHOT_TASK_TYPE, "ClearRulesOverloadedDebt")).isEqualTo(1);
  }

  private void verifyLog(int nbOfUpdatedRules) {
    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly(
      "The SQALE model has been cleaned to remove useless data left over by previous migrations. The technical debt of " + nbOfUpdatedRules
        + " rules was reset to their default values.",
      "=> As a consequence, the overall technical debt of your projects might slightly evolve during the next analysis.");
  }

  private void verifyEmptyLog() {
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }
}
