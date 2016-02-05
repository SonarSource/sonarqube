/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.search;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.process.LoopbackAddress;
import org.sonar.process.ProcessProperties;
import org.sonar.server.es.request.ProxyBulkRequestBuilder;
import org.sonar.server.es.request.ProxyCountRequestBuilder;
import org.sonar.server.es.request.ProxyCreateIndexRequestBuilder;
import org.sonar.server.es.request.ProxyGetRequestBuilder;
import org.sonar.server.es.request.ProxyIndicesExistsRequestBuilder;
import org.sonar.server.es.request.ProxyMultiGetRequestBuilder;
import org.sonar.server.es.request.ProxyPutMappingRequestBuilder;
import org.sonar.server.es.request.ProxyRefreshRequestBuilder;
import org.sonar.server.es.request.ProxySearchRequestBuilder;
import org.sonar.server.es.request.ProxySearchScrollRequestBuilder;

/**
 * ElasticSearch Node used to connect to index.
 */
public class SearchClient implements Startable {

  private final Settings settings;
  private Client nativeClient;

  public SearchClient(Settings settings) {
    this.settings = settings;
  }

  @VisibleForTesting
  public SearchClient(Settings settings, Client nativeClient) {
    this.settings = settings;
    this.nativeClient = nativeClient;
  }

  public Client nativeClient() {
    if (nativeClient == null) {
      throw new IllegalStateException();
    }
    return nativeClient;
  }

  public RefreshRequestBuilder prepareRefresh(String... indices) {
    return new ProxyRefreshRequestBuilder(nativeClient).setIndices(indices);
  }

  public IndicesExistsRequestBuilder prepareIndicesExist(String... indices) {
    return new ProxyIndicesExistsRequestBuilder(nativeClient, indices);
  }

  public CreateIndexRequestBuilder prepareCreate(String index) {
    return new ProxyCreateIndexRequestBuilder(nativeClient, index);
  }

  public PutMappingRequestBuilder preparePutMapping(String... indices) {
    return new ProxyPutMappingRequestBuilder(nativeClient).setIndices(indices);
  }

  public SearchRequestBuilder prepareSearch(String... indices) {
    return new ProxySearchRequestBuilder(nativeClient).setIndices(indices);
  }

  public SearchScrollRequestBuilder prepareSearchScroll(String scrollId) {
    return new ProxySearchScrollRequestBuilder(scrollId, nativeClient);
  }

  public GetRequestBuilder prepareGet() {
    return new ProxyGetRequestBuilder(nativeClient);
  }

  public MultiGetRequestBuilder prepareMultiGet() {
    return new ProxyMultiGetRequestBuilder(nativeClient);
  }

  public CountRequestBuilder prepareCount(String... indices) {
    return new ProxyCountRequestBuilder(nativeClient).setIndices(indices);
  }

  public BulkRequestBuilder prepareBulk() {
    return new ProxyBulkRequestBuilder(nativeClient);
  }

  public DeleteByQueryRequestBuilder prepareDeleteByQuery(String... indices) {
    throw new UnsupportedOperationException("Delete by query must not be used. See https://github.com/elastic/elasticsearch/issues/10067. See alternatives in BulkIndexer.");
  }

  @Override
  public synchronized void start() {
    if (nativeClient == null) {
      ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory());
      org.elasticsearch.common.settings.Settings esSettings = ImmutableSettings.settingsBuilder()
        .put("node.name", StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.CLUSTER_NODE_NAME), "sq_local_client"))
        .put("network.bind_host", StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.SEARCH_HOST), "localhost"))
        .put("node.rack_id", StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.CLUSTER_NODE_NAME), "unknown"))
        .put("cluster.name", StringUtils.defaultIfBlank(settings.getString(ProcessProperties.CLUSTER_NAME), "sonarqube"))
        .build();
      nativeClient = new TransportClient(esSettings);
      ((TransportClient) nativeClient).addTransportAddress(new InetSocketTransportAddress(StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.SEARCH_HOST),
        LoopbackAddress.get()
          .getHostAddress()),
        settings.getInt(ProcessProperties.SEARCH_PORT)));
    }
  }

  @Override
  public void stop() {
    if (nativeClient != null) {
      nativeClient.close();
    }
  }
}
