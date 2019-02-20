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
package org.sonar.server.issue.index;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToManyResilientIndexingListener;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.permission.index.AuthorizationDoc;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;

public class IssueIndexer implements ProjectIndexer, NeedAuthorizationIndexer {

  /**
   * Indicates that es_queue.doc_id references an issue. Only this issue must be indexed.
   */
  private static final String ID_TYPE_ISSUE_KEY = "issueKey";
  /**
   * Indicates that es_queue.doc_id references a project. All the issues of the project must be indexed.
   */
  private static final String ID_TYPE_PROJECT_UUID = "projectUuid";
  private static final Logger LOGGER = Loggers.get(IssueIndexer.class);
  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(TYPE_ISSUE, project -> Qualifiers.PROJECT.equals(project.getQualifier()));
  private static final ImmutableSet<IndexType> INDEX_TYPES = ImmutableSet.of(TYPE_ISSUE);

  private final EsClient esClient;
  private final DbClient dbClient;
  private final IssueIteratorFactory issueIteratorFactory;

  public IssueIndexer(EsClient esClient, DbClient dbClient, IssueIteratorFactory issueIteratorFactory) {
    this.esClient = esClient;
    this.dbClient = dbClient;
    this.issueIteratorFactory = issueIteratorFactory;
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
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    try (IssueIterator issues = issueIteratorFactory.createForAll()) {
      doIndex(issues, Size.LARGE, IndexingListener.FAIL_ON_ERROR);
    }
  }

  @Override
  public void indexOnAnalysis(String branchUuid) {
    try (IssueIterator issues = issueIteratorFactory.createForProject(branchUuid)) {
      doIndex(issues, Size.REGULAR, IndexingListener.FAIL_ON_ERROR);
    }
  }

  @Override
  public Collection<EsQueueDto> prepareForRecovery(DbSession dbSession, Collection<String> projectUuids, ProjectIndexer.Cause cause) {
    switch (cause) {
      case PROJECT_CREATION:
        // nothing to do, issues do not exist at project creation
      case MEASURE_CHANGE:
      case PROJECT_KEY_UPDATE:
      case PROJECT_TAGS_UPDATE:
      case PERMISSION_CHANGE:
        // nothing to do. Measures, permissions, project key and tags are not used in type issues/issue
        return emptyList();

      case PROJECT_DELETION:
        List<EsQueueDto> items = projectUuids.stream()
          .map(projectUuid -> createQueueDto(projectUuid, ID_TYPE_PROJECT_UUID, projectUuid))
          .collect(MoreCollectors.toArrayList(projectUuids.size()));
        return dbClient.esQueueDao().insert(dbSession, items);

      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
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
    ListMultimap<String, EsQueueDto> itemsByProjectKey = ArrayListMultimap.create();
    items.forEach(i -> {
      if (ID_TYPE_ISSUE_KEY.equals(i.getDocIdType())) {
        itemsByIssueKey.put(i.getDocId(), i);
      } else if (ID_TYPE_PROJECT_UUID.equals(i.getDocIdType())) {
        itemsByProjectKey.put(i.getDocId(), i);
      } else {
        LOGGER.error("Unsupported es_queue.doc_id_type for issues. Manual fix is required: " + i);
      }
    });

    IndexingResult result = new IndexingResult();
    result.add(doIndexIssueItems(dbSession, itemsByIssueKey));
    result.add(doIndexProjectItems(dbSession, itemsByProjectKey));
    return result;
  }

  private IndexingResult doIndexIssueItems(DbSession dbSession, ListMultimap<String, EsQueueDto> itemsByIssueKey) {
    if (itemsByIssueKey.isEmpty()) {
      return new IndexingResult();
    }
    IndexingListener listener = new OneToOneResilientIndexingListener(dbClient, dbSession, itemsByIssueKey.values());
    BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, listener);
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

  private IndexingResult doIndexProjectItems(DbSession dbSession, ListMultimap<String, EsQueueDto> itemsByProjectUuid) {
    if (itemsByProjectUuid.isEmpty()) {
      return new IndexingResult();
    }

    // one project, referenced by es_queue.doc_id = many issues
    IndexingListener listener = new OneToManyResilientIndexingListener(dbClient, dbSession, itemsByProjectUuid.values());
    BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, listener);
    bulkIndexer.start();

    for (String projectUuid : itemsByProjectUuid.keySet()) {
      // TODO support loading of multiple projects in a single SQL request
      try (IssueIterator issues = issueIteratorFactory.createForProject(projectUuid)) {
        if (issues.hasNext()) {
          do {
            IssueDoc doc = issues.next();
            bulkIndexer.add(newIndexRequest(doc));
          } while (issues.hasNext());
        } else {
          // project does not exist or has no issues. In both case
          // all the documents related to this project are deleted.
          addProjectDeletionToBulkIndexer(bulkIndexer, projectUuid);
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

    BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, IndexingListener.FAIL_ON_ERROR);
    bulkIndexer.start();
    issueKeys.forEach(issueKey -> bulkIndexer.addDeletion(TYPE_ISSUE.getMainType(), issueKey, AuthorizationDoc.idOf(projectUuid)));
    bulkIndexer.stop();
  }

  @VisibleForTesting
  protected void index(Iterator<IssueDoc> issues) {
    doIndex(issues, Size.LARGE, IndexingListener.FAIL_ON_ERROR);
  }

  private void doIndex(Iterator<IssueDoc> issues, Size size, IndexingListener listener) {
    BulkIndexer bulk = createBulkIndexer(size, listener);
    bulk.start();
    while (issues.hasNext()) {
      IssueDoc issue = issues.next();
      bulk.add(newIndexRequest(issue));
    }
    bulk.stop();
  }

  private IndexRequest newIndexRequest(IssueDoc issue) {
    return esClient.prepareIndex(TYPE_ISSUE.getMainType())
      .setId(issue.getId())
      .setRouting(issue.getRouting().orElseThrow(() -> new IllegalStateException("IssueDoc should define a routing")))
      .setSource(issue.getFields())
      .request();
  }

  private void addProjectDeletionToBulkIndexer(BulkIndexer bulkIndexer, String projectUuid) {
    SearchRequestBuilder search = esClient.prepareSearch(TYPE_ISSUE.getMainType())
      .setRouting(AuthorizationDoc.idOf(projectUuid))
      .setQuery(boolQuery().must(termQuery(FIELD_ISSUE_PROJECT_UUID, projectUuid)));
    bulkIndexer.addDeletion(search);
  }

  private static EsQueueDto createQueueDto(String docId, String docIdType, String projectUuid) {
    return EsQueueDto.create(TYPE_ISSUE.format(), docId, docIdType, projectUuid);
  }

  private BulkIndexer createBulkIndexer(Size size, IndexingListener listener) {
    return new BulkIndexer(esClient, TYPE_ISSUE, size, listener);
  }
}
