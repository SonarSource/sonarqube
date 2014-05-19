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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.WebService;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.qualityprofile.persistence.ActiveRuleDao;
import org.sonar.server.rule2.RuleService;
import org.sonar.server.rule2.persistence.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

public class RulesWebServiceTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();


  private RulesWebService ws;
  private RuleDao ruleDao;
  private DbSession session;

  WsTester wsTester;


  @Before
  public void setUp() throws Exception {
    tester.clearDataStores();
    ruleDao = tester.get(RuleDao.class);
    ws = tester.get(RulesWebService.class);
    wsTester = new WsTester(ws);
    session = tester.get(MyBatis.class).openSession(false);
  }

  @After
  public void after(){
    session.close();
  }

  @Test
  public void define() throws Exception {

    WebService.Context context = new WebService.Context();
    ws.define(context);

    WebService.Controller controller = context.controller("api/rules2");

    assertThat(controller).isNotNull();
    assertThat(controller.actions()).hasSize(6);
    assertThat(controller.action("search")).isNotNull();
    assertThat(controller.action("show")).isNotNull();
    assertThat(controller.action("tags")).isNotNull();
    assertThat(controller.action("set_tags")).isNotNull();
    assertThat(controller.action("set_note")).isNotNull();
    assertThat(controller.action("app")).isNotNull();
  }

  @Test
  public void search_no_rules() throws Exception {

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "search");

    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(), "search_no_rules.json");
  }

  @Test
  public void search_2_rules() throws Exception {
    ruleDao.insert(newRuleDto(RuleKey.of("javascript", "S001")), session);
    ruleDao.insert(newRuleDto(RuleKey.of("javascript", "S002")), session);
    session.commit();

    tester.get(RuleService.class).refresh();

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "search");
    WsTester.Result result = request.execute();

    String json = result.outputAsString();
    String expectedJson = Resources.toString(getClass().getResource("RulesWebServiceTest/search_2_rules.json"), Charsets.UTF_8);

    JSONAssert.assertEquals(expectedJson, json, false);
  }


  @Test
  public void search_debt_rules() throws Exception {
    ruleDao.insert(newRuleDto(RuleKey.of("javascript", "S001"))
      .setDefaultRemediationCoefficient("DefaultCoef")
      .setDefaultRemediationFunction("DefaultFunction")
      .setDefaultRemediationCoefficient("DefaultCoef")
      .setDefaultSubCharacteristicId(1), session);
    session.commit();

    tester.get(RuleService.class).refresh();

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "search");
    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(),"search_debt_rule.json");
  }


  @Test
  public void search_active_rules() throws Exception {
    QualityProfileDto profile = newQualityProfile();
    tester.get(QualityProfileDao.class).insert(profile, session);

    RuleDto rule = newRuleDto(RuleKey.of(profile.getLanguage(), "S001"));
    ruleDao.insert(rule,  session);

    ActiveRuleDto activeRule = newActiveRule(profile, rule);
    tester.get(ActiveRuleDao.class).insert(activeRule, session);

    session.commit();

    tester.get(RuleService.class).refresh();


    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "search");
    request.setParam("q","S001");
    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(),"search_active_rules.json");
  }

  @Test
  public void search_active_rules_params() throws Exception {
    QualityProfileDto profile = newQualityProfile();
    tester.get(QualityProfileDao.class).insert(profile, session);

    RuleDto rule = newRuleDto(RuleKey.of(profile.getLanguage(), "S001"));
    ruleDao.insert(rule, session);

    session.commit();


    RuleParamDto param = RuleParamDto.createFor(rule)
      .setDefaultValue("some value")
      .setType("string")
      .setDescription("My small description")
      .setName("my_var");
    ruleDao.addRuleParam(rule,param, session);

    RuleParamDto param2 = RuleParamDto.createFor(rule)
      .setDefaultValue("other value")
      .setType("integer")
      .setDescription("My small description")
      .setName("the_var");
    ruleDao.addRuleParam(rule,param2, session);

    ActiveRuleDto activeRule = newActiveRule(profile, rule);
    tester.get(ActiveRuleDao.class).insert(activeRule, session);

    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(param)
      .setValue("The VALUE");
    tester.get(ActiveRuleDao.class).addParam(activeRule, activeRuleParam, session);

    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(param2)
      .setValue("The Other Value");
    tester.get(ActiveRuleDao.class).addParam(activeRule, activeRuleParam2, session);
    session.commit();

    tester.get(RuleService.class).refresh();

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "search");
    request.setParam("q", "S001");
    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(),"search_active_rules_params.json");
  }



  @Test
  public void get_tags() throws Exception {
    QualityProfileDto profile = newQualityProfile();
    tester.get(QualityProfileDao.class).insert(profile, session);

    RuleDto rule = newRuleDto(RuleKey.of(profile.getLanguage(), "S001"))
      .setTags(ImmutableSet.of("hello", "world"));
    ruleDao.insert(rule,  session);

    RuleDto rule2 = newRuleDto(RuleKey.of(profile.getLanguage(), "S002"))
      .setTags(ImmutableSet.of("java"))
      .setSystemTags(ImmutableSet.of("sys1"));
    ruleDao.insert(rule2,  session);

    session.commit();
    tester.get(RuleService.class).refresh();


    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "tags");
    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(),"get_tags.json");
  }



  private QualityProfileDto newQualityProfile() {
    return new QualityProfileDto()
      .setLanguage("java")
      .setName("My Profile");
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
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix");
  }

  private ActiveRuleDto  newActiveRule(QualityProfileDto profile, RuleDto rule) {
    return ActiveRuleDto.createFor(profile, rule)
      .setInheritance("NONE")
      .setSeverity("BLOCKER");
  }
}
