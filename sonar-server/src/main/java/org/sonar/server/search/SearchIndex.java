/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.io.BytesStream;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SearchIndex {

  private static final String BULK_EXECUTE_FAILED = "Execution of bulk operation failed";
  private static final String BULK_INTERRUPTED = "Interrupted during bulk operation";

  private static final String PROFILE_DOMAIN = "es";
  private static final Logger LOG = LoggerFactory.getLogger(SearchIndex.class);

  private static final Settings INDEX_DEFAULT_SETTINGS = ImmutableSettings.builder()
    .put("number_of_shards", 1)
    .put("number_of_replicas", 0)
    .put("mapper.dynamic", false)
    .build();

  private static final String INDEX_DEFAULT_MAPPING = "{ \"_default_\": { \"dynamic\": \"strict\" } }";

  private SearchNode searchNode;
  private Client client;
  private Profiling profiling;

  public SearchIndex(SearchNode searchNode, Profiling profiling) {
    this.searchNode = searchNode;
    this.profiling = profiling;
  }

  public void start() {
    this.client = searchNode.client();
  }

  public void stop() {
    if(client != null) {
      client.close();
    }
  }

  /**
   * For full access to the underlying ES client; all other methods are shortcuts.
   */
  public Client client() {
    return client;
  }

  public void put(String index, String type, String id, BytesStream source) {
    internalPut(index, type, id, source, false, null);
  }

  public void putSynchronous(String index, String type, String id, BytesStream source) {
    internalPut(index, type, id, source, true, null);
  }

  public void put(String index, String type, String id, BytesStream source, String parent) {
    internalPut(index, type, id, source, false, parent);
  }

  public void putSynchronous(String index, String type, String id, BytesStream source, String parent) {
    internalPut(index, type, id, source, true, parent);
  }

  private void internalPut(String index, String type, String id, BytesStream source, boolean refresh, String parent) {
    IndexRequestBuilder builder = client.prepareIndex(index, type, id).setSource(source.bytes()).setRefresh(refresh);
    if (parent != null) {
      builder.setParent(parent);
    }
    StopWatch watch = createWatch();
    builder.execute().actionGet();
    watch.stop("put document with id '%s' with type '%s' into index '%s'", id, type, index);
  }

  public void bulkIndex(String index, String type, String[] ids, BytesStream[] sources) {
    BulkRequestBuilder builder = new BulkRequestBuilder(client);
    for (int i=0; i<ids.length; i++) {
      builder.add(client.prepareIndex(index, type, ids[i]).setSource(sources[i].bytes()));
    }
    StopWatch watch = createWatch();
    try {
      doBulkOperation(builder);
    } finally {
      watch.stop("bulk index of %d documents with type '%s' into index '%s'", ids.length, type, index);
    }
  }

  public void bulkIndex(String index, String type, String[] ids, BytesStream[] sources, String[] parentIds) {
    BulkRequestBuilder builder = new BulkRequestBuilder(client);
    for (int i=0; i<ids.length; i++) {
      builder.add(client.prepareIndex(index, type, ids[i]).setParent(parentIds[i]).setSource(sources[i].bytes()));
    }
    StopWatch watch = createWatch();
    try {
      doBulkOperation(builder);
    } finally {
      watch.stop("bulk index of %d child documents with type '%s' into index '%s'", ids.length, type, index);
    }
  }

  public void addMappingFromClasspath(String index, String type, String resourcePath) {
    try {
      URL resource = getClass().getResource(resourcePath);
      if (resource == null) {
        throw new IllegalArgumentException("Could not load unexisting file at " + resourcePath);
      }
      addMapping(index, type, IOUtils.toString(resource));
    } catch(IOException ioException) {
      throw new IllegalArgumentException("Problem loading file at " + resourcePath, ioException);
    }
  }

  private void addMapping(String index, String type, String mapping) {
    IndicesAdminClient indices = client.admin().indices();
    StopWatch watch = createWatch();
    try {
      if (! indices.exists(indices.prepareExists(index).request()).get().isExists()) {
        indices.prepareCreate(index)
          .setSettings(INDEX_DEFAULT_SETTINGS)
          .addMapping("_default_", INDEX_DEFAULT_MAPPING)
          .execute().actionGet();
      }
    } catch (Exception e) {
      LOG.error("While checking for index existence", e);
    } finally {
      watch.stop("create index '%s'", index);
    }

    watch = createWatch();
    try {
      indices.putMapping(Requests.putMappingRequest(index).type(type).source(mapping)).actionGet();
    } catch(ElasticSearchParseException parseException) {
      throw new IllegalArgumentException("Invalid mapping file", parseException);
    } finally {
      watch.stop("put mapping on index '%s' for type '%s'", index, type);
    }
  }

  public List<String> findDocumentIds(SearchQuery searchQuery) {
    SearchRequestBuilder builder = searchQuery.toBuilder(client);
    return findDocumentIds(builder, searchQuery.scrollSize());
  }

  public List<String> findDocumentIds(SearchRequestBuilder builder, int scrollSize) {
    List<String> result = Lists.newArrayList();
    final int scrollTime = 100;

    StopWatch watch = createWatch();
    SearchResponse scrollResp = builder.addField("_id")
      .setSearchType(SearchType.SCAN)
      .setScroll(new TimeValue(scrollTime))
      .setSize(scrollSize).execute().actionGet();
    //Scroll until no hits are returned
    while (true) {
      scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(scrollTime)).execute().actionGet();
      for (SearchHit hit : scrollResp.getHits()) {
        result.add(hit.getId());
      }
      //Break condition: No hits are returned
      if (scrollResp.getHits().getHits().length == 0) {
        break;
      }
    }
    watch.stop("findDocumentIds with request: %s", builderToString(builder));

    return result;
  }

  public SearchHits executeRequest(SearchRequestBuilder builder) {
    StopWatch watch = createWatch();
    try {
      return builder.execute().actionGet().getHits();
    } finally {
      SearchRequest request = builder.request();
      watch.stop("Executed search on ind(ex|ices) '%s' and type(s) '%s' with request: %s",
        Arrays.toString(request.indices()), Arrays.toString(request.types()), builderToString(builder));
    }
  }

  public MultiGetItemResponse[] executeMultiGet(MultiGetRequestBuilder builder) {
    StopWatch watch = createWatch();
    MultiGetItemResponse[] result = null;
    try {
      result = builder.execute().actionGet().getResponses();
    } finally {
      watch.stop("Got %d documents by multiget", result == null ? 0 : result.length);
    }
    return result;
  }

  public long executeCount(CountRequestBuilder builder) {
    StopWatch watch = createWatch();
    long count = 0;
    try {
      count = builder.execute().actionGet().getCount();
    } finally {
      CountRequest request = builder.request();
      watch.stop("Counted %d documents on ind(ex|ices) '%s'",
        count, Arrays.toString(request.indices()));
    }
    return count;
  }

  private String builderToString(SearchRequestBuilder builder) {
    try {
      return builder.internalBuilder().toXContent(XContentFactory.jsonBuilder(), ToXContent.EMPTY_PARAMS)
          .humanReadable(false).string();
    } catch (IOException ioException) {
      LOG.warn("Could not serialize request: " + builder.internalBuilder().toString(), ioException);
      return "<IOException in serialize>";
    }
  }

  public void bulkDelete(String index, String type, String[] ids) {
    BulkRequestBuilder builder = new BulkRequestBuilder(client);
    for (int i=0; i<ids.length; i++) {
      builder.add(client.prepareDelete(index, type, ids[i]));
    }
    StopWatch watch = createWatch();
    try {
      doBulkOperation(builder);
    } finally {
      watch.stop("bulk delete of %d documents with type '%s' from index '%s'", ids.length, type, index);
    }
  }

  private void doBulkOperation(BulkRequestBuilder builder) {
    try {
      BulkResponse bulkResponse = client.bulk(builder.setRefresh(true).request()).get();
      if (bulkResponse.hasFailures()) {
        for (BulkItemResponse bulkItemResponse : bulkResponse.getItems()) {
          if(bulkItemResponse.isFailed()) {
            throw new IllegalStateException("Bulk operation partially executed: " + bulkItemResponse.getFailure().getMessage());
          }
        }
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException(BULK_INTERRUPTED, e);
    } catch (ExecutionException e) {
      throw new IllegalStateException(BULK_EXECUTE_FAILED, e);
    }
  }

  private StopWatch createWatch() {
    return profiling.start(PROFILE_DOMAIN, Level.FULL);
  }
}
