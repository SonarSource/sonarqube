/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.component.es;

import java.util.Date;
import java.util.Iterator;
import javax.annotation.Nullable;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator;
import org.sonar.db.measure.ProjectMeasuresIndexerIterator.ProjectMeasures;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.INDEX_PROJECT_MEASURES;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.TYPE_AUTHORIZATION;
import static org.sonar.server.component.es.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;

public class ProjectMeasuresIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public ProjectMeasuresIndexer(System2 system2, DbClient dbClient, EsClient esClient) {
    super(system2, esClient, 300, INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES, FIELD_ANALYSED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return doIndex(createBulkIndexer(false), lastUpdatedAt, null);
  }

  public void index(String projectUuid) {
    doIndex(createBulkIndexer(false), 0L, projectUuid);
  }

  public void deleteProject(String uuid) {
    esClient
      .prepareDelete(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES, uuid)
      .setRouting(uuid)
      .setRefresh(true)
      .get();
    esClient
      .prepareDelete(INDEX_PROJECT_MEASURES, TYPE_AUTHORIZATION, uuid)
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
    return new IndexRequest(INDEX_PROJECT_MEASURES, TYPE_PROJECT_MEASURES, projectUuid)
      .routing(projectUuid)
      .parent(projectUuid)
      .source(doc.getFields());
  }

  private static ProjectMeasuresDoc toProjectMeasuresDoc(ProjectMeasures projectMeasures) {
    Long analysisDate = projectMeasures.getProject().getAnalysisDate();
    return new ProjectMeasuresDoc()
      .setId(projectMeasures.getProject().getUuid())
      .setKey(projectMeasures.getProject().getKey())
      .setName(projectMeasures.getProject().getName())
      .setQualityGate(projectMeasures.getMeasures().getQualityGateStatus())
      .setAnalysedAt(analysisDate == null ? null : new Date(analysisDate))
      .setMeasuresFromMap(projectMeasures.getMeasures().getNumericMeasures());
  }
}
