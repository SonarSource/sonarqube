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

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_QPROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TEMPLATE_KEY;

public class SearchActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes().addXoo();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private static final String API_ENDPOINT = "api/rules";
  private static final String API_SEARCH_METHOD = "search";

  private DbClient db;
  private RulesWs ws;
  private RuleDao ruleDao;
  private DbSession dbSession;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;

  @Before
  public void setUp() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    ruleDao = tester.get(RuleDao.class);
    ws = tester.get(RulesWs.class);
    dbSession = tester.get(DbClient.class).openSession(false);
    ruleIndexer = tester.get(RuleIndexer.class);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
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
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(PARAM_RULE_KEY, RuleTesting.XOO_X1.toString());
    request.setParam(WebService.Param.FIELDS, "actives");
    WsTester.Result result = request.execute();
    result.assertJson("{\"total\":1,\"p\":1,\"ps\":100,\"rules\":[{\"key\":\"xoo:x1\"}]}");

    request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(PARAM_RULE_KEY, RuleKey.of("xoo", "unknown").toString());
    request.setParam(WebService.Param.FIELDS, "actives");
    result = request.execute();
    result.assertJson("{\"total\":0,\"p\":1,\"ps\":100,\"rules\":[],\"actives\":{}}");
  }

  @Test
  public void search_2_rules() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setType(RuleType.BUG));
    ruleDao.insert(dbSession, RuleTesting.newXooX2()
      .setType(RuleType.VULNERABILITY));
    dbSession.commit();
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    WsTester.Result result = request.execute();

    result.assertJson(getClass(), "search_2_rules.json");
  }

  @Test
  public void search_2_rules_with_fields_selection() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setType(RuleType.CODE_SMELL))
    ;
    ruleDao.insert(dbSession, RuleTesting.newXooX2()
      .setType(RuleType.BUG)
      .setDescription("A *Xoo* rule")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN))
    ;
    dbSession.commit();
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD).setParam(WebService.Param.FIELDS, "name,htmlDesc,mdDesc");
    WsTester.Result result = request.execute();

    result.assertJson(getClass(), "search_2_rules_fields.json");
  }

  @Test
  public void return_mandatory_fields_even_when_setting_f_param() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setName("Rule x1")
      .setType(RuleType.CODE_SMELL))
    ;
    dbSession.commit();
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD).setParam(WebService.Param.FIELDS, "name");
    WsTester.Result result = request.execute();

    result.assertJson(getClass(), "return_mandatory_fields_even_when_setting_f_param.json");
  }

  @Test
  public void return_lang_field() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1());
    dbSession.commit();
    ruleIndexer.index();

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
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD).setParam(WebService.Param.FIELDS, "langName");
    WsTester.Result result = request.execute();

    result.assertJson("{\"total\":1,\"p\":1,\"ps\":100," +
      "\"rules\":[{\"key\":\"xoo:x1\",\"langName\":\"Xoo\"}]}");
    assertThat(result.outputAsString()).doesNotContain("\"lang\"");
    assertThat(result.outputAsString()).doesNotContain("\"name\"");
  }

  @Test
  public void return_lang_key_field_when_language_name_is_not_available() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newDto(RuleKey.of("other", "rule")).setLanguage("unknown"));
    dbSession.commit();
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD).setParam(WebService.Param.FIELDS, "langName");
    WsTester.Result result = request.execute();

    result.assertJson("{\"total\":1,\"p\":1,\"ps\":100," +
      "\"rules\":[{\"key\":\"other:rule\",\"langName\":\"unknown\"}]}");
  }

  @Test
  public void search_debt_rules_with_default_and_overridden_debt_values() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationGapMultiplier("1h")
      .setDefaultRemediationBaseEffort("15min")
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setRemediationGapMultiplier("2h")
      .setRemediationBaseEffort("25min"));
    dbSession.commit();
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "debtRemFn,debtOverloaded,defaultDebtRemFn");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_debt_rules_with_default_and_overridden_debt_values.json");
  }

  @Test
  public void search_debt_rules_with_default_linear_offset_and_overridden_constant_debt() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationGapMultiplier("1h")
      .setDefaultRemediationBaseEffort("15min")
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort("5min"));
    dbSession.commit();
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "debtRemFn,debtOverloaded,defaultDebtRemFn");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_debt_rules_with_default_linear_offset_and_overridden_constant_debt.json");
  }

  @Test
  public void search_debt_rules_with_default_linear_offset_and_overridden_linear_debt() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationGapMultiplier("1h")
      .setDefaultRemediationBaseEffort("15min")
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setRemediationGapMultiplier("1h")
      .setRemediationBaseEffort(null));
    dbSession.commit();
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "debtRemFn,debtOverloaded,defaultDebtRemFn");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_debt_rules_with_default_linear_offset_and_overridden_linear_debt.json");
  }

  @Test
  public void search_template_rules() throws Exception {
    RuleDto templateRule = RuleTesting.newXooX1().setIsTemplate(true);
    ruleDao.insert(dbSession, templateRule);
    RuleDto rule = RuleTesting.newXooX2();
    rule.setTemplateId(templateRule.getId());
    ruleDao.insert(dbSession, rule);
    dbSession.commit();
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "isTemplate");
    request.setParam(PARAM_IS_TEMPLATE, "true");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_template_rules.json");
  }

  @Test
  public void search_custom_rules_from_template_key() throws Exception {
    RuleDto templateRule = RuleTesting.newXooX1().setIsTemplate(true);
    ruleDao.insert(dbSession, templateRule);
    ruleDao.insert(dbSession, RuleTesting.newXooX2().setTemplateId(templateRule.getId()));
    dbSession.commit();
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "templateKey");
    request.setParam(PARAM_TEMPLATE_KEY, "xoo:x1");
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
    ruleIndexer.index();
    activeRuleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.TEXT_QUERY, "x1");
    request.setParam(PARAM_ACTIVATION, "true");
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
    ruleIndexer.index();
    activeRuleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.TEXT_QUERY, "x1");
    request.setParam(PARAM_ACTIVATION, "true");
    request.setParam(PARAM_QPROFILE, profile2.getKey());
    request.setParam(WebService.Param.FIELDS, "actives");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_profile_active_rules.json");

    tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD)
      .setParam(PARAM_ACTIVATION, "true")
      .setParam(PARAM_QPROFILE, "unknown_profile")
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
    ruleIndexer.index();
    activeRuleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.TEXT_QUERY, "x1");
    request.setParam(PARAM_ACTIVATION, "true");
    request.setParam(PARAM_QPROFILE, profile2.getKey());
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
    ruleIndexer.index();
    activeRuleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.TEXT_QUERY, "x1");
    request.setParam(PARAM_ACTIVATION, "true");
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
    ruleIndexer.index();
    activeRuleIndexer.index();

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
    ruleIndexer.index();
    activeRuleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(PARAM_TAGS, "tag1");
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
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(PARAM_STATUSES, "DEPRECATED");
    request.setParam(WebService.Param.FACETS, "statuses");
    request.execute().assertJson(this.getClass(), "statuses_facet_sticky.json");
  }

  @Test
  public void sort_by_name() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1().setName("Dodgy - Consider returning a zero length array rather than null "));
    ruleDao.insert(dbSession, RuleTesting.newXooX2().setName("Bad practice - Creates an empty zip file entry"));
    ruleDao.insert(dbSession, RuleTesting.newXooX3().setName("XPath rule"));

    dbSession.commit();
    ruleIndexer.index();

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
    request.setParam(WebService.Param.SORT, RuleIndexDefinition.FIELD_RULE_NAME);
    request.setParam(WebService.Param.ASCENDING, "false");

    result = request.execute();
    result.assertJson("{\"total\":3,\"p\":1,\"ps\":100,\"rules\":[{\"key\":\"xoo:x3\"},{\"key\":\"xoo:x1\"},{\"key\":\"xoo:x2\"}]}");
  }

  @Test
  public void available_since() throws Exception {
    Date since = new Date();
    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setUpdatedAt(since.getTime())
      .setCreatedAt(since.getTime()));
    ruleDao.insert(dbSession, RuleTesting.newXooX2()
      .setUpdatedAt(since.getTime())
      .setCreatedAt(since.getTime()));

    dbSession.commit();
    dbSession.clearCache();
    ruleIndexer.index();

    // 1. find today's rules
    WsTester.TestRequest request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "");
    request.setParam(PARAM_AVAILABLE_SINCE, DateUtils.formatDate(since));
    request.setParam(WebService.Param.SORT, RuleIndexDefinition.FIELD_RULE_KEY);
    WsTester.Result result = request.execute();
    result.assertJson("{\"total\":2,\"p\":1,\"ps\":100,\"rules\":[{\"key\":\"xoo:x1\"},{\"key\":\"xoo:x2\"}]}");

    // 2. no rules since tomorrow
    request = tester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(WebService.Param.FIELDS, "");
    request.setParam(PARAM_AVAILABLE_SINCE, DateUtils.formatDate(DateUtils.addDays(since, 1)));
    result = request.execute();
    result.assertJson("{\"total\":0,\"p\":1,\"ps\":100,\"rules\":[]}");
  }

  @Test
  public void search_rules_with_deprecated_fields() throws Exception {
    ruleDao.insert(dbSession, RuleTesting.newXooX1()
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationGapMultiplier("1h")
      .setDefaultRemediationBaseEffort("15min")
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setRemediationGapMultiplier("2h")
      .setRemediationBaseEffort("25min"));
    dbSession.commit();
    ruleIndexer.index();

    WsTester.TestRequest request = tester.wsTester()
      .newGetRequest(API_ENDPOINT, API_SEARCH_METHOD)
      .setParam(WebService.Param.FIELDS, "name,defaultDebtRemFn,debtRemFn,effortToFixDescription,debtOverloaded");
    WsTester.Result result = request.execute();

    result.assertJson(getClass(), "search_rules_with_deprecated_fields.json");
  }

  private ActiveRuleDto newActiveRule(QualityProfileDto profile, RuleDto rule) {
    return ActiveRuleDto.createFor(profile, rule)
      .setInheritance(null)
      .setSeverity("BLOCKER");
  }

}
