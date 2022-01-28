/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.fail;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.db.rule.RuleTesting.setCreatedAt;
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
import static org.sonar.server.rule.index.RuleIndex.FACET_LANGUAGES;
import static org.sonar.server.rule.index.RuleIndex.FACET_REPOSITORIES;
import static org.sonar.server.rule.index.RuleIndex.FACET_TAGS;
import static org.sonar.server.rule.index.RuleIndex.FACET_TYPES;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_INSECURE_INTERACTION;
import static org.sonar.server.security.SecurityStandards.SANS_TOP_25_RISKY_RESOURCE;

public class RuleIndexTest {

  private System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public DbTester db = DbTester.create(system2);

  private RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db.getDbClient(), es.client());

  private RuleIndex underTest = new RuleIndex(es.client(), system2);

  @Test
  public void search_all_rules() {
    createRule();
    createRule();
    index();

    SearchIdResult<String> results = underTest.search(new RuleQuery(), new SearchOptions());

    assertThat(results.getTotal()).isEqualTo(2);
    assertThat(results.getUuids()).hasSize(2);
  }

  @Test
  public void search_by_key() {
    RuleDefinitionDto js1 = createRule(
      setRepositoryKey("javascript"),
      setRuleKey("X001"));
    RuleDefinitionDto cobol1 = createRule(
      setRepositoryKey("cobol"),
      setRuleKey("X001"));
    createRule(
      setRepositoryKey("php"),
      setRuleKey("S002"));
    index();

    // key
    RuleQuery query = new RuleQuery().setQueryText("X001");
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).containsOnly(js1.getUuid(), cobol1.getUuid());

    // partial key does not match
    query = new RuleQuery().setQueryText("X00");
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).isEmpty();

    // repo:key -> nice-to-have !
    query = new RuleQuery().setQueryText("javascript:X001");
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).containsOnly(js1.getUuid());
  }

  @Test
  public void search_by_case_insensitive_key() {
    RuleDefinitionDto ruleDto = createRule(
      setRepositoryKey("javascript"),
      setRuleKey("X001"));
    index();

    RuleQuery query = new RuleQuery().setQueryText("x001");
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).containsOnly(ruleDto.getUuid());
  }

  @Test
  public void filter_by_key() {
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

    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(1);

    // partial key does not match
    query = new RuleQuery().setKey("X001");
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).isEmpty();
  }

  @Test
  public void search_name_by_query() {
    createRule(setName("testing the partial match and matching of rule"));
    index();

    // substring
    RuleQuery query = new RuleQuery().setQueryText("test");
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(1);

    // substring
    query = new RuleQuery().setQueryText("partial match");
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(1);

    // case-insensitive
    query = new RuleQuery().setQueryText("TESTING");
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(1);

    // not found
    query = new RuleQuery().setQueryText("not present");
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).isEmpty();
  }

  @Test
  public void search_name_with_protected_chars() {
    RuleDefinitionDto rule = createRule(setName("ja#va&sc\"r:ipt"));
    index();

    RuleQuery protectedCharsQuery = new RuleQuery().setQueryText(rule.getName());
    List<String> results = underTest.search(protectedCharsQuery, new SearchOptions()).getUuids();
    assertThat(results).containsOnly(rule.getUuid());
  }

  @Test
  public void search_content_by_query() {
    // it's important to set all the fields being used by the search (name, desc, key, lang, ...),
    // otherwise the generated random values may raise false-positives
    RuleDefinitionDto rule1 = createJavaRule(rule -> rule.setRuleKey("123")
      .setName("rule 123")
      .setDescription("My great rule CWE-123 which makes your code 1000 times better!"));
    RuleDefinitionDto rule2 = createJavaRule(rule -> rule.setRuleKey("124")
      .setName("rule 124")
      .setDescription("Another great and shiny rule CWE-124"));
    RuleDefinitionDto rule3 = createJavaRule(rule -> rule.setRuleKey("1000")
      .setName("rule 1000")
      .setDescription("Another great rule CWE-1000"));
    RuleDefinitionDto rule4 = createJavaRule(rule -> rule.setRuleKey("404")
      .setName("rule 404")
      .setDescription("<h1>HTML-Geeks</h1><p style=\"color:blue\">special formatting!</p><table><tr><td>inside</td><td>tables</td></tr></table>"));
    RuleDefinitionDto rule5 = createJavaRule(rule -> rule.setRuleKey("405")
      .setName("rule 405")
      .setDescription("internationalization missunderstandings alsdkjfnadklsjfnadkdfnsksdjfn"));
    index();

    // partial match at word boundary
    assertThat(underTest.search(new RuleQuery().setQueryText("CWE"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule1.getUuid(), rule2.getUuid(), rule3.getUuid());

    // full match
    assertThat(underTest.search(new RuleQuery().setQueryText("CWE-123"), new SearchOptions()).getUuids()).containsExactly(rule1.getUuid());

    // match somewhere else in the text
    assertThat(underTest.search(new RuleQuery().setQueryText("CWE-1000"), new SearchOptions()).getUuids()).containsExactly(rule3.getUuid());
    assertThat(underTest.search(new RuleQuery().setQueryText("CWE 1000"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule3.getUuid(), rule1.getUuid());

    // several words
    assertThat(underTest.search(new RuleQuery().setQueryText("great rule"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule1.getUuid(), rule2.getUuid(),
      rule3.getUuid());
    assertThat(underTest.search(new RuleQuery().setQueryText("rule Another"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule2.getUuid(), rule3.getUuid());

    // no matches
    assertThat(underTest.search(new RuleQuery().setQueryText("unexisting"), new SearchOptions()).getUuids()).isEmpty();
    assertThat(underTest.search(new RuleQuery().setQueryText("great rule unexisting"), new SearchOptions()).getUuids()).isEmpty();

    // stopwords
    assertThat(underTest.search(new RuleQuery().setQueryText("and"), new SearchOptions()).getUuids()).isEmpty();
    assertThat(underTest.search(new RuleQuery().setQueryText("great and shiny"), new SearchOptions()).getUuids()).isEmpty();

    // html
    assertThat(underTest.search(new RuleQuery().setQueryText("h1"), new SearchOptions()).getUuids()).isEmpty();
    assertThat(underTest.search(new RuleQuery().setQueryText("style"), new SearchOptions()).getUuids()).isEmpty();
    assertThat(underTest.search(new RuleQuery().setQueryText("special"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule4.getUuid());
    assertThat(underTest.search(new RuleQuery().setQueryText("geeks formatting inside tables"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule4.getUuid());

    // long words
    assertThat(underTest.search(new RuleQuery().setQueryText("missunderstand"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule5.getUuid());
    assertThat(underTest.search(new RuleQuery().setQueryText("missunderstandings"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule5.getUuid());
    assertThat(underTest.search(new RuleQuery().setQueryText("alsdkjfnadklsjfnadkdfnsksdjfn"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule5.getUuid());
    assertThat(underTest.search(new RuleQuery().setQueryText("internationalization"), new SearchOptions()).getUuids()).containsExactlyInAnyOrder(rule5.getUuid());
    assertThat(underTest.search(new RuleQuery().setQueryText("internationalizationBlaBla"), new SearchOptions()).getUuids()).isEmpty();
  }

  @Test
  public void search_by_any_of_repositories() {
    RuleDefinitionDto findbugs = createRule(
      setRepositoryKey("findbugs"),
      setRuleKey("S001"));
    RuleDefinitionDto pmd = createRule(
      setRepositoryKey("pmd"),
      setRuleKey("S002"));
    index();

    RuleQuery query = new RuleQuery().setRepositories(asList("checkstyle", "pmd"));
    SearchIdResult<String> results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsExactly(pmd.getUuid());

    // no results
    query = new RuleQuery().setRepositories(singletonList("checkstyle"));
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setRepositories(emptyList());
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).containsOnly(findbugs.getUuid(), pmd.getUuid());
  }

  @Test
  public void filter_by_tags() {
    RuleDefinitionDto rule1 = createRule(setSystemTags("tag1s"));
    createRuleMetadata(rule1, setTags("tag1"));
    RuleDefinitionDto rule2 = createRule(setSystemTags("tag2s"));
    createRuleMetadata(rule2, setTags("tag2"));
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
  public void tags_facet_supports_selected_value_with_regexp_special_characters() {
    RuleDefinitionDto rule = createRule();
    createRuleMetadata(rule, setTags("misra++"));
    index();

    RuleQuery query = new RuleQuery()
      .setTags(singletonList("misra["));
    SearchOptions options = new SearchOptions().addFacets(FACET_TAGS);

    // do not fail
    assertThat(underTest.search(query, options).getTotal()).isZero();
  }

  @Test
  public void search_by_types() {
    createRule(setType(CODE_SMELL));
    RuleDefinitionDto vulnerability = createRule(setType(VULNERABILITY));
    RuleDefinitionDto bug1 = createRule(setType(BUG));
    RuleDefinitionDto bug2 = createRule(setType(BUG));
    index();

    // find all
    RuleQuery query = new RuleQuery();
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(4);

    // type3 in filter
    query = new RuleQuery().setTypes(of(VULNERABILITY));
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).containsOnly(vulnerability.getUuid());

    query = new RuleQuery().setTypes(of(BUG));
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).containsOnly(bug1.getUuid(), bug2.getUuid());

    // types in query => nothing
    query = new RuleQuery().setQueryText("code smell bug vulnerability");
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).isEmpty();

    // null list => no filter
    query = new RuleQuery().setTypes(emptySet());
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(4);

    // null list => no filter
    query = new RuleQuery().setTypes(null);
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(4);
  }

  @Test
  public void search_by_is_template() {
    RuleDefinitionDto ruleNoTemplate = createRule(setIsTemplate(false));
    RuleDefinitionDto ruleIsTemplate = createRule(setIsTemplate(true));
    index();

    // find all
    RuleQuery query = new RuleQuery();
    SearchIdResult<String> results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).hasSize(2);

    // Only template
    query = new RuleQuery().setIsTemplate(true);
    results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ruleIsTemplate.getUuid());

    // Only not template
    query = new RuleQuery().setIsTemplate(false);
    results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ruleNoTemplate.getUuid());

    // null => no filter
    query = new RuleQuery().setIsTemplate(null);
    results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ruleIsTemplate.getUuid(), ruleNoTemplate.getUuid());
  }

  @Test
  public void search_by_is_external() {
    RuleDefinitionDto ruleIsNotExternal = createRule(setIsExternal(false));
    RuleDefinitionDto ruleIsExternal = createRule(setIsExternal(true));
    index();

    // Only external
    RuleQuery query = new RuleQuery().setIncludeExternal(true);
    SearchIdResult<String> results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ruleIsExternal.getUuid(), ruleIsNotExternal.getUuid());

    // Only not external
    query = new RuleQuery().setIncludeExternal(false);
    results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ruleIsNotExternal.getUuid());
  }

  @Test
  public void search_by_template_key() {
    RuleDefinitionDto template = createRule(setIsTemplate(true));
    RuleDefinitionDto customRule = createRule(setTemplateId(template.getUuid()));
    index();

    // find all
    RuleQuery query = new RuleQuery();
    SearchIdResult<String> results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).hasSize(2);

    // Only custom rule
    query = new RuleQuery().setTemplateKey(template.getKey().toString());
    results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(customRule.getUuid());

    // null => no filter
    query = new RuleQuery().setTemplateKey(null);
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(2);
  }

  @Test
  public void search_by_any_of_languages() {
    createRule(setLanguage("java"));
    RuleDefinitionDto javascript = createRule(setLanguage("js"));
    index();

    RuleQuery query = new RuleQuery().setLanguages(asList("cobol", "js"));
    SearchIdResult<String> results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(javascript.getUuid());

    // no results
    query = new RuleQuery().setLanguages(singletonList("cpp"));
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setLanguages(emptyList());
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setLanguages(null);
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(2);
  }

  @Test
  public void search_by_security_cwe_return_vulnerabilities_and_hotspots_only() {
    RuleDefinitionDto rule1 = createRule(setSecurityStandards(of("cwe:543", "cwe:123", "owaspTop10:a1")), r -> r.setType(VULNERABILITY));
    RuleDefinitionDto rule2 = createRule(setSecurityStandards(of("cwe:543", "owaspTop10:a1")), r -> r.setType(SECURITY_HOTSPOT));
    createRule(setSecurityStandards(of("owaspTop10:a1")), r -> r.setType(CODE_SMELL));
    index();

    RuleQuery query = new RuleQuery().setCwe(of("543"));
    SearchIdResult<String> results = underTest.search(query, new SearchOptions().addFacets("cwe"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule2.getUuid());
  }

  @Test
  public void search_by_security_owaspTop10_return_vulnerabilities_and_hotspots_only() {
    RuleDefinitionDto rule1 = createRule(setSecurityStandards(of("owaspTop10:a1", "owaspTop10:a10", "cwe:543")), r -> r.setType(VULNERABILITY));
    RuleDefinitionDto rule2 = createRule(setSecurityStandards(of("owaspTop10:a10", "cwe:543")), r -> r.setType(SECURITY_HOTSPOT));
    createRule(setSecurityStandards(of("cwe:543")), r -> r.setType(CODE_SMELL));
    index();

    RuleQuery query = new RuleQuery().setOwaspTop10(of("a5", "a10"));
    SearchIdResult<String> results = underTest.search(query, new SearchOptions().addFacets("owaspTop10"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule2.getUuid());
  }

  @Test
  public void search_by_security_sansTop25_return_vulnerabilities_and_hotspots_only() {
    RuleDefinitionDto rule1 = createRule(setSecurityStandards(of("owaspTop10:a1", "owaspTop10:a10", "cwe:89")), r -> r.setType(VULNERABILITY));
    RuleDefinitionDto rule2 = createRule(setSecurityStandards(of("owaspTop10:a10", "cwe:829")), r -> r.setType(SECURITY_HOTSPOT));
    createRule(setSecurityStandards(of("cwe:306")), r -> r.setType(CODE_SMELL));
    index();

    RuleQuery query = new RuleQuery().setSansTop25(of(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE));
    SearchIdResult<String> results = underTest.search(query, new SearchOptions().addFacets("sansTop25"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule2.getUuid());
  }

  @Test
  public void search_by_security_sonarsource_return_vulnerabilities_and_hotspots_only() {
    RuleDefinitionDto rule1 = createRule(setSecurityStandards(of("owaspTop10:a1", "owaspTop10:a10", "cwe:89")), r -> r.setType(VULNERABILITY));
    createRule(setSecurityStandards(of("owaspTop10:a10", "cwe:829")), r -> r.setType(CODE_SMELL));
    RuleDefinitionDto rule3 = createRule(setSecurityStandards(of("cwe:601")), r -> r.setType(SECURITY_HOTSPOT));
    index();

    RuleQuery query = new RuleQuery().setSonarsourceSecurity(of("sql-injection", "open-redirect"));
    SearchIdResult<String> results = underTest.search(query, new SearchOptions().addFacets("sonarsourceSecurity"));
    assertThat(results.getUuids()).containsOnly(rule1.getUuid(), rule3.getUuid());
  }

  @Test
  public void compare_to_another_profile() {
    String xoo = "xoo";
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(xoo));
    QProfileDto anotherProfile = db.qualityProfiles().insert(p -> p.setLanguage(xoo));
    RuleDefinitionDto commonRule = db.rules().insertRule(r -> r.setLanguage(xoo)).getDefinition();
    RuleDefinitionDto profileRule1 = db.rules().insertRule(r -> r.setLanguage(xoo)).getDefinition();
    RuleDefinitionDto profileRule2 = db.rules().insertRule(r -> r.setLanguage(xoo)).getDefinition();
    RuleDefinitionDto profileRule3 = db.rules().insertRule(r -> r.setLanguage(xoo)).getDefinition();
    RuleDefinitionDto anotherProfileRule1 = db.rules().insertRule(r -> r.setLanguage(xoo)).getDefinition();
    RuleDefinitionDto anotherProfileRule2 = db.rules().insertRule(r -> r.setLanguage(xoo)).getDefinition();
    db.qualityProfiles().activateRule(profile, commonRule);
    db.qualityProfiles().activateRule(profile, profileRule1);
    db.qualityProfiles().activateRule(profile, profileRule2);
    db.qualityProfiles().activateRule(profile, profileRule3);
    db.qualityProfiles().activateRule(anotherProfile, commonRule);
    db.qualityProfiles().activateRule(anotherProfile, anotherProfileRule1);
    db.qualityProfiles().activateRule(anotherProfile, anotherProfileRule2);
    index();

    verifySearch(newRuleQuery().setActivation(false).setQProfile(profile).setCompareToQProfile(anotherProfile), anotherProfileRule1, anotherProfileRule2);
    verifySearch(newRuleQuery().setActivation(true).setQProfile(profile).setCompareToQProfile(anotherProfile), commonRule);
    verifySearch(newRuleQuery().setActivation(true).setQProfile(profile).setCompareToQProfile(profile), commonRule, profileRule1, profileRule2, profileRule3);
    verifySearch(newRuleQuery().setActivation(false).setQProfile(profile).setCompareToQProfile(profile));
  }

  @SafeVarargs
  private final RuleDefinitionDto createRule(Consumer<RuleDefinitionDto>... consumers) {
    return db.rules().insert(consumers);
  }

  private RuleDefinitionDto createJavaRule() {
    return createRule(r -> r.setLanguage("java"));
  }

  private RuleDefinitionDto createJavaRule(Consumer<RuleDefinitionDto> consumer) {
    return createRule(r -> r.setLanguage("java"), consumer);
  }

  @SafeVarargs
  private final RuleMetadataDto createRuleMetadata(RuleDefinitionDto rule, Consumer<RuleMetadataDto>... populaters) {
    return db.rules().insertOrUpdateMetadata(rule, populaters);
  }

  @Test
  public void search_by_any_of_severities() {
    createRule(setSeverity(BLOCKER));
    RuleDefinitionDto info = createRule(setSeverity(INFO));
    index();

    RuleQuery query = new RuleQuery().setSeverities(asList(INFO, MINOR));
    SearchIdResult<String> results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(info.getUuid());

    // no results
    query = new RuleQuery().setSeverities(singletonList(MINOR));
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setSeverities(emptyList());
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setSeverities();
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(2);
  }

  @Test
  public void search_by_any_of_statuses() {
    RuleDefinitionDto beta = createRule(setStatus(RuleStatus.BETA));
    RuleDefinitionDto ready = createRule(setStatus(RuleStatus.READY));
    index();

    RuleQuery query = new RuleQuery().setStatuses(asList(RuleStatus.DEPRECATED, RuleStatus.READY));
    SearchIdResult<String> results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsOnly(ready.getUuid());

    // no results
    query = new RuleQuery().setStatuses(singletonList(RuleStatus.DEPRECATED));
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setStatuses(emptyList());
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setStatuses(null);
    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(2);
  }

  @Test
  public void activation_parameter_is_ignored_if_profile_is_not_set() {
    RuleDefinitionDto rule1 = createJavaRule();
    RuleDefinitionDto rule2 = createJavaRule();
    QProfileDto profile1 = createJavaProfile();
    db.qualityProfiles().activateRule(profile1, rule1);
    index();

    // all rules are returned
    verifySearch(newRuleQuery().setActivation(true), rule1, rule2);
  }

  @Test
  public void search_by_activation() {
    RuleDefinitionDto rule1 = createJavaRule();
    RuleDefinitionDto rule2 = createJavaRule();
    RuleDefinitionDto rule3 = createJavaRule();
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

  private void verifySearch(RuleQuery query, RuleDefinitionDto... expectedRules) {
    SearchIdResult<String> result = underTest.search(query, new SearchOptions());
    assertThat(result.getTotal()).isEqualTo(expectedRules.length);
    assertThat(result.getUuids()).hasSize(expectedRules.length);
    for (RuleDefinitionDto expectedRule : expectedRules) {
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
  public void search_by_activation_and_inheritance() {
    RuleDefinitionDto rule1 = createJavaRule();
    RuleDefinitionDto rule2 = createJavaRule();
    RuleDefinitionDto rule3 = createJavaRule();
    RuleDefinitionDto rule4 = createJavaRule();
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
    verifySearch(newRuleQuery().setActivation(true).setQProfile(child).setInheritance(of(INHERITED.name(), OVERRIDES.name())), rule1, rule2, rule3);
  }

  @Test
  public void search_by_activation_and_severity() {
    RuleDefinitionDto major = createRule(setSeverity(MAJOR));
    RuleDefinitionDto minor = createRule(setSeverity(MINOR));
    RuleDefinitionDto info = createRule(setSeverity(INFO));
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
  public void facet_by_activation_severity_is_ignored_when_profile_is_not_specified() {
    RuleDefinitionDto rule = createJavaRule();
    QProfileDto profile = createJavaProfile();
    db.qualityProfiles().activateRule(profile, rule);
    index();

    RuleQuery query = newRuleQuery();
    verifyNoFacet(query, RuleIndex.FACET_ACTIVE_SEVERITIES);
  }

  private void verifyFacet(RuleQuery query, String facet, Map.Entry<String, Long>... expectedBuckets) {
    SearchIdResult<String> result = underTest.search(query, new SearchOptions().addFacets(facet));
    assertThat(result.getFacets().get(facet))
      .containsOnly(expectedBuckets);
  }

  private void verifyNoFacet(RuleQuery query, String facet) {
    SearchIdResult<String> result = underTest.search(query, new SearchOptions().addFacets(facet));
    assertThat(result.getFacets().get(facet)).isNull();
  }

  @Test
  public void listTags_should_return_tags() {
    RuleDefinitionDto rule1 = createRule(setSystemTags("sys1", "sys2"));
    createRuleMetadata(rule1, setTags("tag1"));

    RuleDefinitionDto rule2 = createRule(setSystemTags());
    createRuleMetadata(rule2, setTags("tag2"));

    index();

    assertThat(underTest.listTags(null, 10)).containsOnly("tag1", "tag2", "sys1", "sys2");
  }

  @Test
  public void fail_to_list_tags_when_size_greater_than_500() {
    assertThatThrownBy(() -> underTest.listTags(null, 501))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Page size must be lower than or equals to 500");
  }

  @Test
  public void available_since() {
    RuleDefinitionDto ruleOld = createRule(setCreatedAt(1_000L));
    RuleDefinitionDto ruleOlder = createRule(setCreatedAt(2_000L));
    index();

    // 0. find all rules;
    verifySearch(new RuleQuery(), ruleOld, ruleOlder);

    // 1. find all rules available since a date;
    RuleQuery availableSinceQuery = new RuleQuery().setAvailableSince(2000L);
    verifySearch(availableSinceQuery, ruleOlder);

    // 2. find no new rules since tomorrow.
    RuleQuery availableSinceNowQuery = new RuleQuery().setAvailableSince(3000L);
    verifyEmptySearch(availableSinceNowQuery);
  }

  @Test
  public void global_facet_on_repositories_and_tags() {
    createRule(setRepositoryKey("php"), setSystemTags("sysTag"));
    RuleDefinitionDto rule1 = createRule(setRepositoryKey("php"), setSystemTags());
    createRuleMetadata(rule1, setTags("tag1"));
    RuleDefinitionDto rule2 = createRule(setRepositoryKey("javascript"), setSystemTags());
    createRuleMetadata(rule2, setTags("tag1", "tag2"));
    index();

    // should not have any facet!
    RuleQuery query = new RuleQuery();
    SearchIdResult result1 = underTest.search(query, new SearchOptions());
    assertThat(result1.getFacets().getAll()).isEmpty();

    // should not have any facet on non matching query!
    SearchIdResult result2 = underTest.search(new RuleQuery().setQueryText("aeiou"), new SearchOptions().addFacets(singletonList("repositories")));
    assertThat(result2.getFacets().getAll()).hasSize(1);
    assertThat(result2.getFacets().getAll().get("repositories")).isEmpty();

    // Repositories Facet is preset
    SearchIdResult result3 = underTest.search(query, new SearchOptions().addFacets(asList("repositories", "tags")));
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
    createRule(setRepositoryKey("xoo"), setRuleKey("S001"), setLanguage("java"), setSystemTags(), setType(BUG));
    createRule(setRepositoryKey("xoo"), setRuleKey("S002"), setLanguage("java"), setSystemTags(), setType(CODE_SMELL));
    createRule(setRepositoryKey("xoo"), setRuleKey("S003"), setLanguage("java"), setSystemTags("T1", "T2"), setType(CODE_SMELL));
    createRule(setRepositoryKey("xoo"), setRuleKey("S011"), setLanguage("cobol"), setSystemTags(), setType(CODE_SMELL));
    createRule(setRepositoryKey("xoo"), setRuleKey("S012"), setLanguage("cobol"), setSystemTags(), setType(BUG));
    createRule(setRepositoryKey("foo"), setRuleKey("S013"), setLanguage("cobol"), setSystemTags("T3", "T4"),
      setType(VULNERABILITY));
    createRule(setRepositoryKey("foo"), setRuleKey("S111"), setLanguage("cpp"), setSystemTags(), setType(BUG));
    createRule(setRepositoryKey("foo"), setRuleKey("S112"), setLanguage("cpp"), setSystemTags(), setType(CODE_SMELL));
    createRule(setRepositoryKey("foo"), setRuleKey("S113"), setLanguage("cpp"), setSystemTags("T2", "T3"), setType(CODE_SMELL));
    index();
  }

  @Test
  public void sticky_facets_base() {
    setupStickyFacets();

    RuleQuery query = new RuleQuery();

    assertThat(underTest.search(query, new SearchOptions()).getUuids()).hasSize(9);
  }

  /**
   * Facet with no filters at all
   */
  @Test
  public void sticky_facets_no_filters() {
    setupStickyFacets();
    RuleQuery query = new RuleQuery();

    SearchIdResult<String> result = underTest.search(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES, FACET_REPOSITORIES,
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
  public void sticky_facets_with_1_filter() {
    setupStickyFacets();
    RuleQuery query = new RuleQuery().setLanguages(ImmutableList.of("cpp"));

    SearchIdResult<String> result = underTest.search(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES, FACET_REPOSITORIES, FACET_TAGS)));
    assertThat(result.getUuids()).hasSize(3);
    assertThat(result.getFacets().getAll()).hasSize(3);
    assertThat(result.getFacets().get(FACET_LANGUAGES)).containsOnlyKeys("cpp", "java", "cobol");
    assertThat(result.getFacets().get(FACET_REPOSITORIES)).containsOnlyKeys("foo");
    assertThat(result.getFacets().get(FACET_TAGS)).containsOnlyKeys("T2", "T3");
  }

  @Test
  public void languages_facet_should_return_top_100_items() {
    rangeClosed(1, 101).forEach(i -> db.rules().insert(r -> r.setLanguage("lang" + i)));
    index();

    SearchIdResult<String> result = underTest.search(new RuleQuery(), new SearchOptions().addFacets(singletonList(FACET_LANGUAGES)));

    assertThat(result.getFacets().get(FACET_LANGUAGES)).hasSize(100);
  }

  @Test
  public void repositories_facet_should_return_top_100_items() {
    rangeClosed(1, 101).forEach(i -> db.rules().insert(r -> r.setRepositoryKey("repo" + i)));
    index();

    SearchIdResult<String> result = underTest.search(new RuleQuery(), new SearchOptions().addFacets(singletonList(FACET_REPOSITORIES)));

    assertThat(result.getFacets().get(FACET_REPOSITORIES)).hasSize(100);
  }

  @Test
  public void tags_facet_should_find_tags() {
    RuleDefinitionDto rule = createRule(setSystemTags());
    createRuleMetadata(rule, setTags("bla"));
    index();

    RuleQuery query = new RuleQuery();
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));

    SearchIdResult<String> result = underTest.search(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).contains(entry("bla", 1L));
  }

  @Test
  public void tags_facet_should_return_top_100_items() {
    // default number of items returned in tag facet = 100
    String[] tags = get101Tags();
    createRule(setSystemTags(tags));
    index();

    RuleQuery query = new RuleQuery();
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));
    SearchIdResult<String> result = underTest.search(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).hasSize(100);
    assertThat(result.getFacets().get(FACET_TAGS)).contains(entry("tag0", 1L), entry("tag25", 1L), entry("tag99", 1L));
    assertThat(result.getFacets().get(FACET_TAGS)).doesNotContain(entry("tagA", 1L));
  }

  @Test
  public void tags_facet_should_include_matching_selected_items() {
    // default number of items returned in tag facet = 100
    String[] tags = get101Tags();
    createRule(setSystemTags(tags));
    index();

    RuleQuery query = new RuleQuery()
      .setTags(singletonList("tagA"));
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));
    SearchIdResult<String> result = underTest.search(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).hasSize(101);
    assertThat(result.getFacets().get(FACET_TAGS).entrySet()).extracting(e -> entry(e.getKey(), e.getValue())).contains(

      // check that selected item is added, although there are 100 other items
      entry("tagA", 1L),

      entry("tag0", 1L), entry("tag25", 1L), entry("tag99", 1L));
  }

  @Test
  public void tags_facet_should_be_available() {
    RuleQuery query = new RuleQuery();
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));

    SearchIdResult<String> result = underTest.search(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).isNotNull();
  }

  /**
   * Facet with 2 filters
   * -- lang facet for tag T2
   * -- tag facet for lang cpp
   * -- repository for cpp & T2
   */
  @Test
  public void sticky_facets_with_2_filters() {
    setupStickyFacets();

    RuleQuery query = new RuleQuery()
      .setLanguages(ImmutableList.of("cpp"))
      .setTags(ImmutableList.of("T2"));

    SearchIdResult<String> result = underTest.search(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES, FACET_REPOSITORIES,
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
  public void sticky_facets_with_3_filters() {
    setupStickyFacets();

    RuleQuery query = new RuleQuery()
      .setLanguages(ImmutableList.of("cpp", "java"))
      .setTags(ImmutableList.of("T2"))
      .setTypes(asList(BUG, CODE_SMELL));

    SearchIdResult<String> result = underTest.search(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES, FACET_REPOSITORIES, FACET_TAGS,
      FACET_TYPES)));
    assertThat(result.getUuids()).hasSize(2);
    assertThat(result.getFacets().getAll()).hasSize(4);
    assertThat(result.getFacets().get(FACET_LANGUAGES)).containsOnlyKeys("cpp", "java");
    assertThat(result.getFacets().get(FACET_REPOSITORIES)).containsOnlyKeys("foo", "xoo");
    assertThat(result.getFacets().get(FACET_TAGS)).containsOnlyKeys("T1", "T2", "T3");
    assertThat(result.getFacets().get(FACET_TYPES)).containsOnlyKeys("CODE_SMELL");
  }

  @Test
  public void sort_by_name() {
    RuleDefinitionDto abcd = createRule(setName("abcd"));
    RuleDefinitionDto abc = createRule(setName("ABC"));
    RuleDefinitionDto fgh = createRule(setName("FGH"));
    index();

    // ascending
    RuleQuery query = new RuleQuery().setSortField(RuleIndexDefinition.FIELD_RULE_NAME);
    SearchIdResult<String> results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsExactly(abc.getUuid(), abcd.getUuid(), fgh.getUuid());

    // descending
    query = new RuleQuery().setSortField(RuleIndexDefinition.FIELD_RULE_NAME).setAscendingSort(false);
    results = underTest.search(query, new SearchOptions());
    assertThat(results.getUuids()).containsExactly(fgh.getUuid(), abcd.getUuid(), abc.getUuid());
  }

  @Test
  public void default_sort_is_by_updated_at_desc() {
    RuleDefinitionDto old = createRule(setCreatedAt(1000L), setUpdatedAt(1000L));
    RuleDefinitionDto oldest = createRule(setCreatedAt(1000L), setUpdatedAt(3000L));
    RuleDefinitionDto older = createRule(setCreatedAt(1000L), setUpdatedAt(2000L));
    index();

    SearchIdResult<String> results = underTest.search(new RuleQuery(), new SearchOptions());
    assertThat(results.getUuids()).containsExactly(oldest.getUuid(), older.getUuid(), old.getUuid());
  }

  @Test
  public void fail_sort_by_language() {
    try {
      // Sorting on a field not tagged as sortable
      new RuleQuery().setSortField(RuleIndexDefinition.FIELD_RULE_LANGUAGE);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Field 'lang' is not sortable");
    }
  }

  @Test
  public void paging() {
    createRule();
    createRule();
    createRule();
    index();

    // from 0 to 1 included
    SearchOptions options = new SearchOptions();
    options.setOffset(0).setLimit(2);
    SearchIdResult<String> results = underTest.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getUuids()).hasSize(2);

    // from 0 to 9 included
    options.setOffset(0).setLimit(10);
    results = underTest.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getUuids()).hasSize(3);

    // from 2 to 11 included
    options.setOffset(2).setLimit(10);
    results = underTest.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getUuids()).hasSize(1);
  }

  @Test
  public void search_all_keys_by_query() {
    createRule(setRepositoryKey("javascript"), setRuleKey("X001"));
    createRule(setRepositoryKey("cobol"), setRuleKey("X001"));
    createRule(setRepositoryKey("php"), setRuleKey("S002"));
    index();

    // key
    assertThat(underTest.searchAll(new RuleQuery().setQueryText("X001"))).toIterable().hasSize(2);

    // partial key does not match
    assertThat(underTest.searchAll(new RuleQuery().setQueryText("X00"))).toIterable().isEmpty();

    // repo:key -> nice-to-have !
    assertThat(underTest.searchAll(new RuleQuery().setQueryText("javascript:X001"))).toIterable().hasSize(1);
  }

  @Test
  public void searchAll_keys_by_profile() {
    RuleDefinitionDto rule1 = createRule();
    RuleDefinitionDto rule2 = createRule();
    RuleDefinitionDto rule3 = createRule();
    QProfileDto profile1 = createJavaProfile();
    QProfileDto profile2 = createJavaProfile();
    db.qualityProfiles().activateRule(profile1, rule1);
    db.qualityProfiles().activateRule(profile2, rule1);
    db.qualityProfiles().activateRule(profile1, rule2);
    index();

    // inactive rules on profile
    es.getDocuments(TYPE_RULE);
    es.getDocuments(TYPE_ACTIVE_RULE);
    assertThat(underTest.searchAll(new RuleQuery().setActivation(false).setQProfile(profile2)))
      .toIterable()
      .containsOnly(rule2.getUuid(), rule3.getUuid());

    // active rules on profile
    assertThat(underTest.searchAll(new RuleQuery().setActivation(true).setQProfile(profile2)))
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
