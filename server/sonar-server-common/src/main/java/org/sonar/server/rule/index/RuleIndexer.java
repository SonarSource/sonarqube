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
package org.sonar.server.rule.index;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.es.RuleExtensionId;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.sonar.server.es.ResilientIndexer;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.stream.MoreCollectors.toHashSet;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE_EXTENSION;

public class RuleIndexer implements ResilientIndexer {

  private final EsClient esClient;
  private final DbClient dbClient;

  public RuleIndexer(EsClient esClient, DbClient dbClient) {
    this.esClient = esClient;
    this.dbClient = dbClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(TYPE_RULE, TYPE_RULE_EXTENSION);
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      BulkIndexer bulk = createBulkIndexer(Size.LARGE, IndexingListener.FAIL_ON_ERROR);
      bulk.start();

      // index all definitions and system extensions
      if (uninitializedIndexTypes.contains(TYPE_RULE)) {
        dbClient.ruleDao().scrollIndexingRules(dbSession, dto -> {
          bulk.add(RuleDoc.of(dto).toIndexRequest());
          bulk.add(RuleExtensionDoc.of(dto).toIndexRequest());
        });
      }

      // index all organization extensions
      if (uninitializedIndexTypes.contains(TYPE_RULE_EXTENSION)) {
        dbClient.ruleDao().scrollIndexingRuleExtensions(dbSession, dto -> bulk.add(RuleExtensionDoc.of(dto).toIndexRequest()));
      }

      bulk.stop();
    }
  }

  public void commitAndIndex(DbSession dbSession, int ruleId) {
    commitAndIndex(dbSession, singletonList(ruleId));
  }

  public void commitAndIndex(DbSession dbSession, Collection<Integer> ruleIds) {
    List<EsQueueDto> items = ruleIds.stream()
      .map(RuleIndexer::createQueueDtoForRule)
      .collect(MoreCollectors.toArrayList());

    dbClient.esQueueDao().insert(dbSession, items);
    dbSession.commit();
    postCommit(dbSession, items);
  }

  /**
   * Commit a change on a rule and its extension on the given organization
   */
  public void commitAndIndex(DbSession dbSession, int ruleId, OrganizationDto organization) {
    List<EsQueueDto> items = asList(createQueueDtoForRule(ruleId), createQueueDtoForRuleExtension(ruleId, organization));
    dbClient.esQueueDao().insert(dbSession, items);
    dbSession.commit();
    postCommit(dbSession, items);
  }

  /**
   * This method is used by the Byteman script of integration tests.
   */
  private void postCommit(DbSession dbSession, List<EsQueueDto> items) {
    index(dbSession, items);
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    IndexingResult result = new IndexingResult();
    if (!items.isEmpty()) {
      ListMultimap<String, EsQueueDto> itemsByType = groupItemsByIndexTypeFormat(items);
      doIndexRules(dbSession, itemsByType.get(TYPE_RULE.format())).ifPresent(result::add);
      doIndexRuleExtensions(dbSession, itemsByType.get(TYPE_RULE_EXTENSION.format())).ifPresent(result::add);
    }
    return result;
  }

  private Optional<IndexingResult> doIndexRules(DbSession dbSession, List<EsQueueDto> items) {
    if (items.isEmpty()) {
      return Optional.empty();
    }

    BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, new OneToOneResilientIndexingListener(dbClient, dbSession, items));
    bulkIndexer.start();

    Set<Integer> ruleIds = items
      .stream()
      .map(i -> Integer.parseInt(i.getDocId()))
      .collect(toHashSet(items.size()));

    dbClient.ruleDao().scrollIndexingRulesByKeys(dbSession, ruleIds,
      r -> {
        bulkIndexer.add(RuleDoc.of(r).toIndexRequest());
        bulkIndexer.add(RuleExtensionDoc.of(r).toIndexRequest());
        ruleIds.remove(r.getId());
      });

    // the remaining items reference rows that don't exist in db. They must
    // be deleted from index.
    ruleIds.forEach(ruleId -> {
      bulkIndexer.addDeletion(TYPE_RULE, ruleId.toString(), ruleId.toString());
      bulkIndexer.addDeletion(TYPE_RULE_EXTENSION, RuleExtensionDoc.idOf(ruleId, RuleExtensionScope.system()), ruleId.toString());
    });

    return Optional.of(bulkIndexer.stop());
  }

  private Optional<IndexingResult> doIndexRuleExtensions(DbSession dbSession, List<EsQueueDto> items) {
    if (items.isEmpty()) {
      return Optional.empty();
    }

    BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, new OneToOneResilientIndexingListener(dbClient, dbSession, items));
    bulkIndexer.start();

    Set<RuleExtensionId> docIds = items
      .stream()
      .map(RuleIndexer::explodeRuleExtensionDocId)
      .collect(toHashSet(items.size()));

    dbClient.ruleDao().scrollIndexingRuleExtensionsByIds(dbSession, docIds,
      // only index requests, no deletion requests.
      // Deactivated users are not deleted but updated.
      r -> {
        RuleExtensionId docId = new RuleExtensionId(r.getOrganizationUuid(), r.getRuleId());
        docIds.remove(docId);
        bulkIndexer.add(RuleExtensionDoc.of(r).toIndexRequest());
      });

    // the remaining items reference rows that don't exist in db. They must
    // be deleted from index.
    docIds.forEach(docId -> bulkIndexer.addDeletion(TYPE_RULE_EXTENSION, docId.getId(), String.valueOf(docId.getRuleId())));

    return Optional.of(bulkIndexer.stop());
  }

  private BulkIndexer createBulkIndexer(Size bulkSize, IndexingListener listener) {
    return new BulkIndexer(esClient, TYPE_RULE, bulkSize, listener);
  }

  private static ListMultimap<String, EsQueueDto> groupItemsByIndexTypeFormat(Collection<EsQueueDto> items) {
    return items.stream().collect(MoreCollectors.index(EsQueueDto::getDocType));
  }

  private static RuleExtensionId explodeRuleExtensionDocId(EsQueueDto esQueueDto) {
    checkArgument(TYPE_RULE_EXTENSION.format().equals(esQueueDto.getDocType()));
    return new RuleExtensionId(esQueueDto.getDocId());
  }

  private static EsQueueDto createQueueDtoForRule(int ruleId) {
    String docId = String.valueOf(ruleId);
    return EsQueueDto.create(TYPE_RULE.format(), docId, null, docId);
  }

  private static EsQueueDto createQueueDtoForRuleExtension(int ruleId, OrganizationDto organization) {
    String docId = RuleExtensionDoc.idOf(ruleId, RuleExtensionScope.organization(organization));
    return EsQueueDto.create(TYPE_RULE_EXTENSION.format(), docId, null, String.valueOf(ruleId));
  }

}
