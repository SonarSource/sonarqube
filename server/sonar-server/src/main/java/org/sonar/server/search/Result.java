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
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.*;

public class Result<K> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Result.class);

  private final List<K> hits;
  private final Multimap<String, FacetValue> facets;
  private final long total;
  private final String scrollId;
  private final BaseIndex<K, ?, ?> index;

  public Result(SearchResponse response) {
    this(null, response);
  }

  public Result(@Nullable BaseIndex<K, ?, ?> index, SearchResponse response) {
    this.index = index;
    this.scrollId = response.getScrollId();
    this.facets = LinkedHashMultimap.create();
    this.total = (int) response.getHits().totalHits();
    this.hits = new ArrayList<K>();
    if (index != null) {
      for (SearchHit hit : response.getHits()) {
        this.hits.add(index.toDoc(hit.getSource()));
      }
    }
    if (response.getAggregations() != null) {
      for (Map.Entry<String, Aggregation> facet : response.getAggregations().asMap().entrySet()) {
        this.processAggregation(facet.getValue());
      }
    }
  }

  private void processAggregation(Aggregation aggregation) {
    if (Missing.class.isAssignableFrom(aggregation.getClass())) {
      Missing missing = (Missing) aggregation;
      long docCount = missing.getDocCount();
      if (docCount > 0L) {
        this.facets.put(aggregation.getName().replace("_missing",""), new FacetValue("", docCount));
      }
    } else if (Terms.class.isAssignableFrom(aggregation.getClass())) {
      Terms termAggregation = (Terms) aggregation;
      for (Terms.Bucket value : termAggregation.getBuckets()) {
        this.facets.put(aggregation.getName().replace("_selected",""), new FacetValue(value.getKey(), value.getDocCount()));
      }
    } else if (HasAggregations.class.isAssignableFrom(aggregation.getClass())) {
      HasAggregations hasAggregations = (HasAggregations) aggregation;
      for (Aggregation internalAggregation : hasAggregations.getAggregations()) {
        this.processAggregation(internalAggregation);
      }
    } else {
      LOGGER.warn("Cannot process {} type of aggregation", aggregation.getClass());
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
