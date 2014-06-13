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

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Result<K> {

  private final List<K> hits;
  private final Multimap<String, FacetValue> facets;
  private final long total;
  private final long timeInMillis;
  private final String scrollId;
  private final BaseIndex<K, ?, ?> index;

  public Result(BaseIndex<K, ?, ?> index, SearchResponse response) {
    this.index = index;
    this.scrollId = response.getScrollId();
    this.facets = LinkedListMultimap.create();
    this.total = (int) response.getHits().totalHits();
    this.timeInMillis = response.getTookInMillis();
    this.hits = new ArrayList<K>();
    for (SearchHit hit : response.getHits()) {
      this.hits.add(index.toDoc(hit.getSource()));
    }
    if (response.getAggregations() != null) {
      for (Map.Entry<String, Aggregation> facet : response.getAggregations().asMap().entrySet()) {
        Terms aggregation = (Terms) facet.getValue();
        for (Terms.Bucket value : aggregation.getBuckets()) {
          this.facets.put(facet.getKey(), new FacetValue(value.getKey(), (int) value.getDocCount()));
        }
      }
    }
  }

  public Iterator<K> scroll() {
    Preconditions.checkState(scrollId != null, "Result is not scrollable. Please use QueryOptions.setScroll()");
    return index.scroll(scrollId);
  }

  public List<K> getHits() {
    return hits;
  }

  public long getTotal() {
    return total;
  }

  public long getTimeInMillis() {
    return timeInMillis;
  }

  public Map<String, Collection<FacetValue>> getFacets() {
    return this.facets.asMap();
  }

  @CheckForNull
  public Collection<FacetValue> getFacetValues(String facetName) {
    return this.facets.get(facetName);
  }

  @CheckForNull
  public List<String> getFacetKeys(String facetName) {
    if (this.facets.containsKey(facetName)) {
      List<String> keys = new ArrayList<String>();
      for (FacetValue facetValue : facets.get(facetName)) {
        keys.add(facetValue.getKey());
      }
      return keys;
    }
    return null;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
