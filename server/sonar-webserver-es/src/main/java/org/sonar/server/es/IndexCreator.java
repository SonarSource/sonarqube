/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(IndexCreator.class);

  private final MetadataIndexDefinition metadataIndexDefinition;
  private final MetadataIndex metadataIndex;
  private final EsClient client;
  private final IndexDefinitions definitions;
  private final EsDbCompatibility esDbCompatibility;

  public IndexCreator(EsClient client, IndexDefinitions definitions, MetadataIndexDefinition metadataIndexDefinition,
    MetadataIndex metadataIndex, EsDbCompatibility esDbCompatibility) {
    this.client = client;
    this.definitions = definitions;
    this.metadataIndexDefinition = metadataIndexDefinition;
    this.metadataIndex = metadataIndex;
    this.esDbCompatibility = esDbCompatibility;
  }

  @Override
  public void start() {
    // create the "metadata" index first
    IndexType.IndexMainType metadataMainType = TYPE_METADATA;
    if (!client.indexExists(new GetIndexRequest(metadataMainType.getIndex().getName()))) {
      IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();
      metadataIndexDefinition.define(context);
      NewIndex index = context.getIndices().values().iterator().next();
      createIndex(index.build(), false);
    } else {
      ensureWritable(metadataMainType);
    }

    checkDbCompatibility(definitions.getIndices().values());

    // create indices that do not exist or that have a new definition (different mapping, cluster enabled, ...)
    definitions.getIndices().values().stream()
      .filter(i -> !i.getMainType().equals(metadataMainType))
      .forEach(index -> {
        boolean exists = client.indexExists(new GetIndexRequest(index.getMainType().getIndex().getName()));
        if (!exists) {
          createIndex(index, true);
        } else if (hasDefinitionChange(index)) {
          updateIndex(index);
        } else {
          ensureWritable(index.getMainType());
        }
      });
  }

  private void ensureWritable(IndexType.IndexMainType mainType) {
    if (isReadOnly(mainType)) {
      removeReadOnly(mainType);
    }
  }

  private boolean isReadOnly(IndexType.IndexMainType mainType) {
    String indexName = mainType.getIndex().getName();
    String readOnly = client.getSettings(new GetSettingsRequest().indices(indexName))
      .getSetting(indexName, "index.blocks.read_only_allow_delete");
    return "true".equalsIgnoreCase(readOnly);
  }

  private void removeReadOnly(IndexType.IndexMainType mainType) {
    LOGGER.info("Index [{}] is read-only. Making it writable...", mainType.getIndex().getName());

    String indexName = mainType.getIndex().getName();
    Settings.Builder builder = Settings.builder();
    builder.putNull("index.blocks.read_only_allow_delete");

    client.putSettings(new UpdateSettingsRequest().indices(indexName).settings(builder.build()));
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private void createIndex(BuiltIndex<?> builtIndex, boolean useMetadata) {
    Index index = builtIndex.getMainType().getIndex();
    LOGGER.info(String.format("Create index [%s]", index.getName()));
    Settings.Builder settings = Settings.builder();
    settings.put(builtIndex.getSettings());
    if (useMetadata) {
      metadataIndex.setHash(index, IndexDefinitionHash.of(builtIndex));
      metadataIndex.setInitialized(builtIndex.getMainType(), false);
      builtIndex.getRelationTypes().forEach(relationType -> metadataIndex.setInitialized(relationType, false));
    }
    CreateIndexResponse indexResponse = client.create(new CreateIndexRequest(index.getName()).settings((settings)));

    if (!indexResponse.isAcknowledged()) {
      throw new IllegalStateException("Failed to create index [" + index.getName() + "]");
    }
    client.waitForStatus(ClusterHealthStatus.YELLOW);

    LOGGER.info("Create mapping {}", builtIndex.getMainType().getIndex().getName());
    AcknowledgedResponse mappingResponse = client.putMapping(new PutMappingRequest(builtIndex.getMainType().getIndex().getName())
      .source(builtIndex.getAttributes()));

    if (!mappingResponse.isAcknowledged()) {
      throw new IllegalStateException("Failed to create mapping " + builtIndex.getMainType().getIndex().getName());
    }
    client.waitForStatus(ClusterHealthStatus.YELLOW);
  }

  private void deleteIndex(String indexName) {
    client.deleteIndex(new DeleteIndexRequest(indexName));
  }

  private void updateIndex(BuiltIndex<?> index) {
    String indexName = index.getMainType().getIndex().getName();

    LOGGER.info("Delete Elasticsearch index {} (structure changed)", indexName);
    deleteIndex(indexName);
    createIndex(index, true);
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
    return Arrays.stream(client.getIndex(new GetIndexRequest("_all")).getIndices())
      .filter(definedNames::contains)
      .filter(index -> !DESCRIPTOR.getName().equals(index))
      .toList();
  }
}
