/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;

import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;

public class Facets {

  public static final String TOTAL = "total";
  private static final java.lang.String NO_DATA_PREFIX = "no_data_";

  private final LinkedHashMap<String, LinkedHashMap<String, Long>> facetsByName;
  private final TimeZone timeZone;

  public Facets(LinkedHashMap<String, LinkedHashMap<String, Long>> facetsByName, TimeZone timeZone) {
    this.facetsByName = facetsByName;
    this.timeZone = timeZone;
  }

  public Facets(SearchResponse response, TimeZone timeZone) {
    this.facetsByName = new LinkedHashMap<>();
    this.timeZone = timeZone;
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
    } else if (Filter.class.isAssignableFrom(aggregation.getClass())) {
      processSubAggregations((Filter) aggregation);
    } else if (HasAggregations.class.isAssignableFrom(aggregation.getClass())) {
      processSubAggregations((HasAggregations) aggregation);
    } else if (Histogram.class.isAssignableFrom(aggregation.getClass())) {
      processDateHistogram((Histogram) aggregation);
    } else if (Sum.class.isAssignableFrom(aggregation.getClass())) {
      processSum((Sum) aggregation);
    } else if (MultiBucketsAggregation.class.isAssignableFrom(aggregation.getClass())) {
      processMultiBucketAggregation((MultiBucketsAggregation) aggregation);
    } else {
      throw new IllegalArgumentException("Aggregation type not supported yet: " + aggregation.getClass());
    }
  }

  private void processMissingAggregation(Missing aggregation) {
    long docCount = aggregation.getDocCount();
    if (docCount > 0L) {
      LinkedHashMap<String, Long> facet = getOrCreateFacet(aggregation.getName().replace("_missing", ""));
      if (aggregation.getAggregations().getAsMap().containsKey(FACET_MODE_EFFORT)) {
        facet.put("", Math.round(((Sum) aggregation.getAggregations().get(FACET_MODE_EFFORT)).getValue()));
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
      List<Aggregation> aggregationList = value.getAggregations().asList();
      if (aggregationList.size() == 1) {
        facet.put(value.getKeyAsString(), Math.round(((Sum) aggregationList.get(0)).getValue()));
      } else {
        facet.put(value.getKeyAsString(), value.getDocCount());
      }
    }
  }

  private void processSubAggregations(HasAggregations aggregation) {
    if (Filter.class.isAssignableFrom(aggregation.getClass())) {
      Filter filter = (Filter) aggregation;
      if (filter.getName().startsWith(NO_DATA_PREFIX)) {
        LinkedHashMap<String, Long> facet = getOrCreateFacet(filter.getName().replaceFirst(NO_DATA_PREFIX,""));
        facet.put("NO_DATA", ((Filter) aggregation).getDocCount());
      }
    }

    for (Aggregation sub : aggregation.getAggregations()) {
      processAggregation(sub);
    }
  }

  private void processDateHistogram(Histogram aggregation) {
    LinkedHashMap<String, Long> facet = getOrCreateFacet(aggregation.getName());
    for (Histogram.Bucket value : aggregation.getBuckets()) {
      String day = dateTimeToDate(value.getKeyAsString(), timeZone);
      if (value.getAggregations().getAsMap().containsKey(FACET_MODE_EFFORT)) {
        facet.put(day, Math.round(((Sum) value.getAggregations().get(FACET_MODE_EFFORT)).getValue()));
      } else {
        facet.put(day, value.getDocCount());
      }
    }
  }

  private static String dateTimeToDate(String timestamp, TimeZone timeZone) {
    Date date = parseDateTime(timestamp);
    return date.toInstant().atZone(timeZone.toZoneId()).toLocalDate().toString();
  }

  private void processSum(Sum aggregation) {
    getOrCreateFacet(aggregation.getName()).put(TOTAL, Math.round(aggregation.getValue()));
  }

  private void processMultiBucketAggregation(MultiBucketsAggregation aggregation) {
    LinkedHashMap<String, Long> facet = getOrCreateFacet(aggregation.getName());
    aggregation.getBuckets().forEach(bucket -> facet.put(bucket.getKeyAsString(), bucket.getDocCount()));
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
    return facetsByName.computeIfAbsent(facetName, n -> new LinkedHashMap<>());
  }
}
