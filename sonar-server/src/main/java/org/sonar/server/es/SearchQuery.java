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
package org.sonar.server.es;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.MatchAllFilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder.Operator;

import java.util.Iterator;
import java.util.List;

import static org.elasticsearch.index.query.FilterBuilders.*;

/**
 * This class can be used to build "AND" form queries, to be passed e.g to {@link SearchIndex#findDocumentIds(SearchQuery)}
 * For instance the following code:
 * <blockquote>
   SearchQuery.create("polop")
      .field("field1", "value1")
      .field("field2", "value2")
   </blockquote>
 * ...corresponds to the following query string:<br/>
 * <blockquote>
   polop AND field1:value1 AND field2:value2
   </blockquote>
 * @since 4.1
 */
public class SearchQuery {
  private int scrollSize;
  private String searchString;
  private List<String> indices;
  private List<String> types;
  private Multimap<String, String> fieldCriteria;
  private Multimap<String, String> notFieldCriteria;

  private SearchQuery() {
    scrollSize = 10;
    indices = Lists.newArrayList();
    types = Lists.newArrayList();
    fieldCriteria = ArrayListMultimap.create();
    notFieldCriteria = ArrayListMultimap.create();
  }

  public static SearchQuery create() {
    return new SearchQuery();
  }

  public SearchQuery searchString(String searchString) {
    this.searchString = searchString;
    return this;
  }

  public SearchQuery scrollSize(int scrollSize) {
    this.scrollSize = scrollSize;
    return this;
  }

  int scrollSize() {
    return scrollSize;
  }

  public SearchQuery index(String index) {
    indices.add(index);
    return this;
  }

  public SearchQuery type(String type) {
    types.add(type);
    return this;
  }

  public SearchQuery field(String fieldName, String... fieldValues) {
    fieldCriteria.putAll(fieldName, Lists.newArrayList(fieldValues));
    return this;
  }

  public SearchQuery notField(String fieldName, String... fieldValues) {
    notFieldCriteria.putAll(fieldName, Lists.newArrayList(fieldValues));
    return this;
  }

  SearchRequestBuilder toBuilder(Client client) {
    SearchRequestBuilder builder = client.prepareSearch(indices.toArray(new String[0])).setTypes(types.toArray(new String[0]));

    if (fieldCriteria.isEmpty() && notFieldCriteria.isEmpty() && StringUtils.isBlank(searchString)) {
      builder.setPostFilter(new MatchAllFilterBuilder());
    } else {
      BoolFilterBuilder boolFilter = boolFilter();

      Iterator<String> mustCriteriaIterator = fieldCriteria.keySet().iterator();
      while(mustCriteriaIterator.hasNext()) {
        String field = mustCriteriaIterator.next();
        if (fieldCriteria.get(field).size() > 1) {
          boolFilter.must(termsFilter(field, fieldCriteria.get(field)));
        } else {
          boolFilter.must(termFilter(field, fieldCriteria.get(field)));
        }
      }

      for (String field: notFieldCriteria.keySet()) {
        if (notFieldCriteria.get(field).size() > 1) {
          boolFilter.mustNot(termsFilter(field, notFieldCriteria.get(field)));
        } else {
          boolFilter.mustNot(termFilter(field, notFieldCriteria.get(field)));
        }
      }

      if (StringUtils.isNotBlank(searchString)) {
        boolFilter.must(queryFilter(QueryBuilders.queryString(searchString)
          .defaultOperator(Operator.AND)
          .allowLeadingWildcard(false)));
      }

      builder.setPostFilter(boolFilter);
    }
    return builder;
  }
}
