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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class CreateActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  WsTester wsTester;
  RuleDao ruleDao;
  DbSession session;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    wsTester = tester.get(WsTester.class);
    ruleDao = tester.get(RuleDao.class);
    session = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void create_custom_rule() throws Exception {
    MockUserSession.set()
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN)
      .setLogin("me");

    // Template rule
    RuleDto templateRule = ruleDao.insert(session, RuleTesting.newTemplateRule(RuleKey.of("java", "S001")));
    RuleParamDto param = RuleParamDto.createFor(templateRule).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    ruleDao.addRuleParam(session, templateRule, param);
    session.commit();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "create")
      .setParam("custom_key", "MY_CUSTOM")
      .setParam("template_key", templateRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdown_description", "Description")
      .setParam("severity", "MAJOR")
      .setParam("status", "BETA")
      .setParam("params", "regex=a.*");
    request.execute().assertJson(getClass(), "create_custom_rule.json");
  }

  @Test
  public void create_manual_rule() throws Exception {
    MockUserSession.set()
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN)
      .setLogin("me");

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "create")
      .setParam("manual_key", "MY_MANUAL")
      .setParam("name", "My manual rule")
      .setParam("markdown_description", "Description")
      .setParam("severity", "MAJOR");
    request.execute().assertJson(getClass(), "create_manual_rule.json");
  }

  @Test
  public void create_manual_rule_without_severity() throws Exception {
    MockUserSession.set()
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN)
      .setLogin("me");

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "create")
      .setParam("manual_key", "MY_MANUAL")
      .setParam("name", "My manual rule")
      .setParam("markdown_description", "Description");
    request.execute().assertJson(getClass(), "create_manual_rule_without_severity.json");
  }

  @Test
  public void fail_if_custom_key_and_manual_key_parameters_are_not_set() throws Exception {
    MockUserSession.set()
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN)
      .setLogin("me");

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "create")
      .setParam("key", "MY_MANUAL")
      .setParam("name", "My manual rule")
      .setParam("markdown_description", "Description")
      .setParam("severity", "MAJOR");

    try {
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Either 'custom_key' or 'manual_key' parameters should be set");
    }
  }

  @Test
  public void create_manual_rule_with_prevent_reactivation_param_to_true() throws Exception {
    MockUserSession.set()
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN)
      .setLogin("me");

    String key = "MY_MANUAL";

    // insert a removed rule
    tester.get(RuleDao.class).insert(session, RuleTesting.newManualRule(key)
      .setStatus(RuleStatus.REMOVED)
      .setName("My manual rule")
      .setDescription("Description")
      .setSeverity(Severity.MAJOR));
    session.commit();
    session.clearCache();

    WsTester.TestRequest request = wsTester.newGetRequest("api/rules", "create")
      .setParam("manual_key", key)
      .setParam("name", "My manual rule")
      .setParam("markdown_description", "Description")
      .setParam("severity", "MAJOR")
      .setParam("prevent_reactivation", "true");
    request.execute()
      .assertJson(getClass(), "create_rule_with_prevent_reactivation_param_to_true.json")
      .assertStatus(409);
  }

}
