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

import com.google.common.collect.Maps;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class ViewIndexer extends BaseIndexer {

  private final DbClient dbClient;
  private final EsClient esClient;

  public ViewIndexer(DbClient dbClient, EsClient esClient) {
    super(esClient, 300, ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW);
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    // Index only if index is empty
    long count = esClient.prepareCount(ViewIndexDefinition.INDEX).setTypes(ViewIndexDefinition.TYPE_VIEW).get().getCount();
    if (count == 0) {
      DbSession dbSession = dbClient.openSession(false);
      try {
        Map<String, String> viewAndProjectViewUuidMap = newHashMap();
        for (Map<String, String> viewsMap : dbClient.componentDao().selectAllViewsAndSubViews(dbSession)) {
          viewAndProjectViewUuidMap.put(viewsMap.get("uuid"), viewsMap.get("projectUuid"));
        }
        index(dbSession, viewAndProjectViewUuidMap);
      } finally {
        dbSession.close();
      }
    }
    return 0L;
  }

  public void index(String rootViewUuid) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Map<String, String> viewAndProjectViewUuidMap = newHashMap();
      for (ComponentDto viewOrSubView : dbClient.componentDao().selectModulesTree(dbSession, rootViewUuid)) {
        viewAndProjectViewUuidMap.put(viewOrSubView.uuid(), viewOrSubView.projectUuid());
      }
      index(dbSession, viewAndProjectViewUuidMap);
    } finally {
      dbSession.close();
    }
  }

  private void index(DbSession dbSession, Map<String, String> viewAndProjectViewUuidMap) {
    final BulkIndexer bulk = new BulkIndexer(esClient, ViewIndexDefinition.INDEX);
    bulk.start();
    for (Map.Entry<String, String> entry : viewAndProjectViewUuidMap.entrySet()) {
      doIndex(dbSession, bulk, entry.getKey(), entry.getValue());
    }
    bulk.stop();
  }

  private void doIndex(DbSession dbSession, BulkIndexer bulk, String uuid, String projectUuid) {
    List<String> projects = dbClient.componentDao().selectProjectsFromView(dbSession, uuid, projectUuid);
    bulk.add(newUpsertRequest(new ViewDoc(Maps.<String, Object>newHashMap())
      .setUuid(uuid)
      .setProjects(projects)));
  }

  private UpdateRequest newUpsertRequest(ViewDoc doc) {
    return new UpdateRequest(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW, doc.uuid())
      .doc(doc.getFields())
      .upsert(doc.getFields());
  }

}
