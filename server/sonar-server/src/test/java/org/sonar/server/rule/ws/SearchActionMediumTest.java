/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.rule.ws;

public class SearchActionMediumTest {


  //
  // @Test
  // public void search_profile_active_rules_with_inheritance() throws Exception {
  // QProfileDto profile = QProfileTesting.newXooP1(defaultOrganizationDto);
  // esTester.get(QualityProfileDao.class).insert(dbSession, profile);
  //
  // QProfileDto profile2 = QProfileTesting.newXooP2(defaultOrganizationDto).setParentKee(profile.getKee());
  // esTester.get(QualityProfileDao.class).insert(dbSession, profile2);
  //
  // dbSession.commit();
  //
  // RuleDefinitionDto rule = RuleTesting.newXooX1().getDefinition();
  // insertRule(rule);
  //
  // ActiveRuleDto activeRule = newActiveRule(profile, rule);
  // esTester.get(ActiveRuleDao.class).insert(dbSession, activeRule);
  // ActiveRuleDto activeRule2 = newActiveRule(profile2, rule).setInheritance(ActiveRuleDto.OVERRIDES).setSeverity(Severity.CRITICAL);
  // esTester.get(ActiveRuleDao.class).insert(dbSession, activeRule2);
  //
  // dbSession.commit();
  //
  // activeRuleIndexer.index();
  //
  // WsTester.TestRequest request = esTester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
  // request.setParam(WebService.Param.TEXT_QUERY, "x1");
  // request.setParam(PARAM_ACTIVATION, "true");
  // request.setParam(PARAM_QPROFILE, profile2.getKee());
  // request.setParam(WebService.Param.FIELDS, "actives");
  // WsTester.Result result = request.execute();
  // result.assertJson(this.getClass(), "search_profile_active_rules_inheritance.json");
  // }
  //
  // @Test
  // public void search_all_active_rules_params() throws Exception {
  // QProfileDto profile = QProfileTesting.newXooP1(defaultOrganizationDto);
  // esTester.get(QualityProfileDao.class).insert(dbSession, profile);
  // RuleDefinitionDto rule = RuleTesting.newXooX1().getDefinition();
  // insertRule(rule);
  // dbSession.commit();
  //
  // RuleParamDto param = RuleParamDto.createFor(rule)
  // .setDefaultValue("some value")
  // .setType("string")
  // .setDescription("My small description")
  // .setName("my_var");
  // ruleDao.insertRuleParam(dbSession, rule, param);
  //
  // RuleParamDto param2 = RuleParamDto.createFor(rule)
  // .setDefaultValue("other value")
  // .setType("integer")
  // .setDescription("My small description")
  // .setName("the_var");
  // ruleDao.insertRuleParam(dbSession, rule, param2);
  //
  // ActiveRuleDto activeRule = newActiveRule(profile, rule);
  // esTester.get(ActiveRuleDao.class).insert(dbSession, activeRule);
  //
  // ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(param)
  // .setValue("The VALUE");
  // esTester.get(ActiveRuleDao.class).insertParam(dbSession, activeRule, activeRuleParam);
  //
  // ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(param2)
  // .setValue("The Other Value");
  // esTester.get(ActiveRuleDao.class).insertParam(dbSession, activeRule, activeRuleParam2);
  //
  // dbSession.commit();
  //
  // activeRuleIndexer.index();
  //
  // WsTester.TestRequest request = esTester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
  // request.setParam(WebService.Param.TEXT_QUERY, "x1");
  // request.setParam(PARAM_ACTIVATION, "true");
  // request.setParam(WebService.Param.FIELDS, "params");
  // WsTester.Result result = request.execute();
  //
  // result.assertJson(this.getClass(), "search_active_rules_params.json");
  // }
  //
  // @Test
  // public void get_note_as_markdown_and_html() throws Exception {
  // QProfileDto profile = QProfileTesting.newXooP1("org-123");
  // esTester.get(QualityProfileDao.class).insert(dbSession, profile);
  // RuleDto rule = RuleTesting.newXooX1(defaultOrganizationDto).setNoteData("this is *bold*");
  // insertRule(rule.getDefinition());
  // ruleDao.insertOrUpdate(dbSession, rule.getMetadata().setRuleId(rule.getId()));
  //
  // dbSession.commit();
  //
  // activeRuleIndexer.index();
  //
  // WsTester.TestRequest request = esTester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
  // request.setParam(WebService.Param.FIELDS, "htmlNote, mdNote");
  // WsTester.Result result = request.execute();
  // result.assertJson(this.getClass(), "get_note_as_markdown_and_html.json");
  // }
  //
  // @Test
  // public void filter_by_tags() throws Exception {
  // insertRule(RuleTesting.newRule()
  // .setRepositoryKey("xoo").setRuleKey("x1")
  // .setSystemTags(ImmutableSet.of("tag1")));
  // insertRule(RuleTesting.newRule()
  // .setSystemTags(ImmutableSet.of("tag2")));
  //
  // activeRuleIndexer.index();
  //
  // WsTester.TestRequest request = esTester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
  // request.setParam(PARAM_TAGS, "tag1");
  // request.setParam(WebService.Param.FIELDS, "sysTags, tags");
  // request.setParam(WebService.Param.FACETS, "tags");
  // WsTester.Result result = request.execute();
  // result.assertJson(this.getClass(), "filter_by_tags.json");
  // }
  //
  // @Test
  // public void severities_facet_should_have_all_severities() throws Exception {
  // WsTester.TestRequest request = esTester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
  // request.setParam(WebService.Param.FACETS, "severities");
  // request.execute().assertJson(this.getClass(), "severities_facet.json");
  // }
  //
 //
  //
  // @Test
  // public void sort_by_name() throws Exception {
  // insertRule(RuleTesting.newXooX1()
  // .setName("Dodgy - Consider returning a zero length array rather than null ")
  // .getDefinition());
  // insertRule(RuleTesting.newXooX2()
  // .setName("Bad practice - Creates an empty zip file entry")
  // .getDefinition());
  // insertRule(RuleTesting.newXooX3()
  // .setName("XPath rule")
  // .getDefinition());
  //
  // dbSession.commit();
  //
  // // 1. Sort Name Asc
  // WsTester.TestRequest request = esTester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
  // request.setParam(WebService.Param.FIELDS, "");
  // request.setParam(WebService.Param.SORT, "name");
  // request.setParam(WebService.Param.ASCENDING, "true");
  //
  // WsTester.Result result = request.execute();
  // result.assertJson("{\"total\":3,\"p\":1,\"ps\":100,\"rules\":[{\"key\":\"xoo:x2\"},{\"key\":\"xoo:x1\"},{\"key\":\"xoo:x3\"}]}");
  //
  // // 2. Sort Name DESC
  // request = esTester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
  // request.setParam(WebService.Param.FIELDS, "");
  // request.setParam(WebService.Param.SORT, RuleIndexDefinition.FIELD_RULE_NAME);
  // request.setParam(WebService.Param.ASCENDING, "false");
  //
  // result = request.execute();
  // result.assertJson("{\"total\":3,\"p\":1,\"ps\":100,\"rules\":[{\"key\":\"xoo:x3\"},{\"key\":\"xoo:x1\"},{\"key\":\"xoo:x2\"}]}");
  // }
  //
  // @Test
  // public void available_since() throws Exception {
  // Date since = new Date();
  // insertRule(RuleTesting.newXooX1()
  // .setUpdatedAt(since.getTime())
  // .setCreatedAt(since.getTime())
  // .getDefinition());
  // insertRule(RuleTesting.newXooX2()
  // .setUpdatedAt(since.getTime())
  // .setCreatedAt(since.getTime())
  // .getDefinition());
  //
  // dbSession.commit();
  // dbSession.clearCache();
  //
  // // 1. find today's rules
  // WsTester.TestRequest request = esTester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
  // request.setParam(WebService.Param.FIELDS, "");
  // request.setParam(PARAM_AVAILABLE_SINCE, DateUtils.formatDate(since));
  // request.setParam(WebService.Param.SORT, RuleIndexDefinition.FIELD_RULE_KEY);
  // WsTester.Result result = request.execute();
  // result.assertJson("{\"total\":2,\"p\":1,\"ps\":100,\"rules\":[{\"key\":\"xoo:x1\"},{\"key\":\"xoo:x2\"}]}");
  //
  // // 2. no rules since tomorrow
  // request = esTester.wsTester().newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
  // request.setParam(WebService.Param.FIELDS, "");
  // request.setParam(PARAM_AVAILABLE_SINCE, DateUtils.formatDate(DateUtils.addDays(since, 1)));
  // result = request.execute();
  // result.assertJson("{\"total\":0,\"p\":1,\"ps\":100,\"rules\":[]}");
  // }
  //
  // @Test
  // public void search_rules_with_deprecated_fields() throws Exception {
  // RuleDto ruleDto = RuleTesting.newXooX1(defaultOrganizationDto)
  // .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
  // .setDefRemediationGapMultiplier("1h")
  // .setDefRemediationBaseEffort("15min")
  // .setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
  // .setRemediationGapMultiplier("2h")
  // .setRemediationBaseEffort("25min");
  // insertRule(ruleDto.getDefinition());
  // ruleDao.insertOrUpdate(dbSession, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
  // dbSession.commit();
  //
  // WsTester.TestRequest request = esTester.wsTester()
  // .newGetRequest(API_ENDPOINT, API_SEARCH_METHOD)
  // .setParam(WebService.Param.FIELDS, "name,defaultDebtRemFn,debtRemFn,effortToFixDescription,debtOverloaded");
  // WsTester.Result result = request.execute();
  //
  // result.assertJson(getClass(), "search_rules_with_deprecated_fields.json");
  // }
  //
  // private ActiveRuleDto newActiveRule(QProfileDto profile, RuleDefinitionDto rule) {
  // return ActiveRuleDto.createFor(profile, rule)
  // .setInheritance(null)
  // .setSeverity("BLOCKER");
  // }
  //
  // private void insertRule(RuleDefinitionDto definition) {
  // ruleDao.insert(dbSession, definition);
  // dbSession.commit();
  // ruleIndexer.indexRuleDefinition(definition.getKey());
  // }
}
