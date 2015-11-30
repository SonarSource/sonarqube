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

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbSession;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.debt.DebtTesting;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().addXoo();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private static final String API_ENDPOINT = "api/rules";
  private static final String API_SEARCH_METHOD = "search";

  DbClient db;
  RulesWs ws;
  RuleDao ruleDao;
  DbSession dbSession;
  int softReliabilityId;
  int hardReliabilityId;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    ruleDao = tester.get(RuleDao.class);
    ws = tester.get(RulesWs.class);
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void search_no_rules() throws Exception {
    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "actives");
    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(), "search_no_rules.json");
  }

  @Test
  public void filter_by_key_rules() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1());
    ruleDao.insert(dbSession, RuleTesting.newXooX2());
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchAction.PARAM_KEY, RuleTesting.XOO_X1.toString());
    request.setParam(WebService.Param.FIELDS, "actives");
    WsTester.Result result = request.execute();
    result.assertJson("{\"total\":1,\"p\":1,\"ps\":100,\"rules\":[{\"key\":\"xoo:x1\"}]}");

    request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchAction.PARAM_KEY, RuleKey.of("xoo", "unknown").toString());
    request.setParam(WebService.Param.FIELDS, "actives");
    result = request.execute();
    result.assertJson("{\"total\":0,\"p\":1,\"ps\":100,\"rules\":[],\"actives\":{}}");

  }

  @Test
  public void search_2_rules() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1());
    ruleDao.insert(dbSession, RuleTesting.newXooX2());
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    WsTester.Result result = request.execute();

    result.assertJson(getClass(), "search_2_rules.json");
  }

  @Test
  public void search_2_rules_with_field_selection() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1());
    ruleDao.insert(dbSession, RuleTesting.newXooX2().setDescription("A *Xoo* rule").setDescriptionFormat(RuleDto.Format.MARKDOWN));
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD).setParam(WebService.Param.FIELDS, "name,htmlDesc,mdDesc");
    WsTester.Result result = request.execute();

    result.assertJson(getClass(), "search_2_rules_fields.json");
  }

  @Test
  public void return_lang_field() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1());
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD).setParam(WebService.Param.FIELDS, "lang");
    WsTester.Result result = request.execute();

    result.assertJson("{\"total\":1,\"p\":1,\"ps\":100," +
      "\"rules\":[{\"key\":\"xoo:x1\",\"lang\":\"xoo\"}]}");
    assertThat(result.outputAsString()).doesNotContain("\"langName\"");
    assertThat(result.outputAsString()).doesNotContain("\"name\"");
  }

  @Test
  public void return_lang_name_field() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1());
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD).setParam(WebService.Param.FIELDS, "langName");
    WsTester.Result result = request.execute();

    result.assertJson("{\"total\":1,\"p\":1,\"ps\":100," +
      "\"rules\":[{\"key\":\"xoo:x1\",\"langName\":\"Xoo\"}]}");
    assertThat(result.outputAsString()).doesNotContain("\"lang\"");
    assertThat(result.outputAsString()).doesNotContain("\"name\"");
  }

  @Test
  public void return_lang_key_field_when_language_name_is_not_available() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newDto(RuleKey.of("other", "rule"))).setLanguage("unknown");
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD).setParam(WebService.Param.FIELDS, "langName");
    WsTester.Result result = request.execute();

    result.assertJson("{\"total\":1,\"p\":1,\"ps\":100," +
      "\"rules\":[{\"key\":\"other:rule\",\"langName\":\"unknown\"}]}");
  }

  @Test
  public void search_debt_rules() throws Exception {
    insertDebtCharacteristics(dbSession);

    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationCoefficient("1h")
      .setDefaultRemediationOffset("15min")

      .setSubCharacteristicId(null)
      .setRemediationFunction(null)
      .setRemediationCoefficient(null)
      .setRemediationOffset(null)
      );
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "debtChar,debtCharName,debtSubChar,debtSubCharName,debtRemFn,debtOverloaded,defaultDebtChar,defaultDebtSubChar,defaultDebtRemFn");
    request.setParam(WebService.Param.FACETS, "debt_characteristics");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_debt_rule.json");
  }

  @Test
  public void search_debt_rules_with_default_and_overridden_debt_values() throws Exception {
    insertDebtCharacteristics(dbSession);

    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationCoefficient("1h")
      .setDefaultRemediationOffset("15min")

      .setSubCharacteristicId(softReliabilityId)
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setRemediationCoefficient("2h")
      .setRemediationOffset("25min")
      );
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "debtChar,debtCharName,debtSubChar,debtSubCharName,debtRemFn,debtOverloaded,defaultDebtChar,defaultDebtSubChar,defaultDebtRemFn");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_debt_rules_with_default_and_overridden_debt_values.json");
  }

  @Test
  public void search_debt_rules_with_default_linear_offset_and_overridden_constant_debt() throws Exception {
    insertDebtCharacteristics(dbSession);

    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationCoefficient("1h")
      .setDefaultRemediationOffset("15min")

      .setSubCharacteristicId(softReliabilityId)
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationCoefficient(null)
      .setRemediationOffset("5min")
      );
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "debtChar,debtCharName,debtSubChar,debtSubCharName,debtRemFn,debtOverloaded,defaultDebtChar,defaultDebtSubChar,defaultDebtRemFn");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_debt_rules_with_default_linear_offset_and_overridden_constant_debt.json");
  }

  @Test
  public void search_debt_rules_with_default_linear_offset_and_overridden_linear_debt() throws Exception {
    insertDebtCharacteristics(dbSession);

    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationCoefficient("1h")
      .setDefaultRemediationOffset("15min")

      .setSubCharacteristicId(softReliabilityId)
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setRemediationCoefficient("1h")
      .setRemediationOffset(null)
      );
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "debtChar,debtCharName,debtSubChar,debtSubCharName,debtRemFn,debtOverloaded,defaultDebtChar,defaultDebtSubChar,defaultDebtRemFn");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_debt_rules_with_default_linear_offset_and_overridden_linear_debt.json");
  }

  @Test
  public void debt_characteristics_is_sticky_facet() throws Exception {
    insertDebtCharacteristics(dbSession);

    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationCoefficient("1h")
      .setDefaultRemediationOffset("15min")

      .setSubCharacteristicId(null)
      .setRemediationFunction(null)
      .setRemediationCoefficient(null)
      .setRemediationOffset(null)
      );
    ruleDao.insert(dbSession, RuleTesting.newXooX2()
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationCoefficient("1h")
      .setDefaultRemediationOffset("15min")

      .setSubCharacteristicId(softReliabilityId)
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setRemediationCoefficient("30min")
      .setRemediationOffset("5min")
      );
    ruleDao.insert(dbSession, RuleTesting.newXooX3()
      .setDefaultSubCharacteristicId(null)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationCoefficient("2min")
      .setDefaultRemediationOffset("1min")

      .setSubCharacteristicId(null)
      .setRemediationFunction(null)
      .setRemediationCoefficient(null)
      .setRemediationOffset(null)
      );
    ruleDao.insert(dbSession, RuleTesting.newDto(RuleKey.of("xoo", "x4")).setLanguage("xoo")
      .setDefaultSubCharacteristicId(softReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationCoefficient("2min")
      .setDefaultRemediationOffset("1min")

      .setSubCharacteristicId(-1)
      .setRemediationFunction(null)
      .setRemediationCoefficient(null)
      .setRemediationOffset(null)
      );
    dbSession.commit();

    WsTester.Result result = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD)
      .setParam(WebService.Param.FIELDS, "debtChar,debtCharName,debtSubChar,debtSubCharName,debtRemFn,debtOverloaded,defaultDebtChar,defaultDebtSubChar,defaultDebtRemFn")
      .setParam("debt_characteristics", "SOFT_RELIABILITY")
      .setParam(WebService.Param.FACETS, "debt_characteristics")
      .execute();
    result.assertJson(this.getClass(), "search_debt_rules_sticky.json");
  }

  @Test
  public void search_template_rules() throws Exception {
    RuleDto templateRule = RuleTesting.newXooX1().setIsTemplate(true);
    ruleDao.insert(dbSession, templateRule);
    ruleDao.insert(dbSession, RuleTesting.newXooX2()).setTemplateId(templateRule.getId());
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "isTemplate");
    request.setParam(SearchAction.PARAM_IS_TEMPLATE, "true");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_template_rules.json");
  }

  @Test
  public void search_custom_rules_from_template_key() throws Exception {
    RuleDto templateRule = RuleTesting.newXooX1().setIsTemplate(true);
    ruleDao.insert(dbSession, templateRule);
    ruleDao.insert(dbSession, RuleTesting.newXooX2()).setTemplateId(templateRule.getId());
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "templateKey");
    request.setParam(SearchAction.PARAM_TEMPLATE_KEY, "xoo:x1");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_rules_from_template_key.json");
  }

  @Test
  public void search_all_active_rules() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    tester.get(QualityProfileDao.class).insert(dbSession, profile);

    RuleDto rule = RuleTesting.newXooX1();
    ruleDao.insert(dbSession, rule);

    ActiveRuleDto activeRule = newActiveRule(profile, rule);
    tester.get(ActiveRuleDao.class).insert(dbSession, activeRule);
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.TEXT_QUERY, "x1");
    request.setParam(SearchAction.PARAM_ACTIVATION, "true");
    request.setParam(WebService.Param.FIELDS, "");
    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(), "search_active_rules.json");
  }

  @Test
  public void search_profile_active_rules() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    tester.get(QualityProfileDao.class).insert(dbSession, profile);

    QualityProfileDto profile2 = QProfileTesting.newXooP2();
    tester.get(QualityProfileDao.class).insert(dbSession, profile2);

    dbSession.commit();

    RuleDto rule = RuleTesting.newXooX1();
    ruleDao.insert(dbSession, rule);

    RuleParamDto param = RuleParamDto.createFor(rule)
      .setDefaultValue("some value")
      .setType("string")
      .setDescription("My small description")
      .setName("my_var");
    ruleDao.insertRuleParam(dbSession, rule, param);

    RuleParamDto param2 = RuleParamDto.createFor(rule)
      .setDefaultValue("other value")
      .setType("integer")
      .setDescription("My small description")
      .setName("the_var");
    ruleDao.insertRuleParam(dbSession, rule, param2);

    // SONAR-7083
    RuleParamDto param3 = RuleParamDto.createFor(rule)
      .setDefaultValue(null)
      .setType("string")
      .setDescription("Empty Param")
      .setName("empty_var");
    ruleDao.insertRuleParam(dbSession, rule, param3);

    ActiveRuleDto activeRule = newActiveRule(profile, rule);
    tester.get(ActiveRuleDao.class).insert(dbSession, activeRule);
    ActiveRuleDto activeRule2 = newActiveRule(profile2, rule);
    tester.get(ActiveRuleDao.class).insert(dbSession, activeRule2);

    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(param)
      .setValue("The VALUE");
    tester.get(ActiveRuleDao.class).insertParam(dbSession, activeRule2, activeRuleParam);

    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(param2)
      .setValue("The Other Value");
    tester.get(ActiveRuleDao.class).insertParam(dbSession, activeRule2, activeRuleParam2);

    ActiveRuleParamDto activeRuleParam3 = ActiveRuleParamDto.createFor(param3)
      .setValue(null);
    tester.get(ActiveRuleDao.class).insertParam(dbSession, activeRule2, activeRuleParam3);

    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.TEXT_QUERY, "x1");
    request.setParam(SearchAction.PARAM_ACTIVATION, "true");
    request.setParam(SearchAction.PARAM_QPROFILE, profile2.getKey());
    request.setParam(WebService.Param.FIELDS, "actives");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_profile_active_rules.json");

    tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD)
      .setParam(SearchAction.PARAM_ACTIVATION, "true")
      .setParam(SearchAction.PARAM_QPROFILE, "unknown_profile")
      .setParam(WebService.Param.FIELDS, "actives")
      .execute()
      .assertJson(this.getClass(), "search_no_rules.json");
  }

  @Test
  public void search_profile_active_rules_with_inheritance() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    tester.get(QualityProfileDao.class).insert(dbSession, profile);

    QualityProfileDto profile2 = QProfileTesting.newXooP2().setParentKee(profile.getKee());
    tester.get(QualityProfileDao.class).insert(dbSession, profile2);

    dbSession.commit();

    RuleDto rule = RuleTesting.newXooX1();
    ruleDao.insert(dbSession, rule);

    ActiveRuleDto activeRule = newActiveRule(profile, rule);
    tester.get(ActiveRuleDao.class).insert(dbSession, activeRule);
    ActiveRuleDto activeRule2 = newActiveRule(profile2, rule).setInheritance(ActiveRuleDto.OVERRIDES).setSeverity(Severity.CRITICAL);
    tester.get(ActiveRuleDao.class).insert(dbSession, activeRule2);

    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.TEXT_QUERY, "x1");
    request.setParam(SearchAction.PARAM_ACTIVATION, "true");
    request.setParam(SearchAction.PARAM_QPROFILE, profile2.getKey());
    request.setParam(WebService.Param.FIELDS, "actives");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_profile_active_rules_inheritance.json");
  }

  @Test
  public void search_all_active_rules_params() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    tester.get(QualityProfileDao.class).insert(dbSession, profile);
    RuleDto rule = RuleTesting.newXooX1();
    ruleDao.insert(dbSession, rule);
    dbSession.commit();

    RuleParamDto param = RuleParamDto.createFor(rule)
      .setDefaultValue("some value")
      .setType("string")
      .setDescription("My small description")
      .setName("my_var");
    ruleDao.insertRuleParam(dbSession, rule, param);

    RuleParamDto param2 = RuleParamDto.createFor(rule)
      .setDefaultValue("other value")
      .setType("integer")
      .setDescription("My small description")
      .setName("the_var");
    ruleDao.insertRuleParam(dbSession, rule, param2);

    ActiveRuleDto activeRule = newActiveRule(profile, rule);
    tester.get(ActiveRuleDao.class).insert(dbSession, activeRule);

    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(param)
      .setValue("The VALUE");
    tester.get(ActiveRuleDao.class).insertParam(dbSession, activeRule, activeRuleParam);

    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(param2)
      .setValue("The Other Value");
    tester.get(ActiveRuleDao.class).insertParam(dbSession, activeRule, activeRuleParam2);
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.TEXT_QUERY, "x1");
    request.setParam(SearchAction.PARAM_ACTIVATION, "true");
    request.setParam(WebService.Param.FIELDS, "params");
    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(), "search_active_rules_params.json");
  }

  @Test
  public void get_note_as_markdown_and_html() throws Exception {
    QualityProfileDto profile = QProfileTesting.newXooP1();
    tester.get(QualityProfileDao.class).insert(dbSession, profile);
    RuleDto rule = RuleTesting.newXooX1().setNoteData("this is *bold*");
    ruleDao.insert(dbSession, rule);
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "htmlNote, mdNote");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "get_note_as_markdown_and_html.json");
  }

  @Test
  public void filter_by_tags() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setTags(Collections.<String>emptySet())
      .setSystemTags(ImmutableSet.of("tag1")));
    ruleDao.insert(dbSession, RuleTesting.newXooX2()
      .setTags(Collections.<String>emptySet())
      .setSystemTags(ImmutableSet.of("tag2")));
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchAction.PARAM_TAGS, "tag1");
    request.setParam(WebService.Param.FIELDS, "sysTags, tags");
    request.setParam(WebService.Param.FACETS, "tags");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "filter_by_tags.json");
  }

  @Test
  public void severities_facet_should_have_all_severities() throws Exception {
    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FACETS, "severities");
    request.execute().assertJson(this.getClass(), "severities_facet.json");
  }

  @Test
  public void statuses_facet_should_have_all_statuses_except_removed() throws Exception {
    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FACETS, "statuses");
    request.execute().assertJson(this.getClass(), "statuses_facet.json");
  }

  @Test
  public void statuses_facet_should_be_sticky() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1());
    ruleDao.insert(dbSession, RuleTesting.newXooX2().setStatus(RuleStatus.BETA));
    ruleDao.insert(dbSession, RuleTesting.newXooX3().setStatus(RuleStatus.DEPRECATED));
    dbSession.commit();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchAction.PARAM_STATUSES, "DEPRECATED");
    request.setParam(WebService.Param.FACETS, "statuses");
    request.execute().assertJson(this.getClass(), "statuses_facet_sticky.json");
  }

  @Test
  public void sort_by_name() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1().setName("Dodgy - Consider returning a zero length array rather than null "));
    ruleDao.insert(dbSession, RuleTesting.newXooX2().setName("Bad practice - Creates an empty zip file entry"));
    ruleDao.insert(dbSession, RuleTesting.newXooX3().setName("XPath rule"));
    dbSession.commit();

    // 1. Sort Name Asc
    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "");
    request.setParam(WebService.Param.SORT, "name");
    request.setParam(WebService.Param.ASCENDING, "true");

    WsTester.Result result = request.execute();
    result.assertJson("{\"total\":3,\"p\":1,\"ps\":100,\"rules\":[{\"key\":\"xoo:x2\"},{\"key\":\"xoo:x1\"},{\"key\":\"xoo:x3\"}]}");

    // 2. Sort Name DESC
    request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "");
    request.setParam(WebService.Param.SORT, RuleNormalizer.RuleField.NAME.field());
    request.setParam(WebService.Param.ASCENDING, "false");

    result = request.execute();
    result.assertJson("{\"total\":3,\"p\":1,\"ps\":100,\"rules\":[{\"key\":\"xoo:x3\"},{\"key\":\"xoo:x1\"},{\"key\":\"xoo:x2\"}]}");

  }

  @Test
  public void available_since() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1());
    ruleDao.insert(dbSession, RuleTesting.newXooX2());
    dbSession.commit();
    dbSession.clearCache();

    Date since = new Date();

    // 1. find today's rules
    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "");
    request.setParam(SearchAction.PARAM_AVAILABLE_SINCE, DateUtils.formatDate(since));
    request.setParam(WebService.Param.SORT, RuleNormalizer.RuleField.KEY.field());
    WsTester.Result result = request.execute();
    result.assertJson("{\"total\":2,\"p\":1,\"ps\":100,\"rules\":[{\"key\":\"xoo:x1\"},{\"key\":\"xoo:x2\"}]}");

    Calendar c = Calendar.getInstance();
    c.setTime(since);
    c.add(Calendar.DATE, 1); // number of days to add

    // 2. no rules since tomorrow
    request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "");
    request.setParam(SearchAction.PARAM_AVAILABLE_SINCE, DateUtils.formatDate(c.getTime()));
    result = request.execute();
    result.assertJson("{\"total\":0,\"p\":1,\"ps\":100,\"rules\":[]}");
  }

  private ActiveRuleDto newActiveRule(QualityProfileDto profile, RuleDto rule) {
    return ActiveRuleDto.createFor(profile, rule)
      .setInheritance(null)
      .setSeverity("BLOCKER");
  }

  private void insertDebtCharacteristics(DbSession dbSession) {
    CharacteristicDto reliability = DebtTesting.newCharacteristicDto("RELIABILITY").setName("Reliability");
    db.debtCharacteristicDao().insert(dbSession, reliability);

    CharacteristicDto softReliability = DebtTesting.newCharacteristicDto("SOFT_RELIABILITY").setName("Soft Reliability")
      .setParentId(reliability.getId());
    db.debtCharacteristicDao().insert(dbSession, softReliability);
    softReliabilityId = softReliability.getId();

    CharacteristicDto hardReliability = DebtTesting.newCharacteristicDto("HARD_RELIABILITY").setName("Hard Reliability")
      .setParentId(reliability.getId());
    db.debtCharacteristicDao().insert(dbSession, hardReliability);
    hardReliabilityId = hardReliability.getId();
  }

}
