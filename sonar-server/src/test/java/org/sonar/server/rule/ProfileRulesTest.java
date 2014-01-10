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
package org.sonar.server.rule;

import com.github.tlrx.elasticsearch.test.EsSetup;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.client.Requests;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.Severity;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.qualityprofile.Paging;
import org.sonar.server.qualityprofile.QProfileRule;
import org.sonar.server.search.SearchIndex;
import org.sonar.server.search.SearchNode;
import org.sonar.test.TestUtils;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfileRulesTest {

  private ProfileRules profileRules;
  private EsSetup esSetup;

  @Before
  public void setUp() throws Exception {
    esSetup = new EsSetup();
    esSetup.execute(EsSetup.deleteAll());

    SearchNode searchNode = mock(SearchNode.class);
    when(searchNode.client()).thenReturn(esSetup.client());

    Settings settings = new Settings();
    settings.setProperty("sonar.log.profilingLevel", "FULL");
    SearchIndex index = new SearchIndex(searchNode, new Profiling(settings));
    index.start();
    RuleRegistry registry = new RuleRegistry(index, null, null, null);
    registry.start();
    profileRules = new ProfileRules(index);

    esSetup.client().prepareBulk()
        // On profile 1
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("should_find_active_rules/rule25.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("25").source(testFileAsString("should_find_active_rules/active_rule25.json")))
        // On profile 1 and 2
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("should_find_active_rules/rule759.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("759").source(testFileAsString("should_find_active_rules/active_rule391.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("759").source(testFileAsString("should_find_active_rules/active_rule523.json")))
        // On profile 1
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("should_find_active_rules/rule1482.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("1482").source(testFileAsString("should_find_active_rules/active_rule2702.json")))
        // Rules on no profile
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("should_find_active_rules/rule944.json")))
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("should_find_active_rules/rule719.json")))
      // Removed rule
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("should_find_active_rules/rule860.json")))
      .setRefresh(true).execute().actionGet();
  }

  @After
  public void tearDown() {
    esSetup.terminate();
  }

  @Test
  public void find_profile_rules() {
    Paging paging = Paging.create(10, 1);

    // All rules for profile 1
    List<QProfileRule> rules1 = profileRules.searchProfileRules(ProfileRuleQuery.create(1), paging).rules();
    assertThat(rules1).hasSize(3);
    assertThat(rules1.get(0).key()).isEqualTo("ArchitecturalConstraint");
    assertThat(rules1.get(0).severity()).isEqualTo(Severity.CRITICAL);

    // All rules for profile 2
    List<QProfileRule> rules2 = profileRules.searchProfileRules(ProfileRuleQuery.create(2), paging).rules();
    assertThat(rules2).hasSize(1);
    assertThat(rules2.get(0).id()).isEqualTo(759);
    assertThat(rules2.get(0).activeRuleId()).isEqualTo(523);

    // Inexistent profile
    assertThat(profileRules.searchProfileRules(ProfileRuleQuery.create(3), paging).rules()).hasSize(0);

    // Inexistent name/key
    assertThat(profileRules.searchProfileRules(ProfileRuleQuery.create(1).setNameOrKey("polop"), paging).rules()).hasSize(0);

    // Match on key
    assertThat(profileRules.searchProfileRules(ProfileRuleQuery.create(1).setNameOrKey("DM_CONVERT_CASE"), paging).rules()).hasSize(1);

    // Match on name
    assertThat(profileRules.searchProfileRules(ProfileRuleQuery.create(1).setNameOrKey("Unused Check"), paging).rules()).hasSize(1);

    // Match on repositoryKey
    assertThat(profileRules.searchProfileRules(ProfileRuleQuery.create(1).addRepositoryKeys("findbugs"), paging).rules()).hasSize(1);

    // Match on key, rule has parameters
    List<QProfileRule> rulesWParam = profileRules.searchProfileRules(ProfileRuleQuery.create(1).setNameOrKey("ArchitecturalConstraint"), paging).rules();
    assertThat(rulesWParam).hasSize(1);
    assertThat(rulesWParam.get(0).params()).hasSize(2);
  }

  @Test
  public void find_profile_rules_with_inheritance() {
    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.searchProfileRules(ProfileRuleQuery.create(1), paging).rules();
    assertThat(rules).hasSize(3);

    rules = profileRules.searchProfileRules(ProfileRuleQuery.create(1).setAnyInheritance(true), paging).rules();
    assertThat(rules).hasSize(3);

    rules = profileRules.searchProfileRules(ProfileRuleQuery.create(1).setInheritance(QProfileRule.INHERITED), paging).rules();
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).activeRuleId()).isEqualTo(391);

    rules = profileRules.searchProfileRules(ProfileRuleQuery.create(1).setInheritance(QProfileRule.OVERRIDES), paging).rules();
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).activeRuleId()).isEqualTo(25);

    rules = profileRules.searchProfileRules(ProfileRuleQuery.create(1).setNoInheritance(true), paging).rules();
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).activeRuleId()).isEqualTo(2702);
  }

  @Test
  public void find_profile_rules_sorted_by_name() {
    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.searchProfileRules(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_RULE_NAME).setAsc(true), paging).rules();
    assertThat(rules).hasSize(3);
    assertThat(rules.get(0).name()).isEqualTo("Architectural constraint");
    assertThat(rules.get(1).name()).isEqualTo("Internationalization - Consider using Locale parameterized version of invoked method");
    assertThat(rules.get(2).name()).isEqualTo("Unused Null Check In Equals");

    rules = profileRules.searchProfileRules(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_RULE_NAME).setAsc(false), paging).rules();
    assertThat(rules).hasSize(3);
    assertThat(rules.get(0).name()).isEqualTo("Unused Null Check In Equals");
    assertThat(rules.get(1).name()).isEqualTo("Internationalization - Consider using Locale parameterized version of invoked method");
    assertThat(rules.get(2).name()).isEqualTo("Architectural constraint");
  }

  @Test
  public void find_profile_rules_sorted_by_creation_date() {
    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.searchProfileRules(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_CREATION_DATE).setAsc(true), paging).rules();
    assertThat(rules).hasSize(3);
    assertThat(rules.get(0).key()).isEqualTo("DM_CONVERT_CASE");
    assertThat(rules.get(1).key()).isEqualTo("UnusedNullCheckInEquals");
    assertThat(rules.get(2).key()).isEqualTo("ArchitecturalConstraint");

    rules = profileRules.searchProfileRules(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_CREATION_DATE).setAsc(false), paging).rules();
    assertThat(rules).hasSize(3);
    assertThat(rules.get(0).key()).isEqualTo("ArchitecturalConstraint");
    assertThat(rules.get(1).key()).isEqualTo("UnusedNullCheckInEquals");
    assertThat(rules.get(2).key()).isEqualTo("DM_CONVERT_CASE");
  }

  @Test
  @Ignore("bug in E/S : fail to do a scroll when filter contain has_parent -> return good total_hits but hits is empty")
  public void find_profile_rule_ids() {
    // All rules for profile 1
    List<Integer> result = profileRules.searchProfileRuleIds(ProfileRuleQuery.create(1));
    assertThat(result).hasSize(3);
    assertThat(result.get(0)).isEqualTo(1);
  }

  @Test
  public void find_inactive_profile_rules() {
    Paging paging = Paging.create(10, 1);

    // Search of inactive rule on profile 2
    assertThat(profileRules.searchInactiveProfileRules(ProfileRuleQuery.create(2), paging).rules()).hasSize(4);

    // Search of inactive rule on profile 1
    assertThat(profileRules.searchInactiveProfileRules(ProfileRuleQuery.create(1), paging).rules()).hasSize(2);

    // Match on key
    assertThat(profileRules.searchInactiveProfileRules(ProfileRuleQuery.create(2).setNameOrKey("Boolean expressions"), paging).rules()).hasSize(1);
  }

  @Test
  public void find_inactive_profile_rules_sorted_by_name() {
    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.searchInactiveProfileRules(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_RULE_NAME).setAsc(true), paging).rules();
    assertThat(rules).hasSize(2);
    assertThat(rules.get(0).name()).isEqualTo("Boolean expressions should not be compared to true or false");
    assertThat(rules.get(1).name()).isEqualTo("Double Checked Locking");

    rules = profileRules.searchInactiveProfileRules(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_RULE_NAME).setAsc(false), paging).rules();
    assertThat(rules).hasSize(2);
    assertThat(rules.get(0).name()).isEqualTo("Double Checked Locking");
    assertThat(rules.get(1).name()).isEqualTo("Boolean expressions should not be compared to true or false");
  }

  @Test
  public void find_inactive_profile_rules_sorted_by_creation_date() {
    Paging paging = Paging.create(10, 1);

    List<QProfileRule> rules = profileRules.searchInactiveProfileRules(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_CREATION_DATE).setAsc(true), paging).rules();
    assertThat(rules).hasSize(2);
    assertThat(rules.get(0).key()).isEqualTo("com.puppycrawl.tools.checkstyle.checks.coding.DoubleCheckedLockingCheck");
    assertThat(rules.get(1).key()).isEqualTo("S1125");

    rules = profileRules.searchInactiveProfileRules(ProfileRuleQuery.create(1).setSort(ProfileRuleQuery.SORT_BY_CREATION_DATE).setAsc(false), paging).rules();
    assertThat(rules).hasSize(2);
    assertThat(rules.get(0).key()).isEqualTo("S1125");
    assertThat(rules.get(1).key()).isEqualTo("com.puppycrawl.tools.checkstyle.checks.coding.DoubleCheckedLockingCheck");
  }

  @Test
  public void get_from_active_rule() {
    assertThat(profileRules.getFromActiveRuleId(391)).isNotNull();
  }

  @Test
  public void get_from_rule() {
    assertThat(profileRules.getFromActiveRuleId(25)).isNotNull();
  }

  private String testFileAsString(String testFile) throws Exception {
    return IOUtils.toString(TestUtils.getResource(getClass(), testFile).toURI());
  }
}
