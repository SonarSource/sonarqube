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

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;

public class MigrationEsClientImpl implements MigrationEsClient {
  private final EsClient client;

  public MigrationEsClientImpl(EsClient client) {
    this.client = client;
  }

  @Override
  public void deleteIndexes(String name, String... otherNames) {
    GetMappingsResponse mappings = client.nativeClient().admin().indices().prepareGetMappings("_all").get();
    Set<String> existingIndices = Sets.newHashSet(mappings.mappings().keysIt());
    Stream.concat(Stream.of(name), Arrays.stream(otherNames))
      .distinct()
      .filter(existingIndices::contains)
      .forEach(this::deleteIndex);
  }

  private void deleteIndex(String index) {
    Loggers.get(getClass()).info("Drop Elasticsearch index [{}]", index);
    client.nativeClient().admin().indices().prepareDelete(index).get();
  }
}
