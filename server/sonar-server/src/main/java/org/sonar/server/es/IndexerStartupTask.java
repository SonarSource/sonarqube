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
package org.sonar.server.es;

import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

public class IndexerStartupTask {

  private static final Logger LOG = Loggers.get(IndexerStartupTask.class);

  private final EsClient esClient;
  private final Settings settings;
  private final StartupIndexer[] indexers;

  public IndexerStartupTask(EsClient esClient, Settings settings, StartupIndexer... indexers) {
    this.esClient = esClient;
    this.settings = settings;
    this.indexers = indexers;
  }

  public void execute() {
    if (indexesAreEnabled()) {
      stream(indexers)
      .forEach(this::indexEmptyTypes);
    }
  }

  private boolean indexesAreEnabled() {
    return !settings.getBoolean("sonar.internal.es.disableIndexes");
  }

  private void indexEmptyTypes(StartupIndexer indexer) {
    Set<IndexType> emptyTypes = getEmptyTypes(indexer);
    if (!emptyTypes.isEmpty()) {
      log(indexer, emptyTypes);
      indexer.indexOnStartup(emptyTypes);
    }
  }

  private Set<IndexType> getEmptyTypes(StartupIndexer indexer) {
    return indexer.getIndexTypes().stream().filter(esClient::isEmpty).collect(toSet());
  }

  private void log(StartupIndexer indexer, Set<IndexType> emptyTypes) {
    String s = emptyTypes.size() == 1 ? "" : "s";
    String typeList = emptyTypes.stream().map(Object::toString).collect(Collectors.joining(","));
    String indexerName = indexer.getClass().getSimpleName();
    LOG.info("Full indexing of type{} {} using {}", s, typeList, indexerName);
  }
}
