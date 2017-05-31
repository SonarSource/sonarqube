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
package org.sonar.server.qualityprofile.index;

import com.google.common.collect.Iterators;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.rule.index.RuleIndexDefinition;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.DEACTIVATED;
import static org.sonar.server.qualityprofile.index.ActiveRuleDocTesting.newDoc;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE;

public class ActiveRuleIndexerTest {

  private static final RuleKey RULE_KEY_1 = RuleTesting.XOO_X1;
  private static final RuleKey RULE_KEY_2 = RuleTesting.XOO_X2;
  private static final RuleKey RULE_KEY_3 = RuleTesting.XOO_X3;
  private static final String QUALITY_PROFILE_KEY1 = "qp1";
  private static final String QUALITY_PROFILE_KEY2 = "qp2";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester esTester = new EsTester(new RuleIndexDefinition(new MapSettings()));

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ActiveRuleIndexer underTest = new ActiveRuleIndexer(system2, db.getDbClient(), esTester.client());

  @Test
  public void index_nothing() {
    underTest.index(Iterators.emptyIterator());
    assertThat(esTester.countDocuments(INDEX_TYPE_ACTIVE_RULE)).isZero();
  }

  @Test
  public void test_index() {
    RuleDefinitionDto rule = db.rules().insert();
    OrganizationDto org = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(org);
    db.qualityProfiles().activateRule(profile, rule);

    underTest.index();

    assertThat(esTester.countDocuments(INDEX_TYPE_ACTIVE_RULE)).isEqualTo(1);
  }

  @Test
  public void deleteByProfileKeys_deletes_documents_associated_to_specified_profile() throws Exception {
    indexActiveRules(
      newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, RULE_KEY_1)),
      newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, RULE_KEY_2)),
      newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_2)),
      newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_3)));
    assertThat(esTester.getIds(INDEX_TYPE_ACTIVE_RULE)).hasSize(4);

    underTest.deleteByProfileKeys(asList(QUALITY_PROFILE_KEY1));

    assertThat(esTester.getIds(INDEX_TYPE_ACTIVE_RULE)).containsOnly(
      ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_2).toString(),
      ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_3).toString());
  }

  @Test
  public void deleteByProfileKeys_deletes_documents_associated_to_multiple_profiles() throws Exception {
    indexActiveRules(
      newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, RULE_KEY_1)),
      newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY1, RULE_KEY_2)),
      newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_2)),
      newDoc(ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_3)));
    assertThat(esTester.getIds(INDEX_TYPE_ACTIVE_RULE)).hasSize(4);

    underTest.deleteByProfileKeys(asList(QUALITY_PROFILE_KEY1, QUALITY_PROFILE_KEY2));

    assertThat(esTester.getIds(INDEX_TYPE_ACTIVE_RULE)).isEmpty();
  }

  @Test
  public void index_from_changes_remove_deactivated_rules() throws Exception {
    ActiveRuleKey activeRuleKey1 = ActiveRuleKey.of(QUALITY_PROFILE_KEY1, RULE_KEY_1);
    ActiveRuleKey activeRuleKey2 = ActiveRuleKey.of(QUALITY_PROFILE_KEY1, RULE_KEY_2);
    ActiveRuleKey activeRuleKey3 = ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_2);
    ActiveRuleKey activeRuleKey4 = ActiveRuleKey.of(QUALITY_PROFILE_KEY2, RULE_KEY_3);

    indexActiveRules(
      newDoc(activeRuleKey1),
      newDoc(activeRuleKey2),
      newDoc(activeRuleKey3),
      newDoc(activeRuleKey4));

    assertThat(esTester.getIds(INDEX_TYPE_ACTIVE_RULE)).hasSize(4);

    underTest.index(asList(
      ActiveRuleChange.createFor(ACTIVATED, activeRuleKey1),
      ActiveRuleChange.createFor(DEACTIVATED, activeRuleKey2),
      ActiveRuleChange.createFor(DEACTIVATED, activeRuleKey3)));

    assertThat(esTester.getIds(INDEX_TYPE_ACTIVE_RULE)).containsOnly(
      activeRuleKey1.toString(),
      activeRuleKey4.toString());
  }

  @Test
  public void index_from_changes_index_new_active_rule() throws Exception {
    long yesterday = 1_000;
    long now = 2_000L;
    RuleDefinitionDto rule = db.rules().insert();
    OrganizationDto org = db.organizations().insert();
    QProfileDto profile1 = db.qualityProfiles().insert(org);
    ActiveRuleDto activeRule1 = db.qualityProfiles().activateRule(profile1, rule, ar -> ar.setUpdatedAt(yesterday));

    underTest.index();

    assertThat(esTester.getIds(INDEX_TYPE_ACTIVE_RULE)).containsOnly(activeRule1.getKey().toString());

    // Index another active rule
    QProfileDto profile2 = db.qualityProfiles().insert(org);
    ActiveRuleDto activeRule2 = db.qualityProfiles().activateRule(profile2, rule, ar -> ar.setUpdatedAt(now));

    underTest.index(singletonList(ActiveRuleChange.createFor(ACTIVATED, activeRule2.getKey())));

    assertThat(esTester.getIds(INDEX_TYPE_ACTIVE_RULE)).containsOnly(
      activeRule1.getKey().toString(),
      activeRule2.getKey().toString());
  }

  private void indexActiveRules(ActiveRuleDoc... docs) {
    underTest.index(asList(docs).iterator());
  }

}
