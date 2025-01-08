/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.server.es.metadata.MetadataIndex;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

public class IndexerStartupTask {

  private static final Logger LOG = Loggers.get(IndexerStartupTask.class);

  private final EsClient esClient;
  private final Configuration config;
  private final MetadataIndex metadataIndex;
  private final StartupIndexer[] indexers;

  public IndexerStartupTask(EsClient esClient, Configuration config, MetadataIndex metadataIndex, StartupIndexer... indexers) {
    this.esClient = esClient;
    this.config = config;
    this.metadataIndex = metadataIndex;
    this.indexers = indexers;
  }

  public void execute() {
    if (indexesAreEnabled()) {
      stream(indexers)
        .forEach(this::indexUninitializedTypes);
    }
  }

  private boolean indexesAreEnabled() {
    return !config.getBoolean("sonar.internal.es.disableIndexes").orElse(false);
  }

  private void indexUninitializedTypes(StartupIndexer indexer) {
    Set<IndexType> uninitializedTypes = getUninitializedTypes(indexer);
    if (!uninitializedTypes.isEmpty()) {
      Profiler profiler = Profiler.create(LOG);
      StartupIndexer.Type type = indexer.getType();
      switch (type) {
        case SYNCHRONOUS:
          synchronousIndexing(indexer, uninitializedTypes, profiler);
          break;
        case ASYNCHRONOUS:
          asynchronousIndexing(indexer, uninitializedTypes, profiler);
          break;
        default:
          throw new IllegalArgumentException("Unsupported StartupIndexer type:" + type);
      }
    }
  }

  private void synchronousIndexing(StartupIndexer indexer, Set<IndexType> uninitializedTypes, Profiler profiler) {
    String logMessage = getSynchronousIndexingLogMessage(uninitializedTypes);

    profiler.startInfo(logMessage + "...");
    indexer.indexOnStartup(uninitializedTypes);
    uninitializedTypes.forEach(this::setInitialized);
    profiler.stopInfo(logMessage + " done");
  }

  private void asynchronousIndexing(StartupIndexer indexer, Set<IndexType> uninitializedTypes, Profiler profiler) {
    String logMessage = getAsynchronousIndexingLogMessage(uninitializedTypes);

    profiler.startInfo(logMessage + "...");
    indexer.triggerAsyncIndexOnStartup(uninitializedTypes);
    uninitializedTypes.forEach(this::setInitialized);
    profiler.stopInfo(logMessage + " done");
  }

  private Set<IndexType> getUninitializedTypes(StartupIndexer indexer) {
    return indexer.getIndexTypes().stream()
      .filter(indexType -> !metadataIndex.getInitialized(indexType))
      .collect(toSet());
  }

  private void setInitialized(IndexType indexType) {
    waitForIndexYellow(indexType.getMainType().getIndex().getName());
    metadataIndex.setInitialized(indexType, true);
  }

  private void waitForIndexYellow(String index) {
    esClient.clusterHealth(new ClusterHealthRequest()
      .indices(index)
      .waitForYellowStatus());
  }

  private static String getSynchronousIndexingLogMessage(Set<IndexType> emptyTypes) {
    String s = emptyTypes.size() == 1 ? "" : "s";
    String typeList = emptyTypes.stream().map(Object::toString).collect(Collectors.joining(","));
    return String.format("Indexing of type%s %s", s, typeList);
  }

  private static String getAsynchronousIndexingLogMessage(Set<IndexType> emptyTypes) {
    String s = emptyTypes.size() == 1 ? "" : "s";
    String typeList = emptyTypes.stream().map(Object::toString).collect(Collectors.joining(","));
    return String.format("Trigger asynchronous indexing of type%s %s", s, typeList);
  }
}
