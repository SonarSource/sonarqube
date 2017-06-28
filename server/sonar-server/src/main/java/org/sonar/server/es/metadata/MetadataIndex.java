/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

  private final EsClient esClient;

  public MetadataIndex(EsClient esClient) {
    this.esClient = esClient;
  }

  public Optional<String> getHash(String index) {
    return getMetadata(hashId(index));
  }

  public void setHash(String index, String hash) {
    setMetadata(hash, hashId(index));
  }

  private static String hashId(String index) {
    return index + ".indexStructure";
  }

  public boolean getInitialized(IndexType indexType) {
    return getMetadata(initializedId(indexType)).map(Boolean::parseBoolean).orElse(false);
  }

  public void setInitialized(IndexType indexType, boolean initialized) {
    setMetadata(String.valueOf(initialized), initializedId(indexType));
  }

  private static String initializedId(IndexType indexType) {
    return indexType.getIndex() + "." + indexType.getType() + ".initialized";
  }

  private Optional<String> getMetadata(String id) {
    GetRequestBuilder request = esClient.prepareGet(MetadataIndexDefinition.INDEX_TYPE_METADATA, id).setFields(MetadataIndexDefinition.FIELD_VALUE);
    GetResponse response = request.get();
    if (response.isExists()) {
      GetField field = response.getField(MetadataIndexDefinition.FIELD_VALUE);
      String value = String.valueOf(field.getValue());
      return Optional.of(value);
    }
    return Optional.empty();
  }

  private void setMetadata(String hash, String id) {
    esClient.prepareIndex(MetadataIndexDefinition.INDEX_TYPE_METADATA)
      .setId(id)
      .setSource(MetadataIndexDefinition.FIELD_VALUE, hash)
      .setRefresh(REFRESH_IMMEDIATE) // ES 5: change to setRefreshPolicy
      .get();
  }
}
