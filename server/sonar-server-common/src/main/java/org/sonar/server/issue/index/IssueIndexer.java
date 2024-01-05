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
package org.sonar.server.issue.index;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.es.AnalysisIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EventIndexer;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToManyResilientIndexingListener;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.sonar.server.permission.index.AuthorizationDoc;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;

/**
 * Indexes issues. All issues belong directly to a project branch, so they only change when a project branch changes.
 */
public class IssueIndexer implements EventIndexer, AnalysisIndexer, NeedAuthorizationIndexer {

  /**
   * Indicates that es_queue.doc_id references an issue. Only this issue must be indexed.
   */
  private static final String ID_TYPE_ISSUE_KEY = "issueKey";
  /**
   * Indicates that es_queue.doc_id references a branch. All the issues of the branch must be indexed.
   * Note that the constant is misleading, but we can't update it since there might some items in the DB during the upgrade.
   */
  private static final String ID_TYPE_BRANCH_UUID = "projectUuid";
  /**
   * Indicates that es_queue.doc_id references a project and that all issues in it should be delete.
   */
  private static final String ID_TYPE_DELETE_PROJECT_UUID = "deleteProjectUuid";

  private static final Logger LOGGER = LoggerFactory.getLogger(IssueIndexer.class);
  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(TYPE_ISSUE, entity -> Qualifiers.PROJECT.equals(entity.getQualifier()));
  private static final Set<IndexType> INDEX_TYPES = Set.of(TYPE_ISSUE);

  private final EsClient esClient;
  private final DbClient dbClient;
  private final IssueIteratorFactory issueIteratorFactory;
  private final AsyncIssueIndexing asyncIssueIndexing;

  public IssueIndexer(EsClient esClient, DbClient dbClient, IssueIteratorFactory issueIteratorFactory, AsyncIssueIndexing asyncIssueIndexing) {
    this.esClient = esClient;
    this.dbClient = dbClient;
    this.issueIteratorFactory = issueIteratorFactory;
    this.asyncIssueIndexing = asyncIssueIndexing;
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return INDEX_TYPES;
  }

  @Override
  public Type getType() {
    return Type.ASYNCHRONOUS;
  }

  @Override
  public void triggerAsyncIndexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    asyncIssueIndexing.triggerOnIndexCreation();
  }

  public void indexAllIssues() {
    try (IssueIterator issues = issueIteratorFactory.createForAll()) {
      doIndex(issues);
    }
  }

  @Override
  public void indexOnAnalysis(String branchUuid) {
    try (IssueIterator issues = issueIteratorFactory.createForBranch(branchUuid)) {
      doIndex(issues);
    }
  }

  @Override
  public void indexOnAnalysis(String branchUuid, Collection<String> diffToIndex) {
    if (diffToIndex.isEmpty()) {
      return;
    }
    try (IssueIterator issues = issueIteratorFactory.createForIssueKeys(diffToIndex)) {
      doIndex(issues);
    }
  }

  @Override
  public boolean supportDiffIndexing() {
    return true;
  }

  public void indexProject(String projectUuid) {
    asyncIssueIndexing.triggerForProject(projectUuid);
  }

  @Override
  public Collection<EsQueueDto> prepareForRecoveryOnEntityEvent(DbSession dbSession, Collection<String> entityUuids, Indexers.EntityEvent cause) {
    return switch (cause) {
      case CREATION, PROJECT_KEY_UPDATE, PROJECT_TAGS_UPDATE, PERMISSION_CHANGE ->
        // Nothing to do, issues do not exist at project creation
        // Measures, permissions, project key and tags are not used in type issues/issue
        emptyList();

      case DELETION -> {
        List<EsQueueDto> items = createProjectDeleteRecoveryItems(entityUuids);
        yield dbClient.esQueueDao().insert(dbSession, items);
      }
    };
  }

  @Override
  public Collection<EsQueueDto> prepareForRecoveryOnBranchEvent(DbSession dbSession, Collection<String> branchUuids, Indexers.BranchEvent cause) {
    return switch (cause) {
      case MEASURE_CHANGE ->
        // Measures, permissions, project key and tags are not used in type issues/issue
        emptyList();

      case DELETION, SWITCH_OF_MAIN_BRANCH -> {
        // switch of main branch requires to reindex the project issues
        List<EsQueueDto> items = createBranchRecoveryItems(branchUuids);
        yield dbClient.esQueueDao().insert(dbSession, items);
      }
    };
  }

  private static List<EsQueueDto> createProjectDeleteRecoveryItems(Collection<String> entityUuids) {
    return entityUuids.stream()
      .map(entityUuid -> createQueueDto(entityUuid, ID_TYPE_DELETE_PROJECT_UUID, entityUuid))
      .toList();
  }

  private static List<EsQueueDto> createBranchRecoveryItems(Collection<String> branchUuids) {
    return branchUuids.stream()
      .map(branchUuid -> createQueueDto(branchUuid, ID_TYPE_BRANCH_UUID, branchUuid))
      .toList();
  }

  /**
   * Commits the DB transaction and adds the issues to Elasticsearch index.
   * <p>
   * If indexing fails, then the recovery daemon will retry later and this
   * method successfully returns. Meanwhile these issues will be "eventually
   * consistent" when requesting the index.
   */
  public void commitAndIndexIssues(DbSession dbSession, Collection<IssueDto> issues) {
    ListMultimap<String, EsQueueDto> itemsByIssueKey = ArrayListMultimap.create();
    issues.stream()
      .map(issue -> createQueueDto(issue.getKey(), ID_TYPE_ISSUE_KEY, issue.getProjectUuid()))
      // a mutable ListMultimap is needed for doIndexIssueItems, so MoreCollectors.index() is
      // not used
      .forEach(i -> itemsByIssueKey.put(i.getDocId(), i));
    dbClient.esQueueDao().insert(dbSession, itemsByIssueKey.values());

    dbSession.commit();

    doIndexIssueItems(dbSession, itemsByIssueKey);
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    ListMultimap<String, EsQueueDto> itemsByIssueKey = ArrayListMultimap.create();
    ListMultimap<String, EsQueueDto> itemsByBranchUuid = ArrayListMultimap.create();
    ListMultimap<String, EsQueueDto> itemsByDeleteProjectUuid = ArrayListMultimap.create();

    items.forEach(i -> {
      if (ID_TYPE_ISSUE_KEY.equals(i.getDocIdType())) {
        itemsByIssueKey.put(i.getDocId(), i);
      } else if (ID_TYPE_BRANCH_UUID.equals(i.getDocIdType())) {
        itemsByBranchUuid.put(i.getDocId(), i);
      } else if (ID_TYPE_DELETE_PROJECT_UUID.equals(i.getDocIdType())) {
        itemsByDeleteProjectUuid.put(i.getDocId(), i);
      } else {
        LOGGER.error("Unsupported es_queue.doc_id_type for issues. Manual fix is required: " + i);
      }
    });

    IndexingResult result = new IndexingResult();
    result.add(doIndexIssueItems(dbSession, itemsByIssueKey));
    result.add(doIndexBranchItems(dbSession, itemsByBranchUuid));
    result.add(doDeleteProjectIndexItems(dbSession, itemsByDeleteProjectUuid));
    return result;
  }

  private IndexingResult doIndexIssueItems(DbSession dbSession, ListMultimap<String, EsQueueDto> itemsByIssueKey) {
    if (itemsByIssueKey.isEmpty()) {
      return new IndexingResult();
    }
    IndexingListener listener = new OneToOneResilientIndexingListener(dbClient, dbSession, itemsByIssueKey.values());
    BulkIndexer bulkIndexer = createBulkIndexer(listener);
    bulkIndexer.start();

    try (IssueIterator issues = issueIteratorFactory.createForIssueKeys(itemsByIssueKey.keySet())) {
      while (issues.hasNext()) {
        IssueDoc issue = issues.next();
        bulkIndexer.add(newIndexRequest(issue));
        itemsByIssueKey.removeAll(issue.getId());
      }
    }

    // the remaining uuids reference issues that don't exist in db. They must
    // be deleted from index.
    itemsByIssueKey.values().forEach(
      item -> bulkIndexer.addDeletion(TYPE_ISSUE.getMainType(), item.getDocId(), item.getDocRouting()));

    return bulkIndexer.stop();
  }

  private IndexingResult doDeleteProjectIndexItems(DbSession dbSession, ListMultimap<String, EsQueueDto> itemsByDeleteProjectUuid) {
    IndexingListener listener = new OneToManyResilientIndexingListener(dbClient, dbSession, itemsByDeleteProjectUuid.values());
    BulkIndexer bulkIndexer = createBulkIndexer(listener);
    bulkIndexer.start();
    for (String projectUuid : itemsByDeleteProjectUuid.keySet()) {
      addProjectDeletionToBulkIndexer(bulkIndexer, projectUuid);
    }
    return bulkIndexer.stop();
  }

  private IndexingResult doIndexBranchItems(DbSession dbSession, ListMultimap<String, EsQueueDto> itemsByBranchUuid) {
    if (itemsByBranchUuid.isEmpty()) {
      return new IndexingResult();
    }

    // one branch, referenced by es_queue.doc_id = many issues
    IndexingListener listener = new OneToManyResilientIndexingListener(dbClient, dbSession, itemsByBranchUuid.values());
    BulkIndexer bulkIndexer = createBulkIndexer(listener);
    bulkIndexer.start();

    for (String branchUuid : itemsByBranchUuid.keySet()) {
      try (IssueIterator issues = issueIteratorFactory.createForBranch(branchUuid)) {
        if (issues.hasNext()) {
          do {
            IssueDoc doc = issues.next();
            bulkIndexer.add(newIndexRequest(doc));
          } while (issues.hasNext());
        } else {
          // branch does not exist or has no issues. In both cases,
          // all the documents related to this branch are deleted.
          Optional<BranchDto> branch = dbClient.branchDao().selectByUuid(dbSession, branchUuid);
          branch.ifPresent(b -> addBranchDeletionToBulkIndexer(bulkIndexer, b.getProjectUuid(), b.getUuid()));
        }
      }
    }

    return bulkIndexer.stop();
  }

  // Used by Compute Engine, no need to recovery on errors
  public void deleteByKeys(String projectUuid, Collection<String> issueKeys) {
    if (issueKeys.isEmpty()) {
      return;
    }

    BulkIndexer bulkIndexer = createBulkIndexer(IndexingListener.FAIL_ON_ERROR);
    bulkIndexer.start();
    issueKeys.forEach(issueKey -> bulkIndexer.addDeletion(TYPE_ISSUE.getMainType(), issueKey, AuthorizationDoc.idOf(projectUuid)));
    bulkIndexer.stop();
  }

  @VisibleForTesting
  protected void index(Iterator<IssueDoc> issues) {
    doIndex(issues);
  }

  private void doIndex(Iterator<IssueDoc> issues) {
    BulkIndexer bulk = createBulkIndexer(IndexingListener.FAIL_ON_ERROR);
    bulk.start();
    while (issues.hasNext()) {
      IssueDoc issue = issues.next();
      bulk.add(newIndexRequest(issue));
    }
    bulk.stop();
  }

  private static IndexRequest newIndexRequest(IssueDoc issue) {
    return new IndexRequest(TYPE_ISSUE.getMainType().getIndex().getName())
      .id(issue.getId())
      .routing(issue.getRouting().orElseThrow(() -> new IllegalStateException("IssueDoc should define a routing")))
      .source(issue.getFields());
  }

  private static void addProjectDeletionToBulkIndexer(BulkIndexer bulkIndexer, String projectUuid) {
    SearchRequest search = EsClient.prepareSearch(TYPE_ISSUE.getMainType())
      .routing(AuthorizationDoc.idOf(projectUuid))
      .source(new SearchSourceBuilder().query(boolQuery().must(termQuery(FIELD_ISSUE_PROJECT_UUID, projectUuid))));

    bulkIndexer.addDeletion(search);
  }

  private static void addBranchDeletionToBulkIndexer(BulkIndexer bulkIndexer, String projectUUid, String branchUuid) {
    SearchRequest search = EsClient.prepareSearch(TYPE_ISSUE.getMainType())
      // routing is based on the parent (See BaseDoc#getRouting).
      // The parent is set to the projectUUid when an issue is indexed (See IssueDoc#setProjectUuid). We need to set it here
      // so that the search finds the indexed docs to be deleted.
      .routing(AuthorizationDoc.idOf(projectUUid))
      .source(new SearchSourceBuilder().query(boolQuery().must(termQuery(FIELD_ISSUE_BRANCH_UUID, branchUuid))));

    bulkIndexer.addDeletion(search);
  }

  private static EsQueueDto createQueueDto(String docId, String docIdType, String projectUuid) {
    return EsQueueDto.create(TYPE_ISSUE.format(), docId, docIdType, AuthorizationDoc.idOf(projectUuid));
  }

  private BulkIndexer createBulkIndexer(IndexingListener listener) {
    return new BulkIndexer(esClient, TYPE_ISSUE, Size.REGULAR, listener);
  }
}
