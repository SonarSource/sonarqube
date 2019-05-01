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

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
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
    Map<String, IndexStats> indices = client.nativeClient().admin().indices().prepareStats().get().getIndices();
    Set<String> existingIndices = indices.values().stream().map(IndexStats::getIndex).collect(MoreCollectors.toSet());
    Stream.concat(Stream.of(name), Arrays.stream(otherNames))
      .distinct()
      .filter(existingIndices::contains)
      .forEach(this::deleteIndex);
  }

  @Override
  public void addMappingToExistingIndex(String index, String type, String mappingName, String mappingType, Map<String, String> options) {
    IndexStats stats = client.nativeClient().admin().indices().prepareStats().get().getIndex(index);
    if (stats != null) {
      Loggers.get(getClass()).info("Add mapping [{}] to Elasticsearch index [{}]", mappingName, index);
      String mappingOptions = Stream.concat(Stream.of(Maps.immutableEntry("type", mappingType)), options.entrySet().stream())
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining(","));
      client.nativeClient().admin().indices().preparePutMapping(index)
        .setType(type)
        .setSource(mappingName, mappingOptions)
        .get();
      updatedIndices.add(index);
    }
  }

  @Override
  public Set<String> getUpdatedIndices() {
    return updatedIndices;
  }

  private void deleteIndex(String index) {
    Loggers.get(getClass()).info("Drop Elasticsearch index [{}]", index);
    client.nativeClient().admin().indices().prepareDelete(index).get();
  }
}
