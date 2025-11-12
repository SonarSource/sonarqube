/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.rule.index;

import com.google.common.collect.Sets;
import io.sonarcloud.compliancereports.reports.CategoryTree;
import io.sonarcloud.compliancereports.reports.ComplianceCategoryRules;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.CleanCodeAttributeCategory;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.security.SecurityStandards;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Set.of;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;
import static org.sonar.core.rule.RuleType.BUG;
import static org.sonar.core.rule.RuleType.CODE_SMELL;
import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;
import static org.sonar.core.rule.RuleType.VULNERABILITY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.rule.RuleTesting.setCleanCodeAttribute;
import static org.sonar.db.rule.RuleTesting.setCreatedAt;
import static org.sonar.db.rule.RuleTesting.setImpacts;
import static org.sonar.db.rule.RuleTesting.setIsExternal;
import static org.sonar.db.rule.RuleTesting.setIsTemplate;
import static org.sonar.db.rule.RuleTesting.setLanguage;
import static org.sonar.db.rule.RuleTesting.setName;
import static org.sonar.db.rule.RuleTesting.setRepositoryKey;
import static org.sonar.db.rule.RuleTesting.setRuleKey;
import static org.sonar.db.rule.RuleTesting.setSecurityStandards;
import static org.sonar.db.rule.RuleTesting.setSeverity;
import static org.sonar.db.rule.RuleTesting.setStatus;
import static org.sonar.db.rule.RuleTesting.setSystemTags;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.db.rule.RuleTesting.setTemplateId;
import static org.sonar.db.rule.RuleTesting.setType;
import static org.sonar.db.rule.RuleTesting.setUpdatedAt;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.INHERITED;
import static org.sonar.server.qualityprofile.ActiveRuleInheritance.OVERRIDES;
import static org.sonar.server.rule.index.RuleIndex.COMPLIANCE_FILTER_FACET;
import static org.sonar.server.rule.index.RuleIndex.FACET_LANGUAGES;
import static org.sonar.server.rule.index.RuleIndex.FACET_REPOSITORIES;
import static org.sonar.server.rule.index.RuleIndex.FACET_TAGS;
import static org.sonar.server.rule.index.RuleIndex.FACET_TYPES;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_INSECURE_INTERACTION;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_RISKY_RESOURCE;

class RuleIndexIT {

  private final System2 system2 = new AlwaysIncreasingSystem2();

  @RegisterExtension
  private final EsTester es = EsTester.create();
  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private final ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db.getDbClient(), es.client());
  private final Configuration config = mock(Configuration.class);

  private final RuleIndex underTest = new RuleIndex(es.client(), system2, config);
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @Test
  void search_all_rules() {
    createRule();
    createRule();
    index();

    SearchIdResult<String> results = underTest.searchV2(new RuleQuery(), new SearchOptions());

    assertThat(results.getTotal()).isEqualTo(2);
    assertThat(results.getUuids()).hasSize(2);
  }

  @Test
  void search_by_key() {
    RuleDto js1 = createRule(
      setRepositoryKey("javascript"),
      setRuleKey("X001"));
    RuleDto cobol1 = createRule(
      setRepositoryKey("cobol"),
      setRuleKey("X001"));
    createRule(
      setRepositoryKey("php"),
      setRuleKey("S002"));
    index();

    // key
    RuleQuery query = new RuleQuery().setQueryText("X001");
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).containsOnly(js1.getUuid(), cobol1.getUuid());

    // partial key does not match
    query = new RuleQuery().setQueryText("X00");
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).isEmpty();

    // repo:key -> nice-to-have !
    query = new RuleQuery().setQueryText("javascript:X001");
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).containsOnly(js1.getUuid());
  }

  @Test
  void search_by_case_insensitive_key() {
    RuleDto ruleDto = createRule(
      setRepositoryKey("javascript"),
      setRuleKey("X001"));
    index();

    RuleQuery query = new RuleQuery().setQueryText("x001");
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).containsOnly(ruleDto.getUuid());
  }

  @Test
  void filter_by_key() {
    createRule(
      setRepositoryKey("javascript"),
      setRuleKey("X001"));
    createRule(
      setRepositoryKey("cobol"),
      setRuleKey("X001"));
    createRule(
      setRepositoryKey("php"),
      setRuleKey("S002"));
    index();

    // key
    RuleQuery query = new RuleQuery().setKey(RuleKey.of("javascript", "X001").toString());

    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(1);

    // partial key does not match
    query = new RuleQuery().setKey("X001");
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).isEmpty();
  }

  @Test
  void filter_by_compliance_category_rules() {
    RuleDto rule = createRule(setRepositoryKey("javascript"), setRuleKey("X001"));
    RuleDto rule1 = createRule(setRepositoryKey("cobol"), setRuleKey("X001"));
    RuleDto rule2 = createRule(setRepositoryKey("php"), setRuleKey("S002"));
    createRule(setRepositoryKey("java"), setRuleKey("S002"));
    index();
    ComplianceCategoryRules complianceCategoryRules = new ComplianceCategoryRules(new CategoryTree.CategoryTreeNode(
      "key", Set.of("php:S002", ":X001"), Set.of(), null, false, 0
    ));

    // key
    RuleQuery query = new RuleQuery()
      .setComplianceCategoryRules(List.of(complianceCategoryRules));

    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids())
      .containsOnly(rule.getUuid(), rule1.getUuid(), rule2.getUuid());
  }

  @Test
  void search_name_by_query() {
    createRule(setName("testing the partial match and matching of rule"));
    index();

    // substring
    RuleQuery query = new RuleQuery().setQueryText("test");
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(1);

    // substring
    query = new RuleQuery().setQueryText("partial match");
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(1);

    // case-insensitive
    query = new RuleQuery().setQueryText("TESTING");
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(1);

    // not found
    query = new RuleQuery().setQueryText("not present");
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).isEmpty();
  }

  @Test
  void search_name_with_protected_chars() {
    RuleDto rule = createRule(setName("ja#va&sc\"r:ipt"));
    index();

    RuleQuery protectedCharsQuery = new RuleQuery().setQueryText(rule.getName());
    List<String> results = underTest.searchV2(protectedCharsQuery, new SearchOptions()).getUuids();
    assertThat(results).containsOnly(rule.getUuid());
  }

  @Test
  void search_content_by_query() {
    // it's important to set all the fields being used by the search (name, desc, key, lang, ...),
    // otherwise the generated random values may raise false-positives
    RuleDto rule1 = insertJavaRule("My great rule CWE-123 which makes your code 1000 times better!", "123", "rule 123");
    RuleDto rule2 = insertJavaRule("Another great and shiny rule CWE-124", "124", "rule 124");
    RuleDto rule3 = insertJavaRule("Another great rule CWE-1000", "1000", "rule 1000");
    RuleDto rule4 = insertJavaRule("<h1>HTML-Geeks</h1><p style=\"color:blue\">special " +
      "formatting!</p><table><tr><td>inside</td><td>tables</td></tr></table>", "404", "rule 404");
    RuleDto rule5 = insertJavaRule("internationalization missunderstandings alsdkjfnadklsjfnadkdfnsksdjfn", "405", "rule 405");
    index();

    // partial match at word boundary
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("CWE"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule1.getUuid(), rule2.getUuid(),
      rule3.getUuid());

    // full match
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("CWE-123"), new SearchOptions()).getUuids()).containsExactly(rule1.getUuid());

    // match somewhere else in the text
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("CWE-1000"), new SearchOptions()).getUuids()).containsExactly(rule3.getUuid());
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("CWE 1000"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule3.getUuid(), rule1.getUuid());

    // several words
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("great rule"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule1.getUuid(), rule2.getUuid(),
      rule3.getUuid());
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("rule Another"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule2.getUuid(), rule3.getUuid());

    // no matches
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("unexisting"), new SearchOptions()).getUuids()).isEmpty();
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("great rule unexisting"), new SearchOptions()).getUuids()).isEmpty();

    // stopwords
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("and"), new SearchOptions()).getUuids()).isEmpty();
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("great and shiny"), new SearchOptions()).getUuids()).isEmpty();

    // html
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("h1"), new SearchOptions()).getUuids()).isEmpty();
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("style"), new SearchOptions()).getUuids()).isEmpty();
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("special"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule4.getUuid());
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("geeks formatting inside tables"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule4.getUuid());

    // long words
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("missunderstand"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule5.getUuid());
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("missunderstandings"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule5.getUuid());
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("alsdkjfnadklsjfnadkdfnsksdjfn"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule5.getUuid());
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("internationalization"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule5.getUuid());
    assertThat(underTest.searchV2(new RuleQuery().setQueryText("internationalizationBlaBla"), new SearchOptions()).getUuids()).isEmpty();
  }

  private RuleDto insertJavaRule(String description, String ruleKey, String name) {
    RuleDto javaRule = newRule(createDefaultRuleDescriptionSection(uuidFactory.create(), description))
      .setLanguage("java")
      .setRuleKey(ruleKey)
      .setName(name);
    return db.rules().insert(javaRule);
  }

  @Test
  void search_by_any_of_repositories() {
    RuleDto findbugs = createRule(
      setRepositoryKey("findbugs"),
      setRuleKey("S001"));
    RuleDto pmd = createRule(
      setRepositoryKey("pmd"),
      setRuleKey("S002"));
    index();

    RuleQuery query = new RuleQuery().setRepositories(asList("checkstyle", "pmd"));
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsExactly(pmd.getUuid());

    // no results
    query = new RuleQuery().setRepositories(singletonList("checkstyle"));
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setRepositories(emptyList());
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).containsOnly(findbugs.getUuid(), pmd.getUuid());
  }

  @Test
  void filter_by_tags() {
    RuleDto rule1 = createRule(setSystemTags("tag1s"), setTags("tag1"));
    RuleDto rule2 = createRule(setSystemTags("tag2s"), setTags("tag2"));
    index();

    assertThat(es.countDocuments(TYPE_RULE)).isEqualTo(2);
    // tag2s in filter
    RuleQuery query = new RuleQuery().setTags(of("tag2s"));
    verifySearch(query, rule2);

    // tag2 in filter
    query = new RuleQuery().setTags(of("tag2"));
    verifySearch(query, rule2);

    // empty list => no filter
    query = new RuleQuery().setTags(emptySet());
    verifySearch(query, rule1, rule2);

    // null list => no filter
    query = new RuleQuery().setTags(null);
    verifySearch(query, rule1, rule2);
  }

  @Test
  void tags_facet_supports_selected_value_with_regexp_special_characters() {
    createRule(r -> r.setTags(of("misra++")));
    index();

    RuleQuery query = new RuleQuery()
      .setTags(singletonList("misra["));
    SearchOptions options = new SearchOptions().addFacets(FACET_TAGS);

    // do not fail
    assertThat(underTest.searchV2(query, options).getTotal()).isZero();
  }

  @Test
  void search_by_types() {
    createRule(setType(CODE_SMELL));
    RuleDto vulnerability = createRule(setType(VULNERABILITY));
    RuleDto bug1 = createRule(setType(BUG));
    RuleDto bug2 = createRule(setType(BUG));
    index();

    // find all
    RuleQuery query = new RuleQuery();
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(4);

    // type3 in filter
    query = new RuleQuery().setTypes(of(VULNERABILITY));
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).containsOnly(vulnerability.getUuid());

    query = new RuleQuery().setTypes(of(BUG));
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).containsOnly(bug1.getUuid(), bug2.getUuid());

    // types in query => nothing
    query = new RuleQuery().setQueryText("code smell bug vulnerability");
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).isEmpty();

    // null list => no filter
    query = new RuleQuery().setTypes(emptySet());
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(4);

    // null list => no filter
    query = new RuleQuery().setTypes(null);
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(4);
  }

  @Test
  void search_by_is_template() {
    RuleDto ruleNoTemplate = createRule(setIsTemplate(false));
    RuleDto ruleIsTemplate = createRule(setIsTemplate(true));
    index();

    // find all
    RuleQuery query = new RuleQuery();
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).hasSize(2);

    // Only template
    query = new RuleQuery().setIsTemplate(true);
    results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ruleIsTemplate.getUuid());

    // Only not template
    query = new RuleQuery().setIsTemplate(false);
    results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ruleNoTemplate.getUuid());

    // null => no filter
    query = new RuleQuery().setIsTemplate(null);
    results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ruleIsTemplate.getUuid(), ruleNoTemplate.getUuid());
  }

  @Test
  void search_by_is_external() {
    RuleDto ruleIsNotExternal = createRule(setIsExternal(false));
    RuleDto ruleIsExternal = createRule(setIsExternal(true));
    index();

    // Only external
    RuleQuery query = new RuleQuery().setIncludeExternal(true);
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ruleIsExternal.getUuid(), ruleIsNotExternal.getUuid());

    // Only not external
    query = new RuleQuery().setIncludeExternal(false);
    results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ruleIsNotExternal.getUuid());
  }

  @Test
  void search_by_template_key() {
    RuleDto template = createRule(setIsTemplate(true));
    RuleDto customRule = createRule(setTemplateId(template.getUuid()));
    index();

    // find all
    RuleQuery query = new RuleQuery();
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).hasSize(2);

    // Only custom rule
    query = new RuleQuery().setTemplateKey(template.getKey().toString());
    results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(customRule.getUuid());

    // null => no filter
    query = new RuleQuery().setTemplateKey(null);
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(2);
  }

  @Test
  void search_by_any_of_languages() {
    createRule(setLanguage("java"));
    RuleDto javascript = createRule(setLanguage("js"));
    index();

    RuleQuery query = new RuleQuery().setLanguages(asList("cobol", "js"));
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(javascript.getUuid());

    // no results
    query = new RuleQuery().setLanguages(singletonList("cpp"));
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setLanguages(emptyList());
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setLanguages(null);
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void search_by_security_cwe_return_correct_data_based_on_mode(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    RuleDto rule1 = createRule(setSecurityStandards(of("cwe:543", "cwe:123", "owaspTop10:a1")),
      r -> r.setType(VULNERABILITY).replaceAllDefaultImpacts(List.of(new ImpactDto(SECURITY, Severity.HIGH))));
    RuleDto rule2 = createRule(setSecurityStandards(of("cwe:543", "owaspTop10:a1")), r -> r.setType(SECURITY_HOTSPOT));
    createRule(setSecurityStandards(of("owaspTop10:a1")),
      r -> r.setType(CODE_SMELL).replaceAllDefaultImpacts(List.of(new ImpactDto(MAINTAINABILITY, Severity.HIGH))));
    index();

    RuleQuery query = new RuleQuery().setCwe(of("543"));
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions().addFacets("cwe"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule2.getUuid());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void search_by_security_owaspTop10_2017_return_correct_data_based_on_mode(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    RuleDto rule1 = createRule(setSecurityStandards(of("owaspTop10:a1", "owaspTop10:a10", "cwe:543")),
      r -> r.setType(VULNERABILITY).replaceAllDefaultImpacts(List.of(new ImpactDto(SECURITY, Severity.HIGH))));
    RuleDto rule2 = createRule(setSecurityStandards(of("owaspTop10:a10", "cwe:543")), r -> r.setType(SECURITY_HOTSPOT));
    createRule(setSecurityStandards(of("cwe:543")),
      r -> r.setType(CODE_SMELL).replaceAllDefaultImpacts(List.of(new ImpactDto(MAINTAINABILITY, Severity.HIGH))));
    index();

    RuleQuery query = new RuleQuery().setOwaspTop10(of("a5", "a10"));
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions().addFacets("owaspTop10"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule2.getUuid());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void search_by_security_owaspTop10_2021_return_correct_data_based_on_mode(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    RuleDto rule1 = createRule(setSecurityStandards(of("owaspTop10-2021:a1", "owaspTop10-2021:a10", "cwe:543")),
      r -> r.setType(VULNERABILITY).replaceAllDefaultImpacts(List.of(new ImpactDto(SECURITY, Severity.HIGH))));
    RuleDto rule2 = createRule(setSecurityStandards(of("owaspTop10-2021:a10", "cwe:543")), r -> r.setType(SECURITY_HOTSPOT));
    createRule(setSecurityStandards(of("cwe:543")),
      r -> r.setType(CODE_SMELL).replaceAllDefaultImpacts(List.of(new ImpactDto(MAINTAINABILITY, Severity.HIGH))));
    index();

    RuleQuery query = new RuleQuery().setOwaspTop10For2021(of("a5", "a10"));
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions().addFacets("owaspTop10-2021"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule2.getUuid());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void search_by_security_owaspMobileTop10_2024_return_correct_data_based_on_mode(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    RuleDto rule1 = createRule(setSecurityStandards(of("owaspMobileTop10-2024:m1", "owaspMobileTop10-2024:m10", "cwe:543")),
      r -> r.setType(VULNERABILITY).replaceAllDefaultImpacts(List.of(new ImpactDto(SECURITY, Severity.HIGH))));
    RuleDto rule2 = createRule(setSecurityStandards(of("owaspMobileTop10-2024:m10", "cwe:543")), r -> r.setType(SECURITY_HOTSPOT));
    createRule(setSecurityStandards(of("cwe:543")),
      r -> r.setType(CODE_SMELL).replaceAllDefaultImpacts(List.of(new ImpactDto(MAINTAINABILITY, Severity.HIGH))));
    index();

    RuleQuery query = new RuleQuery().setOwaspMobileTop10For2024(of("m5", "m10"));
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions().addFacets("owaspMobileTop10-2024"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule2.getUuid());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void search_by_security_sansTop25_return_correct_data_based_on_mode(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    RuleDto rule1 = createRule(setSecurityStandards(of("owaspTop10:a1", "owaspTop10:a10", "cwe:89")),
      r -> r.setType(VULNERABILITY).replaceAllDefaultImpacts(List.of(new ImpactDto(SECURITY, Severity.HIGH))));
    RuleDto rule2 = createRule(setSecurityStandards(of("owaspTop10:a10", "cwe:829")), r -> r.setType(SECURITY_HOTSPOT));
    createRule(setSecurityStandards(of("cwe:306")),
      r -> r.setType(CODE_SMELL).replaceAllDefaultImpacts(List.of(new ImpactDto(MAINTAINABILITY, Severity.HIGH))));
    index();

    RuleQuery query = new RuleQuery().setSansTop25(of(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE));
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions().addFacets("sansTop25"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule2.getUuid());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void search_by_security_sonarsource_return_correct_data_based_on_mode(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    RuleDto rule1 = createRule(setSecurityStandards(of("owaspTop10:a1", "owaspTop10-2021:a10", "cwe:89")),
      r -> r.setType(VULNERABILITY).replaceAllDefaultImpacts(List.of(new ImpactDto(SECURITY, Severity.HIGH))));
    createRule(setSecurityStandards(of("owaspTop10:a10", "cwe:829")),
      r -> r.setType(CODE_SMELL).replaceAllDefaultImpacts(List.of(new ImpactDto(MAINTAINABILITY, Severity.HIGH))));
    RuleDto rule3 = createRule(setSecurityStandards(of("cwe:601")), r -> r.setType(SECURITY_HOTSPOT));
    index();

    RuleQuery query = new RuleQuery().setSonarsourceSecurity(of("sql-injection", "open-redirect"));
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions().addFacets("sonarsourceSecurity"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule3.getUuid());
  }

  @Test
  void search_by_security_should_count_issues_based_on_the_mode() {

    // Should not be counted in MQR mode as it has no security impact
    RuleDto rule1 = createRule(setSecurityStandards(of("owaspTop10:a1", "owaspTop10-2021:a10", "cwe:89")), r -> r.setType(VULNERABILITY));

    // Should not be counted in Standard mode because it is not a Vulnerability type
    RuleDto rule2 = createRule(setSecurityStandards(of("owaspTop10:a1", "owaspTop10-2021:a10", "cwe:89")),
      r -> r.setType(CODE_SMELL).replaceAllDefaultImpacts(List.of(new ImpactDto(SECURITY, Severity.HIGH))));

    // Hotspots should be counted in both modes
    RuleDto rule3 = createRule(setSecurityStandards(of("cwe:601")), r -> r.setType(SECURITY_HOTSPOT));
    index();

    RuleQuery query = new RuleQuery().setSonarsourceSecurity(of("sql-injection", "open-redirect"));

    doReturn(Optional.of(true)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions().addFacets("sonarsourceSecurity"));
    assertThat(results.getUuids()).containsOnly(rule2.getUuid(), rule3.getUuid());

    doReturn(Optional.of(false)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    results = underTest.searchV2(query, new SearchOptions().addFacets("sonarsourceSecurity"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule3.getUuid());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void search_by_security_sonarsource_return_complete_list_of_facets(boolean mqrMode) {
    doReturn(Optional.of(mqrMode)).when(config).getBoolean(MULTI_QUALITY_MODE_ENABLED);
    List<RuleDto> rules = new ArrayList<>();

    // Creation of one rule for each standard security category defined (except other)
    for (Map.Entry<SecurityStandards.SQCategory, Set<String>> sqCategorySetEntry : SecurityStandards.CWES_BY_SQ_CATEGORY.entrySet()) {
      rules.add(createRule(setSecurityStandards(of("cwe:" + sqCategorySetEntry.getValue().iterator().next())),
        r -> r.setType(SECURITY_HOTSPOT)));
    }

    // Should be ignore because it is not a hotspot, and not a vulnerability (in Standard mode) and not having Security impact (in MQR mode)
    createRule(setSecurityStandards(of("cwe:787")),
      r -> r.setType(CODE_SMELL).replaceAllDefaultImpacts(List.of(new ImpactDto(MAINTAINABILITY, Severity.HIGH))));
    index();

    RuleQuery query = new RuleQuery();
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions().addFacets("sonarsourceSecurity"));

    assertThat(results.getFacets().get("sonarsourceSecurity"))
      .as("It should have as many facets returned as there are rules defined, and it is not truncated")
      .hasSize(rules.size());
  }

  @Test
  void compare_to_another_profile() {
    String xoo = "xoo";
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(xoo));
    QProfileDto anotherProfile = db.qualityProfiles().insert(p -> p.setLanguage(xoo));
    RuleDto commonRule = db.rules().insertRule(r -> r.setLanguage(xoo));
    RuleDto profileRule1 = db.rules().insertRule(r -> r.setLanguage(xoo));
    RuleDto profileRule2 = db.rules().insertRule(r -> r.setLanguage(xoo));
    RuleDto profileRule3 = db.rules().insertRule(r -> r.setLanguage(xoo));
    RuleDto anotherProfileRule1 = db.rules().insertRule(r -> r.setLanguage(xoo));
    RuleDto anotherProfileRule2 = db.rules().insertRule(r -> r.setLanguage(xoo));
    db.qualityProfiles().activateRule(profile, commonRule);
    db.qualityProfiles().activateRule(profile, profileRule1);
    db.qualityProfiles().activateRule(profile, profileRule2);
    db.qualityProfiles().activateRule(profile, profileRule3);
    db.qualityProfiles().activateRule(anotherProfile, commonRule);
    db.qualityProfiles().activateRule(anotherProfile, anotherProfileRule1);
    db.qualityProfiles().activateRule(anotherProfile, anotherProfileRule2);
    index();

    verifySearch(newRuleQuery().setActivation(false).setQProfile(profile).setCompareToQProfile(anotherProfile), anotherProfileRule1,
      anotherProfileRule2);
    verifySearch(newRuleQuery().setActivation(true).setQProfile(profile).setCompareToQProfile(anotherProfile), commonRule);
    verifySearch(newRuleQuery().setActivation(true).setQProfile(profile).setCompareToQProfile(profile), commonRule, profileRule1,
      profileRule2, profileRule3);
    verifySearch(newRuleQuery().setActivation(false).setQProfile(profile).setCompareToQProfile(profile));
  }

  @SafeVarargs
  private RuleDto createRule(Consumer<RuleDto>... consumers) {
    return db.rules().insert(consumers);
  }

  private RuleDto createJavaRule() {
    return createRule(r -> r.setLanguage("java"));
  }

  @Test
  void search_by_any_of_severities() {
    createRule(setSeverity(BLOCKER));
    RuleDto info = createRule(setSeverity(org.sonar.api.rule.Severity.INFO));
    index();

    RuleQuery query = new RuleQuery().setSeverities(asList(org.sonar.api.rule.Severity.INFO, MINOR));
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(info.getUuid());

    // no results
    query = new RuleQuery().setSeverities(singletonList(MINOR));
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setSeverities(emptyList());
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setSeverities();
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(2);
  }

  @Test
  void search_by_any_of_statuses() {
    createRule(setStatus(RuleStatus.BETA));
    RuleDto ready = createRule(setStatus(RuleStatus.READY));
    index();

    RuleQuery query = new RuleQuery().setStatuses(asList(RuleStatus.DEPRECATED, RuleStatus.READY));
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ready.getUuid());

    // no results
    query = new RuleQuery().setStatuses(singletonList(RuleStatus.DEPRECATED));
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setStatuses(emptyList());
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setStatuses(null);
    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(2);
  }

  @Test
  void activation_parameter_is_ignored_if_profile_is_not_set() {
    RuleDto rule1 = createJavaRule();
    RuleDto rule2 = createJavaRule();
    QProfileDto profile1 = createJavaProfile();
    db.qualityProfiles().activateRule(profile1, rule1);
    index();

    // all rules are returned
    verifySearch(newRuleQuery().setActivation(true), rule1, rule2);
  }

  @Test
  void search_by_activation() {
    RuleDto rule1 = createJavaRule();
    RuleDto rule2 = createJavaRule();
    RuleDto rule3 = createJavaRule();
    QProfileDto profile1 = createJavaProfile();
    QProfileDto profile2 = createJavaProfile();
    db.qualityProfiles().activateRule(profile1, rule1);
    db.qualityProfiles().activateRule(profile2, rule1);
    db.qualityProfiles().activateRule(profile1, rule2);
    index();

    // active rules
    verifySearch(newRuleQuery().setActivation(true).setQProfile(profile1), rule1, rule2);
    verifySearch(newRuleQuery().setActivation(true).setQProfile(profile2), rule1);

    // inactive rules
    verifySearch(newRuleQuery().setActivation(false).setQProfile(profile1), rule3);
    verifySearch(newRuleQuery().setActivation(false).setQProfile(profile2), rule2, rule3);
  }

  private void verifyEmptySearch(RuleQuery query) {
    verifySearch(query);
  }

  private void verifySearch(RuleQuery query, RuleDto... expectedRules) {
    SearchIdResult<String> result = underTest.searchV2(query, new SearchOptions());
    assertThat(result.getTotal()).isEqualTo(expectedRules.length);
    assertThat(result.getUuids()).hasSize(expectedRules.length);
    for (RuleDto expectedRule : expectedRules) {
      assertThat(result.getUuids()).contains(expectedRule.getUuid());
    }
  }

  private void index() {
    ruleIndexer.indexOnStartup(Sets.newHashSet(TYPE_RULE));
    activeRuleIndexer.indexOnStartup(Sets.newHashSet(TYPE_ACTIVE_RULE));
  }

  private RuleQuery newRuleQuery() {
    return new RuleQuery();
  }

  private QProfileDto createJavaProfile() {
    return db.qualityProfiles().insert(p -> p.setLanguage("java"));
  }

  @Test
  void search_by_activation_and_inheritance() {
    RuleDto rule1 = createJavaRule();
    RuleDto rule2 = createJavaRule();
    RuleDto rule3 = createJavaRule();
    RuleDto rule4 = createJavaRule();
    QProfileDto parent = createJavaProfile();
    QProfileDto child = createJavaProfile();
    db.qualityProfiles().activateRule(parent, rule1);
    db.qualityProfiles().activateRule(parent, rule2);
    db.qualityProfiles().activateRule(parent, rule3);
    db.qualityProfiles().activateRule(child, rule1, ar -> ar.setInheritance(INHERITED.name()));
    db.qualityProfiles().activateRule(child, rule2, ar -> ar.setInheritance(OVERRIDES.name()));
    db.qualityProfiles().activateRule(child, rule3, ar -> ar.setInheritance(INHERITED.name()));
    index();

    // all rules
    verifySearch(newRuleQuery(), rule1, rule2, rule3, rule4);

    // inherited/overrides rules on parent
    verifyEmptySearch(newRuleQuery().setActivation(true).setQProfile(parent).setInheritance(of(INHERITED.name())));
    verifyEmptySearch(newRuleQuery().setActivation(true).setQProfile(parent).setInheritance(of(OVERRIDES.name())));

    // inherited/overrides rules on child
    verifySearch(newRuleQuery().setActivation(true).setQProfile(child).setInheritance(of(INHERITED.name())), rule1, rule3);
    verifySearch(newRuleQuery().setActivation(true).setQProfile(child).setInheritance(of(OVERRIDES.name())), rule2);

    // inherited AND overridden on parent
    verifyEmptySearch(newRuleQuery().setActivation(true).setQProfile(parent).setInheritance(of(INHERITED.name(), OVERRIDES.name())));

    // inherited AND overridden on child
    verifySearch(newRuleQuery().setActivation(true).setQProfile(child).setInheritance(of(INHERITED.name(), OVERRIDES.name())), rule1,
      rule2, rule3);
  }

  @Test
  void search_by_activation_and_severity() {
    RuleDto major = createRule(setSeverity(MAJOR));
    RuleDto minor = createRule(setSeverity(MINOR));
    createRule(setSeverity(org.sonar.api.rule.Severity.INFO));
    QProfileDto profile1 = createJavaProfile();
    QProfileDto profile2 = createJavaProfile();
    db.qualityProfiles().activateRule(profile1, major, ar -> ar.setSeverity(BLOCKER));
    db.qualityProfiles().activateRule(profile2, major, ar -> ar.setSeverity(BLOCKER));
    db.qualityProfiles().activateRule(profile1, minor, ar -> ar.setSeverity(CRITICAL));
    index();

    // count activation severities of all active rules
    RuleQuery query = newRuleQuery().setActivation(true).setQProfile(profile1);
    verifySearch(query, major, minor);
    verifyFacet(query, RuleIndex.FACET_ACTIVE_SEVERITIES, entry(BLOCKER, 1L), entry(CRITICAL, 1L));

    // check stickyness of active severity facet
    query = newRuleQuery().setActivation(true).setQProfile(profile1).setActiveSeverities(singletonList(CRITICAL));
    verifySearch(query, minor);
    verifyFacet(query, RuleIndex.FACET_ACTIVE_SEVERITIES, entry(BLOCKER, 1L), entry(CRITICAL, 1L));
  }

  @Test
  void facet_by_activation_severity_is_ignored_when_profile_is_not_specified() {
    RuleDto rule = createJavaRule();
    QProfileDto profile = createJavaProfile();
    db.qualityProfiles().activateRule(profile, rule);
    index();

    RuleQuery query = newRuleQuery();
    verifyNoFacet(query, RuleIndex.FACET_ACTIVE_SEVERITIES);
  }

  @Test
  void search_by_activation_returns_impact_severity_facet() {
    RuleDto rule1 = createRule();
    RuleDto rule2 = createRule(setSeverity(MINOR));
    RuleDto rule3 = createRule();
    RuleDto rule4 = createRule();
    QProfileDto profile1 = createJavaProfile();
    QProfileDto profile2 = createJavaProfile();
    db.qualityProfiles().activateRule(profile1, rule1, ar -> ar.setImpacts(Map.of(SECURITY, Severity.BLOCKER, MAINTAINABILITY,
      Severity.INFO)));
    db.qualityProfiles().activateRule(profile1, rule2, ar -> ar.setImpacts(Map.of(SECURITY, Severity.LOW, MAINTAINABILITY, Severity.INFO,
      RELIABILITY, HIGH)));
    db.qualityProfiles().activateRule(profile1, rule3, ar -> ar.setImpacts(Map.of(SECURITY, Severity.BLOCKER, MAINTAINABILITY,
      Severity.BLOCKER, RELIABILITY, Severity.BLOCKER)));
    db.qualityProfiles().activateRule(profile1, rule4, ar -> ar.setImpacts(Map.of(SECURITY, Severity.MEDIUM, RELIABILITY,
      Severity.MEDIUM)));

    // Ignored because on profile2 which is not part of the query
    db.qualityProfiles().activateRule(profile2, rule1, ar -> ar.setImpacts(Map.of(SECURITY, Severity.BLOCKER, MAINTAINABILITY,
      Severity.INFO)));
    index();

    RuleQuery query = newRuleQuery().setActivation(true).setQProfile(profile1);

    SearchIdResult result = underTest.searchV2(query,
      new SearchOptions().addFacets(singletonList("active_impactSeverities")));

    assertThat(result.getFacets().getAll()).hasSize(1);
    assertThat(result.getFacets().getAll().get("active_impactSeverities"))
      .containsOnly(
        entry("BLOCKER", 2L),
        entry("HIGH", 1L),
        entry("MEDIUM", 1L),
        entry("LOW", 1L),
        entry("INFO", 2L));
  }

  @Test
  void search_by_activation_and_impact_severity_returns_impact_severity_facet() {
    RuleDto rule1 = createRule(setImpacts(List.of(new ImpactDto(SECURITY, Severity.MEDIUM))));
    RuleDto rule2 = createRule(setImpacts(List.of(new ImpactDto(SECURITY, Severity.BLOCKER))));
    RuleDto rule3 = createRule();
    RuleDto rule4 = createRule();
    QProfileDto profile1 = createJavaProfile();
    QProfileDto profile2 = createJavaProfile();
    db.qualityProfiles().activateRule(profile1, rule1, ar -> ar.setImpacts(Map.of(SECURITY, Severity.BLOCKER, MAINTAINABILITY,
      Severity.INFO)).setSeverity("INFO"));
    db.qualityProfiles().activateRule(profile1, rule2, ar -> ar.setImpacts(Map.of(SECURITY, Severity.LOW, MAINTAINABILITY, Severity.INFO,
      RELIABILITY, HIGH)).setSeverity("INFO"));
    db.qualityProfiles().activateRule(profile1, rule3, ar -> ar.setImpacts(Map.of(SECURITY, Severity.BLOCKER, MAINTAINABILITY,
      Severity.BLOCKER, RELIABILITY, Severity.BLOCKER)).setSeverity("MAJOR"));
    db.qualityProfiles().activateRule(profile1, rule4, ar -> ar.setImpacts(Map.of(SECURITY, Severity.MEDIUM, RELIABILITY,
      Severity.MEDIUM)).setSeverity("MAJOR"));

    // Should be ignored because it is on profile2 which is not part of the query
    db.qualityProfiles().activateRule(profile2, rule1, ar -> ar.setImpacts(Map.of(SECURITY, Severity.BLOCKER, MAINTAINABILITY,
      Severity.INFO)));
    index();

    RuleQuery query = newRuleQuery().setActivation(true).setQProfile(profile1).setActiveImpactSeverities(List.of("BLOCKER"));

    SearchIdResult result1 = underTest.searchV2(query, new SearchOptions().addFacets(singletonList("active_impactSeverities")));

    // Filtering on active Impact Severity should have no effect on the Active Severity Facets
    assertThat(result1.getFacets().getAll()).hasSize(1);
    assertThat(result1.getFacets().getAll().get("active_impactSeverities"))
      .containsOnly(
        entry("BLOCKER", 2L),
        entry("HIGH", 1L),
        entry("MEDIUM", 1L),
        entry("LOW", 1L),
        entry("INFO", 2L));
    assertThat(result1.getUuids()).containsExactlyInAnyOrder(rule1.getUuid(), rule3.getUuid());

    query = query.setActiveSeverities(List.of("MAJOR"));

    SearchIdResult result2 = underTest.searchV2(query, new SearchOptions().addFacets(singletonList("active_impactSeverities")));

    // Filters other than "active impact severity" should affect the counts
    assertThat(result2.getFacets().getAll()).hasSize(1);
    assertThat(result2.getFacets().getAll().get("active_impactSeverities"))
      .containsOnly(
        entry("BLOCKER", 1L),
        entry("HIGH", 0L),
        entry("MEDIUM", 1L),
        entry("LOW", 0L),
        entry("INFO", 0L));
    assertThat(result2.getUuids()).containsExactlyInAnyOrder(rule3.getUuid());
  }

  private void verifyFacet(RuleQuery query, String facet, Map.Entry<String, Long>... expectedBuckets) {
    SearchIdResult<String> result = underTest.searchV2(query, new SearchOptions().addFacets(facet));
    assertThat(result.getFacets().get(facet))
      .containsOnly(expectedBuckets);
  }

  private void verifyNoFacet(RuleQuery query, String facet) {
    SearchIdResult<String> result = underTest.searchV2(query, new SearchOptions().addFacets(facet));
    assertThat(result.getFacets().get(facet)).isNull();
  }

  @Test
  void listTags_should_return_tags() {
    createRule(setSystemTags("sys1", "sys2"), setTags("tag1"));
    createRule(setSystemTags(), setTags("tag2"));

    index();

    assertThat(underTest.listTags(null, 10)).containsOnly("tag1", "tag2", "sys1", "sys2");
  }

  @Test
  void fail_to_list_tags_when_size_greater_than_500() {
    assertThatThrownBy(() -> underTest.listTags(null, 501))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Page size must be lower than or equals to 500");
  }

  @Test
  void available_since() {
    RuleDto ruleOld = createRule(setCreatedAt(-2_000L));
    RuleDto ruleOlder = createRule(setCreatedAt(-1_000L));
    index();

    // 0. find all rules;
    verifySearch(new RuleQuery(), ruleOld, ruleOlder);

    // 1. find all rules available since a date;
    RuleQuery availableSinceQuery = new RuleQuery().setAvailableSince(-1000L);
    verifySearch(availableSinceQuery, ruleOlder);

    // 2. find no new rules since tomorrow.
    RuleQuery availableSinceNowQuery = new RuleQuery().setAvailableSince(1000L);
    verifyEmptySearch(availableSinceNowQuery);
  }

  @Test
  void search_by_clean_code_attribute() {
    RuleDto ruleDto = createRule(setRepositoryKey("php"), setCleanCodeAttribute(CleanCodeAttribute.FOCUSED));
    index();

    RuleQuery query = new RuleQuery();
    query.setCleanCodeAttributesCategories(List.of(CleanCodeAttribute.LOGICAL.getAttributeCategory().name()));
    SearchIdResult result1 = underTest.searchV2(query, new SearchOptions());
    assertThat(result1.getUuids()).isEmpty();

    query = new RuleQuery();
    query.setCleanCodeAttributesCategories(List.of(CleanCodeAttribute.FOCUSED.getAttributeCategory().name()));

    SearchIdResult result2 = underTest.searchV2(query, new SearchOptions());

    assertThat(result2.getUuids()).containsOnly(ruleDto.getUuid());
  }

  @Test
  void search_by_software_quality() {
    ImpactDto impactDto = new ImpactDto().setSoftwareQuality(SECURITY).setSeverity(Severity.HIGH);
    RuleDto phpRule = createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto)));
    index();

    RuleQuery query = new RuleQuery();
    SearchIdResult result1 = underTest.searchV2(query.setImpactSoftwareQualities(List.of(MAINTAINABILITY.name())),
      new SearchOptions());
    assertThat(result1.getUuids()).isEmpty();

    query = new RuleQuery();
    SearchIdResult result2 = underTest.searchV2(query.setImpactSoftwareQualities(List.of(SECURITY.name())),
      new SearchOptions());
    assertThat(result2.getUuids()).containsOnly(phpRule.getUuid());
  }

  @Test
  void search_by_severity() {
    ImpactDto impactDto = new ImpactDto().setSoftwareQuality(SECURITY).setSeverity(Severity.BLOCKER);
    RuleDto phpRule = createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto)));
    index();

    RuleQuery query = new RuleQuery();
    SearchIdResult result1 = underTest.searchV2(query.setImpactSeverities(List.of(Severity.MEDIUM.name())), new SearchOptions());
    assertThat(result1.getUuids()).isEmpty();

    query = new RuleQuery();
    SearchIdResult result2 = underTest.searchV2(query.setImpactSeverities(List.of(Severity.BLOCKER.name())), new SearchOptions());
    assertThat(result2.getUuids()).containsOnly(phpRule.getUuid());
  }

  @Test
  void search_should_support_clean_code_attribute_category_facet() {
    createRule(setRepositoryKey("php"), setCleanCodeAttribute(CleanCodeAttribute.FOCUSED));
    createRule(setRepositoryKey("php"), setCleanCodeAttribute(CleanCodeAttribute.LOGICAL));
    index();

    RuleQuery query = new RuleQuery();

    SearchIdResult result2 = underTest.searchV2(query, new SearchOptions().addFacets(singletonList("cleanCodeAttributeCategories")));

    assertThat(result2.getFacets().getAll()).hasSize(1);
    assertThat(result2.getFacets().getAll().get("cleanCodeAttributeCategories")).containsOnly(entry("ADAPTABLE", 1L), entry("INTENTIONAL", 1L));
  }

  @Test
  void search_should_support_clean_code_attribute_category_facet_with_filtering() {
    RuleDto php = createRule(setRepositoryKey("php"), setCleanCodeAttribute(CleanCodeAttribute.FOCUSED));
    RuleDto php1 = createRule(setRepositoryKey("php"), setCleanCodeAttribute(CleanCodeAttribute.LOGICAL));
    RuleDto java = createRule(setRepositoryKey("java"), setCleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL));
    index();

    RuleQuery query = new RuleQuery();

    SearchIdResult result = underTest.searchV2(
      query.setCleanCodeAttributesCategories(List.of(CleanCodeAttributeCategory.CONSISTENT.name(),
        CleanCodeAttributeCategory.ADAPTABLE.name())),
      new SearchOptions().addFacets(singletonList("cleanCodeAttributeCategories")));

    assertThat(result.getUuids()).containsExactlyInAnyOrder(php.getUuid(), java.getUuid());
  }

  @Test
  void search_should_support_software_quality_facet() {
    ImpactDto impactDto = new ImpactDto().setSoftwareQuality(SECURITY).setSeverity(Severity.HIGH);
    ImpactDto impactDto2 = new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(Severity.LOW);
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto2)));
    index();

    RuleQuery query = new RuleQuery();

    SearchIdResult result2 = underTest.searchV2(query, new SearchOptions().addFacets(singletonList("impactSoftwareQualities")));

    assertThat(result2.getFacets().getAll()).hasSize(1);
    assertThat(result2.getFacets().getAll().get("impactSoftwareQualities"))
      .containsOnly(
        entry("SECURITY", 1L),
        entry("MAINTAINABILITY", 1L),
        entry("RELIABILITY", 0L));
  }

  @Test
  void search_should_support_software_quality_facet_with_filtering() {
    ImpactDto impactDto = new ImpactDto().setSoftwareQuality(SECURITY).setSeverity(Severity.HIGH);
    ImpactDto impactDto2 = new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(Severity.LOW);
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto2)));
    index();

    RuleQuery query = new RuleQuery();

    SearchIdResult result2 = underTest.searchV2(query.setImpactSeverities(of(Severity.HIGH.name())),
      new SearchOptions().addFacets(singletonList("impactSoftwareQualities")));

    assertThat(result2.getFacets().getAll()).hasSize(1);
    assertThat(result2.getFacets().getAll().get("impactSoftwareQualities"))
      .containsOnly(
        entry("SECURITY", 1L),
        entry("MAINTAINABILITY", 0L),
        entry("RELIABILITY", 0L));
  }

  @Test
  void search_whenFilteringOnSeverityAndSoftwareQuality_shouldReturnFacet() {
    ImpactDto impactDto = new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(Severity.HIGH);
    ImpactDto impactDto2 = new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(Severity.LOW);
    ImpactDto impactDto3 = new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(Severity.LOW);
    ImpactDto impactDto4 = new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(Severity.BLOCKER);
    ImpactDto impactDto5 = new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(Severity.BLOCKER);
    ImpactDto impactDto6 = new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(Severity.INFO);
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto2)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto3)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto4)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto5)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto6)));
    index();

    RuleQuery query = new RuleQuery();

    SearchIdResult result = underTest.searchV2(
      query.setImpactSeverities(of(Severity.LOW.name())).setImpactSoftwareQualities(List.of(MAINTAINABILITY.name())),
      new SearchOptions().addFacets(List.of("impactSoftwareQualities", "impactSeverities")));

    assertThat(result.getFacets().getAll()).hasSize(2);
    assertThat(result.getFacets().getAll().get("impactSoftwareQualities"))
      .containsOnly(
        entry("SECURITY", 0L),
        entry("MAINTAINABILITY", 1L),
        entry("RELIABILITY", 1L));

    assertThat(result.getFacets().getAll().get("impactSeverities"))
      .containsOnly(
        entry("HIGH", 1L),
        entry("MEDIUM", 0L),
        entry("LOW", 1L),
        entry("INFO", 0L),
        entry("BLOCKER", 1L));
  }

  @Test
  void search_should_support_severity_facet() {
    ImpactDto impactDto = new ImpactDto().setSoftwareQuality(SECURITY).setSeverity(Severity.HIGH);
    ImpactDto impactDto2 = new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(Severity.LOW);
    ImpactDto impactDto3 = new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(Severity.BLOCKER);
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto2, impactDto3)));
    index();

    RuleQuery query = new RuleQuery();

    SearchIdResult result2 = underTest.searchV2(query, new SearchOptions().addFacets(singletonList("impactSeverities")));

    assertThat(result2.getFacets().getAll()).hasSize(1);
    assertThat(result2.getFacets().getAll().get("impactSeverities"))
      .containsOnly(
        entry("LOW", 1L),
        entry("MEDIUM", 0L),
        entry("HIGH", 1L),
        entry("INFO", 0L),
        entry("BLOCKER", 1L));
  }

  @Test
  void search_should_support_severity_facet_with_filters() {
    ImpactDto impactDto = new ImpactDto().setSoftwareQuality(SECURITY).setSeverity(Severity.HIGH);
    ImpactDto impactDto2 = new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(Severity.LOW);
    ImpactDto impactDto3 = new ImpactDto().setSoftwareQuality(SECURITY).setSeverity(Severity.INFO);
    ImpactDto impactDto4 = new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(Severity.INFO);
    ImpactDto impactDto5 = new ImpactDto().setSoftwareQuality(SECURITY).setSeverity(Severity.BLOCKER);
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto2, impactDto3)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto4, impactDto5)));
    index();

    RuleQuery query = new RuleQuery();

    SearchIdResult result2 = underTest.searchV2(query.setImpactSeverities(of("LOW")), new SearchOptions().addFacets(singletonList(
      "impactSeverities")));

    assertThat(result2.getFacets().getAll()).hasSize(1);
    assertThat(result2.getFacets().getAll().get("impactSeverities"))
      .containsOnly(
        entry("LOW", 1L),
        entry("MEDIUM", 0L),
        entry("HIGH", 1L),
        entry("INFO", 2L),
        entry("BLOCKER", 1L));
  }

  @Test
  void search_should_support_software_quality_and_severity_facets_with_filtering() {
    ImpactDto impactDto = new ImpactDto().setSoftwareQuality(SECURITY).setSeverity(Severity.HIGH);
    ImpactDto impactDto2 = new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(Severity.LOW);
    ImpactDto impactDto3 = new ImpactDto().setSoftwareQuality(MAINTAINABILITY).setSeverity(Severity.BLOCKER);
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto2)));
    createRule(setRepositoryKey("php"), setImpacts(List.of(impactDto3)));
    index();

    RuleQuery query = new RuleQuery().setImpactSeverities(of("LOW"))
      .setImpactSoftwareQualities(of(MAINTAINABILITY.name()));
    SearchOptions searchOptions = new SearchOptions().addFacets(List.of("impactSeverities", "impactSoftwareQualities"));
    SearchIdResult result2 = underTest.searchV2(query, searchOptions);

    assertThat(result2.getFacets().getAll()).hasSize(2);
    assertThat(result2.getFacets().getAll().get("impactSeverities"))
      .containsOnly(
        entry("LOW", 1L),
        entry("MEDIUM", 0L),
        entry("HIGH", 0L),
        entry("INFO", 0L),
        entry("BLOCKER", 1L));
    assertThat(result2.getFacets().getAll().get("impactSoftwareQualities")).containsOnly(
      entry(SECURITY.name(), 0L),
      entry(MAINTAINABILITY.name(), 1L),
      entry(SoftwareQuality.RELIABILITY.name(), 0L));
  }

  @Test
  void global_facet_on_repositories_and_tags() {
    createRule(setRepositoryKey("php"), setSystemTags("sysTag"), setTags());
    createRule(setRepositoryKey("php"), setSystemTags(), setTags("tag1"));
    createRule(setRepositoryKey("javascript"), setSystemTags(), setTags("tag1", "tag2"));
    index();

    // should not have any facet!
    RuleQuery query = new RuleQuery();
    SearchIdResult result1 = underTest.searchV2(query, new SearchOptions());
    assertThat(result1.getFacets().getAll()).isEmpty();

    // should not have any facet on non matching query!
    SearchIdResult result2 = underTest.searchV2(new RuleQuery().setQueryText("aeiou"), new SearchOptions().addFacets(singletonList(
      "repositories")));
    assertThat(result2.getFacets().getAll()).hasSize(1);
    assertThat(result2.getFacets().getAll().get("repositories")).isEmpty();

    // Repositories Facet is preset
    SearchIdResult result3 = underTest.searchV2(query, new SearchOptions().addFacets(asList("repositories", "tags")));
    assertThat(result3.getFacets()).isNotNull();

    // Verify the value of a given facet
    Map<String, Long> repoFacets = result3.getFacets().get("repositories");
    assertThat(repoFacets).containsOnly(entry("php", 2L), entry("javascript", 1L));

    // Check that tag facet has both Tags and SystemTags values
    Map<String, Long> tagFacets = result3.getFacets().get("tags");
    assertThat(tagFacets).containsOnly(entry("tag1", 2L), entry("sysTag", 1L), entry("tag2", 1L));

    // Check that there are no other facets
    assertThat(result3.getFacets().getAll()).hasSize(2);
  }

  private void setupStickyFacets() {
    createRule(setRepositoryKey("xoo"), setRuleKey("S001"), setLanguage("java"), setTags(), setSystemTags(), setType(BUG));
    createRule(setRepositoryKey("xoo"), setRuleKey("S002"), setLanguage("java"), setTags(), setSystemTags(), setType(CODE_SMELL));
    createRule(setRepositoryKey("xoo"), setRuleKey("S003"), setLanguage("java"), setTags(), setSystemTags("T1", "T2"), setType(CODE_SMELL));
    createRule(setRepositoryKey("xoo"), setRuleKey("S011"), setLanguage("cobol"), setTags(), setSystemTags(), setType(CODE_SMELL));
    createRule(setRepositoryKey("xoo"), setRuleKey("S012"), setLanguage("cobol"), setTags(), setSystemTags(), setType(BUG));
    createRule(setRepositoryKey("foo"), setRuleKey("S013"), setLanguage("cobol"), setTags(), setSystemTags("T3", "T4"),
      setType(VULNERABILITY));
    createRule(setRepositoryKey("foo"), setRuleKey("S111"), setLanguage("cpp"), setTags(), setSystemTags(), setType(BUG));
    createRule(setRepositoryKey("foo"), setRuleKey("S112"), setLanguage("cpp"), setTags(), setSystemTags(), setType(CODE_SMELL));
    createRule(setRepositoryKey("foo"), setRuleKey("S113"), setLanguage("cpp"), setTags(), setSystemTags("T2", "T3"), setType(CODE_SMELL));
    index();
  }

  @Test
  void sticky_facets_base() {
    setupStickyFacets();

    RuleQuery query = new RuleQuery();

    assertThat(underTest.searchV2(query, new SearchOptions()).getUuids()).hasSize(9);
  }

  /**
   * Facet with no filters at all
   */
  @Test
  void sticky_facets_no_filters() {
    setupStickyFacets();
    RuleQuery query = new RuleQuery();

    SearchIdResult<String> result = underTest.searchV2(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES, FACET_REPOSITORIES,
      FACET_TAGS, FACET_TYPES)));
    Map<String, LinkedHashMap<String, Long>> facets = result.getFacets().getAll();
    assertThat(facets).hasSize(4);
    assertThat(facets.get(FACET_LANGUAGES)).containsOnlyKeys("cpp", "java", "cobol");
    assertThat(facets.get(FACET_REPOSITORIES).keySet()).containsExactly("xoo", "foo");
    assertThat(facets.get(FACET_TAGS)).containsOnlyKeys("T1", "T2", "T3", "T4");
    assertThat(facets.get(FACET_TYPES)).containsOnlyKeys("BUG", "CODE_SMELL", "VULNERABILITY");
  }

  /**
   * Facet with a language filter
   * -- lang facet should still have all language
   */
  @Test
  void sticky_facets_with_1_filter() {
    setupStickyFacets();
    RuleQuery query = new RuleQuery().setLanguages(List.of("cpp"));

    SearchIdResult<String> result = underTest.searchV2(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES, FACET_REPOSITORIES,
      FACET_TAGS)));
    assertThat(result.getUuids()).hasSize(3);
    assertThat(result.getFacets().getAll()).hasSize(3);
    assertThat(result.getFacets().get(FACET_LANGUAGES)).containsOnlyKeys("cpp", "java", "cobol");
    assertThat(result.getFacets().get(FACET_REPOSITORIES)).containsOnlyKeys("foo");
    assertThat(result.getFacets().get(FACET_TAGS)).containsOnlyKeys("T2", "T3");
  }

  @Test
  void languages_facet_should_return_top_100_items() {
    rangeClosed(1, 101).forEach(i -> db.rules().insert(r -> r.setLanguage("lang" + i)));
    index();

    SearchIdResult<String> result = underTest.searchV2(new RuleQuery(), new SearchOptions().addFacets(singletonList(FACET_LANGUAGES)));

    assertThat(result.getFacets().get(FACET_LANGUAGES)).hasSize(100);
  }

  @Test
  void repositories_facet_should_return_top_100_items() {
    rangeClosed(1, 101).forEach(i -> db.rules().insert(r -> r.setRepositoryKey("repo" + i)));
    index();

    SearchIdResult<String> result = underTest.searchV2(new RuleQuery(), new SearchOptions().addFacets(singletonList(FACET_REPOSITORIES)));

    assertThat(result.getFacets().get(FACET_REPOSITORIES)).hasSize(100);
  }

  @Test
  void tags_facet_should_find_tags() {
    createRule(setSystemTags(), setTags("bla"));
    index();

    RuleQuery query = new RuleQuery();
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));

    SearchIdResult<String> result = underTest.searchV2(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).contains(entry("bla", 1L));
  }

  @Test
  void tags_facet_should_return_top_100_items() {
    // default number of items returned in tag facet = 100
    String[] tags = get101Tags();
    createRule(setSystemTags(tags));
    index();

    RuleQuery query = new RuleQuery();
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));
    SearchIdResult<String> result = underTest.searchV2(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).hasSize(100);
    assertThat(result.getFacets().get(FACET_TAGS)).contains(entry("tag0", 1L), entry("tag25", 1L), entry("tag99", 1L));
    assertThat(result.getFacets().get(FACET_TAGS)).doesNotContain(entry("tagA", 1L));
  }

  @Test
  void tags_facet_should_include_matching_selected_items() {
    // default number of items returned in tag facet = 100
    String[] tags = get101Tags();
    createRule(setSystemTags(tags));
    index();

    RuleQuery query = new RuleQuery()
      .setTags(singletonList("tagA"));
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));
    SearchIdResult<String> result = underTest.searchV2(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).hasSize(101);
    assertThat(result.getFacets().get(FACET_TAGS).entrySet()).extracting(e -> entry(e.getKey(), e.getValue())).contains(

      // check that selected item is added, although there are 100 other items
      entry("tagA", 1L),

      entry("tag0", 1L), entry("tag25", 1L), entry("tag99", 1L));
  }

  @Test
  void tags_facet_should_be_available() {
    RuleQuery query = new RuleQuery();
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));

    SearchIdResult<String> result = underTest.searchV2(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).isNotNull();
  }

  /**
   * Facet with 2 filters
   * -- lang facet for tag T2
   * -- tag facet for lang cpp
   * -- repository for cpp & T2
   */
  @Test
  void sticky_facets_with_2_filters() {
    setupStickyFacets();

    RuleQuery query = new RuleQuery()
      .setLanguages(List.of("cpp"))
      .setTags(List.of("T2"));

    SearchIdResult<String> result = underTest.searchV2(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES, FACET_REPOSITORIES,
      FACET_TAGS)));
    assertThat(result.getUuids()).hasSize(1);
    Facets facets = result.getFacets();
    assertThat(facets.getAll()).hasSize(3);
    assertThat(facets.get(FACET_LANGUAGES)).containsOnlyKeys("cpp", "java");
    assertThat(facets.get(FACET_REPOSITORIES)).containsOnlyKeys("foo");
    assertThat(facets.get(FACET_TAGS)).containsOnlyKeys("T2", "T3");
  }

  /**
   * Facet with 3 filters
   * -- lang facet for tag T2
   * -- tag facet for lang cpp & java
   * -- repository for (cpp || java) & T2
   * -- type
   */
  @Test
  void sticky_facets_with_3_filters() {
    setupStickyFacets();

    RuleQuery query = new RuleQuery()
      .setLanguages(List.of("cpp", "java"))
      .setTags(List.of("T2"))
      .setTypes(asList(BUG, CODE_SMELL));

    SearchOptions options = new SearchOptions().addFacets(asList(FACET_LANGUAGES,
      FACET_REPOSITORIES,
      FACET_TAGS,
      FACET_TYPES))
      .addComplianceFacets(List.of("cwe"));

    SearchIdResult<String> result = underTest.searchV2(query, options);
    assertThat(result.getUuids()).hasSize(2);
    assertThat(result.getFacets().getAll()).hasSize(5);
    assertThat(result.getFacets().get(FACET_LANGUAGES)).containsOnlyKeys("cpp", "java");
    assertThat(result.getFacets().get(FACET_REPOSITORIES)).containsOnlyKeys("foo", "xoo");
    assertThat(result.getFacets().get(FACET_TAGS)).containsOnlyKeys("T1", "T2", "T3");
    assertThat(result.getFacets().get(FACET_TYPES)).containsOnlyKeys("CODE_SMELL");
    assertThat(result.getFacets().get(COMPLIANCE_FILTER_FACET)).containsOnlyKeys("foo:S113", "xoo:S003");
  }

  @Test
  void sort_by_name() {
    RuleDto abcd = createRule(setName("abcd"));
    RuleDto abc = createRule(setName("ABC"));
    RuleDto fgh = createRule(setName("FGH"));
    index();

    // ascending
    RuleQuery query = new RuleQuery().setSortField(RuleIndexDefinition.FIELD_RULE_NAME);
    SearchIdResult<String> results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsExactly(abc.getUuid(), abcd.getUuid(), fgh.getUuid());

    // descending
    query = new RuleQuery().setSortField(RuleIndexDefinition.FIELD_RULE_NAME).setAscendingSort(false);
    results = underTest.searchV2(query, new SearchOptions());
    assertThat(results.getUuids()).containsExactly(fgh.getUuid(), abcd.getUuid(), abc.getUuid());
  }

  @Test
  void default_sort_is_by_updated_at_desc() {
    long currentTimeMillis = System.currentTimeMillis();

    RuleDto old = createRule(setCreatedAt(1000L), setUpdatedAt(currentTimeMillis + 1000L));
    RuleDto oldest = createRule(setCreatedAt(1000L), setUpdatedAt(currentTimeMillis + 3000L));
    RuleDto older = createRule(setCreatedAt(1000L), setUpdatedAt(currentTimeMillis + 2000L));
    index();

    SearchIdResult<String> results = underTest.searchV2(new RuleQuery(), new SearchOptions());
    assertThat(results.getUuids()).containsExactly(oldest.getUuid(), older.getUuid(), old.getUuid());
  }

  @Test
  void fail_sort_by_language() {
    try {
      // Sorting on a field not tagged as sortable
      new RuleQuery().setSortField(RuleIndexDefinition.FIELD_RULE_LANGUAGE);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Field 'lang' is not sortable");
    }
  }

  @Test
  void paging() {
    createRule();
    createRule();
    createRule();
    index();

    // from 0 to 1 included
    SearchOptions options = new SearchOptions();
    options.setOffset(0).setLimit(2);
    SearchIdResult<String> results = underTest.searchV2(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getUuids()).hasSize(2);

    // from 0 to 9 included
    options.setOffset(0).setLimit(10);
    results = underTest.searchV2(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getUuids()).hasSize(3);

    // from 2 to 11 included
    options.setOffset(2).setLimit(10);
    results = underTest.searchV2(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getUuids()).hasSize(1);
  }

  @Test
  void search_all_keys_by_query() {
    createRule(setRepositoryKey("javascript"), setRuleKey("X001"));
    createRule(setRepositoryKey("cobol"), setRuleKey("X001"));
    createRule(setRepositoryKey("php"), setRuleKey("S002"));
    index();

    // key
    assertThat(underTest.searchAllV2(new RuleQuery().setQueryText("X001"))).toIterable().hasSize(2);

    // partial key does not match
    assertThat(underTest.searchAllV2(new RuleQuery().setQueryText("X00"))).toIterable().isEmpty();

    // repo:key -> nice-to-have !
    assertThat(underTest.searchAllV2(new RuleQuery().setQueryText("javascript:X001"))).toIterable().hasSize(1);
  }

  @Test
  void searchAll_keys_by_profile() {
    RuleDto rule1 = createRule();
    RuleDto rule2 = createRule();
    RuleDto rule3 = createRule();
    QProfileDto profile1 = createJavaProfile();
    QProfileDto profile2 = createJavaProfile();
    db.qualityProfiles().activateRule(profile1, rule1);
    db.qualityProfiles().activateRule(profile2, rule1);
    db.qualityProfiles().activateRule(profile1, rule2);
    index();

    // inactive rules on profile
    es.getDocuments(TYPE_RULE);
    es.getDocuments(TYPE_ACTIVE_RULE);
    assertThat(underTest.searchAllV2(new RuleQuery().setActivation(false).setQProfile(profile2)))
      .toIterable()
      .containsOnly(rule2.getUuid(), rule3.getUuid());

    // active rules on profile
    assertThat(underTest.searchAllV2(new RuleQuery().setActivation(true).setQProfile(profile2)))
      .toIterable()
      .containsOnly(rule1.getUuid());
  }

  private String[] get101Tags() {
    String[] tags = new String[101];
    for (int i = 0; i < 100; i++) {
      tags[i] = "tag" + i;
    }
    tags[100] = "tagA";
    return tags;
  }
}
