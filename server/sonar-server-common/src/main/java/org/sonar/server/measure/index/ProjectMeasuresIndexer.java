/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.measure.index;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator.ProjectMeasures;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndexer implements ProjectIndexer, NeedAuthorizationIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(INDEX_TYPE_PROJECT_MEASURES, project -> Qualifiers.PROJECT.equals(project.getQualifier()));
  private static final ImmutableSet<IndexType> INDEX_TYPES = ImmutableSet.of(INDEX_TYPE_PROJECT_MEASURES);

  private final DbClient dbClient;
  private final EsClient esClient;

  public ProjectMeasuresIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return INDEX_TYPES;
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    doIndex(Size.LARGE, null);
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  public void indexOnAnalysis(String projectUuid) {
    doIndex(Size.REGULAR, projectUuid);
  }

  @Override
  public Collection<EsQueueDto> prepareForRecovery(DbSession dbSession, Collection<String> projectUuids, ProjectIndexer.Cause cause) {
    switch (cause) {
      case PERMISSION_CHANGE:
        // nothing to do, permissions are not used in type projectmeasures/projectmeasure
        return Collections.emptyList();
      case MEASURE_CHANGE:
      case PROJECT_KEY_UPDATE:
        // project must be re-indexed because key is used in this index
      case PROJECT_CREATION:
        // provisioned projects are supported by WS api/components/search_projects
      case PROJECT_TAGS_UPDATE:
      case PROJECT_DELETION:
        List<EsQueueDto> items = projectUuids.stream()
          .map(projectUuid -> EsQueueDto.create(INDEX_TYPE_PROJECT_MEASURES.format(), projectUuid, null, projectUuid))
          .collect(MoreCollectors.toArrayList(projectUuids.size()));
        return dbClient.esQueueDao().insert(dbSession, items);

      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
  }

  public IndexingResult commitAndIndex(DbSession dbSession, Collection<String> projectUuids) {
    List<EsQueueDto> items = projectUuids.stream()
      .map(projectUuid -> EsQueueDto.create(INDEX_TYPE_PROJECT_MEASURES.format(), projectUuid, null, projectUuid))
      .collect(MoreCollectors.toArrayList(projectUuids.size()));
    dbClient.esQueueDao().insert(dbSession, items);

    dbSession.commit();

    return index(dbSession, items);
  }

  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    if (items.isEmpty()) {
      return new IndexingResult();
    }
    OneToOneResilientIndexingListener listener = new OneToOneResilientIndexingListener(dbClient, dbSession, items);
    BulkIndexer bulkIndexer = createBulkIndexer(Size.REGULAR, listener);
    bulkIndexer.start();

    List<String> projectUuids = items.stream().map(EsQueueDto::getDocId).collect(MoreCollectors.toArrayList(items.size()));
    Iterator<String> it = projectUuids.iterator();
    while (it.hasNext()) {
      String projectUuid = it.next();
      try (ProjectMeasuresIndexerIterator rowIt = ProjectMeasuresIndexerIterator.create(dbSession, projectUuid)) {
        while (rowIt.hasNext()) {
          bulkIndexer.add(newIndexRequest(toProjectMeasuresDoc(rowIt.next())));
          it.remove();
        }
      }
    }

    // the remaining uuids reference projects that don't exist in db. They must
    // be deleted from index.
    projectUuids.forEach(projectUuid -> bulkIndexer.addDeletion(INDEX_TYPE_PROJECT_MEASURES, projectUuid, projectUuid));

    return bulkIndexer.stop();
  }

  private void doIndex(Size size, @Nullable String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false);
      ProjectMeasuresIndexerIterator rowIt = ProjectMeasuresIndexerIterator.create(dbSession, projectUuid)) {

      BulkIndexer bulkIndexer = createBulkIndexer(size, IndexingListener.FAIL_ON_ERROR);
      bulkIndexer.start();
      while (rowIt.hasNext()) {
        ProjectMeasures doc = rowIt.next();
        bulkIndexer.add(newIndexRequest(toProjectMeasuresDoc(doc)));
      }
      bulkIndexer.stop();
    }
  }

  private BulkIndexer createBulkIndexer(Size bulkSize, IndexingListener listener) {
    return new BulkIndexer(esClient, INDEX_TYPE_PROJECT_MEASURES, bulkSize, listener);
  }

  private static IndexRequest newIndexRequest(ProjectMeasuresDoc doc) {
    String projectUuid = doc.getId();
    return new IndexRequest(INDEX_TYPE_PROJECT_MEASURES.getIndex(), INDEX_TYPE_PROJECT_MEASURES.getType(), projectUuid)
      .routing(projectUuid)
      .parent(projectUuid)
      .source(doc.getFields());
  }

  private static ProjectMeasuresDoc toProjectMeasuresDoc(ProjectMeasures projectMeasures) {
    ProjectMeasuresIndexerIterator.Project project = projectMeasures.getProject();
    Long analysisDate = project.getAnalysisDate();
    return new ProjectMeasuresDoc()
      .setId(project.getUuid())
      .setOrganizationUuid(project.getOrganizationUuid())
      .setKey(project.getKey())
      .setName(project.getName())
      .setQualityGateStatus(projectMeasures.getMeasures().getQualityGateStatus())
      .setTags(project.getTags())
      .setAnalysedAt(analysisDate == null ? null : new Date(analysisDate))
      .setMeasuresFromMap(projectMeasures.getMeasures().getNumericMeasures())
      .setLanguages(new ArrayList<>(projectMeasures.getMeasures().getNclocByLanguages().keySet()))
      .setNclocLanguageDistributionFromMap(projectMeasures.getMeasures().getNclocByLanguages());
  }
}
