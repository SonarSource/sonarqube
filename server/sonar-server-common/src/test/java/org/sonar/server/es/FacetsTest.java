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
import co.elastic.clients.elasticsearch._types.aggregations.ChildrenAggregate;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonarqube.ws.client.issue.IssuesWsParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacetsTest {

  @Spy
  private Facets facets = new Facets(new LinkedHashMap<>(), ZoneId.systemDefault());

  private final String testName = "test_agg";

  private LinkedHashMap<String, Long> mockGetOrCreateFacet(String name) {
    LinkedHashMap<String, Long> mockFacet = new LinkedHashMap<>();
    doReturn(mockFacet).when(facets).getOrCreateFacet(name);
    return mockFacet;
  }

  @Test
  void should_call_processMissingAggregationV2_when_isMissing() {
    Aggregate mockAggregate = getMockAggregate(Aggregate.Kind.Missing);
    MissingAggregate mockMissing = mock(MissingAggregate.class);
    when(mockAggregate.missing()).thenReturn(mockMissing);

    facets.processAggregationV2(testName, mockAggregate);

    verify(facets, times(1)).processMissingAggregationV2(testName, mockMissing);
  }

  @Test
  void should_call_processTermsAggregationV2_when_isSterms() {
    Aggregate mockAggregate = getMockAggregate(Aggregate.Kind.Sterms);
    StringTermsAggregate mockTerms = mock(StringTermsAggregate.class);
    when(mockAggregate.sterms()).thenReturn(mockTerms);

    facets.processAggregationV2(testName, mockAggregate);

    verify(facets, times(1)).processTermsAggregationV2(testName, mockTerms);
  }

  @Test
  void should_call_processFilterAggregation_when_isFilter() {
    Aggregate mockAggregate = getMockAggregate(Aggregate.Kind.Filter);
    FilterAggregate mockFilter = mock(FilterAggregate.class);
    when(mockAggregate.filter()).thenReturn(mockFilter);

    facets.processAggregationV2(testName, mockAggregate);

    verify(facets, times(1)).processFilterAggregation(testName, mockFilter);
  }

  @Test
  void should_call_processDateHistogramV2_when_isDateHistogram() {
    Aggregate mockAggregate = getMockAggregate(Aggregate.Kind.DateHistogram);
    DateHistogramAggregate mockHistogram = mock(DateHistogramAggregate.class);
    when(mockAggregate.dateHistogram()).thenReturn(mockHistogram);

    facets.processAggregationV2(testName, mockAggregate);

    verify(facets, times(1)).processDateHistogramV2(testName, mockHistogram);
  }

  @Test
  void should_call_processSumV2_when_isSum() {
    Aggregate mockAggregate = getMockAggregate(Aggregate.Kind.Sum);
    SumAggregate mockSum = mock(SumAggregate.class);
    when(mockAggregate.sum()).thenReturn(mockSum);

    facets.processAggregationV2(testName, mockAggregate);

    verify(facets, times(1)).processSumV2(testName, mockSum);
  }

  @Test
  void should_call_processGlobalAggregation_when_isGlobal() {
    Aggregate mockAggregate = getMockAggregate(Aggregate.Kind.Global);
    GlobalAggregate mockGlobal = mock(GlobalAggregate.class);
    when(mockAggregate.global()).thenReturn(mockGlobal);

    facets.processAggregationV2(testName, mockAggregate);

    verify(facets, times(1)).processGlobalAggregation(testName, mockGlobal);
  }

  @Test
  void should_call_processNestedAggregation_when_isNested() {
    Aggregate mockAggregate = getMockAggregate(Aggregate.Kind.Nested);
    NestedAggregate mockNested = mock(NestedAggregate.class);
    when(mockAggregate.nested()).thenReturn(mockNested);

    facets.processAggregationV2(testName, mockAggregate);

    verify(facets, times(1)).processNestedAggregation(testName, mockNested);
  }

  @Test
  void should_call_processFiltersAggregation_when_isFilters() {
    Aggregate mockAggregate = getMockAggregate(Aggregate.Kind.Filters);
    FiltersAggregate mockFilters = mock(FiltersAggregate.class);
    when(mockAggregate.filters()).thenReturn(mockFilters);

    facets.processAggregationV2(testName, mockAggregate);

    verify(facets, times(1)).processFiltersAggregation(testName, mockFilters);
  }

  @Test
  void should_call_processChildrenAggregation_when_isChildren() {
    Aggregate mockAggregate = getMockAggregate(Aggregate.Kind.Children);
    ChildrenAggregate mockChildren = mock(ChildrenAggregate.class);
    when(mockAggregate.children()).thenReturn(mockChildren);

    facets.processAggregationV2(testName, mockAggregate);

    verify(facets, times(1)).processChildrenAggregation(testName, mockChildren);
  }

  @Test
  void should_throw_IllegalArgumentException_for_unsupported_type() {
    Aggregate mockAggregate = getMockAggregate(Aggregate.Kind.Boxplot);

    assertThrows(IllegalArgumentException.class, () -> facets.processAggregationV2(testName, mockAggregate),
      "Should throw an IllegalArgumentException for unsupported types.");
  }

  @NotNull
  private static Aggregate getMockAggregate(Aggregate.Kind kind) {
    Aggregate mockAggregate = mock(Aggregate.class);
    when(mockAggregate._kind()).thenReturn(kind);
    return mockAggregate;
  }

  @Test
  void processSumV2_should_put_rounded_value_to_total_key() {
    SumAggregate mockSum = mock(SumAggregate.class);
    when(mockSum.value()).thenReturn(12345.678);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(testName);

    facets.processSumV2(testName, mockSum);

    assertEquals(1, capturedFacet.size());
    assertEquals(12346L, capturedFacet.get(Facets.TOTAL).longValue());
  }

  @Test
  void processGlobalAggregation_should_delegate_to_processSubAggregationsV2() {
    GlobalAggregate mockGlobal = mock(GlobalAggregate.class);
    Map<String, Aggregate> mockSubAggs = Collections.singletonMap("sub_agg", mock(Aggregate.class));
    when(mockGlobal.aggregations()).thenReturn(mockSubAggs);
    doNothing().when(facets)
      .processSubAggregationsV2(anyString(), anyMap());
    facets.processGlobalAggregation(testName, mockGlobal);

    verify(facets, times(1)).processSubAggregationsV2(testName, mockSubAggs);
  }

  @Test
  void processNestedAggregation_should_delegate_to_processSubAggregationsV2() {
    NestedAggregate mockNested = mock(NestedAggregate.class);
    Map<String, Aggregate> mockSubAggs = Collections.singletonMap("sub_agg", mock(Aggregate.class));
    when(mockNested.aggregations()).thenReturn(mockSubAggs);
    doNothing().when(facets)
      .processSubAggregationsV2(anyString(), anyMap());

    facets.processNestedAggregation(testName, mockNested);

    verify(facets, times(1)).processSubAggregationsV2(testName, mockSubAggs);
  }

  @Test
  void processChildrenAggregation_should_delegate_to_processSubAggregationsV2() {
    ChildrenAggregate mockChildren = mock(co.elastic.clients.elasticsearch._types.aggregations.ChildrenAggregate.class);
    Map<String, Aggregate> mockSubAggs = Collections.singletonMap("sub_agg", mock(Aggregate.class));

    when(mockChildren.aggregations()).thenReturn(mockSubAggs);
    doNothing().when(facets)
      .processSubAggregationsV2(anyString(), anyMap());

    facets.processChildrenAggregation(testName, mockChildren);

    verify(facets, times(1)).processSubAggregationsV2(testName, mockSubAggs);
  }

  @Test
  void should_use_effort_sum_when_available_and_is_Sum() {
    final long docCount = 5L;
    final double effortValue = 42.5;
    final long expectedValue = Math.round(effortValue);
    final String aggName = "my_field_missing";
    final String facetName = "my_field";

    MissingAggregate mockAgg = mock(MissingAggregate.class);
    SumAggregate mockSumAgg = mock(SumAggregate.class);
    Aggregate mockEffortAgg = mock(Aggregate.class);

    when(mockAgg.docCount()).thenReturn(docCount);

    when(mockEffortAgg.isSum()).thenReturn(true);
    when(mockEffortAgg.sum()).thenReturn(mockSumAgg);
    when(mockSumAgg.value()).thenReturn(effortValue);

    Map<String, Aggregate> subAggs = Collections.singletonMap(IssuesWsParameters.FACET_MODE_EFFORT, mockEffortAgg);
    when(mockAgg.aggregations()).thenReturn(subAggs);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(facetName);

    facets.processMissingAggregationV2(aggName, mockAgg);

    verify(facets, times(1)).getOrCreateFacet(facetName);

    assertTrue(capturedFacet.containsKey(""), "Facet should contain the key for missing aggregation");
    assertEquals(expectedValue, capturedFacet.get("").longValue(), "Facet value should be the rounded effort sum");
  }

  @Test
  void should_fallback_to_docCount_when_effort_agg_is_not_Sum() {
    final long docCount = 15L;
    final String aggName = "my_field_missing";
    final String facetName = "my_field";
    MissingAggregate mockAgg = mock(MissingAggregate.class);
    Aggregate mockEffortAgg = mock(Aggregate.class);

    when(mockAgg.docCount()).thenReturn(docCount);
    when(mockEffortAgg.isSum()).thenReturn(false);

    Map<String, Aggregate> subAggs = Collections.singletonMap(IssuesWsParameters.FACET_MODE_EFFORT, mockEffortAgg);
    when(mockAgg.aggregations()).thenReturn(subAggs);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(facetName);

    facets.processMissingAggregationV2(aggName, mockAgg);

    verify(facets, times(1)).getOrCreateFacet(facetName);
    assertEquals(docCount, capturedFacet.get("").longValue(), "Facet value should be the doc count as fallback");
  }

  @Test
  void should_fallback_to_docCount_when_effort_subAgg_is_missing() {
    final long docCount = 25L;
    final String aggName = "my_field_missing";
    final String facetName = "my_field";

    MissingAggregate mockAgg = mock(MissingAggregate.class);
    when(mockAgg.docCount()).thenReturn(docCount);

    when(mockAgg.aggregations()).thenReturn(Collections.emptyMap());

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(facetName);

    facets.processMissingAggregationV2(aggName, mockAgg);

    verify(facets, times(1)).getOrCreateFacet(facetName);

    assertEquals(docCount, capturedFacet.get("").longValue(), "Facet value should be the doc count");
  }

  @Test
  void should_do_nothing_when_docCount_is_zero() {
    final String aggName = "my_field_missing";
    MissingAggregate mockAgg = mock(MissingAggregate.class);
    when(mockAgg.docCount()).thenReturn(0L);

    facets.processMissingAggregationV2(aggName, mockAgg);

    verify(facets, never()).getOrCreateFacet(anyString());
    verify(facets, times(1)).processMissingAggregationV2(anyString(), any());
    verifyNoMoreInteractions(facets);
  }

  @Test
  void should_clean_facet_name_by_removing_selected_suffix() {
    final String baseName = "tags";
    final String testNameAgg = baseName + Facets.SELECTED_SUB_AGG_NAME_SUFFIX;
    StringTermsAggregate mockAgg = mock(StringTermsAggregate.class);

    Buckets<StringTermsBucket> mockBuckets = mock(Buckets.class);
    when(mockBuckets.array()).thenReturn(Collections.emptyList());
    when(mockAgg.buckets()).thenReturn(mockBuckets);

    mockGetOrCreateFacet(baseName);

    facets.processTermsAggregationV2(testNameAgg, mockAgg);

    verify(facets, times(1)).getOrCreateFacet(baseName);
  }

  @Test
  void should_clean_facet_name_by_removing_double_underscore_suffix() {
    final String baseName = "tags";
    final String complexName = "module__" + baseName + "__long";
    StringTermsAggregate mockAgg = mock(StringTermsAggregate.class);

    Buckets<StringTermsBucket> mockBuckets = mock(Buckets.class);
    when(mockBuckets.array()).thenReturn(Collections.emptyList());
    when(mockAgg.buckets()).thenReturn(mockBuckets);

    mockGetOrCreateFacet("module");

    facets.processTermsAggregationV2(complexName, mockAgg);

    verify(facets, times(1)).getOrCreateFacet("module");
  }

  @Test
  void should_return_immediately_when_subAggregations_is_empty() {
    final String parentName = "project_name";
    Map<String, Aggregate> emptyAggs = Collections.emptyMap();

    facets.processSubAggregationsV2(parentName, emptyAggs);

    verify(facets, never()).processAggregationV2(anyString(), any(Aggregate.class));

    verify(facets, times(1)).processSubAggregationsV2(anyString(), anyMap());
    verifyNoMoreInteractions(facets);
  }

  private DateHistogramBucket mockBucket(String timestamp, long docCount, Aggregate effortAgg) {
    DateHistogramBucket bucket = mock(DateHistogramBucket.class);
    lenient().when(bucket.docCount()).thenReturn(docCount);
    when(bucket.keyAsString()).thenReturn(timestamp);

    Map<String, Aggregate> subAggs = new HashMap<>();
    if (effortAgg != null) {
      subAggs.put(IssuesWsParameters.FACET_MODE_EFFORT, effortAgg);
    }
    when(bucket.aggregations()).thenReturn(subAggs);
    return bucket;
  }

  private Buckets<DateHistogramBucket> mockBuckets(DateHistogramBucket... buckets) {
    Buckets<DateHistogramBucket> mockBuckets = mock(Buckets.class);
    when(mockBuckets.array()).thenReturn(Arrays.asList(buckets));
    return mockBuckets;
  }

  @Test
  void should_return_immediately_when_buckets_is_null() {
    final String aggName = "date_agg";
    DateHistogramAggregate mockAgg = mock(DateHistogramAggregate.class);
    when(mockAgg.buckets()).thenReturn(null);

    facets.processDateHistogramV2(aggName, mockAgg);

    verify(facets, times(1)).getOrCreateFacet(anyString());
    verify(facets, times(1)).processDateHistogramV2(anyString(), any());
    verifyNoMoreInteractions(facets);
  }

  @Test
  void processDateHistogramV2_should_process_buckets_with_docCount() {
    final String aggName = "createdAt";
    final String timestamp1 = "2025-01-15T10:30:00+0000";
    final String timestamp2 = "2025-01-16T14:45:00+0000";
    final long docCount1 = 10L;
    final long docCount2 = 25L;

    DateHistogramAggregate mockAgg = mock(DateHistogramAggregate.class);
    DateHistogramBucket bucket1 = mockBucket(timestamp1, docCount1, null);
    DateHistogramBucket bucket2 = mockBucket(timestamp2, docCount2, null);

    Buckets<DateHistogramBucket> buckets = mockBuckets(bucket1, bucket2);
    when(mockAgg.buckets()).thenReturn(buckets);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(aggName);

    facets.processDateHistogramV2(aggName, mockAgg);

    verify(facets, times(1)).getOrCreateFacet(aggName);
    assertEquals(2, capturedFacet.size());
    assertEquals(docCount1, capturedFacet.get("2025-01-15").longValue());
    assertEquals(docCount2, capturedFacet.get("2025-01-16").longValue());
  }

  @Test
  void processDateHistogramV2_should_use_effort_sum_when_available() {
    final String aggName = "createdAt";
    final String timestamp = "2025-01-15T10:30:00+0000";
    final long docCount = 5L;
    final double effortValue = 123.6;

    DateHistogramAggregate mockAgg = mock(DateHistogramAggregate.class);
    SumAggregate mockSumAgg = mock(SumAggregate.class);
    Aggregate mockEffortAgg = mock(Aggregate.class);

    when(mockEffortAgg.isSum()).thenReturn(true);
    when(mockEffortAgg.sum()).thenReturn(mockSumAgg);
    when(mockSumAgg.value()).thenReturn(effortValue);

    DateHistogramBucket bucket = mockBucket(timestamp, docCount, mockEffortAgg);
    Buckets<DateHistogramBucket> buckets = mockBuckets(bucket);
    when(mockAgg.buckets()).thenReturn(buckets);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(aggName);

    facets.processDateHistogramV2(aggName, mockAgg);

    assertEquals(1, capturedFacet.size());
    assertEquals(Math.round(effortValue), capturedFacet.get("2025-01-15").longValue());
  }

  @Test
  void processDateHistogramV2_should_fallback_to_docCount_when_effort_not_sum() {
    final String aggName = "createdAt";
    final String timestamp = "2025-01-15T10:30:00+0000";
    final long docCount = 15L;

    DateHistogramAggregate mockAgg = mock(DateHistogramAggregate.class);
    Aggregate mockEffortAgg = mock(Aggregate.class);
    when(mockEffortAgg.isSum()).thenReturn(false);

    DateHistogramBucket bucket = mockBucket(timestamp, docCount, mockEffortAgg);
    Buckets<DateHistogramBucket> buckets = mockBuckets(bucket);
    when(mockAgg.buckets()).thenReturn(buckets);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(aggName);

    facets.processDateHistogramV2(aggName, mockAgg);

    assertEquals(1, capturedFacet.size());
    assertEquals(docCount, capturedFacet.get("2025-01-15").longValue());
  }

  @Test
  void processDateHistogramV2_should_return_when_buckets_is_empty() {
    final String aggName = "createdAt";
    DateHistogramAggregate mockAgg = mock(DateHistogramAggregate.class);
    Buckets<DateHistogramBucket> mockBuckets = mock(Buckets.class);
    when(mockBuckets.array()).thenReturn(Collections.emptyList());
    when(mockAgg.buckets()).thenReturn(mockBuckets);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(aggName);

    facets.processDateHistogramV2(aggName, mockAgg);

    assertEquals(0, capturedFacet.size());
  }

  @Test
  void processSubAggregationsV2_should_process_multiple_aggregations_in_order() {
    final String parentName = "parent_agg";
    Aggregate agg1 = mock(Aggregate.class);
    Aggregate agg2 = mock(Aggregate.class);

    Map<String, Aggregate> subAggs = new LinkedHashMap<>();
    subAggs.put("sub_agg_1", agg1);
    subAggs.put("sub_agg_2", agg2);

    doNothing().when(facets).processAggregationV2(anyString(), any(Aggregate.class));

    facets.processSubAggregationsV2(parentName, subAggs);

    verify(facets, times(1)).processAggregationV2("sub_agg_1", agg1);
    verify(facets, times(1)).processAggregationV2("sub_agg_2", agg2);
  }

  @Test
  void processSubAggregationsV2_should_prioritize_matching_parent_name() {
    final String parentName = "severity_filter";
    Aggregate matchingAgg = mock(Aggregate.class);
    Aggregate otherAgg = mock(Aggregate.class);

    Map<String, Aggregate> subAggs = new LinkedHashMap<>();
    subAggs.put("other_agg", otherAgg);
    subAggs.put("severity_filter", matchingAgg);

    doNothing().when(facets).processAggregationV2(anyString(), any(Aggregate.class));

    facets.processSubAggregationsV2(parentName, subAggs);

    verify(facets, times(1)).processAggregationV2("severity_filter", matchingAgg);
    verify(facets, times(1)).processAggregationV2("other_agg", otherAgg);
  }

  @Test
  void processTermsAggregationV2_should_process_buckets_with_docCount() {
    final String aggName = "tags";
    final String key1 = "security";
    final String key2 = "performance";
    final long docCount1 = 10L;
    final long docCount2 = 25L;

    StringTermsAggregate mockAgg = mock(StringTermsAggregate.class);
    StringTermsBucket bucket1 = mock(StringTermsBucket.class);
    StringTermsBucket bucket2 = mock(StringTermsBucket.class);

    when(bucket1.key()).thenReturn(co.elastic.clients.elasticsearch._types.FieldValue.of(key1));
    when(bucket1.docCount()).thenReturn(docCount1);
    when(bucket1.aggregations()).thenReturn(Collections.emptyMap());

    when(bucket2.key()).thenReturn(co.elastic.clients.elasticsearch._types.FieldValue.of(key2));
    when(bucket2.docCount()).thenReturn(docCount2);
    when(bucket2.aggregations()).thenReturn(Collections.emptyMap());

    Buckets<StringTermsBucket> mockBuckets = mock(Buckets.class);
    when(mockBuckets.array()).thenReturn(Arrays.asList(bucket1, bucket2));
    when(mockAgg.buckets()).thenReturn(mockBuckets);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(aggName);

    facets.processTermsAggregationV2(aggName, mockAgg);

    verify(facets, times(1)).getOrCreateFacet(aggName);
    assertEquals(2, capturedFacet.size());
    assertEquals(docCount1, capturedFacet.get(key1).longValue());
    assertEquals(docCount2, capturedFacet.get(key2).longValue());
  }

  @Test
  void processTermsAggregationV2_should_use_sum_aggregation_when_present() {
    final String aggName = "tags";
    final String key = "security";
    final double sumValue = 42.7;

    StringTermsAggregate mockAgg = mock(StringTermsAggregate.class);
    StringTermsBucket bucket = mock(StringTermsBucket.class);
    SumAggregate mockSumAgg = mock(SumAggregate.class);
    Aggregate mockSubAgg = mock(Aggregate.class);

    when(bucket.key()).thenReturn(co.elastic.clients.elasticsearch._types.FieldValue.of(key));
    when(mockSubAgg.isSum()).thenReturn(true);
    when(mockSubAgg.sum()).thenReturn(mockSumAgg);
    when(mockSumAgg.value()).thenReturn(sumValue);
    when(bucket.aggregations()).thenReturn(Collections.singletonMap("effort", mockSubAgg));

    Buckets<StringTermsBucket> mockBuckets = mock(Buckets.class);
    when(mockBuckets.array()).thenReturn(Collections.singletonList(bucket));
    when(mockAgg.buckets()).thenReturn(mockBuckets);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(aggName);

    facets.processTermsAggregationV2(aggName, mockAgg);

    assertEquals(1, capturedFacet.size());
    assertEquals(Math.round(sumValue), capturedFacet.get(key).longValue());
  }

  @Test
  void processTermsAggregationV2_should_use_docCount_when_subAgg_not_sum() {
    final String aggName = "tags";
    final String key = "security";
    final long docCount = 15L;

    StringTermsAggregate mockAgg = mock(StringTermsAggregate.class);
    StringTermsBucket bucket = mock(StringTermsBucket.class);
    Aggregate mockSubAgg = mock(Aggregate.class);

    when(bucket.key()).thenReturn(co.elastic.clients.elasticsearch._types.FieldValue.of(key));
    when(bucket.docCount()).thenReturn(docCount);
    when(mockSubAgg.isSum()).thenReturn(false);
    when(bucket.aggregations()).thenReturn(Collections.singletonMap("other", mockSubAgg));

    Buckets<StringTermsBucket> mockBuckets = mock(Buckets.class);
    when(mockBuckets.array()).thenReturn(Collections.singletonList(bucket));
    when(mockAgg.buckets()).thenReturn(mockBuckets);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(aggName);

    facets.processTermsAggregationV2(aggName, mockAgg);

    assertEquals(1, capturedFacet.size());
    assertEquals(docCount, capturedFacet.get(key).longValue());
  }

  @Test
  void processTermsAggregationV2_should_return_when_buckets_null() {
    final String aggName = "tags";
    StringTermsAggregate mockAgg = mock(StringTermsAggregate.class);
    when(mockAgg.buckets()).thenReturn(null);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(aggName);

    facets.processTermsAggregationV2(aggName, mockAgg);

    assertEquals(0, capturedFacet.size());
  }

  @Test
  void processTermsAggregationV2_should_return_when_buckets_empty() {
    final String aggName = "tags";
    StringTermsAggregate mockAgg = mock(StringTermsAggregate.class);
    Buckets<StringTermsBucket> mockBuckets = mock(Buckets.class);
    when(mockBuckets.array()).thenReturn(Collections.emptyList());
    when(mockAgg.buckets()).thenReturn(mockBuckets);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(aggName);

    facets.processTermsAggregationV2(aggName, mockAgg);

    assertEquals(0, capturedFacet.size());
  }

  @Test
  void processFiltersAggregation_should_handle_keyed_buckets() {
    final String aggName = "filters_agg";
    final String key1 = "filter1";
    final String key2 = "filter2";
    final long docCount1 = 10L;
    final long docCount2 = 20L;

    FiltersAggregate mockAgg = mock(FiltersAggregate.class);
    FiltersBucket bucket1 = mock(co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket.class);
    FiltersBucket bucket2 = mock(co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket.class);

    when(bucket1.docCount()).thenReturn(docCount1);
    when(bucket1.aggregations()).thenReturn(Collections.emptyMap());
    when(bucket2.docCount()).thenReturn(docCount2);
    when(bucket2.aggregations()).thenReturn(Collections.emptyMap());

    Map<String, co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket> keyedBuckets = new LinkedHashMap<>();
    keyedBuckets.put(key1, bucket1);
    keyedBuckets.put(key2, bucket2);

    Buckets<co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket> mockBuckets = mock(Buckets.class);
    when(mockBuckets.isKeyed()).thenReturn(true);
    when(mockBuckets.keyed()).thenReturn(keyedBuckets);
    when(mockAgg.buckets()).thenReturn(mockBuckets);

    LinkedHashMap<String, Long> capturedFacet = mockGetOrCreateFacet(aggName);
    doNothing().when(facets).processSubAggregationsV2(anyString(), anyMap());

    facets.processFiltersAggregation(aggName, mockAgg);

    verify(facets, times(1)).getOrCreateFacet(aggName);
    assertEquals(2, capturedFacet.size());
    assertEquals(docCount1, capturedFacet.get(key1).longValue());
    assertEquals(docCount2, capturedFacet.get(key2).longValue());
    verify(facets, times(1)).processSubAggregationsV2(eq(key1), anyMap());
    verify(facets, times(1)).processSubAggregationsV2(eq(key2), anyMap());
  }

  @Test
  void processFiltersAggregation_should_handle_array_buckets() {
    final String aggName = "filters_agg";

    FiltersAggregate mockAgg = mock(FiltersAggregate.class);
    FiltersBucket bucket = mock(FiltersBucket.class);

    Map<String, Aggregate> subAggs = Collections.singletonMap("sub", mock(Aggregate.class));
    when(bucket.aggregations()).thenReturn(subAggs);

    Buckets<FiltersBucket> mockBuckets = mock(Buckets.class);
    when(mockBuckets.isKeyed()).thenReturn(false);
    when(mockBuckets.array()).thenReturn(Collections.singletonList(bucket));
    when(mockAgg.buckets()).thenReturn(mockBuckets);

    doNothing().when(facets).processSubAggregationsV2(anyString(), anyMap());

    facets.processFiltersAggregation(aggName, mockAgg);

    verify(facets, times(1)).processSubAggregationsV2(aggName, subAggs);
  }

  @Test
  void processFiltersAggregation_should_return_when_buckets_null() {
    final String aggName = "filters_agg";
    FiltersAggregate mockAgg = mock(FiltersAggregate.class);
    when(mockAgg.buckets()).thenReturn(null);

    facets.processFiltersAggregation(aggName, mockAgg);

    verify(facets, never()).getOrCreateFacet(anyString());
    verify(facets, never()).processSubAggregationsV2(anyString(), anyMap());
  }

}
