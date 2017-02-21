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

import org.sonar.api.config.Settings;

import static java.util.Arrays.stream;

public class IndexerStartupTask {

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
        .filter(this::doesIndexContainAtLeastOneEmptyType)
        .forEach(StartupIndexer::indexOnStartup);
    }
  }

  private boolean indexesAreEnabled() {
    return !settings.getBoolean("sonar.internal.es.disableIndexes");
  }

  private boolean doesIndexContainAtLeastOneEmptyType(StartupIndexer indexer) {
    return indexer.getIndexTypes().stream().filter(esClient::isEmpty).findAny().isPresent();
  }
}
