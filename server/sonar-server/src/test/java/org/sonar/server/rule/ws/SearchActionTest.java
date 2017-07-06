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

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.RuleActivatorContextFactory;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.Rule;
import org.sonarqube.ws.Rules.SearchResponse;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.guava.api.Assertions.entry;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.db.rule.RuleTesting.setSystemTags;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_QPROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_RULE_KEY;

public class SearchActionTest {

  private static final String JAVA = "java";

  @org.junit.Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @org.junit.Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system2 = new AlwaysIncreasingSystem2();
  @org.junit.Rule
  public DbTester db = DbTester.create(system2);
  @org.junit.Rule
  public EsTester es = new EsTester(new RuleIndexDefinition(new MapSettings().asConfig()));

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private RuleIndex ruleIndex = new RuleIndex(es.client());
  private RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db.getDbClient(), es.client());
  private Languages languages = LanguageTesting.newLanguages(JAVA, "js");
  private ActiveRuleCompleter activeRuleCompleter = new ActiveRuleCompleter(db.getDbClient(), languages);
  private RuleWsSupport wsSupport = new RuleWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider);
  private RuleQueryFactory ruleQueryFactory = new RuleQueryFactory(db.getDbClient(), wsSupport);
  private MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  private RuleMapper ruleMapper = new RuleMapper(languages, macroInterpreter);
  private SearchAction underTest = new SearchAction(ruleIndex, activeRuleCompleter, ruleQueryFactory, db.getDbClient(), ruleMapper);

  private RuleActivatorContextFactory contextFactory = new RuleActivatorContextFactory(db.getDbClient());
  private TypeValidations typeValidations = new TypeValidations(asList(new StringTypeValidation(), new IntegerTypeValidation()));
  private RuleActivator ruleActivator = new RuleActivator(system2, db.getDbClient(), ruleIndex, contextFactory, typeValidations, activeRuleIndexer,
    userSession);
  private WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void before() {
    doReturn("interpreted").when(macroInterpreter).interpret(anyString());
  }

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.isPost()).isFalse();
    assertThat(def.since()).isEqualTo("4.4");
    assertThat(def.isInternal()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
    assertThat(def.params()).hasSize(23);

    WebService.Param compareToProfile = def.param("compareToProfile");
    assertThat(compareToProfile.since()).isEqualTo("6.5");
    assertThat(compareToProfile.isRequired()).isFalse();
    assertThat(compareToProfile.isInternal()).isTrue();
    assertThat(compareToProfile.description()).isEqualTo("Quality profile key to filter rules that are activated. Meant to compare easily to profile set in 'qprofile'");
  }

  @Test
  public void return_empty_result() {
    Rules.SearchResponse response = ws.newRequest()
      .setParam(WebService.Param.FIELDS, "actives")
      .executeProtobuf(Rules.SearchResponse.class);

    assertThat(response.getTotal()).isEqualTo(0L);
    assertThat(response.getP()).isEqualTo(1);
    assertThat(response.getRulesCount()).isEqualTo(0);
  }

  @Test
  public void return_all_rules() {
    RuleDefinitionDto rule1 = createJavaRule();
    RuleDefinitionDto rule2 = createJavaRule();
    indexRules();

    verify(r -> {
    }, rule1, rule2);
  }

  @Test
  public void filter_by_rule_key() {
    RuleDefinitionDto rule1 = createJavaRule();
    RuleDefinitionDto rule2 = createJavaRule();
    indexRules();

    verify(r -> r.setParam(PARAM_RULE_KEY, rule1.getKey().toString()), rule1);
    verifyNoResults(r -> r.setParam(PARAM_RULE_KEY, "missing"));
  }

  @Test
  public void return_all_rule_fields_by_default() {
    RuleDefinitionDto rule = createJavaRule();
    indexRules();

    Rules.SearchResponse response = ws.newRequest().executeProtobuf(Rules.SearchResponse.class);
    Rules.Rule result = response.getRules(0);
    assertThat(result.getCreatedAt()).isNotEmpty();
    assertThat(result.getEffortToFixDescription()).isNotEmpty();
    assertThat(result.getHtmlDesc()).isNotEmpty();
    assertThat(result.hasIsTemplate()).isTrue();
    assertThat(result.getLang()).isEqualTo(rule.getLanguage());
    assertThat(result.getLangName()).isEqualTo(languages.get(rule.getLanguage()).getName());
    assertThat(result.getName()).isNotEmpty();
    assertThat(result.getRepo()).isNotEmpty();
    assertThat(result.getSeverity()).isNotEmpty();
    assertThat(result.getType().name()).isEqualTo(RuleType.valueOf(rule.getType()).name());
  }

  @Test
  public void return_subset_of_fields() {
    RuleDefinitionDto rule = createJavaRule();
    indexRules();

    Rules.SearchResponse response = ws.newRequest()
      .setParam(WebService.Param.FIELDS, "createdAt,langName")
      .executeProtobuf(Rules.SearchResponse.class);
    Rules.Rule result = response.getRules(0);

    // mandatory fields
    assertThat(result.getKey()).isEqualTo(rule.getKey().toString());
    assertThat(result.getType().getNumber()).isEqualTo(rule.getType());

    // selected fields
    assertThat(result.getCreatedAt()).isNotEmpty();
    assertThat(result.getLangName()).isNotEmpty();

    // not returned fields
    assertThat(result.hasEffortToFixDescription()).isFalse();
    assertThat(result.hasHtmlDesc()).isFalse();
    assertThat(result.hasIsTemplate()).isFalse();
    assertThat(result.hasLang()).isFalse();
    assertThat(result.hasName()).isFalse();
    assertThat(result.hasSeverity()).isFalse();
    assertThat(result.hasRepo()).isFalse();
  }

  @Test
  public void should_filter_on_organization_specific_tags() throws IOException {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto rule1 = createJavaRule();
    RuleMetadataDto metadata1 = insertMetadata(organization, rule1, setTags("tag1", "tag2"));
    RuleDefinitionDto rule2 = createJavaRule();
    RuleMetadataDto metadata2 = insertMetadata(organization, rule2);
    indexRules();

    Consumer<TestRequest> request = r -> r
      .setParam("f", "repo,name")
      .setParam("tags", metadata1.getTags().stream().collect(Collectors.joining(",")))
      .setParam("organization", organization.getKey());
    verify(request, rule1);
  }

  @Test
  public void when_searching_for_several_tags_combine_them_with_OR() throws IOException {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto bothTagsRule = createJavaRule();
    insertMetadata(organization, bothTagsRule, setTags("tag1", "tag2"));
    RuleDefinitionDto oneTagRule = createJavaRule();
    insertMetadata(organization, oneTagRule, setTags("tag1"));
    RuleDefinitionDto otherTagRule = createJavaRule();
    insertMetadata(organization, otherTagRule, setTags("tag2"));
    RuleDefinitionDto noTagRule = createJavaRule();
    insertMetadata(organization, noTagRule, setTags());
    indexRules();

    Consumer<TestRequest> request = r -> r
      .setParam("f", "repo,name")
      .setParam("tags", "tag1,tag2")
      .setParam("organization", organization.getKey());
    verify(request, bothTagsRule, oneTagRule, otherTagRule);
  }

  @Test
  public void should_list_tags_in_tags_facet() throws IOException {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto rule = db.rules().insert(setSystemTags("tag1", "tag3", "tag5", "tag7", "tag9", "x"));
    RuleMetadataDto metadata = insertMetadata(organization, rule, setTags("tag2", "tag4", "tag6", "tag8", "tagA"));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("facets", "tags")
      .setParam("organization", organization.getKey())
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getFacets().getFacets(0).getValuesList()).extracting(v -> entry(v.getVal(), v.getCount()))
      .containsExactly(entry("tag1", 1L), entry("tag2", 1L), entry("tag3", 1L), entry("tag4", 1L), entry("tag5", 1L), entry("tag6", 1L), entry("tag7", 1L), entry("tag8", 1L),
        entry("tag9", 1L), entry("tagA", 1L));
  }

  @Test
  public void should_include_selected_matching_tag_in_facet() throws IOException {
    RuleDefinitionDto rule = db.rules().insert(setSystemTags("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tagA", "x"));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("facets", "tags")
      .setParam("tags", "x")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getFacets().getFacets(0).getValuesList()).extracting(v -> entry(v.getVal(), v.getCount())).contains(entry("x", 1L));
  }

  @Test
  public void should_included_selected_non_matching_tag_in_facet() throws IOException {
    RuleDefinitionDto rule = db.rules().insert(setSystemTags("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tagA"));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("facets", "tags")
      .setParam("tags", "x")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getFacets().getFacets(0).getValuesList()).extracting(v -> entry(v.getVal(), v.getCount())).contains(entry("x", 0L));
  }

  @Test
  public void should_return_organization_specific_tags() throws IOException {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto rule = createJavaRule();
    RuleMetadataDto metadata = insertMetadata(organization, rule, setTags("tag1", "tag2"));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "tags")
      .setParam("organization", organization.getKey())
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
    assertThat(result.getRulesList())
      .extracting(Rule::getTags).flatExtracting(Rules.Tags::getTagsList)
      .containsExactly(metadata.getTags().toArray(new String[0]));
  }

  @Test
  public void should_not_return_tags_of_foreign_organization() throws IOException {
    OrganizationDto organizationWithSpecificTags = db.organizations().insert();
    OrganizationDto myOrganization = db.organizations().insert();
    RuleDefinitionDto rule = db.rules().insert(setSystemTags("system1", "system2"));
    insertMetadata(organizationWithSpecificTags, rule, setTags("tag1", "tag2"));
    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("facets", "tags")
      .setParam("f", "tags")
      .setParam("organization", myOrganization.getKey())
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
    assertThat(result.getFacets().getFacets(0).getValuesList())
      .extracting(v -> tuple(v.getVal(), v.getCount()))
      .containsExactly(
        tuple("system1", 1L),
        tuple("system2", 1L)
      );
  }

  @Test
  public void should_return_specified_fields() throws Exception {
    RuleDefinitionDto rule = createJavaRule();
    indexRules();

    checkField(rule, "repo", Rule::getRepo, rule.getRepositoryKey());
    checkField(rule, "name", Rule::getName, rule.getName());
    checkField(rule, "severity", Rule::getSeverity, rule.getSeverityString());
    checkField(rule, "status", r -> r.getStatus().toString(), rule.getStatus().toString());
    checkField(rule, "internalKey", Rule::getInternalKey, rule.getConfigKey());
    checkField(rule, "isTemplate", Rule::getIsTemplate, rule.isTemplate());
    checkField(rule, "sysTags",
      r -> r.getSysTags().getSysTagsList().stream().collect(Collectors.joining(",")),
      rule.getSystemTags().stream().collect(Collectors.joining(",")));
    checkField(rule, "lang", Rule::getLang, rule.getLanguage());
    checkField(rule, "langName", Rule::getLangName, languages.get(rule.getLanguage()).getName());
    checkField(rule, "gapDescription", Rule::getGapDescription, rule.getGapDescription());
    // to be continued...
  }

  @Test
  public void return_lang_key_field_when_language_name_is_not_available() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    String unknownLanguage = "unknown_" + randomAlphanumeric(5);
    RuleDefinitionDto rule = db.rules().insert(r -> r.setLanguage(unknownLanguage));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "langName")
      .setParam("organization", organization.getKey())
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRulesCount()).isEqualTo(1);

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getLangName()).isEqualTo(unknownLanguage);
  }

  @Test
  public void search_debt_rules_with_default_and_overridden_debt_values() throws Exception {
    RuleDefinitionDto rule = db.rules().insert(r ->
      r.setLanguage("java")
        .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
        .setDefRemediationGapMultiplier("1h")
        .setDefRemediationBaseEffort("15min"));

    RuleMetadataDto metadata = insertMetadata(db.getDefaultOrganization(), rule,
      r -> r.setRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
        .setRemediationGapMultiplier("2h")
        .setRemediationBaseEffort("25min"));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "debtRemFn,debtOverloaded,defaultDebtRemFn")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRulesCount()).isEqualTo(1);

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getDefaultDebtRemFnCoeff()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultDebtRemFnOffset()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getDefaultRemFnBaseEffort()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultRemFnGapMultiplier()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getDebtOverloaded()).isTrue();
    assertThat(searchedRule.getDebtRemFnCoeff()).isEqualTo("2h");
    assertThat(searchedRule.getDebtRemFnOffset()).isEqualTo("25min");
    assertThat(searchedRule.getDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
  }

  @Test
  public void search_debt_rules_with_default_linear_offset_and_overridden_constant_debt() throws Exception {
    RuleDefinitionDto rule = db.rules().insert(r ->
      r.setLanguage("java")
        .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
        .setDefRemediationGapMultiplier("1h")
        .setDefRemediationBaseEffort("15min"));

    RuleMetadataDto metadata = insertMetadata(db.getDefaultOrganization(), rule,
      r -> r.setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
        .setRemediationGapMultiplier(null)
        .setRemediationBaseEffort("5min"));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "debtRemFn,debtOverloaded,defaultDebtRemFn")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRulesCount()).isEqualTo(1);

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getDefaultDebtRemFnCoeff()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultDebtRemFnOffset()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getDefaultRemFnBaseEffort()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultRemFnGapMultiplier()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getDebtOverloaded()).isTrue();
    assertThat(searchedRule.getDebtRemFnCoeff()).isEmpty();
    ;
    assertThat(searchedRule.getDebtRemFnOffset()).isEqualTo("5min");
    assertThat(searchedRule.getDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
  }

  @Test
  public void search_debt_rules_with_default_linear_offset_and_overridden_linear_debt() throws Exception {
    RuleDefinitionDto rule = db.rules().insert(r ->
      r.setLanguage("java")
        .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
        .setDefRemediationGapMultiplier("1h")
        .setDefRemediationBaseEffort("15min"));

    RuleMetadataDto metadata = insertMetadata(db.getDefaultOrganization(), rule,
      r -> r.setRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
        .setRemediationGapMultiplier("1h")
        .setRemediationBaseEffort(null));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "debtRemFn,debtOverloaded,defaultDebtRemFn")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRulesCount()).isEqualTo(1);

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getDefaultDebtRemFnCoeff()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultDebtRemFnOffset()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getDefaultRemFnBaseEffort()).isEqualTo("15min");
    assertThat(searchedRule.getDefaultRemFnGapMultiplier()).isEqualTo("1h");
    assertThat(searchedRule.getDefaultRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(searchedRule.getDebtOverloaded()).isTrue();
    assertThat(searchedRule.getDebtRemFnCoeff()).isEqualTo("1h");
    assertThat(searchedRule.getDebtRemFnOffset()).isEmpty();
    assertThat(searchedRule.getDebtRemFnType()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
  }

  @Test
  public void search_template_rules() throws Exception {
    RuleDefinitionDto templateRule = db.rules().insert(r ->
      r.setLanguage("java")
        .setIsTemplate(true));
    RuleDefinitionDto rule = db.rules().insert(r ->
      r.setLanguage("java")
        .setTemplateId(templateRule.getId()));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "isTemplate")
      .setParam("is_template", "true")
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRulesCount()).isEqualTo(1);

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getIsTemplate()).isTrue();
    assertThat(searchedRule.getKey()).isEqualTo(templateRule.getRepositoryKey() + ":" + templateRule.getRuleKey());
  }

  @Test
  public void search_custom_rules_from_template_key() throws Exception {
    RuleDefinitionDto templateRule = db.rules().insert(r ->
      r.setLanguage("java")
        .setIsTemplate(true));
    RuleDefinitionDto rule = db.rules().insert(r ->
      r.setLanguage("java")
        .setTemplateId(templateRule.getId()));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "templateKey")
      .setParam("template_key", templateRule.getRepositoryKey() + ":" + templateRule.getRuleKey())
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRulesCount()).isEqualTo(1);

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getKey()).isEqualTo(rule.getRepositoryKey() + ":" + rule.getRuleKey());
    assertThat(searchedRule.getTemplateKey()).isEqualTo(templateRule.getRepositoryKey() + ":" + templateRule.getRuleKey());
  }

  @Test
  public void search_all_active_rules() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage("java"));
    RuleDefinitionDto rule = createJavaRule();
    RuleActivation activation = RuleActivation.create(rule.getKey(), BLOCKER, null);
    ruleActivator.activate(db.getSession(), activation, profile);

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "")
      .setParam("q", rule.getName())
      .setParam("activation", "true")
      .setParam("organization", organization.getKey())
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRulesCount()).isEqualTo(1);

    Rule searchedRule = result.getRules(0);
    assertThat(searchedRule).isNotNull();
    assertThat(searchedRule.getKey()).isEqualTo(rule.getRepositoryKey() + ":" + rule.getRuleKey());
    assertThat(searchedRule.getName()).isEqualTo(rule.getName());
  }

  @Test
  public void search_profile_active_rules() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage("java"));
    QProfileDto waterproofProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage("java"));

    RuleDefinitionDto rule = createJavaRule();

    RuleParamDto ruleParam1 = db.rules().insertRuleParam(rule, p ->
      p.setDefaultValue("some value")
        .setType("STRING")
        .setDescription("My small description")
        .setName("my_var"));

    RuleParamDto ruleParam2 = db.rules().insertRuleParam(rule, p ->
      p.setDefaultValue("1")
        .setType("INTEGER")
        .setDescription("My small description")
        .setName("the_var"));

    // SONAR-7083
    RuleParamDto ruleParam3 = db.rules().insertRuleParam(rule, p ->
      p.setDefaultValue(null)
        .setType("STRING")
        .setDescription("Empty Param")
        .setName("empty_var"));

    RuleActivation activation = RuleActivation.create(rule.getKey());
    List<ActiveRuleChange> activeRuleChanges1 = ruleActivator.activate(db.getSession(), activation, profile);
    ruleActivator.activate(db.getSession(), activation, waterproofProfile);

    assertThat(activeRuleChanges1).hasSize(1);

    db.commit();

    indexRules();
    indexActiveRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "actives")
      .setParam("q", rule.getName())
      .setParam("organization", organization.getKey())
      .setParam("activation", "true")
      .setParam("qprofile", profile.getKee())
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRulesCount()).isEqualTo(1);
    assertThat(result.getActives()).isNotNull();
    assertThat(result.getActives().getActives().get(rule.getKey().toString())).isNotNull();
    assertThat(result.getActives().getActives().get(rule.getKey().toString()).getActiveListList()).hasSize(1);

    Rules.Active activeList = result.getActives().getActives().get(rule.getKey().toString()).getActiveList(0);

    // The rule without value is not inserted
    assertThat(activeList.getParamsCount()).isEqualTo(2);
    assertThat(activeList.getParamsList()).extracting("key", "value").containsExactlyInAnyOrder(
      tuple(ruleParam1.getName(), ruleParam1.getDefaultValue()),
      tuple(ruleParam2.getName(), ruleParam2.getDefaultValue())
    );

    String unknownProfile = "unknown_profile" + randomAlphanumeric(5);
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("The specified qualityProfile '" + unknownProfile + "' does not exist");

    ws.newRequest()
      .setParam("activation", "true")
      .setParam("qprofile", unknownProfile)
      .executeProtobuf(SearchResponse.class);
  }

  @Test
  public void search_for_active_rules_when_parameter_value_is_null() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage("java"));

    RuleDefinitionDto rule = createJavaRule();

    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p ->
      p.setDefaultValue("some value")
        .setType("STRING")
        .setDescription("My small description")
        .setName("my_var"));

    RuleActivation activation = RuleActivation.create(rule.getKey());
    List<ActiveRuleChange> activeRuleChanges = ruleActivator.activate(db.getSession(), activation, profile);

    // Insert directly in database a rule parameter with a null value
    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(ruleParam).setValue(null);
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRuleChanges.get(0).getActiveRule(), activeRuleParam);

    db.commit();

    indexRules();
    indexActiveRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "actives")
      .setParam("q", rule.getName())
      .setParam("organization", organization.getKey())
      .setParam("activation", "true")
      .setParam("qprofile", profile.getKee())
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getTotal()).isEqualTo(1);
    assertThat(result.getRulesCount()).isEqualTo(1);
    assertThat(result.getActives()).isNotNull();
    assertThat(result.getActives().getActives().get(rule.getKey().toString())).isNotNull();
    assertThat(result.getActives().getActives().get(rule.getKey().toString()).getActiveListList()).hasSize(1);

    Rules.Active activeList = result.getActives().getActives().get(rule.getKey().toString()).getActiveList(0);
    assertThat(activeList.getParamsCount()).isEqualTo(2);
    assertThat(activeList.getParamsList()).extracting("key", "value").containsExactlyInAnyOrder(
      tuple(ruleParam.getName(), ruleParam.getDefaultValue()),
      tuple(activeRuleParam.getKey(), "")
    );
  }

  @Test
  public void statuses_facet_should_be_sticky() throws Exception {
    RuleDefinitionDto rule1 = db.rules().insert(r -> r.setLanguage("java"));
    RuleDefinitionDto rule2 = db.rules().insert(r -> r.setLanguage("java").setStatus(RuleStatus.BETA));
    RuleDefinitionDto rule3 = db.rules().insert(r -> r.setLanguage("java").setStatus(RuleStatus.DEPRECATED));

    indexRules();

    SearchResponse result = ws.newRequest()
      .setParam("f", "status")
      .setParam("status", "DEPRECATED")
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getRulesCount()).isEqualTo(3);
    assertThat(result.getRulesList()).extracting("key", "status.name").containsExactlyInAnyOrder(
      tuple(rule1.getKey().toString(), rule1.getStatus().name()),
      tuple(rule2.getKey().toString(), rule2.getStatus().name()),
      tuple(rule3.getKey().toString(), rule3.getStatus().name())
    );
  }

  @Test
  public void compare_to_another_profile() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(JAVA));
    QProfileDto anotherProfile = db.qualityProfiles().insert(organization, p -> p.setLanguage(JAVA));
    RuleDefinitionDto commonRule = db.rules().insertRule(r -> r.setLanguage(JAVA)).getDefinition();
    RuleDefinitionDto profileRule1 = db.rules().insertRule(r -> r.setLanguage(JAVA)).getDefinition();
    RuleDefinitionDto profileRule2 = db.rules().insertRule(r -> r.setLanguage(JAVA)).getDefinition();
    RuleDefinitionDto profileRule3 = db.rules().insertRule(r -> r.setLanguage(JAVA)).getDefinition();
    RuleDefinitionDto anotherProfileRule1 = db.rules().insertRule(r -> r.setLanguage(JAVA)).getDefinition();
    RuleDefinitionDto anotherProfileRule2 = db.rules().insertRule(r -> r.setLanguage(JAVA)).getDefinition();
    db.qualityProfiles().activateRule(profile, commonRule);
    db.qualityProfiles().activateRule(profile, profileRule1);
    db.qualityProfiles().activateRule(profile, profileRule2);
    db.qualityProfiles().activateRule(profile, profileRule3);
    db.qualityProfiles().activateRule(anotherProfile, commonRule);
    db.qualityProfiles().activateRule(anotherProfile, anotherProfileRule1);
    db.qualityProfiles().activateRule(anotherProfile, anotherProfileRule2);
    indexRules();
    indexActiveRules();

    SearchResponse result = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QPROFILE, profile.getKee())
      .setParam(PARAM_ACTIVATION, "false")
      .setParam(PARAM_COMPARE_TO_PROFILE, anotherProfile.getKee())
      .executeProtobuf(SearchResponse.class);

    assertThat(result.getRulesList())
      .extracting(Rule::getKey)
      .containsExactlyInAnyOrder(anotherProfileRule1.getKey().toString(), anotherProfileRule2.getKey().toString());
  }

  @SafeVarargs
  private final <T> void checkField(RuleDefinitionDto rule, String fieldName, Extractor<Rule, T> responseExtractor, T... expected) throws IOException {
    SearchResponse result = ws.newRequest()
      .setParam("f", fieldName)
      .executeProtobuf(SearchResponse.class);
    assertThat(result.getRulesList()).extracting(Rule::getKey).containsExactly(rule.getKey().toString());
    assertThat(result.getRulesList()).extracting(responseExtractor).containsExactly(expected);
  }

  @SafeVarargs
  private final RuleMetadataDto insertMetadata(OrganizationDto organization, RuleDefinitionDto rule, Consumer<RuleMetadataDto>... populaters) {
    RuleMetadataDto metadata = db.rules().insertOrUpdateMetadata(rule, organization, populaters);
    ruleIndexer.commitAndIndex(db.getSession(), rule.getKey(), organization);
    return metadata;
  }

  private void verifyNoResults(Consumer<TestRequest> requestPopulator) {
    verify(requestPopulator);
  }

  private void verify(Consumer<TestRequest> requestPopulator, RuleDefinitionDto... expectedRules) {
    TestRequest request = ws.newRequest();
    requestPopulator.accept(request);
    Rules.SearchResponse response = request
      .executeProtobuf(Rules.SearchResponse.class);

    assertThat(response.getP()).isEqualTo(1);
    assertThat(response.getTotal()).isEqualTo(expectedRules.length);
    assertThat(response.getRulesCount()).isEqualTo(expectedRules.length);
    RuleKey[] expectedRuleKeys = stream(expectedRules).map(RuleDefinitionDto::getKey).collect(MoreCollectors.toList()).toArray(new RuleKey[0]);
    assertThat(response.getRulesList())
      .extracting(r -> RuleKey.parse(r.getKey()))
      .containsExactlyInAnyOrder(expectedRuleKeys);
  }

  private void indexRules() {
    ruleIndexer.indexOnStartup(ruleIndexer.getIndexTypes());
  }

  private void indexActiveRules() {
    activeRuleIndexer.indexOnStartup(activeRuleIndexer.getIndexTypes());
  }

  private RuleDefinitionDto createJavaRule() {
    return db.rules().insert(r -> r.setLanguage("java"));
  }
}
