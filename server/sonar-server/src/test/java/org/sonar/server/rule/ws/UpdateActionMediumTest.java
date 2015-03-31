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

package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.NewRule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class UpdateActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  WsTester wsTester;

  RuleService ruleService;
  RuleDao ruleDao;
  DbSession session;

  @Before
  public void setUp() throws Exception {
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
  public void update_custom_rule() throws Exception {
    MockUserSession.set()
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN)
      .setLogin("me");

    // Template rule
    RuleDto templateRule = ruleDao.insert(session, RuleTesting.newTemplateRule(RuleKey.of("java", "S001")));
    RuleParamDto param = RuleParamDto.createFor(templateRule).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    ruleDao.addRuleParam(session, templateRule, param);
    session.commit();

    // Custom rule
    NewRule newRule = NewRule.createForCustomRule("MY_CUSTOM", templateRule.getKey())
      .setName("Old custom")
      .setHtmlDescription("Old description")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA)
      .setParameters(ImmutableMap.of("regex", "a"));
    RuleKey customRuleKey = ruleService.create(newRule);
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "update")
      .setParam("key", customRuleKey.toString())
      .setParam("name", "My custom rule")
      .setParam("markdown_description", "Description")
      .setParam("severity", "MAJOR")
      .setParam("status", "BETA")
      .setParam("params", "regex=a.*");
    request.execute().assertJson(getClass(), "update_custom_rule.json");
  }

  @Test
  public void fail_to_update_custom_when_description_is_empty() throws Exception {
    MockUserSession.set()
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN)
      .setLogin("me");

    // Template rule
    RuleDto templateRule = ruleDao.insert(session, RuleTesting.newTemplateRule(RuleKey.of("java", "S001")));

    // Custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule);
    ruleDao.insert(session, customRule);
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "update")
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
