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
package org.sonar.server.view.index;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.UuidWithProjectUuidDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.StartupIndexer;

import static com.google.common.collect.Maps.newHashMap;
import static org.sonar.server.view.index.ViewIndexDefinition.INDEX_TYPE_VIEW;

public class ViewIndexer implements StartupIndexer {

  private final DbClient dbClient;
  private final EsClient esClient;

  public ViewIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(INDEX_TYPE_VIEW);
  }

  @Override
  public void indexOnStartup(Set<IndexType> emptyIndexTypes) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, String> viewAndProjectViewUuidMap = newHashMap();
      for (UuidWithProjectUuidDto uuidWithProjectUuidDto : dbClient.componentDao().selectAllViewsAndSubViews(dbSession)) {
        viewAndProjectViewUuidMap.put(uuidWithProjectUuidDto.getUuid(), uuidWithProjectUuidDto.getProjectUuid());
      }
      index(dbSession, viewAndProjectViewUuidMap, false, Size.LARGE);
    }
  }

  /**
   * Index a root view : it will load projects on each sub views and index it.
   * Used by the compute engine to reindex a root view.
   * <p/>
   * The views lookup cache will be cleared
   */
  public void index(String rootViewUuid) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Map<String, String> viewAndProjectViewUuidMap = newHashMap();
      for (ComponentDto viewOrSubView : dbClient.componentDao().selectEnabledDescendantModules(dbSession, rootViewUuid)) {
        viewAndProjectViewUuidMap.put(viewOrSubView.uuid(), viewOrSubView.projectUuid());
      }
      index(dbSession, viewAndProjectViewUuidMap, true, Size.REGULAR);
    } finally {
      dbSession.close();
    }
  }

  /**
   * Index a single document.
   * <p/>
   * The views lookup cache will be cleared
   */
  public void index(ViewDoc viewDoc) {
    BulkIndexer bulk = new BulkIndexer(esClient, ViewIndexDefinition.INDEX_TYPE_VIEW.getIndex(), Size.REGULAR);
    bulk.start();
    doIndex(bulk, viewDoc, true);
    bulk.stop();
  }

  private void index(DbSession dbSession, Map<String, String> viewAndProjectViewUuidMap, boolean needClearCache, Size bulkSize) {
    BulkIndexer bulk = new BulkIndexer(esClient, ViewIndexDefinition.INDEX_TYPE_VIEW.getIndex(), bulkSize);
    bulk.start();
    for (Map.Entry<String, String> entry : viewAndProjectViewUuidMap.entrySet()) {
      String viewUuid = entry.getKey();
      List<String> projects = dbClient.componentDao().selectProjectsFromView(dbSession, viewUuid, entry.getValue());
      doIndex(bulk, new ViewDoc()
        .setUuid(viewUuid)
        .setProjects(projects), needClearCache);
    }
    bulk.stop();
  }

  private void doIndex(BulkIndexer bulk, ViewDoc viewDoc, boolean needClearCache) {
    bulk.add(newIndexRequest(viewDoc));
    if (needClearCache) {
      clearLookupCache(viewDoc.uuid());
    }
  }

  private static IndexRequest newIndexRequest(ViewDoc doc) {
    return new IndexRequest(ViewIndexDefinition.INDEX_TYPE_VIEW.getIndex(), ViewIndexDefinition.INDEX_TYPE_VIEW.getType(), doc.uuid())
      .source(doc.getFields());
  }

  private void clearLookupCache(String viewUuid) {
    try {
      esClient.prepareClearCache()
        .setQueryCache(true)
        .get();
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Unable to clear lookup cache of view '%s'", viewUuid), e);
    }
  }

}
