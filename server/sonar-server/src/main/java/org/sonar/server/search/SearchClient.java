/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.search;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsNodes;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.StopWatch;

import java.io.File;

/**
 * ElasticSearch Node used to connect to index.
 */
public class SearchClient extends TransportClient {

  private static final String DEFAULT_HEALTH_TIMEOUT = "30s";
  private static final String SONAR_PATH_HOME = "sonar.path.home";
  private static final String SONAR_PATH_DATA = "sonar.path.data";
  private static final String SONAR_PATH_LOG = "sonar.path.log";

  private final Settings settings;
  private final String healthTimeout;

  protected final Profiling profiling;

  public SearchClient(Settings settings) {
    this(settings, DEFAULT_HEALTH_TIMEOUT);
  }

  @VisibleForTesting
  SearchClient(Settings settings, String healthTimeout) {
    super(ImmutableSettings.settingsBuilder()
        .put("network.bind_host", "localhost")
        .put("node.rack_id", StringUtils.defaultIfEmpty(settings.getString(IndexProperties.NODE_NAME), "unknown"))
        .put("cluster.name", StringUtils.defaultIfBlank(settings.getString(IndexProperties.CLUSTER_NAME), "sonarqube"))
        .build()
    );
    initLogging();
    this.addTransportAddress(new InetSocketTransportAddress("localhost",
      settings.getInt(IndexProperties.NODE_PORT)));
    this.settings = settings;
    this.healthTimeout = healthTimeout;
    this.profiling = new Profiling(settings);
  }

  public NodeHealth getNodeHealth() {
    NodeHealth health = new NodeHealth();
    ClusterStatsResponse clusterStatsResponse = this.admin().cluster().prepareClusterStats().get();

    // Cluster health
    health.setClusterAvailable(clusterStatsResponse.getStatus() != ClusterHealthStatus.RED);

    ClusterStatsNodes nodesStats = clusterStatsResponse.getNodesStats();

    // JVM Heap Usage
    health.setJvmHeapMax(nodesStats.getJvm().getHeapMax().bytes());
    health.setJvmHeapUsed(nodesStats.getJvm().getHeapUsed().bytes());

    // OS Memory Usage ?

    // Disk Usage
    health.setFsTotal(nodesStats.getFs().getTotal().bytes());
    health.setFsAvailable(nodesStats.getFs().getAvailable().bytes());

    // Ping ?

    // Threads
    health.setJvmThreads(nodesStats.getJvm().getThreads());

    // CPU
    health.setProcessCpuPercent(nodesStats.getProcess().getCpuPercent());

    // Open Files
    health.setOpenFiles(nodesStats.getProcess().getAvgOpenFileDescriptors());

    // Uptime
    health.setJvmUptimeMillis(nodesStats.getJvm().getMaxUpTime().getMillis());

    return health;
  }

  private void initLogging() {
    ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory());
  }

  private File esHomeDir() {
    if (!settings.hasKey(SONAR_PATH_HOME)) {
      throw new IllegalStateException("property 'sonar.path.home' is required");
    }
    return new File(settings.getString(SONAR_PATH_HOME));
  }

  private File esDataDir() {
    if (settings.hasKey(SONAR_PATH_DATA)) {
      return new File(settings.getString(SONAR_PATH_DATA), "es");
    } else {
      return new File(settings.getString(SONAR_PATH_HOME), "data/es");
    }
  }

  private File esLogDir() {
    if (settings.hasKey(SONAR_PATH_LOG)) {
      return new File(settings.getString(SONAR_PATH_LOG));
    } else {
      return new File(settings.getString(SONAR_PATH_HOME), "log");
    }
  }

  public <K extends ActionResponse> K execute(ActionRequestBuilder request) {
    StopWatch fullProfile = profiling.start("search", Profiling.Level.FULL);
    ListenableActionFuture acc = request.execute();
    try {

      K response = (K) acc.get();

      if (profiling.isProfilingEnabled(Profiling.Level.BASIC)) {
        if (ToXContent.class.isAssignableFrom(request.getClass())) {
          XContentBuilder debugResponse = XContentFactory.jsonBuilder();
          debugResponse.startObject();
          ((ToXContent) request).toXContent(debugResponse, ToXContent.EMPTY_PARAMS);
          debugResponse.endObject();
          fullProfile.stop("ES Request: %s", debugResponse.string());
        } else {
          fullProfile.stop("ES Request: %s", request.toString().replaceAll("\n", ""));
        }
      }

      if (profiling.isProfilingEnabled(Profiling.Level.FULL)) {
        if (ToXContent.class.isAssignableFrom(response.getClass())) {
          XContentBuilder debugResponse = XContentFactory.jsonBuilder();
          debugResponse.startObject();
          ((ToXContent) response).toXContent(debugResponse, ToXContent.EMPTY_PARAMS);
          debugResponse.endObject();
          fullProfile.stop("ES Response: %s", debugResponse.string());
        } else {
          fullProfile.stop("ES Response: %s", response.toString());
        }
      }
      return response;
    } catch (Exception e) {
      throw new IllegalStateException("ES error: ", e);
    }
  }
}
