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

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.bytes.BytesReference;

final class EsRequestDetails {
  private static final String ON_INDICES_MESSAGE = " on indices '%s'";

  private EsRequestDetails() {
    // this is utility class only
  }

  static String computeDetailsAsString(SearchRequest searchRequest) {
    StringBuilder message = new StringBuilder();
    message.append(String.format("ES search request '%s'", searchRequest));
    if (searchRequest.indices().length > 0) {
      message.append(String.format(ON_INDICES_MESSAGE, Arrays.toString(searchRequest.indices())));
    }
    return message.toString();
  }

  public static String computeDetailsAsString(SearchScrollRequest searchScrollRequest) {
    return String.format("ES search scroll request for scroll id '%s'", searchScrollRequest.scroll());
  }

  static String computeDetailsAsString(DeleteRequest deleteRequest) {
    return new StringBuilder()
      .append("ES delete request of doc ")
      .append(deleteRequest.id())
      .append(" in index ")
      .append(deleteRequest.index())
      .toString();
  }

  static String computeDetailsAsString(RefreshRequest refreshRequest) {
    StringBuilder message = new StringBuilder();
    message.append("ES refresh request");
    if (refreshRequest.indices().length > 0) {
      message.append(String.format(ON_INDICES_MESSAGE, StringUtils.join(refreshRequest.indices(), ",")));
    }
    return message.toString();
  }

  static String computeDetailsAsString(ClearIndicesCacheRequest request) {
    StringBuilder message = new StringBuilder();
    message.append("ES clear cache request");
    if (request.indices().length > 0) {
      message.append(String.format(ON_INDICES_MESSAGE, StringUtils.join(request.indices(), ",")));
    }
    String[] fields = request.fields();
    if (fields != null && fields.length > 0) {
      message.append(String.format(" on fields '%s'", StringUtils.join(fields, ",")));
    }
    if (request.queryCache()) {
      message.append(" with filter cache");
    }
    if (request.fieldDataCache()) {
      message.append(" with field data cache");
    }
    if (request.requestCache()) {
      message.append(" with request cache");
    }
    return message.toString();
  }

  static String computeDetailsAsString(IndexRequest indexRequest) {
    return new StringBuilder().append("ES index request")
      .append(String.format(" for key '%s'", indexRequest.id()))
      .append(String.format(" on index '%s'", indexRequest.index()))
      .toString();
  }

  static String computeDetailsAsString(GetRequest request) {
    return new StringBuilder().append("ES get request")
      .append(String.format(" for key '%s'", request.id()))
      .append(String.format(" on index '%s'", request.index()))
      .toString();
  }

  static String computeDetailsAsString(GetIndexRequest getIndexRequest) {
    StringBuilder message = new StringBuilder();
    message.append("ES indices exists request");
    if (getIndexRequest.indices().length > 0) {
      message.append(String.format(ON_INDICES_MESSAGE, StringUtils.join(getIndexRequest.indices(), ",")));
    }
    return message.toString();
  }

  static String computeDetailsAsString(CreateIndexRequest createIndexRequest) {
    return String.format("ES create index '%s'", createIndexRequest.index());
  }

  static String computeDetailsAsString(PutMappingRequest request) {
    StringBuilder message = new StringBuilder();
    message.append("ES put mapping request");
    if (request.indices().length > 0) {
      message.append(String.format(ON_INDICES_MESSAGE, StringUtils.join(request.indices(), ",")));
    }
    BytesReference source = request.source();
    if (source != null) {
      message.append(String.format(" with source '%s'", source.utf8ToString()));
    }

    return message.toString();
  }

  static String computeDetailsAsString(ClusterHealthRequest clusterHealthRequest) {
    StringBuilder message = new StringBuilder();
    message.append("ES cluster health request");
    String[] indices = clusterHealthRequest.indices();
    if (indices != null && indices.length > 0) {
      message.append(String.format(ON_INDICES_MESSAGE, StringUtils.join(indices, ",")));
    }
    return message.toString();
  }

  static String computeDetailsAsString(String... indices) {
    StringBuilder message = new StringBuilder();
    message.append("ES indices stats request");
    if (indices.length > 0) {
      message.append(String.format(ON_INDICES_MESSAGE, StringUtils.join(indices, ",")));
    }
    return message.toString();
  }

}
