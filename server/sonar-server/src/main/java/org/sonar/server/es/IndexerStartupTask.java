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
import org.elasticsearch.action.admin.cluster.health.ClusterHealthAction;
import org.elasticsearch.action.admin.indices.close.CloseIndexAction;
import org.elasticsearch.action.admin.indices.open.OpenIndexAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.unit.TimeValue;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

public class IndexerStartupTask {

  private static final Logger LOG = Loggers.get(IndexerStartupTask.class);
  private static final String SETTING_PREFIX_INITIAL_INDEXING_FINISHED = "sonarqube_initial_indexing_finished.";

  private final EsClient esClient;
  private final Configuration config;
  private final StartupIndexer[] indexers;

  public IndexerStartupTask(EsClient esClient, Configuration config, StartupIndexer... indexers) {
    this.esClient = esClient;
    this.config = config;
    this.indexers = indexers;
  }

  public void execute() {
    if (indexesAreEnabled()) {
      stream(indexers)
        .forEach(this::indexEmptyTypes);
    }
  }

  private boolean indexesAreEnabled() {
    return !config.getBoolean("sonar.internal.es.disableIndexes").orElse(false);
  }

  private void indexEmptyTypes(StartupIndexer indexer) {
    Set<IndexType> uninizializedTypes = getUninitializedTypes(indexer);
    if (!uninizializedTypes.isEmpty()) {
      Profiler profiler = Profiler.create(LOG);
      profiler.startInfo(getLogMessage(uninizializedTypes, "..."));
      indexer.indexOnStartup(uninizializedTypes);
      uninizializedTypes.forEach(this::setInitialized);
      profiler.stopInfo(getLogMessage(uninizializedTypes, "done"));
    }
  }

  private Set<IndexType> getUninitializedTypes(StartupIndexer indexer) {
    return indexer.getIndexTypes().stream().filter(this::isUninitialized).collect(toSet());
  }

  private boolean isUninitialized(IndexType indexType) {
    return isUninitialized(indexType, esClient);
  }

  public static boolean isUninitialized(IndexType indexType, EsClient esClient) {
    String setting = esClient.nativeClient().admin().indices().prepareGetSettings(indexType.getIndex()).get().getSetting(indexType.getIndex(),
      getInitializedSettingName(indexType));
    return !"true".equals(setting);
  }

  private void setInitialized(IndexType indexType) {
    String index = indexType.getIndex();
    waitForIndexGreen(index);
    closeIndex(index);
    setIndexSetting(index, getInitializedSettingName(indexType), true);
    openIndex(index);
    waitForIndexYellow(index);
  }

  private void closeIndex(String index) {
    Client nativeClient = esClient.nativeClient();
    CloseIndexAction.INSTANCE.newRequestBuilder(nativeClient).setIndices(index).get();
  }

  private void setIndexSetting(String index, String name, boolean value) {
    Client nativeClient = esClient.nativeClient();
    Builder setting = org.elasticsearch.common.settings.Settings.builder().put(name, value);
    nativeClient.admin().indices().prepareUpdateSettings(index).setSettings(setting).get();
  }

  private void openIndex(String index) {
    Client nativeClient = esClient.nativeClient();
    OpenIndexAction.INSTANCE.newRequestBuilder(nativeClient).setIndices(index).get();
  }

  private void waitForIndexYellow(String index) {
    Client nativeClient = esClient.nativeClient();
    ClusterHealthAction.INSTANCE.newRequestBuilder(nativeClient).setIndices(index).setWaitForYellowStatus().get(TimeValue.timeValueMinutes(10));
  }

  private void waitForIndexGreen(String index) {
    Client nativeClient = esClient.nativeClient();
    ClusterHealthAction.INSTANCE.newRequestBuilder(nativeClient).setIndices(index).setWaitForGreenStatus().get(TimeValue.timeValueMinutes(10));
  }

  private static String getInitializedSettingName(IndexType indexType) {
    return "index." + SETTING_PREFIX_INITIAL_INDEXING_FINISHED + indexType.getType();
  }

  private String getLogMessage(Set<IndexType> emptyTypes, String suffix) {
    String s = emptyTypes.size() == 1 ? "" : "s";
    String typeList = emptyTypes.stream().map(Object::toString).collect(Collectors.joining(","));
    return String.format("Indexing of type%s %s %s", s, typeList, suffix);
  }
}
