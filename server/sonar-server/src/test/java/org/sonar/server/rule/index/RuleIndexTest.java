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
package org.sonar.server.rule.index;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.index.ActiveRuleDoc;
import org.sonar.server.qualityprofile.index.ActiveRuleDocTesting;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.entry;
import static org.junit.Assert.fail;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.db.rule.RuleTesting.setCreatedAt;
import static org.sonar.db.rule.RuleTesting.setIsTemplate;
import static org.sonar.db.rule.RuleTesting.setLanguage;
import static org.sonar.db.rule.RuleTesting.setName;
import static org.sonar.db.rule.RuleTesting.setOrganizationUuid;
import static org.sonar.db.rule.RuleTesting.setRepositoryKey;
import static org.sonar.db.rule.RuleTesting.setRuleKey;
import static org.sonar.db.rule.RuleTesting.setSeverity;
import static org.sonar.db.rule.RuleTesting.setStatus;
import static org.sonar.db.rule.RuleTesting.setSystemTags;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.db.rule.RuleTesting.setTemplateId;
import static org.sonar.db.rule.RuleTesting.setType;
import static org.sonar.db.rule.RuleTesting.setUpdatedAt;
import static org.sonar.server.qualityprofile.ActiveRule.Inheritance.INHERITED;
import static org.sonar.server.qualityprofile.ActiveRule.Inheritance.OVERRIDES;
import static org.sonar.server.rule.index.RuleIndex.FACET_LANGUAGES;
import static org.sonar.server.rule.index.RuleIndex.FACET_REPOSITORIES;
import static org.sonar.server.rule.index.RuleIndex.FACET_TAGS;
import static org.sonar.server.rule.index.RuleIndex.FACET_TYPES;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE;

public class RuleIndexTest {

  private static final String QUALITY_PROFILE_KEY1 = "qp1";
  private static final String QUALITY_PROFILE_KEY2 = "qp2";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester tester = new EsTester(new RuleIndexDefinition(new MapSettings()));
  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private RuleIndex index;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;

  @Before
  public void setUp() {
    ruleIndexer = new RuleIndexer(tester.client(), dbTester.getDbClient());
    activeRuleIndexer = new ActiveRuleIndexer(system2, dbTester.getDbClient(), tester.client());
    index = new RuleIndex(tester.client());
  }

  @Test
  public void search_all_rules() {
    createRule();
    createRule();

    SearchIdResult results = index.search(new RuleQuery(), new SearchOptions());

    assertThat(results.getTotal()).isEqualTo(2);
    assertThat(results.getIds()).hasSize(2);
  }

  @Test
  public void search_by_key() {
    RuleDefinitionDto js1 = createRule(
      setRepositoryKey("javascript"),
      setRuleKey("X001"));
    RuleDefinitionDto cobol1 = createRule(
      setRepositoryKey("cobol"),
      setRuleKey("X001"));
    RuleDefinitionDto php2 = createRule(
      setRepositoryKey("php"),
      setRuleKey("S002"));

    // key
    RuleQuery query = new RuleQuery().setQueryText("X001");
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(js1.getKey(), cobol1.getKey());

    // partial key does not match
    query = new RuleQuery().setQueryText("X00");
    assertThat(index.search(query, new SearchOptions()).getIds()).isEmpty();

    // repo:key -> nice-to-have !
    query = new RuleQuery().setQueryText("javascript:X001");
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(js1.getKey());
  }

  @Test
  public void search_by_case_insensitive_key() {
    RuleDefinitionDto ruleDto = createRule(
      setRepositoryKey("javascript"),
      setRuleKey("X001"));

    RuleQuery query = new RuleQuery().setQueryText("x001");
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(ruleDto.getKey());
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

    // key
    RuleQuery query = new RuleQuery().setKey(RuleKey.of("javascript", "X001").toString());

    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(1);

    // partial key does not match
    query = new RuleQuery().setKey("X001");
    assertThat(index.search(query, new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void search_name_by_query() {
    createRule(setName("testing the partial match and matching of rule"));

    // substring
    RuleQuery query = new RuleQuery().setQueryText("test");
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(1);

    // substring
    query = new RuleQuery().setQueryText("partial match");
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(1);

    // case-insensitive
    query = new RuleQuery().setQueryText("TESTING");
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(1);

    // not found
    query = new RuleQuery().setQueryText("not present");
    assertThat(index.search(query, new SearchOptions()).getIds()).isEmpty();
  }

  @Test
  public void search_name_with_protected_chars() {
    String nameWithProtectedChars = "ja#va&sc\"r:ipt";

    RuleDefinitionDto ruleDto = createRule(setName(nameWithProtectedChars));

    RuleQuery protectedCharsQuery = new RuleQuery().setQueryText(nameWithProtectedChars);
    List<RuleKey> results = index.search(protectedCharsQuery, new SearchOptions()).getIds();
    assertThat(results).containsOnly(ruleDto.getKey());
  }

  @Test
  public void search_by_any_of_repositories() {
    RuleDefinitionDto findbugs = createRule(
      setRepositoryKey("findbugs"),
      setRuleKey("S001"));
    RuleDefinitionDto pmd = createRule(
      setRepositoryKey("pmd"),
      setRuleKey("S002"));

    RuleQuery query = new RuleQuery().setRepositories(asList("checkstyle", "pmd"));
    SearchIdResult results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).containsOnly(pmd.getKey());

    // no results
    query = new RuleQuery().setRepositories(singletonList("checkstyle"));
    assertThat(index.search(query, new SearchOptions()).getIds()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setRepositories(emptyList());
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(findbugs.getKey(), pmd.getKey());
  }

  @Test
  public void filter_by_tags() {
    OrganizationDto organization = dbTester.organizations().insert();

    RuleDefinitionDto rule1 = createRule(setSystemTags("tag1s"));
    createRuleMetadata(rule1, organization, setTags("tag1"));
    RuleDefinitionDto rule2 = createRule(setSystemTags("tag2s"));
    createRuleMetadata(rule2, organization, setTags("tag2"));

    // find all
    RuleQuery query = new RuleQuery();
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(rule1.getKey(), rule2.getKey());

    // tag2s in filter
    query = new RuleQuery().setOrganizationUuid(organization.getUuid()).setTags(of("tag2s"));
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(rule2.getKey());

    // tag2 in filter
    query = new RuleQuery().setOrganizationUuid(organization.getUuid()).setTags(of("tag2"));
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(rule2.getKey());

    // empty list => no filter
    query = new RuleQuery().setTags(emptySet());
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(rule1.getKey(), rule2.getKey());

    // null list => no filter
    query = new RuleQuery().setTags(null);
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(rule1.getKey(), rule2.getKey());
  }

  @SafeVarargs
  private final RuleDefinitionDto createRule(Consumer<RuleDefinitionDto>... populaters) {
    RuleDefinitionDto ruleDto = dbTester.rules().insert(populaters);
    ruleIndexer.indexRuleDefinition(ruleDto.getKey());
    return ruleDto;
  }

  @SafeVarargs
  private final RuleMetadataDto createRuleMetadata(RuleDefinitionDto rule, OrganizationDto organization, Consumer<RuleMetadataDto>... populaters) {
    RuleMetadataDto ruleMetadataDto = dbTester.rules().insertOrUpdateMetadata(rule, organization, populaters);
    ruleIndexer.indexRuleExtension(organization, rule.getKey());
    return ruleMetadataDto;
  }

  @Test
  public void tags_facet_supports_selected_value_with_regexp_special_characters() {
    OrganizationDto organization = dbTester.organizations().insert();

    RuleDefinitionDto rule = createRule();
    createRuleMetadata(rule, organization, setTags("misra++"));

    RuleQuery query = new RuleQuery()
      .setOrganizationUuid(organization.getUuid())
      .setTags(singletonList("misra["));
    SearchOptions options = new SearchOptions().addFacets(FACET_TAGS);

    // do not fail
    assertThat(index.search(query, options).getTotal()).isEqualTo(0);
  }

  @Test
  public void search_by_types() {
    RuleDefinitionDto codeSmell = createRule(setType(CODE_SMELL));
    RuleDefinitionDto vulnerability = createRule(setType(VULNERABILITY));
    RuleDefinitionDto bug1 = createRule(setType(BUG));
    RuleDefinitionDto bug2 = createRule(setType(BUG));

    // find all
    RuleQuery query = new RuleQuery();
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(4);

    // type3 in filter
    query = new RuleQuery().setTypes(of(VULNERABILITY));
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(vulnerability.getKey());

    query = new RuleQuery().setTypes(of(BUG));
    assertThat(index.search(query, new SearchOptions()).getIds()).containsOnly(bug1.getKey(), bug2.getKey());

    // types in query => nothing
    query = new RuleQuery().setQueryText("code smell bug vulnerability");
    assertThat(index.search(query, new SearchOptions()).getIds()).isEmpty();

    // null list => no filter
    query = new RuleQuery().setTypes(emptySet());
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(4);

    // null list => no filter
    query = new RuleQuery().setTypes(null);
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(4);
  }

  @Test
  public void search_by_is_template() {
    RuleDefinitionDto ruleNoTemplate = createRule(setIsTemplate(false));
    RuleDefinitionDto ruleIsTemplate = createRule(setIsTemplate(true));

    // find all
    RuleQuery query = new RuleQuery();
    SearchIdResult results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).hasSize(2);

    // Only template
    query = new RuleQuery().setIsTemplate(true);
    results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).containsOnly(ruleIsTemplate.getKey());

    // Only not template
    query = new RuleQuery().setIsTemplate(false);
    results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).containsOnly(ruleNoTemplate.getKey());

    // null => no filter
    query = new RuleQuery().setIsTemplate(null);
    results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).containsOnly(ruleIsTemplate.getKey(), ruleNoTemplate.getKey());
  }

  @Test
  public void search_by_template_key() {
    RuleDefinitionDto template = createRule(setIsTemplate(true));
    RuleDefinitionDto customRule = createRule(setTemplateId(template.getId()));

    // find all
    RuleQuery query = new RuleQuery();
    SearchIdResult results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).hasSize(2);

    // Only custom rule
    query = new RuleQuery().setTemplateKey(template.getKey().toString());
    results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).containsOnly(customRule.getKey());

    // null => no filter
    query = new RuleQuery().setTemplateKey(null);
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(2);
  }

  @Test
  public void search_by_any_of_languages() {
    RuleDefinitionDto java = createRule(setLanguage("java"));
    RuleDefinitionDto javascript = createRule(setLanguage("js"));

    RuleQuery query = new RuleQuery().setLanguages(asList("cobol", "js"));
    SearchIdResult results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).containsOnly(javascript.getKey());

    // no results
    query = new RuleQuery().setLanguages(singletonList("cpp"));
    assertThat(index.search(query, new SearchOptions()).getIds()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setLanguages(emptyList());
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setLanguages(null);
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(2);
  }

  @Test
  public void search_by_any_of_severities() {
    RuleDefinitionDto blocker = createRule(setSeverity(BLOCKER));
    RuleDefinitionDto info = createRule(setSeverity(INFO));

    RuleQuery query = new RuleQuery().setSeverities(asList(INFO, MINOR));
    SearchIdResult results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).containsOnly(info.getKey());

    // no results
    query = new RuleQuery().setSeverities(singletonList(MINOR));
    assertThat(index.search(query, new SearchOptions()).getIds()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setSeverities(emptyList());
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setSeverities();
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(2);
  }

  @Test
  public void search_by_any_of_statuses() {
    RuleDefinitionDto beta = createRule(setStatus(RuleStatus.BETA));
    RuleDefinitionDto ready = createRule(setStatus(RuleStatus.READY));

    RuleQuery query = new RuleQuery().setStatuses(asList(RuleStatus.DEPRECATED, RuleStatus.READY));
    SearchIdResult<RuleKey> results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).containsOnly(ready.getKey());

    // no results
    query = new RuleQuery().setStatuses(singletonList(RuleStatus.DEPRECATED));
    assertThat(index.search(query, new SearchOptions()).getIds()).isEmpty();

    // empty list => no filter
    query = new RuleQuery().setStatuses(emptyList());
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(2);

    // null list => no filter
    query = new RuleQuery().setStatuses(null);
    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(2);
  }

  @Test
  public void search_by_profile() throws InterruptedException {
    RuleDefinitionDto rule1 = createRule();
    RuleDefinitionDto rule2 = createRule();
    RuleDefinitionDto rule3 = createRule();

    indexActiveRules(
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, rule1.getKey())),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, rule1.getKey())),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, rule2.getKey())));

    assertThat(tester.countDocuments(INDEX_TYPE_ACTIVE_RULE)).isEqualTo(3);

    // 1. get all active rules.
    assertThat(index.search(new RuleQuery().setActivation(true), new SearchOptions()).getIds())
      .containsOnly(rule1.getKey(), rule2.getKey());

    // 2. get all inactive rules.
    assertThat(index.search(new RuleQuery().setActivation(false), new SearchOptions()).getIds())
      .containsOnly(rule3.getKey());

    // 3. get all rules not active on profile
    assertThat(index.search(new RuleQuery().setActivation(false).setQProfileKey(QUALITY_PROFILE_KEY2), new SearchOptions()).getIds())
      .containsOnly(rule2.getKey(), rule3.getKey());

    // 4. get all active rules on profile
    assertThat(index.search(new RuleQuery().setActivation(true).setQProfileKey(QUALITY_PROFILE_KEY2), new SearchOptions()).getIds())
      .containsOnly(rule1.getKey());
  }

  @Test
  public void search_by_profile_and_inheritance() {
    RuleDefinitionDto rule1 = createRule();
    RuleDefinitionDto rule2 = createRule();
    RuleDefinitionDto rule3 = createRule();
    RuleDefinitionDto rule4 = createRule();

    ActiveRuleKey activeRuleKey1 = ActiveRuleKey.of(QUALITY_PROFILE_KEY1, rule1.getKey());
    ActiveRuleKey activeRuleKey2 = ActiveRuleKey.of(QUALITY_PROFILE_KEY1, rule2.getKey());
    ActiveRuleKey activeRuleKey3 = ActiveRuleKey.of(QUALITY_PROFILE_KEY1, rule3.getKey());

    indexActiveRules(
      ActiveRuleDocTesting.newDoc(activeRuleKey1),
      ActiveRuleDocTesting.newDoc(activeRuleKey2),
      ActiveRuleDocTesting.newDoc(activeRuleKey3),
      // Profile 2 is a child a profile 1
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, rule1.getKey())).setInheritance(INHERITED.name()),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, rule2.getKey())).setInheritance(OVERRIDES.name()),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, rule3.getKey())).setInheritance(INHERITED.name()));

    // 0. get all rules
    assertThat(index.search(new RuleQuery(), new SearchOptions()).getIds())
      .hasSize(4);

    // 1. get all active rules
    assertThat(index.search(new RuleQuery()
      .setActivation(true), new SearchOptions()).getIds())
        .hasSize(3);

    // 2. get all inactive rules.
    assertThat(index.search(new RuleQuery()
      .setActivation(false), new SearchOptions()).getIds())
        .containsOnly(rule4.getKey());

    // 3. get Inherited Rules on profile1
    assertThat(index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(QUALITY_PROFILE_KEY1)
      .setInheritance(of(INHERITED.name())),
      new SearchOptions()).getIds())
        .isEmpty();

    // 4. get Inherited Rules on profile2
    assertThat(index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(QUALITY_PROFILE_KEY2)
      .setInheritance(of(INHERITED.name())),
      new SearchOptions()).getIds())
        .hasSize(2);

    // 5. get Overridden Rules on profile1
    assertThat(index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(QUALITY_PROFILE_KEY1)
      .setInheritance(of(OVERRIDES.name())),
      new SearchOptions()).getIds())
        .isEmpty();

    // 6. get Overridden Rules on profile2
    assertThat(index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(QUALITY_PROFILE_KEY2)
      .setInheritance(of(OVERRIDES.name())),
      new SearchOptions()).getIds())
        .hasSize(1);

    // 7. get Inherited AND Overridden Rules on profile1
    assertThat(index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(QUALITY_PROFILE_KEY1)
      .setInheritance(of(INHERITED.name(), OVERRIDES.name())),
      new SearchOptions()).getIds())
        .isEmpty();

    // 8. get Inherited AND Overridden Rules on profile2
    assertThat(index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(QUALITY_PROFILE_KEY2)
      .setInheritance(of(INHERITED.name(), OVERRIDES.name())),
      new SearchOptions()).getIds())
        .hasSize(3);
  }

  @Test
  public void search_by_profile_and_active_severity() {
    RuleDefinitionDto major = createRule(setSeverity(MAJOR));
    RuleDefinitionDto minor = createRule(setSeverity(MINOR));
    RuleDefinitionDto info = createRule(setSeverity(INFO));

    indexActiveRules(
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, major.getKey())).setSeverity(BLOCKER),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, major.getKey())).setSeverity(BLOCKER),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, minor.getKey())).setSeverity(CRITICAL));

    // 1. get all active rules.
    assertThat(index.search(new RuleQuery().setActivation(true).setQProfileKey(QUALITY_PROFILE_KEY1), new SearchOptions()).getIds())
      .hasSize(2);

    // 2. get rules with active severity critical.
    SearchIdResult<RuleKey> result = index.search(new RuleQuery().setActivation(true)
      .setQProfileKey(QUALITY_PROFILE_KEY1).setActiveSeverities(singletonList(CRITICAL)),
      new SearchOptions().addFacets(singletonList(RuleIndex.FACET_ACTIVE_SEVERITIES)));
    assertThat(result.getIds()).containsOnly(minor.getKey());

    // check stickyness of active severity facet
    assertThat(result.getFacets().get(RuleIndex.FACET_ACTIVE_SEVERITIES)).containsOnly(entry(BLOCKER, 1L), entry(CRITICAL, 1L));

    // 3. count activation severities of all active rules
    result = index.search(new RuleQuery(), new SearchOptions().addFacets(singletonList(RuleIndex.FACET_ACTIVE_SEVERITIES)));
    assertThat(result.getIds()).hasSize(3);
    assertThat(result.getFacets().get(RuleIndex.FACET_ACTIVE_SEVERITIES)).containsOnly(entry(BLOCKER, 2L), entry(CRITICAL, 1L));
  }

  @Test
  public void listTags_should_return_both_system_tags_and_organization_specific_tags() {
    OrganizationDto organization = dbTester.organizations().insert();

    RuleDefinitionDto rule1 = createRule(setSystemTags("sys1", "sys2"));
    createRuleMetadata(rule1, organization,
      setOrganizationUuid(organization.getUuid()),
      setTags("tag1"));

    RuleDefinitionDto rule2 = createRule(setSystemTags());
    createRuleMetadata(rule2, organization,
      setOrganizationUuid(organization.getUuid()),
      setTags("tag2"));

    assertThat(index.listTags(organization, null, 10)).containsOnly("tag1", "tag2", "sys1", "sys2");
  }

  @Test
  public void listTags_must_not_return_tags_of_other_organizations() {
    OrganizationDto organization1 = dbTester.organizations().insert();
    RuleDefinitionDto rule1 = createRule(setSystemTags("sys1"));
    createRuleMetadata(rule1, organization1,
      setOrganizationUuid(organization1.getUuid()),
      setTags("tag1"));

    OrganizationDto organization2 = dbTester.organizations().insert();
    RuleDefinitionDto rule2 = createRule(setSystemTags("sys2"));
    createRuleMetadata(rule2, organization2,
      setOrganizationUuid(organization2.getUuid()),
      setTags("tag2"));

    OrganizationDto organization3 = dbTester.organizations().insert();

    assertThat(index.listTags(organization1, null, 10)).containsOnly("tag1", "sys1", "sys2");
    assertThat(index.listTags(organization2, null, 10)).containsOnly("tag2", "sys1", "sys2");
    assertThat(index.listTags(organization3, null, 10)).containsOnly("sys1", "sys2");
  }

  @Test
  public void available_since() throws InterruptedException {
    RuleKey ruleOld = createRule(setCreatedAt(1000L)).getKey();
    RuleKey ruleOlder = createRule(setCreatedAt(2000L)).getKey();

    // 0. find all rules;
    assertThat(index.search(new RuleQuery(), new SearchOptions()).getIds()).containsOnly(ruleOld, ruleOlder);

    // 1. find all rules available since a date;
    RuleQuery availableSinceQuery = new RuleQuery().setAvailableSince(2000L);
    assertThat(index.search(availableSinceQuery, new SearchOptions()).getIds()).containsOnly(ruleOlder);

    // 2. find no new rules since tomorrow.
    RuleQuery availableSinceNowQuery = new RuleQuery().setAvailableSince(3000L);
    assertThat(index.search(availableSinceNowQuery, new SearchOptions()).getIds()).containsOnly();
  }

  @Test
  public void global_facet_on_repositories_and_tags() {
    OrganizationDto organization = dbTester.organizations().insert();

    createRule(setRepositoryKey("php"), setSystemTags("sysTag"));
    RuleDefinitionDto rule1 = createRule(setRepositoryKey("php"), setSystemTags());
    createRuleMetadata(rule1, organization, setTags("tag1"));
    RuleDefinitionDto rule2 = createRule(setRepositoryKey("javascript"), setSystemTags());
    createRuleMetadata(rule2, organization, setTags("tag1", "tag2"));

    // should not have any facet!
    RuleQuery query = new RuleQuery().setOrganizationUuid(organization.getUuid());
    SearchIdResult result1 = index.search(query, new SearchOptions());
    assertThat(result1.getFacets().getAll()).isEmpty();

    // should not have any facet on non matching query!
    SearchIdResult result2 = index.search(new RuleQuery().setQueryText("aeiou"), new SearchOptions().addFacets(singletonList("repositories")));
    assertThat(result2.getFacets().getAll()).hasSize(1);
    assertThat(result2.getFacets().getAll().get("repositories")).isEmpty();

    // Repositories Facet is preset
    SearchIdResult result3 = index.search(query, new SearchOptions().addFacets(asList("repositories", "tags")));
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

  private void sticky_facet_rule_setup() {
    insertRuleDefinition(setRepositoryKey("xoo"), setRuleKey("S001"), setLanguage("java"), setSystemTags(), setType(BUG));
    insertRuleDefinition(setRepositoryKey("xoo"), setRuleKey("S002"), setLanguage("java"), setSystemTags(), setType(CODE_SMELL));
    insertRuleDefinition(setRepositoryKey("xoo"), setRuleKey("S003"), setLanguage("java"), setSystemTags("T1", "T2"), setType(CODE_SMELL));
    insertRuleDefinition(setRepositoryKey("xoo"), setRuleKey("S011"), setLanguage("cobol"), setSystemTags(), setType(CODE_SMELL));
    insertRuleDefinition(setRepositoryKey("xoo"), setRuleKey("S012"), setLanguage("cobol"), setSystemTags(), setType(BUG));
    insertRuleDefinition(setRepositoryKey("foo"), setRuleKey("S013"), setLanguage("cobol"), setSystemTags("T3", "T4"), setType(VULNERABILITY));
    insertRuleDefinition(setRepositoryKey("foo"), setRuleKey("S111"), setLanguage("cpp"), setSystemTags(), setType(BUG));
    insertRuleDefinition(setRepositoryKey("foo"), setRuleKey("S112"), setLanguage("cpp"), setSystemTags(), setType(CODE_SMELL));
    insertRuleDefinition(setRepositoryKey("foo"), setRuleKey("S113"), setLanguage("cpp"), setSystemTags("T2", "T3"), setType(CODE_SMELL));
  }

  @Test
  public void sticky_facets_base() {
    sticky_facet_rule_setup();

    RuleQuery query = new RuleQuery();

    assertThat(index.search(query, new SearchOptions()).getIds()).hasSize(9);
  }

  /**
   * Facet with no filters at all
   */
  @Test
  public void sticky_facets_no_filters() {
    sticky_facet_rule_setup();

    RuleQuery query = new RuleQuery()
      .setOrganizationUuid("some_uuid");

    SearchIdResult result = index.search(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES, FACET_REPOSITORIES,
      FACET_TAGS, FACET_TYPES)));
    assertThat(result.getFacets().getAll()).hasSize(4);
    assertThat(result.getFacets().getAll().get(FACET_LANGUAGES).keySet()).containsOnly("cpp", "java", "cobol");
    assertThat(result.getFacets().getAll().get(FACET_REPOSITORIES).keySet()).containsExactly("xoo", "foo");
    assertThat(result.getFacets().getAll().get(FACET_TAGS).keySet()).containsOnly("T1", "T2", "T3", "T4");
    assertThat(result.getFacets().getAll().get(FACET_TYPES).keySet()).containsOnly("BUG", "CODE_SMELL", "VULNERABILITY");
  }

  /**
   * Facet with a language filter
   * -- lang facet should still have all language
   */
  @Test
  public void sticky_facets_with_1_filter() {
    sticky_facet_rule_setup();

    RuleQuery query = new RuleQuery()
      .setOrganizationUuid("some_uuid")
      .setLanguages(ImmutableList.of("cpp"));

    SearchIdResult result = index.search(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES,
      FACET_REPOSITORIES, FACET_TAGS)));
    assertThat(result.getIds()).hasSize(3);
    assertThat(result.getFacets().getAll()).hasSize(3);
    assertThat(result.getFacets().get(FACET_LANGUAGES).keySet()).containsOnly("cpp", "java", "cobol");
    assertThat(result.getFacets().get(FACET_REPOSITORIES).keySet()).containsOnly("foo");
    assertThat(result.getFacets().get(FACET_TAGS).keySet()).containsOnly("T2", "T3");
  }

  @Test
  public void tags_facet_should_find_tags_of_specified_organization() {
    OrganizationDto organization = dbTester.organizations().insert();

    RuleDefinitionDto rule = insertRuleDefinition(setSystemTags());
    insertRuleMetaData(organization, rule, setTags("bla"));

    RuleQuery query = new RuleQuery()
      .setOrganizationUuid(organization.getUuid());
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));

    SearchIdResult result = index.search(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).contains(entry("bla", 1L));
  }

  @Test
  public void tags_facet_should_return_top_10_items() {
    // default number of items returned in facets = 10
    RuleDefinitionDto rule = insertRuleDefinition(setSystemTags("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tagA", "tagB"));

    RuleQuery query = new RuleQuery()
      .setOrganizationUuid(dbTester.getDefaultOrganization().getUuid());
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));
    SearchIdResult result = index.search(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).containsExactly(entry("tag1", 1L), entry("tag2", 1L), entry("tag3", 1L), entry("tag4", 1L), entry("tag5", 1L),
      entry("tag6", 1L), entry("tag7", 1L), entry("tag8", 1L), entry("tag9", 1L), entry("tagA", 1L));
  }

  @Test
  public void tags_facet_should_include_matching_selected_items() {
    // default number of items returned in facets = 10
    RuleDefinitionDto rule = insertRuleDefinition(setSystemTags("tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tagA", "tagB"));

    RuleQuery query = new RuleQuery()
      .setOrganizationUuid(dbTester.getDefaultOrganization().getUuid())
      .setTags(singletonList("tagB"));
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));
    SearchIdResult result = index.search(query, options);
    assertThat(result.getFacets().get(FACET_TAGS).entrySet()).extracting(e -> entry(e.getKey(), e.getValue())).containsExactly(

      // check that selected item is added, although there are 10 other items
      entry("tagB", 1L),

      entry("tag1", 1L), entry("tag2", 1L), entry("tag3", 1L), entry("tag4", 1L), entry("tag5", 1L), entry("tag6", 1L), entry("tag7", 1L), entry("tag8", 1L), entry("tag9", 1L),
      entry("tagA", 1L));
  }

  @Test
  public void tags_facet_should_not_find_tags_of_any_other_organization() {
    OrganizationDto organization1 = dbTester.organizations().insert();
    OrganizationDto organization2 = dbTester.organizations().insert();

    RuleDefinitionDto rule = insertRuleDefinition(setSystemTags());
    insertRuleMetaData(organization1, rule, setTags("bla1"));
    insertRuleMetaData(organization2, rule, setTags("bla2"));

    RuleQuery query = new RuleQuery()
      .setOrganizationUuid(organization2.getUuid());
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));

    SearchIdResult result = index.search(query, options);
    assertThat(result.getFacets().get(FACET_TAGS).entrySet()).extracting(e -> entry(e.getKey(), e.getValue())).containsExactly(
      entry("bla2", 1L)
    );
  }

  @SafeVarargs
  private final void insertRuleMetaData(OrganizationDto organization, RuleDefinitionDto rule, Consumer<RuleMetadataDto>... consumers) {
    dbTester.rules().insertOrUpdateMetadata(rule, organization, consumers);
    ruleIndexer.indexRuleExtension(organization, rule.getKey());
  }

  @Test
  public void tags_facet_should_be_available_if_organization_is_speficied() {
    RuleQuery query = new RuleQuery()
      .setOrganizationUuid("some_org_id");
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));

    SearchIdResult result = index.search(query, options);
    assertThat(result.getFacets().get(FACET_TAGS)).isNotNull();
  }

  @Test
  public void tags_facet_should_be_unavailable_if_no_organization_is_specfified() {
    RuleQuery query = new RuleQuery();
    SearchOptions options = new SearchOptions().addFacets(singletonList(FACET_TAGS));

    thrown.expectMessage("Cannot use tags facet, if no organization is specified.");
    index.search(query, options);
  }

  /**
   * Facet with 2 filters
   * -- lang facet for tag T2
   * -- tag facet for lang cpp
   * -- repository for cpp & T2
   */
  @Test
  public void sticky_facets_with_2_filters() {
    sticky_facet_rule_setup();

    RuleQuery query = new RuleQuery()
      .setOrganizationUuid("some_uuid")
      .setLanguages(ImmutableList.of("cpp"))
      .setTags(ImmutableList.of("T2"));

    SearchIdResult result = index.search(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES, FACET_REPOSITORIES, FACET_TAGS)));
    assertThat(result.getIds()).hasSize(1);
    assertThat(result.getFacets().getAll()).hasSize(3);
    assertThat(result.getFacets().get(FACET_LANGUAGES).keySet()).containsOnly("cpp", "java");
    assertThat(result.getFacets().get(FACET_REPOSITORIES).keySet()).containsOnly("foo");
    assertThat(result.getFacets().get(FACET_TAGS).keySet()).containsOnly("T2", "T3");
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
    sticky_facet_rule_setup();

    RuleQuery query = new RuleQuery()
      .setOrganizationUuid("some_uuid")
      .setLanguages(ImmutableList.of("cpp", "java"))
      .setTags(ImmutableList.of("T2"))
      .setTypes(asList(BUG, CODE_SMELL));

    SearchIdResult result = index.search(query, new SearchOptions().addFacets(asList(FACET_LANGUAGES, FACET_REPOSITORIES, FACET_TAGS,
      FACET_TYPES)));
    assertThat(result.getIds()).hasSize(2);
    assertThat(result.getFacets().getAll()).hasSize(4);
    assertThat(result.getFacets().get(FACET_LANGUAGES).keySet()).containsOnly("cpp", "java");
    assertThat(result.getFacets().get(FACET_REPOSITORIES).keySet()).containsOnly("foo", "xoo");
    assertThat(result.getFacets().get(FACET_TAGS).keySet()).containsOnly("T1", "T2", "T3");
    assertThat(result.getFacets().get(FACET_TYPES).keySet()).containsOnly("CODE_SMELL");
  }

  @Test
  public void sort_by_name() {
    RuleDefinitionDto abcd = insertRuleDefinition(setName("abcd"));
    RuleDefinitionDto abc = insertRuleDefinition(setName("ABC"));
    RuleDefinitionDto fgh = insertRuleDefinition(setName("FGH"));

    // ascending
    RuleQuery query = new RuleQuery().setSortField(RuleIndexDefinition.FIELD_RULE_NAME);
    SearchIdResult<RuleKey> results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).containsExactly(abc.getKey(), abcd.getKey(), fgh.getKey());

    // descending
    query = new RuleQuery().setSortField(RuleIndexDefinition.FIELD_RULE_NAME).setAscendingSort(false);
    results = index.search(query, new SearchOptions());
    assertThat(results.getIds()).containsExactly(fgh.getKey(), abcd.getKey(), abc.getKey());
  }

  @Test
  public void default_sort_is_by_updated_at_desc() {
    RuleDefinitionDto old = insertRuleDefinition(setCreatedAt(1000L), setUpdatedAt(1000L));
    RuleDefinitionDto oldest = insertRuleDefinition(setCreatedAt(1000L), setUpdatedAt(3000L));
    RuleDefinitionDto older = insertRuleDefinition(setCreatedAt(1000L), setUpdatedAt(2000L));

    SearchIdResult<RuleKey> results = index.search(new RuleQuery(), new SearchOptions());
    assertThat(results.getIds()).containsExactly(oldest.getKey(), older.getKey(), old.getKey());
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
    insertRuleDefinition();
    insertRuleDefinition();
    insertRuleDefinition();

    // from 0 to 1 included
    SearchOptions options = new SearchOptions();
    options.setOffset(0).setLimit(2);
    SearchIdResult results = index.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getIds()).hasSize(2);

    // from 0 to 9 included
    options.setOffset(0).setLimit(10);
    results = index.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getIds()).hasSize(3);

    // from 2 to 11 included
    options.setOffset(2).setLimit(10);
    results = index.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getIds()).hasSize(1);

    // from 2 to 11 included
    options.setOffset(2).setLimit(0);
    results = index.search(new RuleQuery(), options);
    assertThat(results.getTotal()).isEqualTo(3);
    assertThat(results.getIds()).hasSize(1);
  }

  @Test
  public void search_all_keys_by_query() {
    insertRuleDefinition(setRepositoryKey("javascript"), setRuleKey("X001"));
    insertRuleDefinition(setRepositoryKey("cobol"), setRuleKey("X001"));
    insertRuleDefinition(setRepositoryKey("php"), setRuleKey("S002"));

    // key
    assertThat(index.searchAll(new RuleQuery().setQueryText("X001"))).hasSize(2);

    // partial key does not match
    assertThat(index.searchAll(new RuleQuery().setQueryText("X00"))).isEmpty();

    // repo:key -> nice-to-have !
    assertThat(index.searchAll(new RuleQuery().setQueryText("javascript:X001"))).hasSize(1);
  }

  @Test
  public void search_all_keys_by_profile() {
    RuleDefinitionDto rule1 = insertRuleDefinition();
    RuleDefinitionDto rule2 = insertRuleDefinition();
    RuleDefinitionDto rule3 = insertRuleDefinition();

    RuleKey rule1Key = rule1.getKey();
    RuleKey rule2Key = rule2.getKey();
    RuleKey rule3Key = rule3.getKey();

    indexActiveRules(
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, rule1Key)),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, rule1Key)),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, rule2Key)));

    assertThat(tester.countDocuments(INDEX_TYPE_ACTIVE_RULE)).isEqualTo(3);

    // 1. get all active rules.
    assertThat(index.searchAll(new RuleQuery().setActivation(true)))
      .containsOnly(rule1Key, rule2Key);

    // 2. get all inactive rules.
    assertThat(index.searchAll(new RuleQuery().setActivation(false)))
      .containsOnly(rule3Key);

    // 3. get all rules not active on profile
    assertThat(index.searchAll(new RuleQuery().setActivation(false).setQProfileKey(QUALITY_PROFILE_KEY2)))
      .containsOnly(rule2Key, rule3Key);

    // 4. get all active rules on profile
    assertThat(index.searchAll(new RuleQuery().setActivation(true).setQProfileKey(QUALITY_PROFILE_KEY2)))
      .containsOnly(rule1Key);
  }

  @Test
  public void search_all_keys_by_profile_in_specific_organization() {
    RuleDefinitionDto rule1 = insertRuleDefinition();
    RuleDefinitionDto rule2 = insertRuleDefinition();
    RuleDefinitionDto rule3 = insertRuleDefinition();

    RuleKey rule1Key = rule1.getKey();
    RuleKey rule2Key = rule2.getKey();
    RuleKey rule3Key = rule3.getKey();

    OrganizationDto organization = dbTester.organizations().insert();

    indexActiveRules(
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, rule1Key)).setOrganizationUuid(organization.getUuid()),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, rule1Key)).setOrganizationUuid(organization.getUuid()),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, rule2Key)).setOrganizationUuid(organization.getUuid()));

    assertThat(tester.countDocuments(INDEX_TYPE_ACTIVE_RULE)).isEqualTo(3);

    // 1. get all active rules.
    assertThat(index.searchAll(new RuleQuery().setActivation(true).setOrganizationUuid(organization.getUuid())))
      .containsOnly(rule1Key, rule2Key);

    // 2. get all inactive rules.
    assertThat(index.searchAll(new RuleQuery().setActivation(false).setOrganizationUuid(organization.getUuid())))
      .containsOnly(rule3Key);

    // 3. get all rules not active on profile
    assertThat(index.searchAll(new RuleQuery().setActivation(false).setOrganizationUuid(organization.getUuid()).setQProfileKey(QUALITY_PROFILE_KEY2)))
      .containsOnly(rule2Key, rule3Key);

    // 4. get all active rules on profile
    assertThat(index.searchAll(new RuleQuery().setActivation(true).setOrganizationUuid(organization.getUuid()).setQProfileKey(QUALITY_PROFILE_KEY2)))
      .containsOnly(rule1Key);
  }

  @SafeVarargs
  private final RuleDefinitionDto insertRuleDefinition(Consumer<RuleDefinitionDto>... populaters) {
    RuleDefinitionDto ruleDefinitionDto = dbTester.rules().insert(populaters);
    ruleIndexer.indexRuleDefinition(ruleDefinitionDto.getKey());
    return ruleDefinitionDto;
  }

  private void indexActiveRules(ActiveRuleDoc... docs) {
    activeRuleIndexer.index(asList(docs).iterator());
  }
}
