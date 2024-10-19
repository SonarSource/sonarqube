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
package org.sonar.server.rule.index;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.es.RuleExtensionId;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleForIndexingDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.sonar.server.es.ResilientIndexer;
import org.sonar.server.security.SecurityStandards;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE_EXTENSION;
import static org.sonar.server.security.SecurityStandards.SQ_CATEGORY_KEYS_ORDERING;

public class RuleIndexer implements ResilientIndexer {
  private static final Logger LOG = LoggerFactory.getLogger(RuleIndexer.class);

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
    indexAll(Size.LARGE, uninitializedIndexTypes);
  }

  public void indexAll() {
    indexAll(Size.REGULAR, getIndexTypes());
  }

  private void indexAll(Size bulkSize, Set<IndexType> uninitializedIndexTypes) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      BulkIndexer bulk = createBulkIndexer(bulkSize, IndexingListener.FAIL_ON_ERROR);
      bulk.start();

      // index all definitions and system extensions
      if (uninitializedIndexTypes.contains(TYPE_RULE)) {
        dbClient.ruleDao().selectIndexingRules(dbSession, dto -> {
          bulk.add(ruleDocOf(dto).toIndexRequest());
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

  public void commitAndIndex(DbSession dbSession, String ruleUuid) {
    commitAndIndex(dbSession, singletonList(ruleUuid));
  }

  public void commitAndIndex(DbSession dbSession, Collection<String> ruleUuids) {
    List<EsQueueDto> items = ruleUuids.stream()
      .map(RuleIndexer::createQueueDtoForRule)
      .toList();

    dbClient.esQueueDao().insert(dbSession, items);
    dbSession.commit();
    postCommit(dbSession, items);
  }

  /**
   * Commit a change on a rule and its extension
   */
  public void commitAndIndex(DbSession dbSession, String ruleUuid, OrganizationDto organization) {
    List<EsQueueDto> items = asList(createQueueDtoForRule(ruleUuid, organization), createQueueDtoForRuleExtension(ruleUuid, organization));
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

    Set<String> ruleUuids = items
      .stream()
      .map(EsQueueDto::getDocId)
      .collect(Collectors.toSet());

    dbClient.ruleDao().selectIndexingRulesByKeys(dbSession, ruleUuids,
      r -> {
        bulkIndexer.add(ruleDocOf(r).toIndexRequest());
        bulkIndexer.add(RuleExtensionDoc.of(r).toIndexRequest());
        ruleUuids.remove(r.getUuid());
      });

    // the remaining items reference rows that don't exist in db. They must be deleted from index.
    ruleUuids.forEach(ruleUuid -> {
      bulkIndexer.addDeletion(TYPE_RULE, ruleUuid, ruleUuid);
      bulkIndexer.addDeletion(TYPE_RULE_EXTENSION, RuleExtensionDoc.idOf(ruleUuid, RuleExtensionScope.system()), ruleUuid);
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
              RuleExtensionId docId = new RuleExtensionId(r.getOrganizationUuid(), r.getRuleUuid());
              docIds.remove(docId);
              bulkIndexer.add(RuleExtensionDoc.of(r).toIndexRequest());
            });

    // the remaining items reference rows that don't exist in db. They must
    // be deleted from index.
    docIds.forEach(docId -> bulkIndexer.addDeletion(TYPE_RULE_EXTENSION, docId.getId(), docId.getRuleUuid()));

    return Optional.of(bulkIndexer.stop());
  }

  private static RuleDoc ruleDocOf(RuleForIndexingDto dto) {
    SecurityStandards securityStandards = SecurityStandards.fromSecurityStandards(dto.getSecurityStandards());
    if (!securityStandards.getIgnoredSQCategories().isEmpty()) {
      LOG.debug(
        "Rule {} with CWEs '{}' maps to multiple SQ Security Categories: {}",
        dto.getRuleKey(),
        String.join(", ", securityStandards.getCwe()),
        concat(Stream.of(securityStandards.getSqCategory()), securityStandards.getIgnoredSQCategories().stream())
          .map(SecurityStandards.SQCategory::getKey)
          .sorted(SQ_CATEGORY_KEYS_ORDERING)
          .collect(joining(", ")));
    }
    return RuleDoc.createFrom(dto, securityStandards);
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

  private static EsQueueDto createQueueDtoForRule(String ruleUuid) {
    return EsQueueDto.create(TYPE_RULE.format(), ruleUuid, null, ruleUuid);
  }

  private static EsQueueDto createQueueDtoForRule(String ruleUuid, OrganizationDto organization) {
    String docId = RuleExtensionDoc.idOf(ruleUuid, RuleExtensionScope.organization(organization));
    return EsQueueDto.create(TYPE_RULE_EXTENSION.format(), docId, null, ruleUuid);
  }

  private static EsQueueDto createQueueDtoForRuleExtension(String ruleUuid, OrganizationDto organization) {
    String docId = RuleExtensionDoc.idOf(ruleUuid, RuleExtensionScope.organization(organization));
    return EsQueueDto.create(TYPE_RULE_EXTENSION.format(), docId, null, ruleUuid);
  }
}
