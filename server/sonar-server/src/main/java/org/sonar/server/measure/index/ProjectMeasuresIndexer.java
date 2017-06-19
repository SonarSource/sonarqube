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
package org.sonar.server.measure.index;

import com.google.common.collect.ImmutableSet;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator.ProjectMeasures;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndexer implements ProjectIndexer, NeedAuthorizationIndexer, StartupIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(INDEX_TYPE_PROJECT_MEASURES, project -> Qualifiers.PROJECT.equals(project.getQualifier()));

  private final DbClient dbClient;
  private final EsClient esClient;

  public ProjectMeasuresIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(INDEX_TYPE_PROJECT_MEASURES);
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
    doIndex(createBulkIndexer(Size.LARGE), (String) null);
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  public void indexProject(String projectUuid, Cause cause) {
    switch (cause) {
      case PROJECT_KEY_UPDATE:
        // project must be re-indexed because key is used in this index
      case PROJECT_CREATION:
        // provisioned projects are supported by WS api/components/search_projects
      case NEW_ANALYSIS:
      case PROJECT_TAGS_UPDATE:
        doIndex(createBulkIndexer(Size.REGULAR), projectUuid);
        break;
      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
  }

  @Override
  public void deleteProject(String uuid) {
    esClient
      .prepareDelete(INDEX_TYPE_PROJECT_MEASURES, uuid)
      .setRouting(uuid)
      .setRefresh(true)
      .get();
  }

  private void doIndex(BulkIndexer bulk, @Nullable String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false);
      ProjectMeasuresIndexerIterator rowIt = ProjectMeasuresIndexerIterator.create(dbSession, projectUuid)) {
      doIndex(bulk, rowIt);
    }
  }

  private static void doIndex(BulkIndexer bulk, Iterator<ProjectMeasures> docs) {
    bulk.start();
    while (docs.hasNext()) {
      ProjectMeasures doc = docs.next();
      bulk.add(newIndexRequest(toProjectMeasuresDoc(doc)));
    }
    bulk.stop();
  }

  private BulkIndexer createBulkIndexer(Size bulkSize) {
    return new BulkIndexer(esClient, INDEX_TYPE_PROJECT_MEASURES.getIndex(), bulkSize);
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
      .setLanguages(projectMeasures.getMeasures().getLanguages());
  }
}
