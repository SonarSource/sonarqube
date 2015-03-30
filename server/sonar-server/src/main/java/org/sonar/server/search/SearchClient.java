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

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.explain.ExplainRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.mlt.MoreLikeThisRequestBuilder;
import org.elasticsearch.action.percolate.MultiPercolateRequestBuilder;
import org.elasticsearch.action.percolate.PercolateRequestBuilder;
import org.elasticsearch.action.search.ClearScrollRequestBuilder;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.termvector.MultiTermVectorsRequestBuilder;
import org.elasticsearch.action.termvector.TermVectorRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.picocontainer.Startable;
import org.sonar.api.config.Settings;
import org.sonar.process.LoopbackAddress;
import org.sonar.process.ProcessProperties;
import org.sonar.server.es.request.*;

/**
 * ElasticSearch Node used to connect to index.
 */
public class SearchClient extends TransportClient implements Startable {

  public SearchClient(Settings settings) {
    super(ImmutableSettings.settingsBuilder()
      .put("node.name", StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.CLUSTER_NODE_NAME), "sq_local_client"))
      .put("network.bind_host", StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.SEARCH_HOST), "localhost"))
      .put("node.rack_id", StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.CLUSTER_NODE_NAME), "unknown"))
      .put("cluster.name", StringUtils.defaultIfBlank(settings.getString(ProcessProperties.CLUSTER_NAME), "sonarqube"))
      .build());
    initLogging();
    this.addTransportAddress(new InetSocketTransportAddress(StringUtils.defaultIfEmpty(settings.getString(ProcessProperties.SEARCH_HOST), LoopbackAddress.get().getHostAddress()),
      settings.getInt(ProcessProperties.SEARCH_PORT)));
  }

  private void initLogging() {
    ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory());
  }

  public RefreshRequestBuilder prepareRefresh(String... indices) {
    return new ProxyRefreshRequestBuilder(this).setIndices(indices);
  }

  public IndicesExistsRequestBuilder prepareIndicesExist(String... indices) {
    return new ProxyIndicesExistsRequestBuilder(this, indices);
  }

  public CreateIndexRequestBuilder prepareCreate(String index) {
    return new ProxyCreateIndexRequestBuilder(this, index);
  }

  public PutMappingRequestBuilder preparePutMapping(String... indices) {
    return new ProxyPutMappingRequestBuilder(this).setIndices(indices);
  }

  @Override
  public SearchRequestBuilder prepareSearch(String... indices) {
    return new ProxySearchRequestBuilder(this).setIndices(indices);
  }

  @Override
  public SearchScrollRequestBuilder prepareSearchScroll(String scrollId) {
    return new ProxySearchScrollRequestBuilder(scrollId, this);
  }

  @Override
  public GetRequestBuilder prepareGet() {
    return new ProxyGetRequestBuilder(this);
  }

  @Override
  public MultiGetRequestBuilder prepareMultiGet() {
    return new ProxyMultiGetRequestBuilder(this);
  }

  @Override
  public CountRequestBuilder prepareCount(String... indices) {
    return new ProxyCountRequestBuilder(this).setIndices(indices);
  }

  @Override
  public BulkRequestBuilder prepareBulk() {
    return new ProxyBulkRequestBuilder(this);
  }

  @Override
  public DeleteByQueryRequestBuilder prepareDeleteByQuery(String... indices) {
    throw new UnsupportedOperationException("Delete by query must not be used. See https://github.com/elastic/elasticsearch/issues/10067. See alternatives in BulkIndexer.");
  }

  // ****************************************************************************************************************
  // Not yet implemented methods
  // ****************************************************************************************************************

  @Override
  public GetRequestBuilder prepareGet(String index, String type, String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public IndexRequestBuilder prepareIndex() {
    throw throwNotYetImplemented();
  }

  @Override
  public IndexRequestBuilder prepareIndex(String index, String type) {
    throw throwNotYetImplemented();
  }

  @Override
  public IndexRequestBuilder prepareIndex(String index, String type, @Nullable String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public MultiSearchRequestBuilder prepareMultiSearch() {
    throw throwNotYetImplemented();
  }

  @Override
  public UpdateRequestBuilder prepareUpdate() {
    throw throwNotYetImplemented();
  }

  @Override
  public UpdateRequestBuilder prepareUpdate(String index, String type, String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public DeleteRequestBuilder prepareDelete() {
    throw throwNotYetImplemented();
  }

  @Override
  public DeleteRequestBuilder prepareDelete(String index, String type, String id) {
    return new ProxyDeleteRequestBuilder(this, index).setType(type).setId(id);
  }

  @Override
  public PercolateRequestBuilder preparePercolate() {
    throw throwNotYetImplemented();
  }

  @Override
  public MultiPercolateRequestBuilder prepareMultiPercolate() {
    throw throwNotYetImplemented();
  }

  @Override
  public SuggestRequestBuilder prepareSuggest(String... indices) {
    throw throwNotYetImplemented();
  }

  @Override
  public MoreLikeThisRequestBuilder prepareMoreLikeThis(String index, String type, String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public TermVectorRequestBuilder prepareTermVector(String index, String type, String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public MultiTermVectorsRequestBuilder prepareMultiTermVectors() {
    throw throwNotYetImplemented();
  }

  @Override
  public ExplainRequestBuilder prepareExplain(String index, String type, String id) {
    throw throwNotYetImplemented();
  }

  @Override
  public ClearScrollRequestBuilder prepareClearScroll() {
    throw throwNotYetImplemented();
  }

  private static IllegalStateException throwNotYetImplemented() {
    return new IllegalStateException("Not yet implemented");
  }

  @Override
  public void start() {
    // nothing to do
  }

  @Override
  public void stop() {
    close();
  }
}
