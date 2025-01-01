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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;

public class MigrationEsClientImpl implements MigrationEsClient {
  private static final Logger LOG = LoggerFactory.getLogger(MigrationEsClientImpl.class);
  private final EsClient client;

  public MigrationEsClientImpl(EsClient client) {
    this.client = client;
  }

  @Override
  public void deleteIndexes(String name, String... otherNames) {
    String[] indices = client.getIndex(new GetIndexRequest("_all")).getIndices();
    Set<String> existingIndices = Arrays.stream(indices).collect(Collectors.toSet());
    String[] toDelete = Stream.concat(Stream.of(name), Arrays.stream(otherNames))
      .distinct()
      .filter(existingIndices::contains)
      .toArray(String[]::new);
    if (toDelete.length > 0) {
      deleteIndex(toDelete);
    }
  }

  private void deleteIndex(String... indices) {
    LOG.info("Drop Elasticsearch indices [{}]", String.join(",", indices));
    client.deleteIndex(new DeleteIndexRequest(indices));
  }
}
