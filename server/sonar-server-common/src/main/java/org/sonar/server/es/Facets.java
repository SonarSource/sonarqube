/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import co.elastic.clients.elasticsearch._types.aggregations.ChildrenAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.FilterAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FiltersAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket;
import co.elastic.clients.elasticsearch._types.aggregations.GlobalAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MissingAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.RangeAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.RangeBucket;
import co.elastic.clients.elasticsearch._types.aggregations.ReverseNestedAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.time.Instant;
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

  public <T> Facets(SearchResponse<T> response, ZoneId timeZone) {
    this.facetsByName = new LinkedHashMap<>();
    this.timeZone = timeZone;
    Map<String, Aggregate> aggregations = response.aggregations();
    if (aggregations != null) {
      for (Map.Entry<String, Aggregate> entry : aggregations.entrySet()) {
        processAggregation(entry.getKey(), entry.getValue());
      }
    }
  }

  void processAggregation(String name, Aggregate aggregate) {
    switch (aggregate._kind()) {
      case Missing -> processMissingAggregation(name, aggregate.missing());
      case Sterms -> processTermsAggregation(name, aggregate.sterms());
      case Lterms -> processLongTermsAggregation(name, aggregate.lterms());
      case Filter -> processFilterAggregation(name, aggregate.filter());
      case DateHistogram -> processDateHistogram(name, aggregate.dateHistogram());
      case Sum -> processSum(name, aggregate.sum());
      case Global -> processGlobalAggregation(name, aggregate.global());
      case Nested -> processNestedAggregation(name, aggregate.nested());
      case ReverseNested -> processReverseNestedAggregation(name, aggregate.reverseNested());
      case Filters -> processFiltersAggregation(name, aggregate.filters());
      case Children -> processChildrenAggregation(name, aggregate.children());
      case Range -> processRangeAggregation(name, aggregate.range());
      default -> throw new IllegalArgumentException("Aggregation type not supported yet: " + aggregate.getClass());
    }
  }

  void processRangeAggregation(String name, RangeAggregate aggregation) {
    LinkedHashMap<String, Long> facet = getOrCreateFacet(name);
    Buckets<RangeBucket> buckets = aggregation.buckets();
    if (buckets == null) {
      return;
    }
    for (RangeBucket bucket : buckets.array()) {
      facet.put(rangeBucketKey(bucket), bucket.docCount());
    }
  }

  private static String rangeBucketKey(RangeBucket bucket) {
    if (bucket.key() != null) {
      return bucket.key();
    }
    String from = bucket.from() != null ? bucket.from().toString() : "*";
    String to = bucket.to() != null ? bucket.to().toString() : "*";
    return from + "-" + to;
  }

  void processMissingAggregation(String name, MissingAggregate aggregation) {
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

  void processLongTermsAggregation(String name, LongTermsAggregate aggregation) {
    String facetName = name;
    if (facetName.contains("__") && !facetName.startsWith("__")) {
      facetName = facetName.substring(0, facetName.indexOf("__"));
    }
    facetName = facetName.replace(SELECTED_SUB_AGG_NAME_SUFFIX, "");
    LinkedHashMap<String, Long> facet = getOrCreateFacet(facetName);

    Buckets<LongTermsBucket> buckets = aggregation.buckets();
    if (buckets == null || buckets.array().isEmpty()) {
      return;
    }

    for (LongTermsBucket bucket : buckets.array()) {
      String key = bucket.keyAsString() != null ? bucket.keyAsString() : Long.toString(bucket.key());
      Map<String, Aggregate> subAggs = bucket.aggregations();
      if (subAggs != null && subAggs.size() == 1) {
        Aggregate subAgg = subAggs.values().iterator().next();
        if (subAgg.isSum()) {
          facet.put(key, Math.round(subAgg.sum().value()));
        } else {
          facet.put(key, bucket.docCount());
        }
      } else {
        facet.put(key, bucket.docCount());
      }
    }
  }

  void processTermsAggregation(String name, StringTermsAggregate aggregation) {
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

  void processSubAggregations(String parentName, Map<String, Aggregate> subAggregations) {
    if (subAggregations.isEmpty()) {
      return;
    }

    List<Map.Entry<String, Aggregate>> orderedEntries = new ArrayList<>(subAggregations.entrySet());
    orderedEntries.sort(
      Comparator.comparing(
        (Map.Entry<String, Aggregate> entry) -> isNameMatchingParent(parentName, entry.getKey()))
        .reversed());

    for (Map.Entry<String, Aggregate> entry : orderedEntries) {
      // For NO_DATA filter sub-aggregations, use the sub-aggregation's own name so the NO_DATA_PREFIX check fires.
      String childName = entry.getKey().startsWith(NO_DATA_PREFIX) ? entry.getKey() : parentName;
      processAggregation(childName, entry.getValue());
    }
  }

  static boolean isNameMatchingParent(String parentName, String aggregationName) {
    return aggregationName.equals(parentName) ||
      aggregationName.equals(FILTER_BY_RULE_PREFIX + parentName.replace(FILTER_SUFFIX, ""));
  }

  void processDateHistogram(String name, DateHistogramAggregate aggregation) {
    LinkedHashMap<String, Long> facet = getOrCreateFacet(name);
    Buckets<DateHistogramBucket> buckets = aggregation.buckets();
    if (buckets == null || buckets.array().isEmpty()) {
      return;
    }
    for (DateHistogramBucket bucket : buckets.array()) {
      // The ES8 client only populates keyAsString when the date_histogram aggregation specifies
      // an explicit `format`. Fall back to formatting bucket.key() (epoch millis) so that buckets
      // are never silently dropped — matches the ES7 client behavior where key_as_string was
      // always available.
      String day = bucket.keyAsString() != null
        ? dateTimeToDate(bucket.keyAsString(), timeZone)
        : epochMillisToDate(bucket.key(), timeZone);
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

  static String dateTimeToDate(String timestamp, ZoneId timeZone) {
    Date date = parseDateTime(timestamp);
    return date.toInstant().atZone(timeZone).toLocalDate().toString();
  }

  static String epochMillisToDate(long epochMillis, ZoneId timeZone) {
    return Instant.ofEpochMilli(epochMillis).atZone(timeZone).toLocalDate().toString();
  }

  void processSum(String name, SumAggregate aggregation) {
    getOrCreateFacet(name).put(TOTAL, Math.round(aggregation.value()));
  }

  void processFilterAggregation(String name, FilterAggregate aggregation) {
    if (name.startsWith(NO_DATA_PREFIX)) {
      LinkedHashMap<String, Long> facet = getOrCreateFacet(NO_DATA_PREFIX_PATTERN.matcher(name).replaceFirst(""));
      facet.put("NO_DATA", aggregation.docCount());
    }
    processSubAggregations(name, aggregation.aggregations());
  }

  void processGlobalAggregation(String name, GlobalAggregate aggregation) {
    processSubAggregations(name, aggregation.aggregations());
  }

  void processNestedAggregation(String name, NestedAggregate aggregation) {
    processSubAggregations(name, aggregation.aggregations());
  }

  void processReverseNestedAggregation(String name, ReverseNestedAggregate aggregation) {
    processSubAggregations(name, aggregation.aggregations());
  }

  void processChildrenAggregation(String name, ChildrenAggregate aggregation) {
    processSubAggregations(name, aggregation.aggregations());
  }

  public void processFiltersAggregation(String name, FiltersAggregate aggregation) {
    Buckets<FiltersBucket> buckets = aggregation.buckets();

    if (buckets == null) {
      return;
    }

    if (buckets.isKeyed()) {
      processKeyedFiltersBuckets(name, buckets.keyed());
    } else {
      processNonKeyedFiltersBuckets(name, buckets.array());
    }
  }

  private void processKeyedFiltersBuckets(String name, Map<String, FiltersBucket> keyedBuckets) {
    LinkedHashMap<String, Long> facet = getOrCreateFacet(name);

    for (Map.Entry<String, FiltersBucket> entry : keyedBuckets.entrySet()) {
      String bucketKey = entry.getKey();
      FiltersBucket bucket = entry.getValue();
      long docCount = getBucketDocCount(bucket);

      facet.put(bucketKey, docCount);
      processSubAggregations(bucketKey, bucket.aggregations());
    }
  }

  private static long getBucketDocCount(FiltersBucket bucket) {
    long docCount = bucket.docCount();

    // Check for a reverse nested aggregation to use its doc_count
    Map<String, Aggregate> subAggs = bucket.aggregations();
    if (subAggs != null && !subAggs.isEmpty()) {
      for (Aggregate subAgg : subAggs.values()) {
        if (subAgg.isReverseNested()) {
          // Use the doc count from the reverse nested aggregation
          docCount = subAgg.reverseNested().docCount();
          break;
        }
      }
    }
    return docCount;
  }

  private void processNonKeyedFiltersBuckets(String name, List<FiltersBucket> arrayBuckets) {
    for (FiltersBucket bucket : arrayBuckets) {
      // Non-keyed buckets occur when a Filters aggregation is defined in Elasticsearch with "keyed: false"
      // (the default is "keyed: false" in some versions or APIs). In this case, buckets are returned as an array
      // without explicit keys, and only the order of filters in the request can be used to associate results.
      // Since we do not have a specific key for each bucket, we cannot populate the facet map with meaningful keys.
      // Therefore, we only process sub-aggregations for each bucket. For more details, see:
      // https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-filters-aggregation.html
      processSubAggregations(name, bucket.aggregations());
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
