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

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.rule.index.RuleIndexDefinition;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.DEACTIVATED;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE;

public class ActiveRuleIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = new EsTester(RuleIndexDefinition.createForTest(new MapSettings()));

  private ActiveRuleIndexer underTest = new ActiveRuleIndexer(db.getDbClient(), es.client(), new ActiveRuleIteratorFactory(db.getDbClient()));
  private RuleDefinitionDto rule1;
  private RuleDefinitionDto rule2;
  private OrganizationDto org;
  private QProfileDto profile1;
  private QProfileDto profile2;

  @Before
  public void before() {
    rule1 = db.rules().insert();
    rule2 = db.rules().insert();
    org = db.organizations().insert();
    profile1 = db.qualityProfiles().insert(org);
    profile2 = db.qualityProfiles().insert(org);
  }

  @Test
  public void getIndexTypes() {
    assertThat(underTest.getIndexTypes()).containsExactly(RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE);
  }

  @Test
  public void indexOnStartup_does_nothing_if_no_data() {
    underTest.indexOnStartup(emptySet());
    assertThat(es.countDocuments(INDEX_TYPE_ACTIVE_RULE)).isZero();
  }

  @Test
  public void indexOnStartup_indexes_all_data() {
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(profile1, rule1);

    underTest.indexOnStartup(emptySet());

    List<ActiveRuleDoc> docs = es.getDocuments(INDEX_TYPE_ACTIVE_RULE, ActiveRuleDoc.class);
    assertThat(docs).hasSize(1);
    verify(docs.get(0), rule1, profile1, activeRule);
  }

  @Test
  public void deleteByProfiles() throws Exception {
    ActiveRuleDto activeRule1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto activeRule2 = db.qualityProfiles().activateRule(profile2, rule1);
    ActiveRuleDto activeRule3 = db.qualityProfiles().activateRule(profile2, rule2);
    index();

    underTest.deleteByProfiles(singletonList(profile2));

    verifyOnlyIndexed(activeRule1);
  }

  @Test
  public void deleteByProfiles_does_nothing_if_profiles_are_not_indexed() throws Exception {
    ActiveRuleDto activeRule1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto activeRule2 = db.qualityProfiles().activateRule(profile2, rule1);
    ActiveRuleDto activeRule3 = db.qualityProfiles().activateRule(profile2, rule2);
    assertThat(es.countDocuments(INDEX_TYPE_ACTIVE_RULE)).isEqualTo(0);

    underTest.deleteByProfiles(singletonList(profile2));

    assertThat(es.countDocuments(INDEX_TYPE_ACTIVE_RULE)).isEqualTo(0);
  }

  @Test
  public void indexRuleProfile() throws Exception {
    ActiveRuleDto activeRule1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto activeRule2 = db.qualityProfiles().activateRule(profile2, rule1);
    ActiveRuleDto activeRule3 = db.qualityProfiles().activateRule(profile2, rule2);

    indexProfile(profile2);

    verifyOnlyIndexed(activeRule2, activeRule3);
  }

  @Test
  public void indexChanges_puts_documents() throws Exception {
    ActiveRuleDto activeRule1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto activeRule2 = db.qualityProfiles().activateRule(profile2, rule1);
    ActiveRuleDto nonIndexed = db.qualityProfiles().activateRule(profile2, rule2);

    underTest.indexChanges(db.getSession(), asList(
      newChange(ACTIVATED, activeRule1), newChange(ACTIVATED, activeRule2)));

    verifyOnlyIndexed(activeRule1, activeRule2);
  }

  @Test
  public void indexChanges_deletes_documents_when_type_is_DEACTIVATED() throws Exception {
    ActiveRuleDto activeRule1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto activeRule2 = db.qualityProfiles().activateRule(profile2, rule1);
    underTest.indexChanges(db.getSession(), asList(
      newChange(ACTIVATED, activeRule1), newChange(ACTIVATED, activeRule2)));
    assertThat(es.countDocuments(RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE)).isEqualTo(2);

    underTest.indexChanges(db.getSession(), singletonList(newChange(DEACTIVATED, activeRule1)));

    verifyOnlyIndexed(activeRule2);
  }

  @Test
  public void deleteByRuleKeys() {
    ActiveRuleDto active1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto active2 = db.qualityProfiles().activateRule(profile2, rule1);
    ActiveRuleDto onRule2 = db.qualityProfiles().activateRule(profile2, rule2);
    index();

    underTest.deleteByRuleKeys(singletonList(rule2.getKey()));

    verifyOnlyIndexed(active1, active2);
  }

  private void verifyOnlyIndexed(ActiveRuleDto... expected) {
    List<String> docs = es.getIds(INDEX_TYPE_ACTIVE_RULE);
    assertThat(docs).hasSize(expected.length);
    for (ActiveRuleDto activeRuleDto : expected) {
      assertThat(docs).contains(activeRuleDto.getId().toString());
    }
  }

  private ActiveRuleChange newChange(ActiveRuleChange.Type type, ActiveRuleDto activeRule) {
    return new ActiveRuleChange(type, activeRule);
  }

  private void indexProfile(QProfileDto profile) {
    underTest.indexRuleProfile(db.getSession(), RulesProfileDto.from(profile));
  }

  private void verify(ActiveRuleDoc doc1, RuleDefinitionDto rule, QProfileDto profile, ActiveRuleDto activeRule) {
    assertThat(doc1)
      .matches(doc -> doc.getRuleKey().equals(rule.getKey()))
      .matches(doc -> doc.getId().equals("" + activeRule.getId()))
      .matches(doc -> doc.getRuleProfileUuid().equals(profile.getRulesProfileUuid()))
      .matches(doc -> doc.getRuleRepository().equals(rule.getRepositoryKey()))
      .matches(doc -> doc.getSeverity().equals(activeRule.getSeverityString()));
  }

  private void index() {
    underTest.indexOnStartup(emptySet());
  }

}
