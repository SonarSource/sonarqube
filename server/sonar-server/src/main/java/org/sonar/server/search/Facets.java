/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.search;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class Facets {

  private static final Logger LOGGER = Loggers.get(Facets.class);

  private final Multimap<String, FacetValue> facetValues;

  public Facets(SearchResponse response) {
    facetValues = LinkedHashMultimap.create();

    if (response.getAggregations() != null) {
      for (Aggregation facet : response.getAggregations()) {
        this.processAggregation(facet);
      }
    }
  }

  private void processAggregation(Aggregation aggregation) {
    if (Missing.class.isAssignableFrom(aggregation.getClass())) {
      processMissingAggregation(aggregation);
    } else if (Terms.class.isAssignableFrom(aggregation.getClass())) {
      processTermsAggregation(aggregation);
    } else if (HasAggregations.class.isAssignableFrom(aggregation.getClass())) {
      processSubAggregations(aggregation);
    } else if (Histogram.class.isAssignableFrom(aggregation.getClass())) {
      processDateHistogram(aggregation);
    } else {
      LOGGER.warn("Cannot process {} type of aggregation", aggregation.getClass());
    }
  }

  private void processMissingAggregation(Aggregation aggregation) {
    Missing missing = (Missing) aggregation;
    long docCount = missing.getDocCount();
    if (docCount > 0L) {
      String facetName = aggregation.getName();
      if (facetName.contains("__") && !facetName.startsWith("__")) {
        facetName = facetName.substring(0, facetName.indexOf("__"));
      }
      this.facetValues.put(facetName.replace("_missing", ""), new FacetValue("", docCount));
    }
  }

  private void processTermsAggregation(Aggregation aggregation) {
    Terms termAggregation = (Terms) aggregation;
    for (Terms.Bucket value : termAggregation.getBuckets()) {
      String facetName = aggregation.getName();
      if (facetName.contains("__") && !facetName.startsWith("__")) {
        facetName = facetName.substring(0, facetName.indexOf("__"));
      }
      facetName = facetName.replace("_selected", "");
      this.facetValues.put(facetName, new FacetValue(value.getKeyAsString(), value.getDocCount()));
    }
  }

  private void processSubAggregations(Aggregation aggregation) {
    HasAggregations hasAggregations = (HasAggregations) aggregation;
    for (Aggregation internalAggregation : hasAggregations.getAggregations()) {
      this.processAggregation(internalAggregation);
    }
  }

  private void processDateHistogram(Aggregation aggregation) {
    Histogram dateHistogram = (Histogram) aggregation;
    for (Histogram.Bucket value : dateHistogram.getBuckets()) {
      this.facetValues.put(dateHistogram.getName(), new FacetValue(value.getKeyAsString(), value.getDocCount()));
    }
  }

  public Map<String, Collection<FacetValue>> getFacets() {
    return this.facetValues.asMap();
  }

  public Collection<FacetValue> getFacetValues(String facetName) {
    return this.facetValues.get(facetName);
  }

  public List<String> getFacetKeys(String facetName) {
    List<String> keys = new ArrayList<>();
    if (this.facetValues.containsKey(facetName)) {
      for (FacetValue facetValue : facetValues.get(facetName)) {
        keys.add(facetValue.getKey());
      }
    }
    return keys;
  }

  @Override
  public String toString() {
    return facetValues.toString();
  }
}
