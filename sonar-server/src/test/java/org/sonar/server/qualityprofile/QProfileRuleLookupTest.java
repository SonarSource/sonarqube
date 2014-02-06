/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.qualityprofile;

import com.github.tlrx.elasticsearch.test.EsSetup;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.Severity;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.es.ESIndex;
import org.sonar.server.es.ESNode;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.test.TestUtils;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QProfileRuleLookupTest {

  private QProfileRuleLookup profileRules;
  private EsSetup esSetup;

  @Before
  public void setUp() throws Exception {
    esSetup = new EsSetup(ImmutableSettings.builder()
      .loadFromUrl(ESNode.class.getResource("config/elasticsearch.json"))
      .build()
      );
    esSetup.execute(EsSetup.deleteAll());

    ESNode searchNode = mock(ESNode.class);
    when(searchNode.client()).thenReturn(esSetup.client());

    Settings settings = new Settings();
    settings.setProperty("sonar.log.profilingLevel", "FULL");
    ESIndex index = new ESIndex(searchNode, new Profiling(settings));
    index.start();
    RuleRegistry registry = new RuleRegistry(index, null);
    registry.start();
    ESActiveRule esActiveRule = new ESActiveRule(index, null, null, null);
    esActiveRule.start();
    profileRules = new QProfileRuleLookup(index);

    esSetup.client().prepareBulk()
      // On profile 1
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("shared/rule25.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("25").source(testFileAsString("shared/active_rule25.json")))
        // On profile 1 and 2
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("shared/rule759.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("759").source(testFileAsString("shared/active_rule391.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("759").source(testFileAsString("shared/active_rule523.json")))
        // On profile 1
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("shared/rule1482.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("1482").source(testFileAsString("shared/active_rule2702.json")))
        // Rules on no profile
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("shared/rule944.json")))
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("shared/rule719.json")))
        // Removed rule
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("shared/rule860.json")))
      .setRefresh(true).execute().actionGet();
  }

  @After
  public void tearDown() {
    esSetup.terminate();
  }

  @Test
  public void find_by_rule_id() {
    assertThat(profileRules.findByRuleId(25)).isNotNull();
    assertThat(profileRules.findByRuleId(9999)).isNull();
  }

  @Test
  public void find_by_active_rule_id() {
    assertThat(profileRules.findByActiveRuleId(391)).isNotNull();
    assertThat(profileRules.findByActiveRuleId(9999)).isNull();
  }

  @Test
  public void find_by_active_rule_id_with_inheritance() throws Exception {
    esSetup.client().prepareBulk()
      // Parent active rule
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("25").source(testFileAsString("find_active_rules_with_inheritance/active_rule25.json")))
        // Child active rule
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("759").source(testFileAsString("find_active_rules_with_inheritance/active_rule391.json")))
      .setRefresh(true)
      .execute().actionGet();

    QProfileRule child = profileRules.findByActiveRuleId(391);
    assertThat(child.inheritance()).isEqualTo("INHERITED");
    assertThat(child.activeRuleParentId()).isEqualTo(25);
  }

  @Test
  public void find_by_profile_id_and_rule_id() {
    // Rules on profiles
    QProfileRule rule = profileRules.findByProfileIdAndRuleId(1, 759);
    assertThat(rule).isNotNull();
    assertThat(rule.key()).isEqualTo("UnusedNullCheckInEquals");
    assertThat(rule.severity()).isEqualTo(Severity.MAJOR);

    // Rules on no profiles
    assertThat(profileRules.findByProfileIdAndRuleId(1, 944)).isNull();

    // Not existing rule
    assertThat(profileRules.findByProfileIdAndRuleId(1, 99999)).isNull();
  }

  @Test
  public void find_profile_rules() {
    Paging paging = Paging.create(10, 1);

    // All rules for profile 1
    List<QProfileRule> rules1 = profileRules.search(ProfileRuleQuery.create(1), paging).rules();
    assertThat(rules1).hasSize(3);
    assertThat(rules1.get(0).key()).isEqualTo("ArchitecturalConstraint");
    assertThat(rules1.get(0).severity()).isEqualTo(Severity.CRITICAL);

    // All rules for profile 2
    List<QProfileRule> rules2 = profileRules.search(ProfileRuleQuery.create(2), paging).rules();
    assertThat(rules2).hasSize(1);
    assertThat(rules2.get(0).id()).isEqualTo(759);
    assertThat(rules2.get(0).activeRuleId()).isEqualTo(523);

    // Match on key
    assertThat(profileRules.search(ProfileRuleQuery.create(1).setNameOrKey("DM_CONVERT_CASE"), paging).rules()).hasSize(1);

    // Match on name
    assertThat(profileRules.search(ProfileRuleQuery.create(1).setNameOrKey("Unused Check"), paging).rules()).hasSize(1);
    assertThat(profileRules.search(ProfileRuleQuery.create(1).setNameOrKey("unus"), paging).rules()).hasSize(1);

    // Match on repositoryKey
    assertThat(profileRules.search(ProfileRuleQuery.create(1).addRepositoryKeys("findbugs"), paging).rules()).hasSize(1);

    assertThat(profileRules.search(ProfileRuleQuery.create(1).addSeverities(Severity.CRITICAL), paging).rules()).hasSize(1);
    // Active rule 25 is in MINOR (rule 25 is in INFO)
    assertThat(profileRules.search(ProfileRuleQuery.create(1).addSeverities(Severity.INFO), paging).rules()).isEmpty();

    // Match on key, rule has parameters
    List<QProfileRule> rulesWParam = profileRules.search(ProfileRuleQuery.create(1).setNameOrKey("ArchitecturalConstraint"), paging).rules();
    assertThat(rulesWParam).hasSize(1);
    assertThat(rulesWParam.get(0).params()).hasSize(2);

    // Inexistent profile
    assertThat(profileRules.search(ProfileRuleQuery.create(3), paging).rules()).hasSize(0);

    // Inexistent name/key
    assertThat(profileRules.search(ProfileRuleQuery.create(1).setNameOrKey("polop"), paging).rules()).hasSize(0);
  }

  @Test
  public void find_profile_rules_with_inheritance() {
    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.search(ProfileRuleQuery.create(1), paging).rules();
    assertThat(rules).hasSize(3);

    rules = profileRules.search(ProfileRuleQuery.create(1).setAnyInheritance(true), paging).rules();
    assertThat(rules).hasSize(3);

    rules = profileRules.search(ProfileRuleQuery.create(1).setInheritance(QProfileRule.INHERITED), paging).rules();
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).activeRuleId()).isEqualTo(391);

    rules = profileRules.search(ProfileRuleQuery.create(1).setInheritance(QProfileRule.OVERRIDES), paging).rules();
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).activeRuleId()).isEqualTo(25);

    rules = profileRules.search(ProfileRuleQuery.create(1).setNoInheritance(true), paging).rules();
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).activeRuleId()).isEqualTo(2702);
  }

  @Test
  public void find_profile_rules_sorted_by_name() {
    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.search(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_RULE_NAME).setAsc(true), paging).rules();
    assertThat(rules).hasSize(3);
    assertThat(rules.get(0).name()).isEqualTo("Architectural constraint");
    assertThat(rules.get(1).name()).isEqualTo("Internationalization - Consider using Locale parameterized version of invoked method");
    assertThat(rules.get(2).name()).isEqualTo("Unused Null Check In Equals");

    rules = profileRules.search(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_RULE_NAME).setAsc(false), paging).rules();
    assertThat(rules).hasSize(3);
    assertThat(rules.get(0).name()).isEqualTo("Unused Null Check In Equals");
    assertThat(rules.get(1).name()).isEqualTo("Internationalization - Consider using Locale parameterized version of invoked method");
    assertThat(rules.get(2).name()).isEqualTo("Architectural constraint");
  }

  @Test
  public void find_profile_rules_sorted_by_creation_date() {
    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.search(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_CREATION_DATE).setAsc(true), paging).rules();
    assertThat(rules).hasSize(3);
    assertThat(rules.get(0).key()).isEqualTo("DM_CONVERT_CASE");
    assertThat(rules.get(1).key()).isEqualTo("UnusedNullCheckInEquals");
    assertThat(rules.get(2).key()).isEqualTo("ArchitecturalConstraint");

    rules = profileRules.search(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_CREATION_DATE).setAsc(false), paging).rules();
    assertThat(rules).hasSize(3);
    assertThat(rules.get(0).key()).isEqualTo("ArchitecturalConstraint");
    assertThat(rules.get(1).key()).isEqualTo("UnusedNullCheckInEquals");
    assertThat(rules.get(2).key()).isEqualTo("DM_CONVERT_CASE");
  }

  @Test
  public void find_profile_rules_with_paging() {
    List<QProfileRule> rules = profileRules.search(ProfileRuleQuery.create(1), Paging.create(2, 1)).rules();
    assertThat(rules).hasSize(2);
    assertThat(rules.get(0).key()).isEqualTo("ArchitecturalConstraint");
    assertThat(rules.get(1).key()).isEqualTo("DM_CONVERT_CASE");

    rules = profileRules.search(ProfileRuleQuery.create(1), Paging.create(2, 2)).rules();
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).key()).isEqualTo("UnusedNullCheckInEquals");
  }

  @Test
  public void find_profile_rule_ids() {
    // All active rules for profile 1
    List<Integer> result = profileRules.searchProfileRuleIds(ProfileRuleQuery.create(1));
    assertThat(result).hasSize(3);
    assertThat(result).containsOnly(25, 391, 2702);

    assertThat(profileRules.searchProfileRuleIds(ProfileRuleQuery.create(1).addSeverities(Severity.CRITICAL))).hasSize(1);
    assertThat(profileRules.searchProfileRuleIds(ProfileRuleQuery.create(1).addSeverities(Severity.INFO))).isEmpty();
  }

  @Test
  public void find_profile_rule_ids_overriding_page_size() {
    List<Integer> result = profileRules.searchProfileRuleIds(ProfileRuleQuery.create(1), 1);
    assertThat(result).hasSize(3);
  }

  @Test
  public void find_parent_active_rule() throws Exception {
    QProfileRule rule = profileRules.findByActiveRuleId(391);
    QProfileRule parent = profileRules.findParentProfileRule(rule);
    assertThat(parent.id()).isEqualTo(25);
  }

  @Test
  public void find_parent_active_rule_return_null_when_no_parent() throws Exception {
    QProfileRule rule = profileRules.findByActiveRuleId(25);
    assertThat(profileRules.findParentProfileRule(rule)).isNull();
  }

  @Test
  public void count_profile_rules() {
    // All rules for profile 1
    assertThat(profileRules.countProfileRules(ProfileRuleQuery.create(1))).isEqualTo(3);

    // All rules for profile 2
    assertThat(profileRules.countProfileRules(ProfileRuleQuery.create(2))).isEqualTo(1);

    // Match on key
    assertThat(profileRules.countProfileRules(ProfileRuleQuery.create(1).setNameOrKey("DM_CONVERT_CASE"))).isEqualTo(1);
  }

  @Test
  public void count_profile_rules_by_rule_id() {
    assertThat(profileRules.countProfileRules(759)).isEqualTo(2);
    assertThat(profileRules.countProfileRules(25)).isEqualTo(1);
  }

  @Test
  public void find_inactive_profile_rules() {
    Paging paging = Paging.create(10, 1);

    // Search of inactive rule on profile 2
    assertThat(profileRules.searchInactives(ProfileRuleQuery.create(2), paging).rules()).hasSize(4);

    // Search of inactive rule on profile 1
    assertThat(profileRules.searchInactives(ProfileRuleQuery.create(1), paging).rules()).hasSize(2);

    // Match on key
    assertThat(profileRules.searchInactives(ProfileRuleQuery.create(2).setNameOrKey("Boolean expressions"), paging).rules()).hasSize(1);

    // Mach on severity
    assertThat(profileRules.searchInactives(ProfileRuleQuery.create(2).addSeverities(Severity.INFO), paging).rules()).hasSize(1);
  }

  @Test
  public void find_rules_with_tags() throws Exception {
    esSetup.client().prepareBulk()
      // id = 2303, tags = empty
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("find_inactive_rules_with_tags/tags_empty.json")))
      // active rule with parent having no tag
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("2303").source(testFileAsString("find_inactive_rules_with_tags/active_rule_empty.json")))
      // id = 2304, tags = taga
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("find_inactive_rules_with_tags/tags_a.json")))
      // id = 2305, tags = taga, tagb
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("find_inactive_rules_with_tags/tags_ab.json")))
      // id = 2306, tags = tagb, tagc
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("find_inactive_rules_with_tags/tags_bc.json")))
      // id = 2307, tags = taga, tagc, tage
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("find_inactive_rules_with_tags/tags_ace.json")))
      // active rule with parent having tags
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("2307").source(testFileAsString("find_inactive_rules_with_tags/active_rule_ace.json")))
      .setRefresh(true)
      .execute().actionGet();

    Paging paging = Paging.create(10, 1);

    // all rules
    List<QProfileRule> rules = profileRules.searchInactives(ProfileRuleQuery.create(1).setLanguage("xoo"), paging).rules();
    assertThat(rules).hasSize(5);

    // check mapping is correctly loaded
    assertThat(rules.get(0).systemTags()).containsOnly("taga");
    assertThat(rules.get(0).adminTags()).isEmpty();
    assertThat(rules.get(1).systemTags()).containsOnly("tagb");
    assertThat(rules.get(1).adminTags()).containsOnly("taga");
    assertThat(rules.get(2).systemTags()).containsOnly("taga", "tagc");
    assertThat(rules.get(2).adminTags()).containsOnly("tage");
    assertThat(rules.get(3).systemTags()).containsOnly("tagb", "tagc");
    assertThat(rules.get(3).adminTags()).isEmpty();
    assertThat(rules.get(4).systemTags()).isEmpty();
    assertThat(rules.get(4).adminTags()).isEmpty();

    // check search for inactive rules
    assertThat(profileRules.searchInactiveProfileRuleIds(ProfileRuleQuery.create(2).setLanguage("xoo").addTags("taga")))
      .hasSize(2).containsOnly(2304, 2305);
    assertThat(profileRules.searchInactiveProfileRuleIds(ProfileRuleQuery.create(2).setLanguage("xoo").addTags("taga", "tagb")))
      .hasSize(1).containsOnly(2305);

    // check search for active rules
    assertThat(profileRules.searchProfileRuleIds(ProfileRuleQuery.create(2).setLanguage("xoo").addTags("taga")))
      .hasSize(1).containsOnly(2307);
    assertThat(profileRules.searchProfileRuleIds(ProfileRuleQuery.create(2).setLanguage("xoo").addTags("tagb")))
      .isEmpty();
  }

  @Test
  public void find_inactive_profile_rules_sorted_by_name() {
    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.searchInactives(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_RULE_NAME).setAsc(true), paging).rules();
    assertThat(rules).hasSize(2);
    assertThat(rules.get(0).name()).isEqualTo("Boolean expressions should not be compared to true or false");
    assertThat(rules.get(1).name()).isEqualTo("Double Checked Locking");

    rules = profileRules.searchInactives(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_RULE_NAME).setAsc(false), paging).rules();
    assertThat(rules).hasSize(2);
    assertThat(rules.get(0).name()).isEqualTo("Double Checked Locking");
    assertThat(rules.get(1).name()).isEqualTo("Boolean expressions should not be compared to true or false");
  }

  @Test
  public void find_inactive_rules_sorted_ignoring_case() throws Exception {
    esSetup.client().prepareBulk()
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("find_inactive_rules_sorted_ignoring_case/rule_A.json")))
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("find_inactive_rules_sorted_ignoring_case/rule_b.json")))
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("find_inactive_rules_sorted_ignoring_case/rule_C.json")))
      .setRefresh(true)
      .execute().actionGet();

    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.searchInactives(ProfileRuleQuery.create(1).setLanguage("xoo").setSort(ProfileRuleQuery.SORT_BY_RULE_NAME).setAsc(true), paging).rules();
    assertThat(rules).hasSize(3);
    assertThat(rules.get(0).name()).isEqualTo("A first rule");
    assertThat(rules.get(1).name()).isEqualTo("b second rule");
    assertThat(rules.get(2).name()).isEqualTo("C third rule");
  }

  @Test
  public void find_inactive_profile_rules_sorted_by_creation_date() {
    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.searchInactives(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_CREATION_DATE).setAsc(true), paging).rules();
    assertThat(rules).hasSize(2);
    assertThat(rules.get(0).key()).isEqualTo("com.puppycrawl.tools.checkstyle.checks.coding.DoubleCheckedLockingCheck");
    assertThat(rules.get(1).key()).isEqualTo("S1125");

    rules = profileRules.searchInactives(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_CREATION_DATE).setAsc(false), paging).rules();
    assertThat(rules).hasSize(2);
    assertThat(rules.get(0).key()).isEqualTo("S1125");
    assertThat(rules.get(1).key()).isEqualTo("com.puppycrawl.tools.checkstyle.checks.coding.DoubleCheckedLockingCheck");
  }

  @Test
  public void find_inactive_profile_rule_ids() {
    // Search of inactive rule on profile 2
    assertThat(profileRules.searchInactiveProfileRuleIds(ProfileRuleQuery.create(2))).hasSize(4);

    // Search of inactive rule on profile 1
    assertThat(profileRules.searchInactiveProfileRuleIds(ProfileRuleQuery.create(1))).hasSize(2);

    // Match on key
    assertThat(profileRules.searchInactiveProfileRuleIds(ProfileRuleQuery.create(2).setNameOrKey("Boolean expressions"))).hasSize(1);

    // Mach on severity
    assertThat(profileRules.searchInactiveProfileRuleIds(ProfileRuleQuery.create(2).addSeverities(Severity.INFO))).hasSize(1);
  }

  @Test
  public void count_inactive_profile_rules() {
    // All rules for profile 1
    assertThat(profileRules.countInactiveProfileRules(ProfileRuleQuery.create(2))).isEqualTo(4);

    // All rules for profile 2
    assertThat(profileRules.countInactiveProfileRules(ProfileRuleQuery.create(1))).isEqualTo(2);

    // Match on key
    assertThat(profileRules.countInactiveProfileRules(ProfileRuleQuery.create(2).setNameOrKey("Boolean expressions"))).isEqualTo(1);

    // Mach on severity
    assertThat(profileRules.countInactiveProfileRules(ProfileRuleQuery.create(2).addSeverities(Severity.INFO))).isEqualTo(1);
  }

  @Test
  public void get_from_active_rule() {
    assertThat(profileRules.findByActiveRuleId(391)).isNotNull();
  }

  @Test
  public void get_from_rule() {
    assertThat(profileRules.findByActiveRuleId(25)).isNotNull();
  }

  private String testFileAsString(String testFile) throws Exception {
    return IOUtils.toString(TestUtils.getResource(getClass(), testFile).toURI());
  }
}
