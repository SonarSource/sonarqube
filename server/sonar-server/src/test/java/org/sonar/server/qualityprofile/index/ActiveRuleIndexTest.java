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
package org.sonar.server.qualityprofile.index;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.rule.index.RuleDoc;
import org.sonar.server.rule.index.RuleDocTesting;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.search.FacetValue;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.server.qualityprofile.ActiveRule.Inheritance.INHERITED;
import static org.sonar.server.qualityprofile.ActiveRule.Inheritance.OVERRIDES;
import static org.sonar.server.rule.index.RuleDocTesting.newDoc;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;

public class ActiveRuleIndexTest {

  private static final RuleKey RULE_KEY_1 = RuleTesting.XOO_X1;
  private static final RuleKey RULE_KEY_2 = RuleTesting.XOO_X2;
  private static final String QUALITY_PROFILE_KEY1 = "qp1";
  private static final String QUALITY_PROFILE_KEY2 = "qp2";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester tester = new EsTester(new RuleIndexDefinition(new MapSettings()));

  ActiveRuleIndex index;
  ActiveRuleIndexer activeRuleIndexer;
  RuleIndexer ruleIndexer;

  @Before
  public void setUp() {
    activeRuleIndexer = new ActiveRuleIndexer(system2, null, tester.client());
    ruleIndexer = new RuleIndexer(system2, null, tester.client());
    index = new ActiveRuleIndex(tester.client());
  }

  @Test
  public void count_all_by_quality_profile_key() {
    indexRules(RuleDocTesting.newDoc(RULE_KEY_1));

    indexActiveRules(
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, RULE_KEY_1)),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_1)));

    // 0. Test base case
    assertThat(tester.countDocuments(INDEX, TYPE_ACTIVE_RULE)).isEqualTo(2);

    // 1. Assert by term aggregation;
    assertThat(index.countAllByQualityProfileKey()).containsOnly(entry(QUALITY_PROFILE_KEY1, 1L), entry(QUALITY_PROFILE_KEY2, 1L));
  }

  @Test
  public void stats_for_all() {
    indexRules(
      newDoc(RULE_KEY_1),
      newDoc(RULE_KEY_2));

    ActiveRuleKey activeRuleKey1 = ActiveRuleKey.of(QUALITY_PROFILE_KEY1, RULE_KEY_1);
    ActiveRuleKey activeRuleKey2 = ActiveRuleKey.of(QUALITY_PROFILE_KEY1, RULE_KEY_2);

    indexActiveRules(
      ActiveRuleDocTesting.newDoc(activeRuleKey1).setSeverity(BLOCKER),
      ActiveRuleDocTesting.newDoc(activeRuleKey2).setSeverity(MINOR),
      // Profile 2 is a child a profile 1
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_1)).setSeverity(MAJOR).setInheritance(INHERITED.name()),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_2)).setSeverity(BLOCKER).setInheritance(OVERRIDES.name()));

    // 0. Test base case
    assertThat(tester.countDocuments(INDEX, TYPE_ACTIVE_RULE)).isEqualTo(4);

    // 1. Assert by term aggregation;
    Map<String, Multimap<String, FacetValue>> stats = index.getStatsByProfileKeys(ImmutableList.of(QUALITY_PROFILE_KEY1, QUALITY_PROFILE_KEY2));
    assertThat(stats).hasSize(2);
  }

  /**
   * SONAR-5844
   */
  @Test
  public void stats_for_all_with_lof_of_profiles() {
    indexRules(RuleDocTesting.newDoc(RULE_KEY_1), RuleDocTesting.newDoc(RULE_KEY_2));

    indexActiveRules(
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, RULE_KEY_1)),
      ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_1)));

    List<String> profileKeys = new ArrayList<>();
    List<ActiveRuleDoc> docs = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      String profileKey = "profile-" + i;
      profileKeys.add(profileKey);
      docs.add(ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(profileKey, RULE_KEY_1)).setSeverity(BLOCKER));
      docs.add(ActiveRuleDocTesting.newDoc(ActiveRuleKey.of(profileKey, RULE_KEY_2)).setSeverity(MAJOR));
    }
    indexActiveRules(docs.toArray(new ActiveRuleDoc[] {}));

    Map<String, Multimap<String, FacetValue>> stats = index.getStatsByProfileKeys(profileKeys);
    assertThat(stats).hasSize(30);
  }

  private void indexActiveRules(ActiveRuleDoc... docs) {
    activeRuleIndexer.index(asList(docs).iterator());
  }

  private void indexRules(RuleDoc... rules) {
    ruleIndexer.index(asList(rules).iterator());
  }

}
