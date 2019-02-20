/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.qualityprofile.ActiveRuleChange;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;

public class ActiveRuleIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = EsTester.create();

  private ActiveRuleIndexer underTest = new ActiveRuleIndexer(db.getDbClient(), es.client());
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
    assertThat(underTest.getIndexTypes()).containsExactly(TYPE_ACTIVE_RULE);
  }

  @Test
  public void indexOnStartup_does_nothing_if_no_data() {
    underTest.indexOnStartup(emptySet());
    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isZero();
  }

  @Test
  public void indexOnStartup_indexes_all_data() {
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(profile1, rule1);

    underTest.indexOnStartup(emptySet());

    List<ActiveRuleDoc> docs = es.getDocuments(TYPE_ACTIVE_RULE, ActiveRuleDoc.class);
    assertThat(docs).hasSize(1);
    verify(docs.get(0), profile1, activeRule);
    assertThatEsQueueTableIsEmpty();
  }

  @Test
  public void test_commitAndIndex() {
    ActiveRuleDto ar1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto ar2 = db.qualityProfiles().activateRule(profile2, rule1);
    ActiveRuleDto ar3 = db.qualityProfiles().activateRule(profile2, rule2);

    commitAndIndex(rule1, ar1, ar2);

    verifyOnlyIndexed(ar1, ar2);
    assertThatEsQueueTableIsEmpty();
  }

  @Test
  public void commitAndIndex_empty_list() {
    ActiveRuleDto ar = db.qualityProfiles().activateRule(profile1, rule1);

    underTest.commitAndIndex(db.getSession(), Collections.emptyList());

    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isEqualTo(0);
    assertThatEsQueueTableIsEmpty();
  }

  @Test
  public void commitAndIndex_keeps_elements_to_recover_in_ES_QUEUE_on_errors() {
    ActiveRuleDto ar = db.qualityProfiles().activateRule(profile1, rule1);
    es.lockWrites(TYPE_ACTIVE_RULE);

    commitAndIndex(rule1, ar);

    EsQueueDto expectedItem = EsQueueDto.create(TYPE_ACTIVE_RULE.format(), "ar_" + ar.getId(), "activeRuleId", valueOf(ar.getRuleId()));
    assertThatEsQueueContainsExactly(expectedItem);
    es.unlockWrites(TYPE_ACTIVE_RULE);
  }

  @Test
  public void commitAndIndex_deletes_the_documents_that_dont_exist_in_database() {
    ActiveRuleDto ar = db.qualityProfiles().activateRule(profile1, rule1);
    indexAll();
    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isEqualTo(1);

    db.getDbClient().activeRuleDao().delete(db.getSession(), ar.getKey());
    commitAndIndex(rule1, ar);

    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isEqualTo(0);
    assertThatEsQueueTableIsEmpty();
  }

  @Test
  public void index_fails_and_deletes_doc_if_docIdType_is_unsupported() {
    EsQueueDto item = EsQueueDto.create(TYPE_ACTIVE_RULE.format(), "the_id", "unsupported", "the_routing");
    db.getDbClient().esQueueDao().insert(db.getSession(), item);

    underTest.index(db.getSession(), asList(item));

    assertThatEsQueueTableIsEmpty();
    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isEqualTo(0);
  }

  @Test
  public void commitDeletionOfProfiles() {
    ActiveRuleDto ar1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto ar2 = db.qualityProfiles().activateRule(profile2, rule1);
    ActiveRuleDto ar3 = db.qualityProfiles().activateRule(profile2, rule2);
    indexAll();
    db.getDbClient().qualityProfileDao().deleteRulesProfilesByUuids(db.getSession(), singletonList(profile2.getRulesProfileUuid()));

    underTest.commitDeletionOfProfiles(db.getSession(), singletonList(profile2));

    verifyOnlyIndexed(ar1);
  }

  @Test
  public void commitDeletionOfProfiles_does_nothing_if_profiles_are_not_indexed() {
    db.qualityProfiles().activateRule(profile1, rule1);
    indexAll();
    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isEqualTo(1);

    underTest.commitDeletionOfProfiles(db.getSession(), singletonList(profile2));

    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isEqualTo(1);
  }

  private void assertThatEsQueueTableIsEmpty() {
    assertThat(db.countRowsOfTable(db.getSession(), "es_queue")).isEqualTo(0);
  }

  private void assertThatEsQueueContainsExactly(EsQueueDto expected) {
    Collection<EsQueueDto> items = db.getDbClient().esQueueDao().selectForRecovery(db.getSession(), system2.now() + 1_000, 10);
    assertThat(items)
      .extracting(EsQueueDto::getDocId, EsQueueDto::getDocIdType, EsQueueDto::getDocRouting)
      .containsExactlyInAnyOrder(Tuple.tuple(expected.getDocId(), expected.getDocIdType(), expected.getDocRouting()));
  }

  private void commitAndIndex(RuleDefinitionDto rule, ActiveRuleDto... ar) {
    underTest.commitAndIndex(db.getSession(), stream(ar)
      .map(a -> new ActiveRuleChange(ActiveRuleChange.Type.ACTIVATED, a, rule))
      .collect(Collectors.toList()));
  }

  private void verifyOnlyIndexed(ActiveRuleDto... expected) {
    List<String> docs = es.getIds(TYPE_ACTIVE_RULE);
    assertThat(docs).hasSize(expected.length);
    for (ActiveRuleDto activeRuleDto : expected) {
      assertThat(docs).contains("ar_" + activeRuleDto.getId());
    }
  }

  private void verify(ActiveRuleDoc doc1, QProfileDto profile, ActiveRuleDto activeRule) {
    assertThat(doc1)
      .matches(doc -> doc.getId().equals("ar_" + activeRule.getId()))
      .matches(doc -> doc.getRuleProfileUuid().equals(profile.getRulesProfileUuid()))
      .matches(doc -> doc.getSeverity().equals(activeRule.getSeverityString()));
  }

  private void indexAll() {
    underTest.indexOnStartup(emptySet());
  }

}
