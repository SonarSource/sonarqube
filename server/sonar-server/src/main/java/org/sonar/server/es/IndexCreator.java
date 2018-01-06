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
package org.sonar.server.es;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.es.IndexDefinitions.Index;
import org.sonar.server.es.metadata.EsDbCompatibility;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexDefinition;

/**
 * Creates/deletes all indices in Elasticsearch during server startup.
 */
@ServerSide
public class IndexCreator implements Startable {

  private static final Logger LOGGER = Loggers.get(IndexCreator.class);
  private static final String PROPERY_DISABLE_CHECK = "sonar.search.disableDropOnDbMigration";

  private final MetadataIndexDefinition metadataIndexDefinition;
  private final MetadataIndex metadataIndex;
  private final EsClient client;
  private final IndexDefinitions definitions;
  private final EsDbCompatibility esDbCompatibility;
  private final Configuration configuration;

  public IndexCreator(EsClient client, IndexDefinitions definitions, MetadataIndexDefinition metadataIndexDefinition,
    MetadataIndex metadataIndex, EsDbCompatibility esDbCompatibility, Configuration configuration) {
    this.client = client;
    this.definitions = definitions;
    this.metadataIndexDefinition = metadataIndexDefinition;
    this.metadataIndex = metadataIndex;
    this.esDbCompatibility = esDbCompatibility;
    this.configuration = configuration;
  }

  @Override
  public void start() {
    // create the "metadata" index first
    if (!client.prepareIndicesExist(MetadataIndexDefinition.INDEX_TYPE_METADATA.getIndex()).get().isExists()) {
      IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();
      metadataIndexDefinition.define(context);
      NewIndex index = context.getIndices().values().iterator().next();
      createIndex(new Index(index), false);
    }

    checkDbCompatibility();

    // create indices that do not exist or that have a new definition (different mapping, cluster enabled, ...)
    for (Index index : definitions.getIndices().values()) {
      boolean exists = client.prepareIndicesExist(index.getName()).get().isExists();
      if (exists && !index.getName().equals(MetadataIndexDefinition.INDEX_TYPE_METADATA.getIndex()) && hasDefinitionChange(index)) {
        LOGGER.info("Delete Elasticsearch index {} (structure changed)", index.getName());
        deleteIndex(index.getName());
        exists = false;
      }
      if (!exists) {
        createIndex(index, true);
      }
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private void createIndex(Index index, boolean useMetadata) {
    LOGGER.info(String.format("Create index %s", index.getName()));
    Settings.Builder settings = Settings.builder();
    settings.put(index.getSettings());
    if (useMetadata) {
      metadataIndex.setHash(index.getName(), IndexDefinitionHash.of(index));
      for (IndexDefinitions.IndexType type : index.getTypes().values()) {
        metadataIndex.setInitialized(new IndexType(index.getName(), type.getName()), false);
      }
    }
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

  private boolean hasDefinitionChange(Index index) {
    return metadataIndex.getHash(index.getName())
      .map(hash -> {
        String defHash = IndexDefinitionHash.of(index);
        return !StringUtils.equals(hash, defHash);
      }).orElse(true);
  }

  private void checkDbCompatibility() {
    boolean disabledCheck = configuration.getBoolean(PROPERY_DISABLE_CHECK).orElse(false);
    if (disabledCheck) {
      LOGGER.warn("Automatic drop of search indices in turned off (see property " + PROPERY_DISABLE_CHECK + ")");
    }

    List<String> existingIndices = loadExistingIndicesExceptMetadata();
    if (!disabledCheck && !existingIndices.isEmpty()) {
      boolean delete = false;
      if (!esDbCompatibility.hasSameDbVendor()) {
        LOGGER.info("Delete Elasticsearch indices (DB vendor changed)");
        delete = true;
      } else if (!esDbCompatibility.hasSameDbSchemaVersion()) {
        LOGGER.info("Delete Elasticsearch indices (DB schema changed)");
        delete = true;
      }
      if (delete) {
        existingIndices.forEach(this::deleteIndex);
      }
    }
    esDbCompatibility.markAsCompatible();
  }

  private List<String> loadExistingIndicesExceptMetadata() {
    return Arrays.stream(client.nativeClient().admin().indices().prepareGetIndex().get().getIndices())
      .filter(index -> !MetadataIndexDefinition.INDEX_TYPE_METADATA.getIndex().equals(index))
      .collect(Collectors.toList());
  }
}
