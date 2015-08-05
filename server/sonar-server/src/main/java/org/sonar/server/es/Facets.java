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
package org.sonar.server.es;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;

public class Facets {

  public static final String TOTAL = "total";

  private final LinkedHashMap<String, LinkedHashMap<String, Long>> facetsByName;

  public Facets(LinkedHashMap<String, LinkedHashMap<String, Long>> facetsByName) {
    this.facetsByName = facetsByName;
  }

  public Facets(SearchResponse response) {
    this.facetsByName = new LinkedHashMap<>();
    Aggregations aggregations = response.getAggregations();
    if (aggregations != null) {
      for (Aggregation facet : aggregations) {
        processAggregation(facet);
      }
    }
  }

  private void processAggregation(Aggregation aggregation) {
    if (Missing.class.isAssignableFrom(aggregation.getClass())) {
      processMissingAggregation((Missing) aggregation);
    } else if (Terms.class.isAssignableFrom(aggregation.getClass())) {
      processTermsAggregation((Terms) aggregation);
    } else if (HasAggregations.class.isAssignableFrom(aggregation.getClass())) {
      processSubAggregations((HasAggregations) aggregation);
    } else if (DateHistogram.class.isAssignableFrom(aggregation.getClass())) {
      processDateHistogram((DateHistogram) aggregation);
    } else if (Sum.class.isAssignableFrom(aggregation.getClass())) {
      processSum((Sum) aggregation);
    } else {
      throw new IllegalArgumentException("Aggregation type not supported yet: " + aggregation.getClass());
    }
  }

  private void processMissingAggregation(Missing aggregation) {
    long docCount = aggregation.getDocCount();
    if (docCount > 0L) {
      LinkedHashMap<String, Long> facet = getOrCreateFacet(aggregation.getName().replace("_missing", ""));
      if (aggregation.getAggregations().getAsMap().containsKey("debt")) {
        facet.put("", Math.round(((Sum) aggregation.getAggregations().get("debt")).getValue()));
      } else {
        facet.put("", docCount);
      }
    }
  }

  private void processTermsAggregation(Terms aggregation) {
    String facetName = aggregation.getName();
    // TODO document this naming convention
    if (facetName.contains("__") && !facetName.startsWith("__")) {
      facetName = facetName.substring(0, facetName.indexOf("__"));
    }
    facetName = facetName.replace("_selected", "");
    LinkedHashMap<String, Long> facet = getOrCreateFacet(facetName);
    for (Terms.Bucket value : aggregation.getBuckets()) {
      if (value.getAggregations().getAsMap().containsKey("debt")) {
        facet.put(value.getKey(), Math.round(((Sum) value.getAggregations().get("debt")).getValue()));
      } else {
        facet.put(value.getKey(), value.getDocCount());
      }
    }
  }

  private void processSubAggregations(HasAggregations aggregation) {
    for (Aggregation sub : aggregation.getAggregations()) {
      processAggregation(sub);
    }
  }

  private void processDateHistogram(DateHistogram aggregation) {
    LinkedHashMap<String, Long> facet = getOrCreateFacet(aggregation.getName());
    for (DateHistogram.Bucket value : aggregation.getBuckets()) {
      if (value.getAggregations().getAsMap().containsKey("debt")) {
        facet.put(value.getKey(), Math.round(((Sum) value.getAggregations().get("debt")).getValue()));
      } else {
        facet.put(value.getKey(), value.getDocCount());
      }
    }
  }

  private void processSum(Sum aggregation) {
    getOrCreateFacet(aggregation.getName()).put(TOTAL, Math.round(aggregation.getValue()));
  }

  public boolean contains(String facetName) {
    return facetsByName.containsKey(facetName);
  }

  /**
   * The buckets of the given facet. Null if the facet does not exist
   */
  @CheckForNull
  public LinkedHashMap<String, Long> get(String facetName) {
    return facetsByName.get(facetName);
  }

  public Map<String, LinkedHashMap<String, Long>> getAll() {
    return facetsByName;
  }

  /**
   * Value of the facet bucket. Null if the facet or the bucket do not exist.
   */
  @CheckForNull
  public Long getBucketValue(String facetName, String bucketKey) {
    LinkedHashMap<String, Long> facet = facetsByName.get(facetName);
    if (facet != null) {
      return facet.get(bucketKey);
    }
    return null;
  }

  public Set<String> getBucketKeys(String facetName) {
    LinkedHashMap<String, Long> facet = facetsByName.get(facetName);
    if (facet != null) {
      return facet.keySet();
    }
    return Collections.emptySet();
  }

  public Set<String> getNames() {
    return facetsByName.keySet();
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SIMPLE_STYLE);
  }

  private LinkedHashMap<String, Long> getOrCreateFacet(String facetName) {
    LinkedHashMap<String, Long> facet = facetsByName.get(facetName);
    if (facet == null) {
      facet = new LinkedHashMap<>();
      facetsByName.put(facetName, facet);
    }
    return facet;
  }
}
