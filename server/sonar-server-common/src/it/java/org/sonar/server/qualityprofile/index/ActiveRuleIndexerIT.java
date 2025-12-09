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
package org.sonar.server.qualityprofile.index;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.qualityprofile.ActiveRuleChange;

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;

class ActiveRuleIndexerIT {

  private final System2 system2 = System2.INSTANCE;

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  @RegisterExtension
  private final EsTester es = EsTester.create();

  private final ActiveRuleIndexer underTest = new ActiveRuleIndexer(db.getDbClient(), es.client());
  private RuleDto rule1;
  private RuleDto rule2;
  private QProfileDto profile1;
  private QProfileDto profile2;

  @BeforeEach
  void before() {
    rule1 = db.rules().insert(r -> r.replaceAllDefaultImpacts(List.of(new ImpactDto(SECURITY, BLOCKER), new ImpactDto(RELIABILITY, LOW))));
    rule2 = db.rules().insert();
    profile1 = db.qualityProfiles().insert();
    profile2 = db.qualityProfiles().insert();
  }

  @Test
  void getIndexTypes() {
    assertThat(underTest.getIndexTypes()).containsExactly(TYPE_ACTIVE_RULE);
  }

  @Test
  void indexOnStartup_does_nothing_if_no_data() {
    underTest.indexOnStartup(emptySet());
    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isZero();
  }

  @Test
  void indexOnStartup_indexes_all_data() {
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(profile1, rule1);

    underTest.indexOnStartup(emptySet());

    List<ActiveRuleDoc> docs = es.getDocuments(TYPE_ACTIVE_RULE, ActiveRuleDoc.class);
    assertThat(docs).hasSize(1);
    verify(docs.get(0), profile1, activeRule);
    assertThatEsQueueTableIsEmpty();
  }

  @Test
  void indexAll_indexes_all_data() {
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(profile1, rule1);

    underTest.indexAll();

    List<ActiveRuleDoc> docs = es.getDocuments(TYPE_ACTIVE_RULE, ActiveRuleDoc.class);
    assertThat(docs).hasSize(1);
    verify(docs.get(0), profile1, activeRule);
    assertThatEsQueueTableIsEmpty();
  }

  @Test
  void test_commitAndIndex() {
    ActiveRuleDto ar1 = db.qualityProfiles().activateRule(profile1, rule1);
    ActiveRuleDto ar2 = db.qualityProfiles().activateRule(profile2, rule1);
    db.qualityProfiles().activateRule(profile2, rule2);

    commitAndIndex(rule1, ar1, ar2);

    verifyOnlyIndexed(ar1, ar2);
    assertThatEsQueueTableIsEmpty();
  }

  @Test
  void commitAndIndex_empty_list() {
    db.qualityProfiles().activateRule(profile1, rule1);

    underTest.commitAndIndex(db.getSession(), Collections.emptyList());

    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isZero();
    assertThatEsQueueTableIsEmpty();
  }

  @Test
  void commitAndIndex_keeps_elements_to_recover_in_ES_QUEUE_on_errors() {
    ActiveRuleDto ar = db.qualityProfiles().activateRule(profile1, rule1);
    es.lockWrites(TYPE_ACTIVE_RULE);

    commitAndIndex(rule1, ar);

    EsQueueDto expectedItem = EsQueueDto.create(TYPE_ACTIVE_RULE.format(), "ar_" + ar.getUuid(), "activeRuleUuid", ar.getRuleUuid());
    assertThatEsQueueContainsExactly(expectedItem);
    es.unlockWrites(TYPE_ACTIVE_RULE);
  }

  @Test
  void commitAndIndex_deletes_the_documents_that_dont_exist_in_database() {
    ActiveRuleDto ar = db.qualityProfiles().activateRule(profile1, rule1);
    indexAll();
    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isOne();

    db.getDbClient().activeRuleDao().delete(db.getSession(), ar.getKey());
    commitAndIndex(rule1, ar);

    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isZero();
    assertThatEsQueueTableIsEmpty();
  }

  @Test
  void index_fails_and_deletes_doc_if_docIdType_is_unsupported() {
    EsQueueDto item = EsQueueDto.create(TYPE_ACTIVE_RULE.format(), "the_id", "unsupported", "the_routing");
    db.getDbClient().esQueueDao().insert(db.getSession(), item);

    underTest.index(db.getSession(), singletonList(item));

    assertThatEsQueueTableIsEmpty();
    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isZero();
  }

  @Test
  void commitDeletionOfProfiles() {
    ActiveRuleDto ar1 = db.qualityProfiles().activateRule(profile1, rule1);
    db.qualityProfiles().activateRule(profile2, rule1);
    db.qualityProfiles().activateRule(profile2, rule2);
    indexAll();
    db.getDbClient().qualityProfileDao().deleteRulesProfilesByUuids(db.getSession(), singletonList(profile2.getRulesProfileUuid()));

    underTest.commitDeletionOfProfiles(db.getSession(), singletonList(profile2));

    verifyOnlyIndexed(ar1);
  }

  @Test
  void commitDeletionOfProfiles_does_nothing_if_profiles_are_not_indexed() {
    db.qualityProfiles().activateRule(profile1, rule1);
    indexAll();
    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isOne();

    underTest.commitDeletionOfProfiles(db.getSession(), singletonList(profile2));

    assertThat(es.countDocuments(TYPE_ACTIVE_RULE)).isOne();
  }

  private void assertThatEsQueueTableIsEmpty() {
    assertThat(db.countRowsOfTable(db.getSession(), "es_queue")).isZero();
  }

  private void assertThatEsQueueContainsExactly(EsQueueDto expected) {
    Collection<EsQueueDto> items = db.getDbClient().esQueueDao().selectForRecovery(db.getSession(), system2.now() + 1_000, 10);
    assertThat(items)
      .extracting(EsQueueDto::getDocId, EsQueueDto::getDocIdType, EsQueueDto::getDocRouting)
      .containsExactlyInAnyOrder(Tuple.tuple(expected.getDocId(), expected.getDocIdType(), expected.getDocRouting()));
  }

  private void commitAndIndex(RuleDto rule, ActiveRuleDto... ar) {
    underTest.commitAndIndex(db.getSession(), stream(ar)
      .map(a -> new ActiveRuleChange(ActiveRuleChange.Type.ACTIVATED, a, rule))
      .toList());
  }

  private void verifyOnlyIndexed(ActiveRuleDto... expected) {
    List<String> docs = es.getIds(TYPE_ACTIVE_RULE);
    assertThat(docs).hasSize(expected.length);
    for (ActiveRuleDto activeRuleDto : expected) {
      assertThat(docs).contains("ar_" + activeRuleDto.getUuid());
    }
  }

  private void verify(ActiveRuleDoc doc1, QProfileDto profile, ActiveRuleDto activeRule) {
    assertThat(doc1)
      .matches(doc -> doc.getId().equals("ar_" + activeRule.getUuid()))
      .matches(doc -> doc.getRuleProfileUuid().equals(profile.getRulesProfileUuid()))
      .matches(doc -> doc.getSeverity().equals(activeRule.getSeverityString()))
      .matches(doc -> doc.getImpacts().equals(activeRule.getImpacts()));
  }

  private void indexAll() {
    underTest.indexOnStartup(emptySet());
  }

}
