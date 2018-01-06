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
package org.sonar.server.es.metadata;

import java.util.Optional;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.index.get.GetField;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexType;

import static org.sonar.server.es.DefaultIndexSettings.REFRESH_IMMEDIATE;

public class MetadataIndex {

  private static final String DB_VENDOR_KEY = "dbVendor";
  private static final String DB_SCHEMA_VERSION_KEY = "dbSchemaVersion";

  private final EsClient esClient;

  public MetadataIndex(EsClient esClient) {
    this.esClient = esClient;
  }

  public Optional<String> getHash(String index) {
    return getMetadata(hashId(index));
  }

  public void setHash(String index, String hash) {
    setMetadata(hashId(index), hash);
  }

  private static String hashId(String index) {
    return index + ".indexStructure";
  }

  public boolean getInitialized(IndexType indexType) {
    return getMetadata(initializedId(indexType)).map(Boolean::parseBoolean).orElse(false);
  }

  public void setInitialized(IndexType indexType, boolean initialized) {
    setMetadata(initializedId(indexType), String.valueOf(initialized));
  }

  private static String initializedId(IndexType indexType) {
    return indexType.getIndex() + "." + indexType.getType() + ".initialized";
  }

  public Optional<String> getDbVendor() {
    return getMetadata(DB_VENDOR_KEY);
  }

  public Optional<Long> getDbSchemaVersion() {
    return getMetadata(DB_SCHEMA_VERSION_KEY).map(Long::parseLong);
  }

  public void setDbMetadata(String vendor, long schemaVersion) {
    setMetadata(DB_VENDOR_KEY, vendor);
    setMetadata(DB_SCHEMA_VERSION_KEY, String.valueOf(schemaVersion));
  }

  private Optional<String> getMetadata(String id) {
    GetRequestBuilder request = esClient.prepareGet(MetadataIndexDefinition.INDEX_TYPE_METADATA, id)
      .setStoredFields(MetadataIndexDefinition.FIELD_VALUE);
    GetResponse response = request.get();
    if (response.isExists()) {
      GetField field = response.getField(MetadataIndexDefinition.FIELD_VALUE);
      String value = String.valueOf(field.getValue());
      return Optional.of(value);
    }
    return Optional.empty();
  }

  private void setMetadata(String id, String value) {
    esClient.prepareIndex(MetadataIndexDefinition.INDEX_TYPE_METADATA)
      .setId(id)
      .setSource(MetadataIndexDefinition.FIELD_VALUE, value)
      .setRefreshPolicy(REFRESH_IMMEDIATE)
      .get();
  }
}
