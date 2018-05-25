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
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.Client;
import org.sonar.server.platform.db.migration.es.ElasticsearchClient;

import static java.util.Objects.requireNonNull;

public class SimpleEsClientImpl implements ElasticsearchClient {
  private final Client nativeClient;

  public SimpleEsClientImpl(Client nativeClient) {
    this.nativeClient = requireNonNull(nativeClient);
  }

  /**
   * This method is reentrant and does not fail if indexName or otherIndexNames does not exist
   */
  public void deleteIndexes(String... indexNames) {
    if (indexNames.length == 0) {
      return;
    }

    GetMappingsResponse getMappingsResponse = nativeClient.admin().indices().prepareGetMappings("_all").get();
    String[] allIndexes = getMappingsResponse.mappings().keys().toArray(String.class);
    String[] intersection = intersection(indexNames, allIndexes);
    nativeClient.admin().indices().prepareDelete(intersection).get();
  }

  private String[] intersection(String[] a, String[] b) {
    return Arrays.stream(a)
      .distinct()
      .filter(x -> Arrays.stream(b).anyMatch(y -> y != null && y.equals(x)))
      .toArray(String[]::new);
  }
}
