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

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.FilterAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FiltersAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket;
import co.elastic.clients.elasticsearch._types.aggregations.GlobalAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.MissingAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Sum;

import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;

public class Facets {

  public static final String SELECTED_SUB_AGG_NAME_SUFFIX = "_selected";
  public static final String TOTAL = "total";
  private static final String NO_DATA_PREFIX = "no_data_";
  private static final Pattern NO_DATA_PREFIX_PATTERN = Pattern.compile(Pattern.quote(NO_DATA_PREFIX));
  private static final String FILTER_SUFFIX = "_filter";
  private static final String FILTER_BY_RULE_PREFIX = "filter_by_rule_types_";

  private final LinkedHashMap<String, LinkedHashMap<String, Long>> facetsByName;
  private final ZoneId timeZone;

  public Facets(LinkedHashMap<String, LinkedHashMap<String, Long>> facetsByName, ZoneId timeZone) {
    this.facetsByName = facetsByName;
    this.timeZone = timeZone;
  }

  /**
   * @deprecated Use the constructor that accepts co.elastic.clients.elasticsearch.core.SearchResponse instead.
   * That constructor is based on the new Elasticsearch Java API Client (8.x).
   */
  @Deprecated(since = "2025.6", forRemoval = true)
  public Facets(SearchResponse response, ZoneId timeZone) {
    this.facetsByName = new LinkedHashMap<>();
    this.timeZone = timeZone;
    Aggregations aggregations = response.getAggregations();
    if (aggregations != null) {
      for (Aggregation facet : aggregations) {
        processAggregation(facet);
      }
    }
  }

  public <T> Facets(co.elastic.clients.elasticsearch.core.SearchResponse<T> response, ZoneId timeZone) {
    this.facetsByName = new LinkedHashMap<>();
    this.timeZone = timeZone;
    Map<String, Aggregate> aggregations = response.aggregations();
    if (aggregations != null) {
      for (Map.Entry<String, Aggregate> entry : aggregations.entrySet()) {
        processAggregationV2(entry.getKey(), entry.getValue());
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

  void processAggregationV2(String name, Aggregate aggregate) {
    switch (aggregate._kind()) {
      case Missing -> processMissingAggregationV2(name, aggregate.missing());
      case Sterms -> processTermsAggregationV2(name, aggregate.sterms());
      case Filter -> processFilterAggregation(name, aggregate.filter());
      case DateHistogram -> processDateHistogramV2(name, aggregate.dateHistogram());
      case Sum -> processSumV2(name, aggregate.sum());
      case Global -> processGlobalAggregation(name, aggregate.global());
      case Nested -> processNestedAggregation(name, aggregate.nested());
      case Filters -> processFiltersAggregation(name, aggregate.filters());
      case Children -> processChildrenAggregation(name, aggregate.children());
      default -> throw new IllegalArgumentException("Aggregation type not supported yet: " + aggregate.getClass());
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

  void processMissingAggregationV2(String name, MissingAggregate aggregation) {
    long docCount = aggregation.docCount();
    if (docCount > 0L) {
      LinkedHashMap<String, Long> facet = getOrCreateFacet(name.replace("_missing", ""));
      Map<String, Aggregate> subAggs = aggregation.aggregations();
      if (subAggs != null && subAggs.containsKey(FACET_MODE_EFFORT)) {
        Aggregate effortAgg = subAggs.get(FACET_MODE_EFFORT);
        if (effortAgg.isSum()) {
          facet.put("", Math.round(effortAgg.sum().value()));
        } else {
          facet.put("", docCount);
        }
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
    facetName = facetName.replace(SELECTED_SUB_AGG_NAME_SUFFIX, "");
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

  void processTermsAggregationV2(String name, StringTermsAggregate aggregation) {
    String facetName = name;
    if (facetName.contains("__") && !facetName.startsWith("__")) {
      facetName = facetName.substring(0, facetName.indexOf("__"));
    }
    facetName = facetName.replace(SELECTED_SUB_AGG_NAME_SUFFIX, "");
    LinkedHashMap<String, Long> facet = getOrCreateFacet(facetName);

    Buckets<StringTermsBucket> buckets = aggregation.buckets();
    if (buckets == null || buckets.array().isEmpty()) {
      return;
    }

    for (StringTermsBucket bucket : buckets.array()) {
      Map<String, Aggregate> subAggs = bucket.aggregations();

      if (subAggs != null && subAggs.size() == 1) {
        Aggregate subAgg = subAggs.values().iterator().next();
        if (subAgg.isSum()) {
          facet.put(bucket.key().stringValue(), Math.round(subAgg.sum().value()));
        } else {
          facet.put(bucket.key().stringValue(), bucket.docCount());
        }
      } else {
        facet.put(bucket.key().stringValue(), bucket.docCount());
      }
    }
  }

  private void processSubAggregations(HasAggregations aggregation) {
    if (Filter.class.isAssignableFrom(aggregation.getClass())) {
      Filter filter = (Filter) aggregation;
      if (filter.getName().startsWith(NO_DATA_PREFIX)) {
        LinkedHashMap<String, Long> facet = getOrCreateFacet(filter.getName().replaceFirst(NO_DATA_PREFIX, ""));
        facet.put("NO_DATA", ((Filter) aggregation).getDocCount());
      }
    }

    for (Aggregation sub : getOrderedAggregations(aggregation)) {
      processAggregation(sub);
    }
  }

  void processSubAggregationsV2(String parentName, Map<String, Aggregate> subAggregations) {
    if (subAggregations.isEmpty()) {
      return;
    }

    List<Map.Entry<String, Aggregate>> orderedEntries = new ArrayList<>(subAggregations.entrySet());
    orderedEntries.sort(
      Comparator.comparing(
        (Map.Entry<String, Aggregate> entry) -> isNameMatchingParent(parentName, entry.getKey()))
        .reversed());

    for (Map.Entry<String, Aggregate> entry : orderedEntries) {
      processAggregationV2(entry.getKey(), entry.getValue());
    }
  }

  private static List<Aggregation> getOrderedAggregations(HasAggregations topAggregation) {
    String topAggregationName = ((Aggregation) topAggregation).getName();
    List<Aggregation> orderedAggregations = new ArrayList<>();
    for (Aggregation aggregation : topAggregation.getAggregations()) {
      if (isNameMatchingTopAggregation(topAggregationName, aggregation.getName())) {
        orderedAggregations.add(0, aggregation);
      } else {
        orderedAggregations.add(aggregation);
      }
    }
    return orderedAggregations;
  }

  private static boolean isNameMatchingTopAggregation(String topAggregationName, String aggregationName) {
    return aggregationName.equals(topAggregationName) ||
      aggregationName.equals(FILTER_BY_RULE_PREFIX + topAggregationName.replace(FILTER_SUFFIX, ""));
  }

  static boolean isNameMatchingParent(String parentName, String aggregationName) {
    return aggregationName.equals(parentName) ||
      aggregationName.equals(FILTER_BY_RULE_PREFIX + parentName.replace(FILTER_SUFFIX, ""));
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

  void processDateHistogramV2(String name, DateHistogramAggregate aggregation) {
    LinkedHashMap<String, Long> facet = getOrCreateFacet(name);
    Buckets<DateHistogramBucket> buckets = aggregation.buckets();
    if (buckets == null || buckets.array().isEmpty()) {
      return;
    }
    for (DateHistogramBucket bucket : buckets.array()) {
      if (bucket.keyAsString() != null) {
        String day = dateTimeToDate(bucket.keyAsString(), timeZone);
        Map<String, Aggregate> subAggs = bucket.aggregations();
        if (subAggs != null && subAggs.containsKey(FACET_MODE_EFFORT)) {
          Aggregate effortAgg = subAggs.get(FACET_MODE_EFFORT);
          if (effortAgg.isSum()) {
            facet.put(day, Math.round(effortAgg.sum().value()));
          } else {
            facet.put(day, bucket.docCount());
          }
        } else {
          facet.put(day, bucket.docCount());
        }
      }
    }
  }

  static String dateTimeToDate(String timestamp, ZoneId timeZone) {
    Date date = parseDateTime(timestamp);
    return date.toInstant().atZone(timeZone).toLocalDate().toString();
  }

  private void processSum(Sum aggregation) {
    getOrCreateFacet(aggregation.getName()).put(TOTAL, Math.round(aggregation.getValue()));
  }

  void processSumV2(String name, SumAggregate aggregation) {
    getOrCreateFacet(name).put(TOTAL, Math.round(aggregation.value()));
  }

  private void processMultiBucketAggregation(MultiBucketsAggregation aggregation) {
    LinkedHashMap<String, Long> facet = getOrCreateFacet(aggregation.getName());
    aggregation.getBuckets().forEach(bucket -> {
      if (!bucket.getAggregations().asList().isEmpty()) {
        Aggregation next = bucket.getAggregations().iterator().next();
        if (next instanceof ReverseNested reverseNestedBucket) {
          facet.put(bucket.getKeyAsString(), reverseNestedBucket.getDocCount());
        }
      } else {
        facet.put(bucket.getKeyAsString(), bucket.getDocCount());
      }
    });
  }

  void processFilterAggregation(String name, FilterAggregate aggregation) {
    if (name.startsWith(NO_DATA_PREFIX)) {
      LinkedHashMap<String, Long> facet = getOrCreateFacet(NO_DATA_PREFIX_PATTERN.matcher(name).replaceFirst(""));
      facet.put("NO_DATA", aggregation.docCount());
    }
    processSubAggregationsV2(name, aggregation.aggregations());
  }

  void processGlobalAggregation(String name, GlobalAggregate aggregation) {
    processSubAggregationsV2(name, aggregation.aggregations());
  }

  void processNestedAggregation(String name, NestedAggregate aggregation) {
    processSubAggregationsV2(name, aggregation.aggregations());
  }

  void processChildrenAggregation(String name, co.elastic.clients.elasticsearch._types.aggregations.ChildrenAggregate aggregation) {
    processSubAggregationsV2(name, aggregation.aggregations());
  }

  void processFiltersAggregation(String name, FiltersAggregate aggregation) {
    Buckets<FiltersBucket> buckets = aggregation.buckets();

    if (buckets == null) {
      return;
    }
    if (buckets.isKeyed()) {
      LinkedHashMap<String, Long> facet = getOrCreateFacet(name);
      for (Map.Entry<String, FiltersBucket> entry : buckets.keyed().entrySet()) {
        facet.put(entry.getKey(), entry.getValue().docCount());
        processSubAggregationsV2(entry.getKey(), entry.getValue().aggregations());
      }
    } else {
      for (FiltersBucket bucket : buckets.array()) {
        processSubAggregationsV2(name, bucket.aggregations());
      }
    }
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

  LinkedHashMap<String, Long> getOrCreateFacet(String facetName) {
    return facetsByName.computeIfAbsent(facetName, n -> new LinkedHashMap<>());
  }
}
