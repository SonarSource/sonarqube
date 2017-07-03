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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.es.RuleExtensionId;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleExtensionForIndexingDto;
import org.sonar.db.rule.RuleForIndexingDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.ResiliencyIndexingListener;
import org.sonar.server.es.ResilientIndexer;
import org.sonar.server.es.StartupIndexer;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.stream.MoreCollectors.toHashSet;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_RULE_EXTENSION;

public class RuleIndexer implements StartupIndexer, ResilientIndexer {

  private final EsClient esClient;
  private final DbClient dbClient;

  public RuleIndexer(EsClient esClient, DbClient dbClient) {
    this.esClient = esClient;
    this.dbClient = dbClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(INDEX_TYPE_RULE, INDEX_TYPE_RULE_EXTENSION);
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      BulkIndexer bulk = createBulkIndexer(Size.LARGE, IndexingListener.noop());
      bulk.start();

      // index all definitions and system extensions
      if (uninitializedIndexTypes.contains(INDEX_TYPE_RULE)) {
        dbClient.ruleDao().scrollIndexingRules(dbSession, dto -> {
          bulk.add(newRuleDocIndexRequest(dto));
          bulk.add(newRuleExtensionDocIndexRequest(dto));
        });
      }

      // index all organization extensions
      if (uninitializedIndexTypes.contains(INDEX_TYPE_RULE_EXTENSION)) {
        dbClient.ruleDao().scrollIndexingRuleExtensions(dbSession, dto -> {
          bulk.add(newRuleExtensionDocIndexRequest(dto));
        });
      }

      bulk.stop();
    }
  }

  public void commitAndIndex(DbSession dbSession, RuleKey ruleKey) {
    commitAndIndex(dbSession, singletonList(ruleKey));
  }

  public void commitAndIndex(DbSession dbSession, Collection<RuleKey> ruleDtos) {
    List<EsQueueDto> items = ruleDtos.stream()
      .map(key -> EsQueueDto.create(EsQueueDto.Type.RULE, key.toString()))
      .collect(MoreCollectors.toArrayList());

    dbClient.esQueueDao().insert(dbSession, items);
    dbSession.commit();
    postCommit(dbSession, ruleDtos, items);
  }

  public void commitAndIndex(DbSession dbSession, OrganizationDto organizationDto, RuleKey ruleKey) {
    List<EsQueueDto> items = singletonList(EsQueueDto.create(EsQueueDto.Type.RULE_EXTENSION, ruleKey + "|" + organizationDto.getUuid()));

    dbClient.esQueueDao().insert(dbSession, items);
    dbSession.commit();
    postCommit(dbSession, ruleKey, organizationDto, items);
  }

  /**
   * Entry point for Byteman tests. See directory tests/resilience.
   * The parameter "ruleKeys" is used only by the Byteman script.
   */
  private void postCommit(DbSession dbSession, Collection<RuleKey> ruleKeys, Collection<EsQueueDto> items) {
    index(dbSession, items);
  }

  private void postCommit(DbSession dbSession, RuleKey ruleKeys, OrganizationDto organizationDto, Collection<EsQueueDto> items) {
    index(dbSession, items);
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    if (items.isEmpty()) {
      return new IndexingResult();
    }

    IndexingResult result = new IndexingResult();

    ListMultimap<EsQueueDto.Type, EsQueueDto> itemsByType = groupItemsByType(items);

    result.add(doIndexRules(dbSession, itemsByType.get(EsQueueDto.Type.RULE)));
    result.add(doIndexRuleExtensions(dbSession, itemsByType.get(EsQueueDto.Type.RULE_EXTENSION)));

    return result;
  }

  private IndexingResult doIndexRules(DbSession dbSession, List<EsQueueDto> items) {
    BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, new ResiliencyIndexingListener(dbClient, dbSession, items));
    bulkIndexer.start();

    Set<RuleKey> rules = items
      .stream()
      .filter(i -> {
        requireNonNull(i.getDocId(), () -> "BUG - " + i + " has not been persisted before indexing");
        return i.getDocType() == EsQueueDto.Type.RULE;
      })
      .map(i -> RuleKey.parse(i.getDocId()))
      .collect(toHashSet(items.size()));

    dbClient.ruleDao().scrollIndexingRulesByKeys(dbSession, rules,
      // only index requests, no deletion requests.
      // Deactivated users are not deleted but updated.
      r -> {
        rules.remove(r.getRuleKey());
        bulkIndexer.add(newRuleDocIndexRequest(r));
        bulkIndexer.add(newRuleExtensionDocIndexRequest(r));
      });

    // the remaining items reference rows that don't exist in db. They must
    // be deleted from index.
    rules.forEach(r -> bulkIndexer.addDeletion(RuleIndexDefinition.INDEX_TYPE_RULE, r.toString()));
    rules.forEach(r -> bulkIndexer.addDeletion(RuleIndexDefinition.INDEX_TYPE_RULE_EXTENSION, r.toString()));

    return bulkIndexer.stop();
  }

  private IndexingResult doIndexRuleExtensions(DbSession dbSession, List<EsQueueDto> items) {
    BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, new ResiliencyIndexingListener(dbClient, dbSession, items));
    bulkIndexer.start();

    Set<RuleExtensionId> docIds = items
      .stream()
      .filter(i -> {
        requireNonNull(i.getDocId(), () -> "BUG - " + i + " has not been persisted before indexing");
        return i.getDocType() == EsQueueDto.Type.RULE_EXTENSION;
      })
      .map(RuleIndexer::explodeRuleExtensionDocId)
      .collect(toHashSet(items.size()));

    dbClient.ruleDao().scrollIndexingRuleExtensionsByIds(dbSession, docIds,
      // only index requests, no deletion requests.
      // Deactivated users are not deleted but updated.
      r -> {
        docIds.remove(new RuleExtensionId(r.getOrganizationUuid(), r.getPluginName(), r.getPluginRuleKey()));
        bulkIndexer.add(newRuleExtensionDocIndexRequest(r));
      });

    // the remaining items reference rows that don't exist in db. They must
    // be deleted from index.
    docIds.forEach(r -> bulkIndexer.addDeletion(RuleIndexDefinition.INDEX_TYPE_RULE_EXTENSION, r.getId()));

    return bulkIndexer.stop();
  }

  private static IndexRequest newRuleDocIndexRequest(RuleForIndexingDto ruleForIndexingDto) {
    RuleDoc doc = RuleDoc.of(ruleForIndexingDto);

    return new IndexRequest(INDEX_TYPE_RULE.getIndex(), INDEX_TYPE_RULE.getType())
      .id(doc.key().toString())
      .routing(doc.getRouting())
      .source(doc.getFields());
  }

  private static IndexRequest newRuleExtensionDocIndexRequest(RuleForIndexingDto ruleForIndexingDto) {
    RuleExtensionDoc ruleExtensionDoc = RuleExtensionDoc.of(ruleForIndexingDto);

    return new IndexRequest(INDEX_TYPE_RULE_EXTENSION.getIndex(), INDEX_TYPE_RULE_EXTENSION.getType())
      .id(ruleExtensionDoc.getId())
      .routing(ruleExtensionDoc.getRouting())
      .parent(ruleExtensionDoc.getParent())
      .source(ruleExtensionDoc.getFields());
  }

  private static IndexRequest newRuleExtensionDocIndexRequest(RuleExtensionForIndexingDto ruleExtensionForIndexingDto) {
    RuleExtensionDoc doc = RuleExtensionDoc.of(ruleExtensionForIndexingDto);
    return new IndexRequest(INDEX_TYPE_RULE_EXTENSION.getIndex(), INDEX_TYPE_RULE_EXTENSION.getType())
      .id(doc.getId())
      .routing(doc.getRouting())
      .parent(doc.getParent())
      .source(doc.getFields());
  }

  private BulkIndexer createBulkIndexer(Size bulkSize, IndexingListener listener) {
    return new BulkIndexer(esClient, INDEX_TYPE_RULE.getIndex(), bulkSize, listener);
  }

  private static ListMultimap<EsQueueDto.Type, EsQueueDto> groupItemsByType(Collection<EsQueueDto> items) {
    return items.stream().collect(MoreCollectors.index(EsQueueDto::getDocType));
  }

  private static RuleExtensionId explodeRuleExtensionDocId(EsQueueDto esQueueDto) {
    checkArgument(esQueueDto.getDocType() == EsQueueDto.Type.RULE_EXTENSION);
    return new RuleExtensionId(esQueueDto.getDocId());
  }
}
