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
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.facet.FacetBuilders;

import java.util.List;
import java.util.Map;

/**
 * This class can be used to build "AND" form queries, eventually with query facets, to be passed to {@link SearchIndex#find(SearchQuery)}
 * For instance the following code:
 * <blockquote>
   SearchQuery.create("polop")
      .field("field1", "value1")
      .field("field2", "value2")
   </blockquote>
 * ...yields the following query string:<br/>
 * <blockquote>
   polop AND field1:value1 AND field2:value2
   </blockquote>
 * @since 4.1
 */
public class SearchQuery {
  private String searchString;
  private List<String> indices;
  private List<String> types;
  private Map<String, String> fieldCriteria;
  private Map<String, String> termFacets;

  private SearchQuery() {
    indices = Lists.newArrayList();
    types = Lists.newArrayList();
    fieldCriteria = Maps.newLinkedHashMap();
    termFacets = Maps.newHashMap();
  }

  public static SearchQuery create() {
    return new SearchQuery();
  }

  public static SearchQuery create(String searchString) {
    SearchQuery searchQuery = new SearchQuery();
    searchQuery.searchString = searchString;
    return searchQuery;
  }

  public SearchQuery index(String index) {
    indices.add(index);
    return this;
  }

  public SearchQuery field(String fieldName, String fieldValue) {
    fieldCriteria.put(fieldName, fieldValue);
    return this;
  }

  public SearchQuery facet(String facetName, String fieldName) {
    fieldCriteria.put(facetName, fieldName);
    return this;
  }

  private SearchRequestBuilder addFacets(SearchRequestBuilder builder) {
    for (String facetName: termFacets.keySet()) {
      builder.addFacet(FacetBuilders.termsFacet(facetName).field(termFacets.get(facetName)));
    }
    return builder;
  }

  String getQueryString() {
    List<String> criteria = Lists.newArrayList();
    if (StringUtils.isNotBlank(searchString)) {
      criteria.add(searchString);
    }
    for (String fieldName: fieldCriteria.keySet()) {
      criteria.add(String.format("%s:%s", fieldName, fieldCriteria.get(fieldName)));
    }
    return StringUtils.join(criteria, " AND ");
  }

  SearchRequestBuilder toBuilder(Client client) {
    SearchRequestBuilder builder = client.prepareSearch(indices.toArray(new String[0])).setTypes(types.toArray(new String[0]));
    String queryString = getQueryString();
    if (StringUtils.isBlank(queryString)) {
      builder.setQuery(QueryBuilders.matchAllQuery());
    } else {
      builder.setQuery(queryString);
    }
    return addFacets(builder);
  }
}
