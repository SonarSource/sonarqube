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
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.io.BytesStream;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.TimeProfiler;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;

public class SearchIndex {

  private static final Logger LOG = LoggerFactory.getLogger(SearchIndex.class);

  private SearchNode searchNode;
  private Client client;

  public SearchIndex(SearchNode searchNode) {
    this.searchNode = searchNode;
  }

  public void start() {
    this.client = searchNode.client();
  }

  public void stop() {
    if(client != null) {
      client.close();
    }
  }

  public void put(String index, String type, String id, BytesStream source) {
    internalPut(index, type, id, source, false);
  }

  public void putSynchronous(String index, String type, String id, BytesStream source) {
    internalPut(index, type, id, source, true);
  }

  private void internalPut(String index, String type, String id, BytesStream source, boolean refresh) {
    IndexRequestBuilder builder = client.prepareIndex(index, type, id).setSource(source.bytes()).setRefresh(refresh);
    TimeProfiler profiler = newDebugProfiler();
    profiler.start(format("put document with id '%s' with type '%s' into index '%s'", id, type, index));
    builder.execute().actionGet();
    profiler.stop();
  }

  public void bulkIndex(String index, String type, String[] ids, BytesStream[] sources) {
    BulkRequestBuilder builder = new BulkRequestBuilder(client);
    for (int i=0; i<ids.length; i++) {
      builder.add(client.prepareIndex(index, type, ids[i]).setSource(sources[i].bytes()));
    }
    TimeProfiler profiler = newDebugProfiler();
    try {
      profiler.start(format("bulk index of %d documents with type '%s' into index '%s'", ids.length, type, index));
      BulkResponse bulkResponse = client.bulk(builder.setRefresh(true).request()).get();
      if (bulkResponse.hasFailures()) {
        // Retry once per failed doc -- ugly
        for (BulkItemResponse bulkItemResponse : bulkResponse.getItems()) {
          if(bulkItemResponse.isFailed()) {
            int itemId = bulkItemResponse.getItemId();
            put(index, type, ids[itemId], sources[itemId]);
          }
        }
      }
    } catch (InterruptedException e) {
      LOG.error("Interrupted during bulk operation", e);
    } catch (ExecutionException e) {
      LOG.error("Execution of bulk operation failed", e);
    } finally {
      profiler.stop();
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
    TimeProfiler profiler = newDebugProfiler();
    try {
      if (! indices.exists(new IndicesExistsRequest(index)).get().isExists()) {
        profiler.start(format("create index '%s'", index));
        indices.prepareCreate(index).execute().actionGet();
      }
    } catch (Exception e) {
      LOG.error("While checking for index existence", e);
    } finally {
      profiler.stop();
    }

    profiler.start(format("put mapping on index '%s' for type '%s'", index, type));
    try {
      indices.putMapping(Requests.putMappingRequest(index).type(type).source(mapping)).actionGet();
    } catch(ElasticSearchParseException parseException) {
      throw new IllegalArgumentException("Invalid mapping file", parseException);
    } finally {
      profiler.stop();
    }
  }

  public List<String> findDocumentIds(SearchQuery searchQuery) {
    List<String> result = Lists.newArrayList();
    final int scrollTime = 100;

    SearchRequestBuilder builder = searchQuery.toBuilder(client);
    LOG.debug("findDocumentIds" + builder.internalBuilder().toString());
    TimeProfiler profiler = newDebugProfiler();
    profiler.start("findDocumentIds");
    SearchResponse scrollResp = builder.addField("_id")
            .setSearchType(SearchType.SCAN)
            .setScroll(new TimeValue(scrollTime))
            .setSize(searchQuery.scrollSize()).execute().actionGet();
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
    profiler.stop();

    return result;
  }

  private TimeProfiler newDebugProfiler() {
    TimeProfiler profiler = new TimeProfiler();
    profiler.setLogger(LOG);
    return profiler;
  }

}
