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
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.rule.NewCustomRule;
import org.sonar.server.rule.RuleCreator;
import org.sonar.server.rule.RuleService;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.api.server.debt.DebtRemediationFunction.Type.LINEAR;
import static org.sonar.api.server.debt.DebtRemediationFunction.Type.LINEAR_OFFSET;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_BASE_EFFORT;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_GAP_MULTIPLIER;
import static org.sonar.server.rule.ws.UpdateAction.PARAM_REMEDIATION_FN_TYPE;

public class UpdateActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester).login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

  WsTester wsTester;

  RuleService ruleService;
  RuleDao ruleDao;
  DbSession session;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    wsTester = tester.get(WsTester.class);
    ruleService = tester.get(RuleService.class);
    ruleDao = tester.get(RuleDao.class);
    session = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void update_rule_remediation_function() throws Exception {
    RuleDto rule = RuleTesting.newXooX1()
      .setDefaultRemediationFunction(LINEAR.toString())
      .setDefaultRemediationGapMultiplier("10d")
      .setDefaultRemediationBaseEffort(null)
      .setRemediationFunction(null)
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort(null);
    ruleDao.insert(session, rule);
    session.commit();

    WsTester.TestRequest request = wsTester.newPostRequest("api/rules", "update")
      .setParam("key", rule.getKey().toString())
      .setParam(PARAM_REMEDIATION_FN_TYPE, LINEAR_OFFSET.toString())
      .setParam(PARAM_REMEDIATION_FN_GAP_MULTIPLIER, "15d")
      .setParam(PARAM_REMEDIATION_FN_BASE_EFFORT, "5min");
    request.execute().assertJson(getClass(), "update_rule_remediation_function.json");
  }

  @Test
  public void update_custom_rule_with_deprecated_remediation_function_parameters() throws Exception {
    RuleDto rule = RuleTesting.newXooX1()
      .setDefaultRemediationFunction(LINEAR_OFFSET.toString())
      .setDefaultRemediationGapMultiplier("10d")
      .setDefaultRemediationBaseEffort("5min")
      .setRemediationFunction(LINEAR_OFFSET.toString())
      .setRemediationGapMultiplier("15min")
      .setRemediationBaseEffort("3h");
    ruleDao.insert(session, rule);
    session.commit();

    WsTester.TestRequest request = wsTester.newPostRequest("api/rules", "update")
      .setParam("key", rule.getKey().toString())
      .setParam("debt_remediation_fn_type", LINEAR_OFFSET.toString())
      .setParam("debt_remediation_fy_coeff", "11d")
      .setParam("debt_remediation_fn_offset", "6min");
    request.execute().assertJson(getClass(), "deprecated_remediation_function.json");
  }

  @Test
  public void update_custom_rule() throws Exception {
    // Template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(session, templateRule);
    RuleParamDto param = RuleParamDto.createFor(templateRule).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    ruleDao.insertRuleParam(session, templateRule, param);
    session.commit();

    // Custom rule
    NewCustomRule newRule = NewCustomRule.createForCustomRule("MY_CUSTOM", templateRule.getKey())
      .setName("Old custom")
      .setHtmlDescription("Old description")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA)
      .setParameters(ImmutableMap.of("regex", "a"));

    RuleKey customRuleKey = tester.get(RuleCreator.class).create(newRule);
    session.clearCache();

    WsTester.TestRequest request = wsTester.newPostRequest("api/rules", "update")
      .setParam("key", customRuleKey.toString())
      .setParam("name", "My custom rule")
      .setParam("markdown_description", "Description")
      .setParam("severity", "MAJOR")
      .setParam("status", "BETA")
      .setParam("params", "regex=a.*");
    request.execute().assertJson(getClass(), "update_custom_rule.json");
  }

  @Test
  public void fail_to_update_custom_when_description_is_empty() {
    // Template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(session, templateRule);

    // Custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule);
    ruleDao.insert(session, customRule);
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newPostRequest("api/rules", "update")
      .setParam("key", customRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdown_description", "");

    try {
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The description is missing");
    }
  }

}
