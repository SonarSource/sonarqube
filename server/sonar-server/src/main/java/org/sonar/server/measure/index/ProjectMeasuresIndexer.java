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

import java.util.Date;
import java.util.Iterator;
import javax.annotation.Nullable;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator.ProjectMeasures;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.permission.index.AuthorizationScope;
import org.sonar.server.permission.index.NeedAuthorizationIndexer;

import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURE;

public class ProjectMeasuresIndexer extends BaseIndexer implements ProjectIndexer, NeedAuthorizationIndexer {

  private static final AuthorizationScope AUTHORIZATION_SCOPE = new AuthorizationScope(INDEX_PROJECT_MEASURES, project -> Qualifiers.PROJECT.equals(project.getQualifier()));

  private final DbClient dbClient;

  public ProjectMeasuresIndexer(System2 system2, DbClient dbClient, EsClient esClient) {
    super(system2, esClient, 300, INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE, FIELD_ANALYSED_AT);
    this.dbClient = dbClient;
  }

  @Override
  public AuthorizationScope getAuthorizationScope() {
    return AUTHORIZATION_SCOPE;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return doIndex(createBulkIndexer(false), lastUpdatedAt, null);
  }

  @Override
  public void indexProject(String projectUuid, Cause cause) {
    switch (cause) {
      case PROJECT_KEY_UPDATE:
        // project must be re-indexed because key is used in this index
      case PROJECT_CREATION:
        // provisioned projects are supported by WS api/components/search_projects
      case NEW_ANALYSIS:
        doIndex(createBulkIndexer(false), 0L, projectUuid);
        break;
      default:
        // defensive case
        throw new IllegalStateException("Unsupported cause: " + cause);
    }
  }

  @Override
  public void deleteProject(String uuid) {
    esClient
      .prepareDelete(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE, uuid)
      .setRouting(uuid)
      .setRefresh(true)
      .get();
  }

  private long doIndex(BulkIndexer bulk, long lastUpdatedAt, @Nullable String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false);
      ProjectMeasuresIndexerIterator rowIt = ProjectMeasuresIndexerIterator.create(dbSession, lastUpdatedAt, projectUuid)) {
      return doIndex(bulk, rowIt);
    }
  }

  private static long doIndex(BulkIndexer bulk, Iterator<ProjectMeasures> docs) {
    bulk.start();
    long maxDate = 0L;
    while (docs.hasNext()) {
      ProjectMeasures doc = docs.next();
      bulk.add(newIndexRequest(toProjectMeasuresDoc(doc)));

      Long analysisDate = doc.getProject().getAnalysisDate();
      // it's more efficient to sort programmatically than in SQL on some databases (MySQL for instance)
      maxDate = Math.max(maxDate, analysisDate == null ? 0L : analysisDate);
    }
    bulk.stop();
    return maxDate;
  }

  private BulkIndexer createBulkIndexer(boolean large) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_PROJECT_MEASURES);
    bulk.setLarge(large);
    return bulk;
  }

  private static IndexRequest newIndexRequest(ProjectMeasuresDoc doc) {
    String projectUuid = doc.getId();
    return new IndexRequest(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURE, projectUuid)
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
      .setQualityGate(projectMeasures.getMeasures().getQualityGateStatus())
      .setAnalysedAt(analysisDate == null ? null : new Date(analysisDate))
      .setMeasuresFromMap(projectMeasures.getMeasures().getNumericMeasures());
  }
}
