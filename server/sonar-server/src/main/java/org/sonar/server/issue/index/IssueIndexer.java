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
package org.sonar.server.issue.index;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.sonar.api.resources.Qualifiers;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.INDEX_TYPE_ISSUE;

public class IssueIndexer implements ProjectIndexer, NeedAuthorizationIndexer, StartupIndexer {

  private static final String DELETE_ERROR_MESSAGE = "Fail to delete some issues of project [%s]";
  private static final int MAX_BATCH_SIZE = 1000;
  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(INDEX_TYPE_ISSUE, project -> Qualifiers.PROJECT.equals(project.getQualifier()));

  private final EsClient esClient;
  private final IssueIteratorFactory issueIteratorFactory;

  public IssueIndexer(EsClient esClient, IssueIteratorFactory issueIteratorFactory) {
    this.esClient = esClient;
    this.issueIteratorFactory = issueIteratorFactory;
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(INDEX_TYPE_ISSUE);
  }

  @Override
  public void indexOnStartup(Set<IndexType> emptyIndexTypes) {
    doIndex(createBulkIndexer(Size.LARGE), (String) null);
  }

  @Override
  public void indexProject(String projectUuid, Cause cause) {
    switch (cause) {
      case PROJECT_CREATION:
        // nothing to do, issues do not exist at project creation
      case PROJECT_KEY_UPDATE:
      case PROJECT_TAGS_UPDATE:
        // nothing to do, project key and tags are not used in this index
        break;
      case NEW_ANALYSIS:
        doIndex(createBulkIndexer(Size.REGULAR), projectUuid);
        break;
      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
  }

  /**
   * For benchmarks
   */
  public void index(Iterator<IssueDoc> issues) {
    doIndex(createBulkIndexer(Size.REGULAR), issues);
  }

  public void index(Collection<String> issueKeys) {
    doIndex(createBulkIndexer(Size.REGULAR), issueKeys);
  }

  private void doIndex(BulkIndexer bulk, Collection<String> issueKeys) {
    try (IssueIterator issues = issueIteratorFactory.createForIssueKeys(issueKeys)) {
      doIndex(bulk, issues);
    }
  }

  private void doIndex(BulkIndexer bulk, @Nullable String projectUuid) {
    try (IssueIterator issues = issueIteratorFactory.createForProject(projectUuid)) {
      doIndex(bulk, issues);
    }
  }

  private static void doIndex(BulkIndexer bulk, Iterator<IssueDoc> issues) {
    bulk.start();
    while (issues.hasNext()) {
      IssueDoc issue = issues.next();
      bulk.add(newIndexRequest(issue));
    }
    bulk.stop();
  }

  @Override
  public void deleteProject(String uuid) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_TYPE_ISSUE.getIndex(), Size.REGULAR);
    bulk.start();
    SearchRequestBuilder search = esClient.prepareSearch(INDEX_TYPE_ISSUE)
      .setRouting(uuid)
      .setQuery(boolQuery().must(termQuery(FIELD_ISSUE_PROJECT_UUID, uuid)));
    bulk.addDeletion(search);
    bulk.stop();
  }

  public void deleteByKeys(String projectUuid, List<String> issueKeys) {
    if (issueKeys.isEmpty()) {
      return;
    }

    int count = 0;
    BulkRequestBuilder builder = esClient.prepareBulk();
    for (String issueKey : issueKeys) {
      builder.add(esClient.prepareDelete(INDEX_TYPE_ISSUE, issueKey)
        .setRefresh(false)
        .setRouting(projectUuid));
      count++;
      if (count >= MAX_BATCH_SIZE) {
        EsUtils.executeBulkRequest(builder, DELETE_ERROR_MESSAGE, projectUuid);
        builder = esClient.prepareBulk();
        count = 0;
      }
    }
    EsUtils.executeBulkRequest(builder, DELETE_ERROR_MESSAGE, projectUuid);
    esClient.prepareRefresh(INDEX_TYPE_ISSUE.getIndex()).get();
  }

  private BulkIndexer createBulkIndexer(Size bulkSize) {
    return new BulkIndexer(esClient, INDEX_TYPE_ISSUE.getIndex(), bulkSize);
  }

  private static IndexRequest newIndexRequest(IssueDoc issue) {
    String projectUuid = issue.projectUuid();

    return new IndexRequest(INDEX_TYPE_ISSUE.getIndex(), INDEX_TYPE_ISSUE.getType(), issue.key())
      .routing(projectUuid)
      .parent(projectUuid)
      .source(issue.getFields());
  }
}
