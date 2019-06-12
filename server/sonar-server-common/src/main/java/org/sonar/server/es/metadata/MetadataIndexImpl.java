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
package org.sonar.server.es.metadata;

import java.util.Optional;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.document.DocumentField;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexMainType;
import org.sonar.server.es.IndexType.IndexRelationType;

import static org.sonar.server.es.metadata.MetadataIndexDefinition.TYPE_METADATA;
import static org.sonar.server.es.newindex.DefaultIndexSettings.REFRESH_IMMEDIATE;

public class MetadataIndexImpl implements MetadataIndex {

  private static final String DB_VENDOR_KEY = "dbVendor";

  private final EsClient esClient;

  public MetadataIndexImpl(EsClient esClient) {
    this.esClient = esClient;
  }

  @Override
  public Optional<String> getHash(Index index) {
    return getMetadata(hashId(index));
  }

  @Override
  public void setHash(Index index, String hash) {
    setMetadata(hashId(index), hash);
  }

  private static String hashId(Index index) {
    return index.getName() + ".indexStructure";
  }

  @Override
  public boolean getInitialized(IndexType indexType) {
    return getMetadata(initializedId(indexType)).map(Boolean::parseBoolean).orElse(false);
  }

  @Override
  public void setInitialized(IndexType indexType, boolean initialized) {
    setMetadata(initializedId(indexType), String.valueOf(initialized));
  }

  private static String initializedId(IndexType indexType) {
    if (indexType instanceof IndexMainType) {
      IndexMainType mainType = (IndexMainType) indexType;
      return mainType.getIndex().getName() + "." + mainType.getType() + ".initialized";
    }
    if (indexType instanceof IndexRelationType) {
      IndexRelationType relationType = (IndexRelationType) indexType;
      IndexMainType mainType = relationType.getMainType();
      return mainType.getIndex().getName() + "." + mainType.getType() + "." + relationType.getName() + ".initialized";
    }
    throw new IllegalArgumentException("Unsupported IndexType " + indexType.getClass());
  }

  @Override
  public Optional<String> getDbVendor() {
    return getMetadata(DB_VENDOR_KEY);
  }

  @Override
  public void setDbMetadata(String vendor) {
    setMetadata(DB_VENDOR_KEY, vendor);
  }

  private Optional<String> getMetadata(String id) {
    GetRequestBuilder request = esClient.prepareGet(TYPE_METADATA, id)
      .setStoredFields(MetadataIndexDefinition.FIELD_VALUE);
    GetResponse response = request.get();
    if (response.isExists()) {
      DocumentField field = response.getField(MetadataIndexDefinition.FIELD_VALUE);
      return Optional.of(field.getValue());
    }
    return Optional.empty();
  }

  private void setMetadata(String id, String value) {
    esClient.prepareIndex(TYPE_METADATA)
      .setId(id)
      .setSource(MetadataIndexDefinition.FIELD_VALUE, value)
      .setRefreshPolicy(REFRESH_IMMEDIATE)
      .get();
  }
}
