/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.qualityprofile.IndexedActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.SeverityUtil;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.sonar.server.es.ResilientIndexer;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.qualityprofile.index.ActiveRuleDoc.docIdOf;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;

public class ActiveRuleIndexer implements ResilientIndexer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveRuleIndexer.class);
  private static final String ID_TYPE_ACTIVE_RULE_UUID = "activeRuleUuid";
  private static final String ID_TYPE_RULE_PROFILE_UUID = "ruleProfileUuid";

  private final DbClient dbClient;
  private final EsClient esClient;

  public ActiveRuleIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    indexAll(Size.LARGE);
  }

  public void indexAll() {
    indexAll(Size.REGULAR);
  }

  private void indexAll(Size bulkSize) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      BulkIndexer bulkIndexer = createBulkIndexer(bulkSize, IndexingListener.FAIL_ON_ERROR);
      bulkIndexer.start();
      dbClient.activeRuleDao().scrollAllForIndexing(dbSession, ar -> bulkIndexer.add(newIndexRequest(ar)));
      bulkIndexer.stop();
    }
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(TYPE_ACTIVE_RULE);
  }

  public void commitAndIndex(DbSession dbSession, Collection<ActiveRuleChange> changes) {
    List<EsQueueDto> items = changes.stream()
      .map(ActiveRuleChange::getActiveRule)
      .map(ar -> newQueueDto(docIdOf(ar.getUuid()), ID_TYPE_ACTIVE_RULE_UUID, ar.getRuleUuid()))
      .toList();

    dbClient.esQueueDao().insert(dbSession, items);
    dbSession.commit();
    postCommit(dbSession, items);
  }

  public void commitDeletionOfProfiles(DbSession dbSession, Collection<QProfileDto> profiles) {
    List<EsQueueDto> items = profiles.stream()
      .map(QProfileDto::getRulesProfileUuid)
      .distinct()
      .map(ruleProfileUuid -> newQueueDto(ruleProfileUuid, ID_TYPE_RULE_PROFILE_UUID, null))
      .toList();

    dbClient.esQueueDao().insert(dbSession, items);
    dbSession.commit();
    postCommit(dbSession, items);
  }

  /**
   * Entry point for Byteman tests. See directory tests/resilience.
   */
  private void postCommit(DbSession dbSession, Collection<EsQueueDto> items) {
    index(dbSession, items);
  }

  /**
   * @return the number of items that have been successfully indexed
   */
  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    IndexingResult result = new IndexingResult();

    if (items.isEmpty()) {
      return result;
    }

    Map<String, EsQueueDto> activeRuleItems = new HashMap<>();
    Map<String, EsQueueDto> ruleProfileItems = new HashMap<>();
    items.forEach(i -> {
      if (ID_TYPE_RULE_PROFILE_UUID.equals(i.getDocIdType())) {
        ruleProfileItems.put(i.getDocId(), i);
      } else if (ID_TYPE_ACTIVE_RULE_UUID.equals(i.getDocIdType())) {
        activeRuleItems.put(i.getDocId(), i);
      } else {
        LOGGER.error("Unsupported es_queue.doc_id_type. Removing row from queue: " + i);
        deleteQueueDto(dbSession, i);
      }
    });

    if (!activeRuleItems.isEmpty()) {
      result.add(doIndexActiveRules(dbSession, activeRuleItems));
    }
    if (!ruleProfileItems.isEmpty()) {
      result.add(doIndexRuleProfiles(dbSession, ruleProfileItems));
    }
    return result;
  }

  private IndexingResult doIndexActiveRules(DbSession dbSession, Map<String, EsQueueDto> activeRuleItems) {
    OneToOneResilientIndexingListener listener = new OneToOneResilientIndexingListener(dbClient, dbSession, activeRuleItems.values());
    BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, listener);
    bulkIndexer.start();
    Map<String, EsQueueDto> remaining = new HashMap<>(activeRuleItems);
    dbClient.activeRuleDao().scrollByUuidsForIndexing(dbSession, toActiveRuleUuids(activeRuleItems),
      i -> {
        remaining.remove(docIdOf(i.getUuid()));
        bulkIndexer.add(newIndexRequest(i));
      });

    // the remaining ids reference rows that don't exist in db. They must
    // be deleted from index.
    remaining.values().forEach(item -> bulkIndexer.addDeletion(TYPE_ACTIVE_RULE,
      item.getDocId(), item.getDocRouting()));
    return bulkIndexer.stop();
  }

  private static Collection<String> toActiveRuleUuids(Map<String, EsQueueDto> activeRuleItems) {
    Set<String> docIds = activeRuleItems.keySet();
    return docIds.stream()
      .map(ActiveRuleDoc::activeRuleUuidOf)
      .collect(Collectors.toSet());
  }

  private IndexingResult doIndexRuleProfiles(DbSession dbSession, Map<String, EsQueueDto> ruleProfileItems) {
    IndexingResult result = new IndexingResult();

    for (Map.Entry<String, EsQueueDto> entry : ruleProfileItems.entrySet()) {
      String ruleProfileUUid = entry.getKey();
      EsQueueDto item = entry.getValue();
      IndexingResult profileResult;

      RulesProfileDto profile = dbClient.qualityProfileDao().selectRuleProfile(dbSession, ruleProfileUUid);
      if (profile == null) {
        // profile does not exist anymore in db --> related documents must be deleted from index rules/activeRule
        SearchRequest search = EsClient.prepareSearch(TYPE_ACTIVE_RULE.getMainType())
          .source(new SearchSourceBuilder().query(QueryBuilders.boolQuery().must(termQuery(FIELD_ACTIVE_RULE_PROFILE_UUID, ruleProfileUUid))));
        profileResult = BulkIndexer.delete(esClient, TYPE_ACTIVE_RULE, search);

      } else {
        BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, IndexingListener.FAIL_ON_ERROR);
        bulkIndexer.start();
        dbClient.activeRuleDao().scrollByRuleProfileForIndexing(dbSession, ruleProfileUUid, i -> bulkIndexer.add(newIndexRequest(i)));
        profileResult = bulkIndexer.stop();
      }

      if (profileResult.isSuccess()) {
        deleteQueueDto(dbSession, item);
      }
      result.add(profileResult);
    }

    return result;
  }

  private void deleteQueueDto(DbSession dbSession, EsQueueDto item) {
    dbClient.esQueueDao().delete(dbSession, item);
    dbSession.commit();
  }

  private BulkIndexer createBulkIndexer(Size size, IndexingListener listener) {
    return new BulkIndexer(esClient, TYPE_ACTIVE_RULE, size, listener);
  }

  private static IndexRequest newIndexRequest(IndexedActiveRuleDto dto) {
    ActiveRuleDoc doc = new ActiveRuleDoc(dto.getUuid())
      .setRuleUuid(dto.getRuleUuid())
      .setRuleProfileUuid(dto.getRuleProfileUuid())
      .setSeverity(SeverityUtil.getSeverityFromOrdinal(dto.getSeverity()))
      .setPrioritizedRule(dto.getPrioritizedRule());
    // all the fields must be present, even if value is null
    String inheritance = dto.getInheritance();
    doc.setInheritance(inheritance == null ? ActiveRuleInheritance.NONE.name() : inheritance);
    return doc.toIndexRequest();
  }

  private static EsQueueDto newQueueDto(String docId, String docIdType, @Nullable String routing) {
    return EsQueueDto.create(TYPE_ACTIVE_RULE.format(), docId, docIdType, routing);
  }
}
