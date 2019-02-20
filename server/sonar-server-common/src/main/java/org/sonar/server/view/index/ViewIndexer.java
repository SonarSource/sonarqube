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
package org.sonar.server.view.index;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.UuidWithProjectUuidDto;
import org.sonar.db.es.EsQueueDto;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.BulkIndexer.Size;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexingListener;
import org.sonar.server.es.IndexingResult;
import org.sonar.server.es.OneToOneResilientIndexingListener;
import org.sonar.server.es.ResilientIndexer;

import static com.google.common.collect.Maps.newHashMap;
import static org.sonar.core.util.stream.MoreCollectors.toHashSet;
import static org.sonar.server.view.index.ViewIndexDefinition.TYPE_VIEW;

public class ViewIndexer implements ResilientIndexer {

  private final DbClient dbClient;
  private final EsClient esClient;

  public ViewIndexer(DbClient dbClient, EsClient esClient) {
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  @Override
  public Set<IndexType> getIndexTypes() {
    return ImmutableSet.of(TYPE_VIEW);
  }

  @Override
  public void indexOnStartup(Set<IndexType> uninitializedIndexTypes) {
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
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, String> viewAndProjectViewUuidMap = newHashMap();
      for (ComponentDto viewOrSubView : dbClient.componentDao().selectEnabledDescendantModules(dbSession, rootViewUuid)) {
        viewAndProjectViewUuidMap.put(viewOrSubView.uuid(), viewOrSubView.projectUuid());
      }
      index(dbSession, viewAndProjectViewUuidMap, true, Size.REGULAR);
    }
  }

  /**
   * Index a single document.
   * <p/>
   * The views lookup cache will be cleared
   */
  public void index(ViewDoc viewDoc) {
    BulkIndexer bulk = new BulkIndexer(esClient, TYPE_VIEW, Size.REGULAR);
    bulk.start();
    doIndex(bulk, viewDoc, true);
    bulk.stop();
  }

  private void index(DbSession dbSession, Map<String, String> viewAndProjectViewUuidMap, boolean needClearCache, Size bulkSize) {
    BulkIndexer bulk = new BulkIndexer(esClient, TYPE_VIEW, bulkSize);
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
    IndexType.IndexMainType mainType = TYPE_VIEW;
    return new IndexRequest(mainType.getIndex().getName(), mainType.getType())
      .id(doc.getId())
      .routing(doc.getRouting().orElse(null))
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

  /**
   * This is based on the fact that a WebService is only calling {@link ViewIndexer#delete(DbSession, Collection)}
   * So the resiliency is only taking in account a deletion of view component
   * A safety check is done by not deleting any component that still exist in database.
   *
   * This should not occur but prevent any misuse on this resiliency
   */
  @Override
  public IndexingResult index(DbSession dbSession, Collection<EsQueueDto> items) {
    if (items.isEmpty()) {
      return new IndexingResult();
    }

    Set<String> views = items
      .stream()
      .map(EsQueueDto::getDocId)
      .collect(toHashSet(items.size()));

    BulkIndexer bulkIndexer = newBulkIndexer(Size.REGULAR, new OneToOneResilientIndexingListener(dbClient, dbSession, items));
    bulkIndexer.start();

    // Safety check to remove all views that may not have been deleted
    views.removeAll(dbClient.componentDao().selectExistingUuids(dbSession, views));
    views.forEach(v -> bulkIndexer.addDeletion(TYPE_VIEW, v));
    return bulkIndexer.stop();
  }

  public void delete(DbSession dbSession, Collection<String> viewUuids) {
    List<EsQueueDto> items = viewUuids.stream()
      .map(l -> EsQueueDto.create(TYPE_VIEW.format(), l))
      .collect(MoreCollectors.toArrayList());

    dbClient.esQueueDao().insert(dbSession, items);
    dbSession.commit();
    index(dbSession, items);
  }

  private BulkIndexer newBulkIndexer(Size bulkSize, IndexingListener listener) {
    return new BulkIndexer(esClient, TYPE_VIEW, bulkSize, listener);
  }
}
