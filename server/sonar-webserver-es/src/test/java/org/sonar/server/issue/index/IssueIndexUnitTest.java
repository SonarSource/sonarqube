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
package org.sonar.server.issue.index;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.FilterAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.ValueCountAggregate;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.util.ObjectBuilder;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.joda.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_DEFAULT_VALUE;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;

class IssueIndexUnitTest {

  private final EsClient esClient = mock(EsClient.class);
  private final System2 system2 = mock(System2.class);
  private final UserSession userSession = mock(UserSession.class);
  private final WebAuthorizationTypeSupport authorizationTypeSupport = mock(WebAuthorizationTypeSupport.class);
  private final Configuration config = mock(Configuration.class);

  private IssueIndex underTest;

  @BeforeEach
  void setUp() {
    when(config.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.empty());
    underTest = new IssueIndex(esClient, system2, userSession, authorizationTypeSupport, config);
  }

  // ---------- computeDateHistogramBucketSize ----------

  @Test
  void computeDateHistogramBucketSize_returnsDay_whenShorterThan20Days() {
    assertThat(IssueIndex.computeDateHistogramBucketSize(Duration.standardDays(1))).isEqualTo(CalendarInterval.Day);
    assertThat(IssueIndex.computeDateHistogramBucketSize(Duration.standardDays(5))).isEqualTo(CalendarInterval.Day);
    assertThat(IssueIndex.computeDateHistogramBucketSize(Duration.standardDays(19))).isEqualTo(CalendarInterval.Day);
  }

  @Test
  void computeDateHistogramBucketSize_returnsWeek_whenBetween20DaysAnd20Weeks() {
    assertThat(IssueIndex.computeDateHistogramBucketSize(Duration.standardDays(20))).isEqualTo(CalendarInterval.Week);
    assertThat(IssueIndex.computeDateHistogramBucketSize(Duration.standardDays(50))).isEqualTo(CalendarInterval.Week);
  }

  @Test
  void computeDateHistogramBucketSize_returnsMonth_whenBetween20WeeksAnd20Months() {
    assertThat(IssueIndex.computeDateHistogramBucketSize(Duration.standardDays(20 * 7))).isEqualTo(CalendarInterval.Month);
    assertThat(IssueIndex.computeDateHistogramBucketSize(Duration.standardDays(200))).isEqualTo(CalendarInterval.Month);
  }

  @Test
  void computeDateHistogramBucketSize_returnsYear_whenLongerThan20Months() {
    assertThat(IssueIndex.computeDateHistogramBucketSize(Duration.standardDays(20 * 30))).isEqualTo(CalendarInterval.Year);
    assertThat(IssueIndex.computeDateHistogramBucketSize(Duration.standardDays(1000))).isEqualTo(CalendarInterval.Year);
  }

  // ---------- searchProjectStatistics guards (no ES call) ----------

  @Test
  void searchProjectStatistics_throwsIllegalState_whenSizeMismatch() {
    List<String> projectUuids = List.of("p1", "p2");
    List<Long> createdAfter = List.of(1000L);

    assertThatThrownBy(() -> underTest.searchProjectStatistics(projectUuids, createdAfter, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Expected same size");
  }

  @Test
  void searchProjectStatistics_returnsEmpty_whenInputEmpty() {
    assertThat(underTest.searchProjectStatistics(List.of(), List.of(), null)).isEmpty();
  }

  // ---------- validateCreationDateBounds via search() ----------

  @Test
  void search_throwsIllegalArgument_whenCreatedAfterIsInTheFuture() {
    when(system2.now()).thenReturn(1_000_000L);
    IssueQuery query = IssueQuery.builder()
      .createdAfter(new Date(Long.MAX_VALUE))
      .build();
    SearchOptions options = new SearchOptions();

    assertThatThrownBy(() -> underTest.search(query, options))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Start bound cannot be in the future");
  }

  @Test
  void search_throwsIllegalArgument_whenCreatedAfterIsAfterCreatedBefore() {
    long now = System.currentTimeMillis();
    when(system2.now()).thenReturn(now + Duration.standardDays(365).getMillis());
    IssueQuery query = IssueQuery.builder()
      .createdAfter(new Date(now + Duration.standardDays(10).getMillis()))
      .createdBefore(new Date(now))
      .build();
    SearchOptions options = new SearchOptions();

    assertThatThrownBy(() -> underTest.search(query, options))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Start bound cannot be larger or equal to end bound");
  }

  // ---------- search() happy path with mocked ES ----------

  @Test
  @SuppressWarnings("unchecked")
  void search_returnsMockedResponse() {
    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);
    when(system2.now()).thenReturn(System.currentTimeMillis() + Duration.standardDays(1).getMillis());

    IssueQuery query = IssueQuery.builder().build();
    SearchResponse<Object> result = underTest.search(query, new SearchOptions());

    assertThat(result).isSameAs(mockResponse);
  }

  // ---------- searchTags with mocked ES ----------

  @Test
  @SuppressWarnings("unchecked")
  void searchTags_returnsTagsFromAggregation() {
    StringTermsBucket bucket1 = mock(StringTermsBucket.class);
    StringTermsBucket bucket2 = mock(StringTermsBucket.class);
    co.elastic.clients.elasticsearch._types.FieldValue fv1 = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
    co.elastic.clients.elasticsearch._types.FieldValue fv2 = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
    when(fv1.stringValue()).thenReturn("security");
    when(fv2.stringValue()).thenReturn("performance");
    when(bucket1.key()).thenReturn(fv1);
    when(bucket2.key()).thenReturn(fv2);

    StringTermsAggregate sterms = mock(StringTermsAggregate.class);
    co.elastic.clients.elasticsearch._types.aggregations.Buckets<StringTermsBucket> buckets = mock(
      co.elastic.clients.elasticsearch._types.aggregations.Buckets.class);
    when(buckets.array()).thenReturn(List.of(bucket1, bucket2));
    when(sterms.buckets()).thenReturn(buckets);

    Aggregate agg = mock(Aggregate.class);
    when(agg.sterms()).thenReturn(sterms);

    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Map.of("_ref", agg));
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<String> tags = underTest.searchTags(IssueQuery.builder().build(), null, 10);

    assertThat(tags).containsExactly("security", "performance");
  }

  @Test
  @SuppressWarnings("unchecked")
  void searchTags_withTextQuery_returnsFilteredTags() {
    StringTermsBucket bucket = mock(StringTermsBucket.class);
    co.elastic.clients.elasticsearch._types.FieldValue fv = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
    when(fv.stringValue()).thenReturn("security");
    when(bucket.key()).thenReturn(fv);

    StringTermsAggregate sterms = mock(StringTermsAggregate.class);
    co.elastic.clients.elasticsearch._types.aggregations.Buckets<StringTermsBucket> buckets = mock(
      co.elastic.clients.elasticsearch._types.aggregations.Buckets.class);
    when(buckets.array()).thenReturn(List.of(bucket));
    when(sterms.buckets()).thenReturn(buckets);

    Aggregate agg = mock(Aggregate.class);
    when(agg.sterms()).thenReturn(sterms);

    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Map.of("_ref", agg));
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<String> tags = underTest.searchTags(IssueQuery.builder().build(), "secu", 5);

    assertThat(tags).containsExactly("security");
  }

  // ---------- countTags with mocked ES ----------

  @Test
  @SuppressWarnings("unchecked")
  void countTags_returnsTagCountsMap() {
    StringTermsBucket bucket1 = mock(StringTermsBucket.class);
    StringTermsBucket bucket2 = mock(StringTermsBucket.class);
    co.elastic.clients.elasticsearch._types.FieldValue fv1 = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
    co.elastic.clients.elasticsearch._types.FieldValue fv2 = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
    when(fv1.stringValue()).thenReturn("bug");
    when(fv2.stringValue()).thenReturn("security");
    when(bucket1.key()).thenReturn(fv1);
    when(bucket2.key()).thenReturn(fv2);
    when(bucket1.docCount()).thenReturn(5L);
    when(bucket2.docCount()).thenReturn(3L);

    StringTermsAggregate sterms = mock(StringTermsAggregate.class);
    co.elastic.clients.elasticsearch._types.aggregations.Buckets<StringTermsBucket> buckets = mock(
      co.elastic.clients.elasticsearch._types.aggregations.Buckets.class);
    when(buckets.array()).thenReturn(List.of(bucket1, bucket2));
    when(sterms.buckets()).thenReturn(buckets);

    Aggregate agg = mock(Aggregate.class);
    when(agg.sterms()).thenReturn(sterms);

    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Map.of("_ref", agg));
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    Map<String, Long> result = underTest.countTags(IssueQuery.builder().build(), 10);

    assertThat(result).containsEntry("bug", 5L).containsEntry("security", 3L);
  }

  // ---------- searchAuthors with mocked ES ----------

  @Test
  @SuppressWarnings("unchecked")
  void searchAuthors_returnsAuthorList() {
    StringTermsBucket bucket = mock(StringTermsBucket.class);
    co.elastic.clients.elasticsearch._types.FieldValue fv = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
    when(fv.stringValue()).thenReturn("john.doe");
    when(bucket.key()).thenReturn(fv);

    StringTermsAggregate sterms = mock(StringTermsAggregate.class);
    co.elastic.clients.elasticsearch._types.aggregations.Buckets<StringTermsBucket> buckets = mock(
      co.elastic.clients.elasticsearch._types.aggregations.Buckets.class);
    when(buckets.array()).thenReturn(List.of(bucket));
    when(sterms.buckets()).thenReturn(buckets);

    Aggregate agg = mock(Aggregate.class);
    when(agg.sterms()).thenReturn(sterms);

    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Map.of("_ref", agg));
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<String> authors = underTest.searchAuthors(IssueQuery.builder().build(), null, 10);

    assertThat(authors).containsExactly("john.doe");
  }

  // ---------- searchProjectStatistics with mocked ES ----------

  @Test
  @SuppressWarnings("unchecked")
  void searchProjectStatistics_returnsResults_whenCountAboveZero() {
    co.elastic.clients.elasticsearch._types.FieldValue branchFv = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
    when(branchFv.stringValue()).thenReturn("branch-uuid-1");

    ValueCountAggregate countAgg = mock(ValueCountAggregate.class);
    when(countAgg.value()).thenReturn(3.0);

    co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate maxAgg = mock(
      co.elastic.clients.elasticsearch._types.aggregations.MaxAggregate.class);
    when(maxAgg.value()).thenReturn(1_700_000_000_000.0);

    Aggregate countAggregate = mock(Aggregate.class);
    when(countAggregate.valueCount()).thenReturn(countAgg);

    Aggregate maxAggregate = mock(Aggregate.class);
    when(maxAggregate.max()).thenReturn(maxAgg);

    StringTermsBucket branchBucket = mock(StringTermsBucket.class);
    when(branchBucket.key()).thenReturn(branchFv);
    when(branchBucket.aggregations()).thenReturn(Map.of(
      "count", countAggregate,
      "maxFuncCreatedAt", maxAggregate));

    co.elastic.clients.elasticsearch._types.aggregations.Buckets<StringTermsBucket> branchBuckets = mock(
      co.elastic.clients.elasticsearch._types.aggregations.Buckets.class);
    when(branchBuckets.array()).thenReturn(List.of(branchBucket));

    StringTermsAggregate branchTerms = mock(StringTermsAggregate.class);
    when(branchTerms.buckets()).thenReturn(branchBuckets);

    Aggregate branchTermsAgg = mock(Aggregate.class);
    when(branchTermsAgg.sterms()).thenReturn(branchTerms);

    FilterAggregate projectFilter = mock(FilterAggregate.class);
    when(projectFilter.aggregations()).thenReturn(Map.of("branchUuid", branchTermsAgg));

    Aggregate projectAgg = mock(Aggregate.class);
    when(projectAgg.filter()).thenReturn(projectFilter);

    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Map.of("project-uuid-1", projectAgg));
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<ProjectStatistics> results = underTest.searchProjectStatistics(
      List.of("project-uuid-1"), List.of(0L), null);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getProjectUuid()).isEqualTo("branch-uuid-1");
    assertThat(results.get(0).getIssueCount()).isEqualTo(3L);
  }

  @Test
  @SuppressWarnings("unchecked")
  void searchProjectStatistics_skipsResults_whenCountIsZero() {
    co.elastic.clients.elasticsearch._types.FieldValue branchFv = mock(co.elastic.clients.elasticsearch._types.FieldValue.class);
    when(branchFv.stringValue()).thenReturn("branch-uuid-1");

    ValueCountAggregate countAgg = mock(ValueCountAggregate.class);
    when(countAgg.value()).thenReturn(0.0);

    Aggregate countAggregate = mock(Aggregate.class);
    when(countAggregate.valueCount()).thenReturn(countAgg);

    StringTermsBucket branchBucket = mock(StringTermsBucket.class);
    when(branchBucket.key()).thenReturn(branchFv);
    when(branchBucket.aggregations()).thenReturn(Map.of("count", countAggregate));

    co.elastic.clients.elasticsearch._types.aggregations.Buckets<StringTermsBucket> branchBuckets = mock(
      co.elastic.clients.elasticsearch._types.aggregations.Buckets.class);
    when(branchBuckets.array()).thenReturn(List.of(branchBucket));

    StringTermsAggregate branchTerms = mock(StringTermsAggregate.class);
    when(branchTerms.buckets()).thenReturn(branchBuckets);

    Aggregate branchTermsAgg = mock(Aggregate.class);
    when(branchTermsAgg.sterms()).thenReturn(branchTerms);

    FilterAggregate projectFilter = mock(FilterAggregate.class);
    when(projectFilter.aggregations()).thenReturn(Map.of("branchUuid", branchTermsAgg));

    Aggregate projectAgg = mock(Aggregate.class);
    when(projectAgg.filter()).thenReturn(projectFilter);

    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Map.of("project-uuid-1", projectAgg));
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<ProjectStatistics> results = underTest.searchProjectStatistics(
      List.of("project-uuid-1"), List.of(0L), null);

    assertThat(results).isEmpty();
  }

  // ---------- security reports with mocked ES ----------

  @Test
  @SuppressWarnings("unchecked")
  void getCweTop25Reports_returnsEmptyList_whenNoAggregations() {
    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Collections.emptyMap());
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<SecurityStandardCategoryStatistics> result = underTest.getCweTop25Reports("project-uuid", false);

    assertThat(result).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void getSonarSourceReport_returnsEmptyList_whenNoAggregations() {
    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Collections.emptyMap());
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<SecurityStandardCategoryStatistics> result = underTest.getSonarSourceReport("project-uuid", false, false);

    assertThat(result).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void getPciDssReport_returnsEmptyList_whenNoAggregations() {
    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Collections.emptyMap());
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<SecurityStandardCategoryStatistics> result = underTest.getPciDssReport(
      "project-uuid", false, org.sonar.api.server.rule.RulesDefinition.PciDssVersion.V3_2);

    assertThat(result).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void getOwaspTop10Report_returnsEmptyList_whenNoAggregations() {
    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Collections.emptyMap());
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<SecurityStandardCategoryStatistics> result = underTest.getOwaspTop10Report(
      "project-uuid", false, false, org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version.Y2021);

    assertThat(result).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void getCasaReport_returnsEmptyList_whenNoAggregations() {
    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Collections.emptyMap());
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<SecurityStandardCategoryStatistics> result = underTest.getCasaReport("project-uuid", false);

    assertThat(result).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void getStigReport_returnsEmptyList_whenNoAggregations() {
    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Collections.emptyMap());
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<SecurityStandardCategoryStatistics> result = underTest.getStigReport(
      "project-uuid", false, org.sonar.api.server.rule.RulesDefinition.StigVersion.ASD_V5R3);

    assertThat(result).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void getOwaspAsvsReport_returnsEmptyList_whenNoAggregations() {
    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Collections.emptyMap());
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<SecurityStandardCategoryStatistics> result = underTest.getOwaspAsvsReport(
      "project-uuid", false, org.sonar.api.server.rule.RulesDefinition.OwaspAsvsVersion.V4_0, 1);

    assertThat(result).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void getOwaspMobileTop10Report_returnsEmptyList_whenNoAggregations() {
    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(mockResponse.aggregations()).thenReturn(Collections.emptyMap());
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);

    List<SecurityStandardCategoryStatistics> result = underTest.getOwaspMobileTop10Report(
      "project-uuid", false, false, org.sonar.api.server.rule.RulesDefinition.OwaspMobileTop10Version.Y2024);

    assertThat(result).isEmpty();
  }

  // ---------- MQR mode affects the security-category query ----------

  /**
   * In standard (non-MQR) mode, the security-category clause used by security facet/filters matches issues whose
   * {@code type} is {@code VULNERABILITY} or {@code SECURITY_HOTSPOT}. A CWE filter forces that clause into the built request.
   */
  @Test
  void search_withCweFilter_standardMode_buildsTypeBasedSecurityCategoryQuery() {
    when(config.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(false));
    underTest = new IssueIndex(esClient, system2, userSession, authorizationTypeSupport, config);
    mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().cwe(List.of("89")).build();

    underTest.search(query, new SearchOptions());

    String requestJson = captureSearchRequestJson();
    assertThat(requestJson)
      .contains("VULNERABILITY")
      .contains("SECURITY_HOTSPOT");
  }

  /**
   * In MQR mode, the same security-category clause instead matches issues with a SECURITY software-quality impact (or a
   * security hotspot), so the built request must reference the nested {@code impacts.softwareQuality} field and must not fall
   * back to matching the {@code VULNERABILITY} type.
   */
  @Test
  void search_withCweFilter_mqrMode_buildsImpactBasedSecurityCategoryQuery() {
    when(config.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(true));
    underTest = new IssueIndex(esClient, system2, userSession, authorizationTypeSupport, config);
    mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().cwe(List.of("89")).build();

    underTest.search(query, new SearchOptions());

    String requestJson = captureSearchRequestJson();
    assertThat(requestJson)
      .contains("impacts.softwareQuality")
      .contains("SECURITY")
      .doesNotContain("VULNERABILITY");
  }

  @Test
  void search_withMqrModeEmpty_usesDefaultWhichIsMqr() {
    when(config.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.empty());
    underTest = new IssueIndex(esClient, system2, userSession, authorizationTypeSupport, config);
    mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().cwe(List.of("89")).build();

    underTest.search(query, new SearchOptions());

    // MULTI_QUALITY_MODE_DEFAULT_VALUE is true, so an empty setting must behave like MQR mode
    assertThat(MULTI_QUALITY_MODE_DEFAULT_VALUE).isTrue();
    assertThat(captureSearchRequestJson()).doesNotContain("VULNERABILITY");
  }

  @SuppressWarnings("unchecked")
  private String captureSearchRequestJson() {
    ArgumentCaptor<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>> captor = ArgumentCaptor.forClass(Function.class);
    verify(esClient).searchV2(captor.capture(), eq(Object.class));
    SearchRequest request = captor.getValue().apply(new SearchRequest.Builder()).build();
    return toJson(request);
  }

  private static String toJson(JsonpSerializable serializable) {
    StringWriter writer = new StringWriter();
    JsonProvider provider = JsonProvider.provider();
    JsonGenerator generator = provider.createGenerator(writer);
    JacksonJsonpMapper mapper = new JacksonJsonpMapper();
    serializable.serialize(generator, mapper);
    generator.close();
    return writer.toString();
  }

  // ---------- search() driving createAllFilters branches ----------

  @SuppressWarnings("unchecked")
  private SearchResponse<Object> mockEmptySearchResponse() {
    SearchResponse<Object> mockResponse = mock(SearchResponse.class);
    when(esClient.searchV2(any(), eq(Object.class))).thenReturn(mockResponse);
    // far-future "now" so createdAfter/createdBefore date bounds always validate
    when(system2.now()).thenReturn(Long.MAX_VALUE);
    return mockResponse;
  }

  @Test
  void search_withAssignedTrueFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().assigned(true).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withAssignedFalseFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().assigned(false).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withResolvedTrueFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().resolved(true).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withResolvedFalseFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().resolved(false).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withTermsBasedFilters() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .issueKeys(List.of("issue-1", "issue-2"))
      .assigneeUuids(List.of("user-1"))
      .scopes(List.of("MAIN"))
      .languages(List.of("java"))
      .tags(List.of("security"))
      .types(List.of("BUG"))
      .resolutions(List.of("FIXED"))
      .authors(List.of("john.doe"))
      .ruleUuids(List.of("rule-1"))
      .statuses(List.of("OPEN"))
      .issueStatuses(List.of("OPEN"))
      .codeVariants(List.of("variant-1"))
      .cleanCodeAttributesCategories(List.of("ADAPTABLE"))
      .linkedTicketStatuses(List.of("TODO"))
      .prioritizedRule(true)
      .fromSonarQubeUpdate(true)
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withSeverityFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().severities(List.of("BLOCKER", "MAJOR")).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withImpactSoftwareQualitiesOnlyFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().impactSoftwareQualities(List.of("SECURITY")).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withImpactSeveritiesOnlyFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().impactSeverities(List.of("HIGH")).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withBothImpactSoftwareQualitiesAndSeveritiesFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .impactSoftwareQualities(List.of("SECURITY"))
      .impactSeverities(List.of("HIGH"))
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withSecurityCategoryFilters() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .owaspTop10(List.of("a1"))
      .owaspTop10For2021(List.of("a1"))
      .owaspMobileTop10For2024(List.of("m1"))
      .stigAsdR5V3(List.of("V-222400"))
      .sansTop25(List.of("insecure-interaction"))
      .cwe(List.of("89"))
      .sonarsourceSecurity(List.of("sql-injection"))
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withSecurityCategoryPrefixFilters() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .pciDss32(List.of("1"))
      .pciDss40(List.of("1.2"))
      .casa(List.of("1"))
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withOwaspAsvs40Filter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .owaspAsvs40(List.of("1.1.1"))
      .owaspAsvsLevel(2)
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withSecurityCategoryFilters_mqrModeEnabled() {
    when(config.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(true));
    underTest = new IssueIndex(esClient, system2, userSession, authorizationTypeSupport, config);
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().cwe(List.of("89")).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withComplianceCategoryRulesFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().complianceCategoryRules(List.of("rule-1", "rule-2")).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withComponentRelatedFilters() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .componentUuids(List.of("comp-1"))
      .projectUuids(List.of("project-1"))
      .directories(List.of("src/main"))
      .files(List.of("file-1"))
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withOnComponentOnly_skipsProjectFilters() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .componentUuids(List.of("comp-1"))
      .onComponentOnly(true)
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withBranchFilters() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .mainBranch(false)
      .branchUuid("branch-1")
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withViewFilters() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().viewUuids(List.of("view-1")).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withApplicationBranchViewFilters() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .viewUuids(List.of("view-1"))
      .branchUuid("app-branch-1")
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withDateFilters() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    long now = System.currentTimeMillis();
    IssueQuery query = IssueQuery.builder()
      .createdAfter(new Date(now - Duration.standardDays(10).getMillis()), true)
      .createdBefore(new Date(now))
      .createdAt(new Date(now - Duration.standardDays(5).getMillis()))
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withCreatedAfterExclusive() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    long now = System.currentTimeMillis();
    IssueQuery query = IssueQuery.builder()
      .createdAfter(new Date(now - Duration.standardDays(10).getMillis()), false)
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withNewCodeOnReferenceFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().newCodeOnReference(true).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withCreatedAfterByProjectUuidsFilter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    long now = System.currentTimeMillis();
    IssueQuery query = IssueQuery.builder()
      .createdAfterByProjectUuids(Map.of(
        "project-1", new IssueQuery.PeriodStart(new Date(now - Duration.standardDays(7).getMillis()), true)))
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  // ---------- search() driving createSortOptions branches ----------

  @Test
  void search_withSortByCreationDateAscending() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .sort(IssueQuery.SORT_BY_CREATION_DATE)
      .asc(true)
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withSortBySeverityDescending() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .sort(IssueQuery.SORT_BY_SEVERITY)
      .asc(false)
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  @Test
  void search_withHotspotsSort() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder()
      .sort(IssueQuery.SORT_HOTSPOTS)
      .asc(true)
      .build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  // ---------- search() driving configureRouting branch ----------

  @Test
  void search_withProjectUuidsAndNoFacets_setsRouting() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    IssueQuery query = IssueQuery.builder().projectUuids(List.of("project-1", "project-2")).build();

    assertThat(underTest.search(query, new SearchOptions())).isSameAs(mockResponse);
  }

  // ---------- search() driving configureTopAggregations (facets) ----------

  @Test
  void search_withTermFacets() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    SearchOptions options = new SearchOptions().addFacets(
      "statuses", "issueStatuses", "projects", "directories", "files", "scopes",
      "languages", "rules", "author", "tags", "types", "codeVariants",
      "cleanCodeAttributeCategories", "prioritizedRule", "fromSonarQubeUpdate", "linkedTicketStatus");
    IssueQuery query = IssueQuery.builder().build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }

  @Test
  void search_withSecurityCategoryFacets() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    SearchOptions options = new SearchOptions().addFacets(
      "pciDss-3.2", "pciDss-4.0", "owaspAsvs-4.0", "owaspMobileTop10-2024",
      "owaspTop10", "owaspTop10-2021", "stig-ASD_V5R3", "casa", "sansTop25", "cwe", "sonarsourceSecurity");
    IssueQuery query = IssueQuery.builder().build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }

  @Test
  void search_withSecurityCategoryFacets_mqrModeEnabled() {
    when(config.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(true));
    underTest = new IssueIndex(esClient, system2, userSession, authorizationTypeSupport, config);
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    SearchOptions options = new SearchOptions().addFacets("cwe", "owaspTop10");
    IssueQuery query = IssueQuery.builder().build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }

  @Test
  void search_withSecurityCategoryFacetsAndSelectedValues() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    SearchOptions options = new SearchOptions().addFacets("cwe", "owaspTop10");
    IssueQuery query = IssueQuery.builder()
      .cwe(List.of("89"))
      .owaspTop10(List.of("a1"))
      .build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }

  @ParameterizedTest
  @ValueSource(strings = {"severities", "impactSoftwareQualities", "impactSeverities", "resolutions", "assigned_to_me"})
  void search_withSingleFacet(String facet) {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    SearchOptions options = new SearchOptions().addFacets(facet);
    IssueQuery query = IssueQuery.builder().build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }

  @Test
  void search_withImpactSoftwareQualityFacetAndSelectedSeverities() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    SearchOptions options = new SearchOptions().addFacets("impactSoftwareQualities");
    IssueQuery query = IssueQuery.builder().impactSeverities(List.of("HIGH")).build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }

  @Test
  void search_withImpactSeverityFacetAndSelectedSoftwareQualities() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    SearchOptions options = new SearchOptions().addFacets("impactSeverities");
    IssueQuery query = IssueQuery.builder().impactSoftwareQualities(List.of("SECURITY")).build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }

  @Test
  void search_withAssigneesFacet() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    SearchOptions options = new SearchOptions().addFacets("assignees");
    IssueQuery query = IssueQuery.builder().assigneeUuids(List.of("user-1")).build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }

  @Test
  void search_withCreatedAtFacet_usingCreatedAfter() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    when(system2.getDefaultTimeZone()).thenReturn(java.util.TimeZone.getTimeZone("UTC"));
    long now = System.currentTimeMillis();
    SearchOptions options = new SearchOptions().addFacets("createdAt");
    IssueQuery query = IssueQuery.builder()
      .createdAfter(new Date(now - Duration.standardDays(30).getMillis()), true)
      .createdBefore(new Date(now))
      .build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }

  @Test
  void search_withComplianceFacets() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    SearchOptions options = new SearchOptions().addComplianceFacets(List.of("compliance"));
    IssueQuery query = IssueQuery.builder().build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }

  @Test
  void search_withFacetsAndEffortFacetMode() {
    SearchResponse<Object> mockResponse = mockEmptySearchResponse();
    SearchOptions options = new SearchOptions().addFacets("resolutions", "assignees");
    IssueQuery query = IssueQuery.builder().facetMode("effort").build();

    assertThat(underTest.search(query, options)).isSameAs(mockResponse);
  }
}
