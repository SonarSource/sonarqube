/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.server.es.metadata.EsDbCompatibility;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexDefinition;
import org.sonar.server.es.newindex.BuiltIndex;
import org.sonar.server.es.newindex.NewIndex;

import static org.apache.commons.lang3.Strings.CS;
import static org.sonar.server.es.metadata.MetadataIndexDefinition.DESCRIPTOR;
import static org.sonar.server.es.metadata.MetadataIndexDefinition.TYPE_METADATA;

/**
 * Creates/deletes all indices in Elasticsearch during server startup.
 */
@ServerSide
public class IndexCreator implements Startable {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndexCreator.class);
  public static final String INDEX_BLOCKS_READ_ONLY_ALLOW_DELETE = "index.blocks.read_only_allow_delete";

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
    if (!client.indexExistsV2(req -> req.index(metadataMainType.getIndex().getName()))) {
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
        boolean exists = client.indexExistsV2(req -> req.index(index.getMainType().getIndex().getName()));
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
    IndexSettings indexSettings = client.getSettingsV2(req -> req.index(indexName))
      .get(indexName)
      .settings();

    if (indexSettings == null || indexSettings.otherSettings() == null) {
      return false;
    }

    // Access the setting from the other settings map
    String readOnly = Optional.ofNullable(indexSettings.otherSettings()
        .get(INDEX_BLOCKS_READ_ONLY_ALLOW_DELETE))
      .map(jsonData -> jsonData.to(String.class))
      .orElse(null);

    return "true".equalsIgnoreCase(readOnly);
  }

  private void removeReadOnly(IndexType.IndexMainType mainType) {
    LOGGER.info("Index [{}] is read-only. Making it writable...", mainType.getIndex().getName());

    String indexName = mainType.getIndex().getName();

    // To remove a setting in ES, we need to set it to null using JsonData.of with a null String value
    // The new API requires us to build a proper null value
    client.putSettingsV2(req -> req
      .index(indexName)
      .settings(s -> s.otherSettings(Map.of(INDEX_BLOCKS_READ_ONLY_ALLOW_DELETE, JsonData.fromJson("null"))))
    );
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private void createIndex(BuiltIndex<?> builtIndex, boolean useMetadata) {
    Index index = builtIndex.getMainType().getIndex();
    LOGGER.info("Create index [{}]", index.getName());

    if (useMetadata) {
      metadataIndex.setHash(index, IndexDefinitionHash.of(builtIndex));
      metadataIndex.setInitialized(builtIndex.getMainType(), false);
      builtIndex.getRelationTypes().forEach(relationType -> metadataIndex.setInitialized(relationType, false));
    }

    CreateIndexResponse indexResponse = client.createIndexV2(cir -> cir
      .index(index.getName())
      .withJson(new StringReader("{\"settings\":" + builtIndex.getSettings().toString() + "}"))
    );

    if (!indexResponse.acknowledged()) {
      throw new IllegalStateException("Failed to create index [" + index.getName() + "]");
    }

    client.waitForStatus(ClusterHealthStatus.YELLOW);

    LOGGER.info("Create mapping {}", builtIndex.getMainType().getIndex().getName());

    PutMappingResponse putMappingResponse = client.putMappingV2(pmr -> {
        try {
          return pmr
            .index(builtIndex.getMainType().getIndex().getName())
            .withJson(new StringReader(
              new ObjectMapper().writeValueAsString(builtIndex.getAttributes())
            ));
        } catch (JsonProcessingException e) {
          throw new IllegalStateException("Cannot serialize mapping for index" + builtIndex.getMainType().getIndex().getName(), e);
        }
      }
    );

    if (!putMappingResponse.acknowledged()) {
      throw new IllegalStateException("Failed to create mapping " + builtIndex.getMainType().getIndex().getName());
    }
    client.waitForStatus(ClusterHealthStatus.YELLOW);
  }

  private void deleteIndex(String indexName) {
    client.deleteIndexV2(indexName);
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
        return !CS.equals(hash, defHash);
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
    return client.getIndexV2("_all").result().keySet()
      .stream()
      .filter(definedNames::contains)
      .filter(index -> !DESCRIPTOR.getName().equals(index))
      .toList();
  }
}
