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

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;

public class MigrationEsClientImpl implements MigrationEsClient {
  private final EsClient client;
  private final Set<String> updatedIndices = new HashSet<>();

  public MigrationEsClientImpl(EsClient client) {
    this.client = client;
  }

  @Override
  public void deleteIndexes(String name, String... otherNames) {
    String[] indices = client.getIndex(new GetIndexRequest("_all")).getIndices();
    Set<String> existingIndices = Arrays.stream(indices).collect(MoreCollectors.toSet());
    Stream.concat(Stream.of(name), Arrays.stream(otherNames))
      .distinct()
      .filter(existingIndices::contains)
      .forEach(this::deleteIndex);
  }

  @Override
  public void addMappingToExistingIndex(String index, String type, String mappingName, String mappingType, Map<String, String> options) {
    String[] indices = client.getIndex(new GetIndexRequest(index)).getIndices();
    if (indices != null && indices.length == 1) {
      Loggers.get(getClass()).info("Add mapping [{}] to Elasticsearch index [{}]", mappingName, index);
      String mappingOptions = Stream.concat(Stream.of(Maps.immutableEntry("type", mappingType)), options.entrySet().stream())
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining(","));
      client.putMapping(new PutMappingRequest(index)
        .type(type)
        .source(mappingName, mappingOptions));
      updatedIndices.add(index);
    } else {
      throw new IllegalStateException("Expected only one index to be found, actual [" + String.join(",", indices) + "]");
    }
  }

  @Override
  public Set<String> getUpdatedIndices() {
    return updatedIndices;
  }

  private void deleteIndex(String index) {
    Loggers.get(getClass()).info("Drop Elasticsearch index [{}]", index);
    client.deleteIndex(new DeleteIndexRequest(index));
  }
}
