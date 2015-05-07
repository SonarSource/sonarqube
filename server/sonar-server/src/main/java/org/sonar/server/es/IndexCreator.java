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
package org.sonar.server.es;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.picocontainer.Startable;
import org.sonar.api.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.Map;

/**
 * Creates/deletes all indices in Elasticsearch during server startup.
 */
@ServerSide
public class IndexCreator implements Startable {

  private static final Logger LOGGER = Loggers.get(IndexCreator.class);

  /**
   * Internal setting stored on index to know its version. It's used to re-create index
   * when something changed between versions.
   */
  private static final String SETTING_HASH = "sonar_hash";

  private final EsClient client;
  private final IndexDefinitions definitions;

  public IndexCreator(EsClient client, IndexDefinitions definitions) {
    this.client = client;
    this.definitions = definitions;
  }

  @Override
  public void start() {
    // create indices that do not exist or that have a new definition (different mapping, cluster enabled, ...)
    for (IndexDefinitions.Index index : definitions.getIndices().values()) {
      boolean exists = client.prepareIndicesExist(index.getName()).get().isExists();
      if (exists && needsToDeleteIndex(index)) {
        LOGGER.info(String.format("Delete index %s (settings changed)", index.getName()));
        deleteIndex(index.getName());
        exists = false;
      }
      if (!exists) {
        createIndex(index);
      }
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private void createIndex(IndexDefinitions.Index index) {
    LOGGER.info(String.format("Create index %s", index.getName()));
    ImmutableSettings.Builder settings = ImmutableSettings.builder();
    settings.put(index.getSettings());
    settings.put(SETTING_HASH, new IndexDefinitionHash().of(index));
    CreateIndexResponse indexResponse = client
      .prepareCreate(index.getName())
      .setSettings(settings)
      .get();
    if (!indexResponse.isAcknowledged()) {
      throw new IllegalStateException("Failed to create index " + index.getName());
    }
    client.waitForStatus(ClusterHealthStatus.YELLOW);

    // create types
    for (Map.Entry<String, IndexDefinitions.IndexType> entry : index.getTypes().entrySet()) {
      LOGGER.info(String.format("Create type %s/%s", index.getName(), entry.getKey()));
      PutMappingResponse mappingResponse = client.preparePutMapping(index.getName())
        .setType(entry.getKey())
        .setIgnoreConflicts(false)
        .setSource(entry.getValue().getAttributes())
        .get();
      if (!mappingResponse.isAcknowledged()) {
        throw new IllegalStateException("Failed to create type " + entry.getKey());
      }
    }
    client.waitForStatus(ClusterHealthStatus.YELLOW);
  }

  private void deleteIndex(String indexName) {
    client.nativeClient().admin().indices().prepareDelete(indexName).get();
  }

  private boolean needsToDeleteIndex(IndexDefinitions.Index index) {
    boolean toBeDeleted = false;
    String hash = client.nativeClient().admin().indices().prepareGetSettings(index.getName()).get().getSetting(index.getName(), "index." + SETTING_HASH);
    if (hash != null) {
      String defHash = new IndexDefinitionHash().of(index);
      toBeDeleted = !StringUtils.equals(hash, defHash);
    }
    return toBeDeleted;
  }
}
