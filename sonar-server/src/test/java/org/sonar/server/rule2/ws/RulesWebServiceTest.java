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
package org.sonar.server.rule2.ws;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.rule2.RuleDao;
import org.sonar.server.rule2.RuleService;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

public class RulesWebServiceTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();


  private RulesWebService ws;
  private RuleDao ruleDao;

  WsTester wsTester;

  @Before
  public void setUp() throws Exception {
    ruleDao = tester.get(RuleDao.class);
    ws = tester.get(RulesWebService.class);
    wsTester = new WsTester(ws);

  }

  @Test
  public void define() throws Exception {

    WebService.Context context = new WebService.Context();
    ws.define(context);

    WebService.Controller controller = context.controller("api/rules2");

    assertThat(controller).isNotNull();
    assertThat(controller.actions()).hasSize(4);
    assertThat(controller.action("search")).isNotNull();
    assertThat(controller.action("show")).isNotNull();
    assertThat(controller.action("tags")).isNotNull();
    assertThat(controller.action("set_tags")).isNotNull();
  }

  @Test
  public void search_no_rules() throws Exception {

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "search");
    System.out.println("request.toString() = " + request.toString());

    WsTester.Result result = request.execute();
    assertThat(result.outputAsString()).isEqualTo("{\"total\":0,\"rules\":[]}");
  }

  @Test
  public void search_2_rules() throws Exception {
    DbSession session = tester.get(MyBatis.class).openSession(false);
    ruleDao.insert(newRuleDto(RuleKey.of("javascript", "S001")), session);
    ruleDao.insert(newRuleDto(RuleKey.of("javascript", "S002")), session);
    session.commit();
    session.close();
    tester.get(RuleService.class).refresh();

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "search");
    WsTester.Result result = request.execute();
    //TODO
  }


  private RuleDto newRuleDto(RuleKey ruleKey) {
    return new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setStatus(RuleStatus.READY.toString())
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.SINGLE)
      .setLanguage("js")
      .setRemediationFunction("linear")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix")
      .setCreatedAt(DateUtils.parseDate("2013-12-16"))
      .setUpdatedAt(DateUtils.parseDate("2013-12-17"));
  }
}


