/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.view.index;

import java.util.List;
import java.util.Map;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.UuidWithProjectUuidDto;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;
import org.sonar.server.issue.index.IssueIndex;

import static com.google.common.collect.Maps.newHashMap;

public class ViewIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public ViewIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 300, ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW, "updatedAt");
    this.dbClient = dbClient;
  }

  /**
   * Index all views if the index is empty (Only used on startup).
   * It's currently not possible to index only data from db that are not existing in the index because we have no way to last when the structure of a view is changed :
   * - Either the definition has changed -> No updated at column in the projects table,
   * - Either the view is defined by a regex -> A new analysed project automatically steps into the view.
   * <p/>
   * The views lookup cache will not be cleared
   */
  @Override
  protected long doIndex(long lastUpdatedAt) {
    long count = esClient.prepareCount(ViewIndexDefinition.INDEX).setTypes(ViewIndexDefinition.TYPE_VIEW).get().getCount();
    if (count == 0) {
      DbSession dbSession = dbClient.openSession(false);
      try {
        Map<String, String> viewAndProjectViewUuidMap = newHashMap();
        for (UuidWithProjectUuidDto uuidWithProjectUuidDto : dbClient.componentDao().selectAllViewsAndSubViews(dbSession)) {
          viewAndProjectViewUuidMap.put(uuidWithProjectUuidDto.getUuid(), uuidWithProjectUuidDto.getProjectUuid());
        }
        index(dbSession, viewAndProjectViewUuidMap, false);
      } finally {
        dbSession.close();
      }
    }
    return 0L;
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
      index(dbSession, viewAndProjectViewUuidMap, true);
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
    final BulkIndexer bulk = new BulkIndexer(esClient, ViewIndexDefinition.INDEX);
    bulk.start();
    doIndex(bulk, viewDoc, true);
    bulk.stop();
  }

  private void index(DbSession dbSession, Map<String, String> viewAndProjectViewUuidMap, boolean needClearCache) {
    final BulkIndexer bulk = new BulkIndexer(esClient, ViewIndexDefinition.INDEX);
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
    bulk.add(newUpsertRequest(viewDoc));
    if (needClearCache) {
      clearLookupCache(viewDoc.uuid());
    }
  }

  private static UpdateRequest newUpsertRequest(ViewDoc doc) {
    return new UpdateRequest(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW, doc.uuid())
      .doc(doc.getFields())
      .upsert(doc.getFields());
  }

  private void clearLookupCache(String viewUuid) {
    try {
      esClient.prepareClearCache()
        .setFilterCache(true)
        .setFilterKeys(IssueIndex.viewsLookupCacheKey(viewUuid))
        .get();
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Unable to clear lookup cache of view '%s'", viewUuid), e);
    }
  }

}
