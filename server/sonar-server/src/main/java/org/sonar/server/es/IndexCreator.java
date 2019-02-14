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
package org.sonar.server.es;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;
import org.sonar.server.es.metadata.EsDbCompatibility;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexDefinition;
import org.sonar.server.es.newindex.BuiltIndex;
import org.sonar.server.es.newindex.NewIndex;

import static org.sonar.server.es.metadata.MetadataIndexDefinition.DESCRIPTOR;
import static org.sonar.server.es.metadata.MetadataIndexDefinition.TYPE_METADATA;

/**
 * Creates/deletes all indices in Elasticsearch during server startup.
 */
@ServerSide
public class IndexCreator implements Startable {

  private static final Logger LOGGER = Loggers.get(IndexCreator.class);

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
    IndexType.IndexMainType metadataMainType = TYPE_METADATA;
    if (!client.prepareIndicesExist(metadataMainType.getIndex()).get().isExists()) {
      IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();
      metadataIndexDefinition.define(context);
      NewIndex index = context.getIndices().values().iterator().next();
      createIndex(index.build(), false);
    }

    checkDbCompatibility(definitions.getIndices().values());

    // create indices that do not exist or that have a new definition (different mapping, cluster enabled, ...)
    for (BuiltIndex<?> builtIndex : definitions.getIndices().values()) {
      Index index = builtIndex.getMainType().getIndex();
      String indexName = index.getName();
      boolean exists = client.prepareIndicesExist(index).get().isExists();
      if (exists && !builtIndex.getMainType().equals(metadataMainType) && hasDefinitionChange(builtIndex)) {
        verifyNotBlueGreenDeployment(indexName);
        LOGGER.info("Delete Elasticsearch index {} (structure changed)", indexName);
        deleteIndex(indexName);
        exists = false;
      }
      if (!exists) {
        createIndex(builtIndex, true);
      }
    }
  }

  private void verifyNotBlueGreenDeployment(String indexToBeDeleted) {
    if (configuration.getBoolean(ProcessProperties.Property.BLUE_GREEN_ENABLED.getKey()).orElse(false)) {
      throw new IllegalStateException("Blue/green deployment is not supported. Elasticsearch index [" + indexToBeDeleted + "] changed and needs to be dropped.");
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private void createIndex(BuiltIndex<?> builtIndex, boolean useMetadata) {
    Index index = builtIndex.getMainType().getIndex();
    LOGGER.info(String.format("Create index %s", index.getName()));
    Settings.Builder settings = Settings.builder();
    settings.put(builtIndex.getSettings());
    if (useMetadata) {
      metadataIndex.setHash(index, IndexDefinitionHash.of(builtIndex));
      metadataIndex.setInitialized(builtIndex.getMainType(), false);
      builtIndex.getRelationTypes().forEach(relationType -> metadataIndex.setInitialized(relationType, false));
    }
    CreateIndexResponse indexResponse = client
      .prepareCreate(index)
      .setSettings(settings)
      .get();
    if (!indexResponse.isAcknowledged()) {
      throw new IllegalStateException("Failed to create index " + index.getName());
    }
    client.waitForStatus(ClusterHealthStatus.YELLOW);

    // create types
    LOGGER.info("Create type {}", builtIndex.getMainType().format());
    AcknowledgedResponse mappingResponse = client.preparePutMapping(index)
      .setType(builtIndex.getMainType().getType())
      .setSource(builtIndex.getAttributes())
      .get();
    if (!mappingResponse.isAcknowledged()) {
      throw new IllegalStateException("Failed to create type " + builtIndex.getMainType().getType());
    }
    client.waitForStatus(ClusterHealthStatus.YELLOW);
  }

  private void deleteIndex(String indexName) {
    client.nativeClient().admin().indices().prepareDelete(indexName).get();
  }

  private boolean hasDefinitionChange(BuiltIndex<?> index) {
    return metadataIndex.getHash(index.getMainType().getIndex())
      .map(hash -> {
        String defHash = IndexDefinitionHash.of(index);
        return !StringUtils.equals(hash, defHash);
      }).orElse(true);
  }

  private void checkDbCompatibility(Collection<BuiltIndex> definitions) {
    List<String> existingIndices = loadExistingIndicesExceptMetadata(definitions);
    if (!existingIndices.isEmpty()) {
      boolean delete = false;
      if (!esDbCompatibility.hasSameDbVendor()) {
        LOGGER.info("Delete Elasticsearch indices (DB vendor changed)");
        delete = true;
      }
      if (delete) {
        existingIndices.forEach(this::deleteIndex);
      }
    }
    esDbCompatibility.markAsCompatible();
  }

  private List<String> loadExistingIndicesExceptMetadata(Collection<BuiltIndex> definitions) {
    Set<String> definedNames = definitions.stream()
      .map(t -> t.getMainType().getIndex().getName())
      .collect(Collectors.toSet());
    return Arrays.stream(client.nativeClient().admin().indices().prepareGetIndex().get().getIndices())
      .filter(definedNames::contains)
      .filter(index -> !DESCRIPTOR.getName().equals(index))
      .collect(Collectors.toList());
  }
}
