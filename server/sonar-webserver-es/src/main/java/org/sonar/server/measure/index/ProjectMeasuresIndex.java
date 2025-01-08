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
package org.sonar.server.measure.index;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator.KeyedFilter;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.newindex.DefaultIndexSettingsElement;
import org.sonar.server.es.searchrequest.NestedFieldTopAggregationDefinition;
import org.sonar.server.es.searchrequest.RequestFiltersComputer;
import org.sonar.server.es.searchrequest.RequestFiltersComputer.AllFilters;
import org.sonar.server.es.searchrequest.SimpleFieldTopAggregationDefinition;
import org.sonar.server.es.searchrequest.SubAggregationHelper;
import org.sonar.server.es.searchrequest.TopAggregationDefinition;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.NestedFieldFilterScope;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.SimpleFieldFilterScope;
import org.sonar.server.es.searchrequest.TopAggregationHelper;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filters;
import static org.elasticsearch.search.aggregations.AggregationBuilders.sum;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REVIEW_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY;
import static org.sonar.server.es.EsUtils.escapeSpecialRegexChars;
import static org.sonar.server.es.EsUtils.termsToMap;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.STICKY;
import static org.sonar.server.es.searchrequest.TopAggregationHelper.NO_EXTRA_FILTER;
import static org.sonar.server.measure.index.ProjectMeasuresDoc.QUALITY_GATE_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndex.Facet.ALERT_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndex.Facet.LANGUAGES;
import static org.sonar.server.measure.index.ProjectMeasuresIndex.Facet.TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_CREATED_AT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_LANGUAGES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES_MEASURE_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES_MEASURE_VALUE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NAME;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NCLOC_DISTRIBUTION;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NCLOC_DISTRIBUTION_LANGUAGE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NCLOC_DISTRIBUTION_NCLOC;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALITY_GATE_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.SUB_FIELD_MEASURES_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_CREATION_DATE;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_LAST_ANALYSIS_DATE;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGES;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_QUALIFIER;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_TAGS;

@ServerSide
public class ProjectMeasuresIndex {
  private static final int FACET_DEFAULT_SIZE = 10;

  private static final double[] LINES_THRESHOLDS = {1_000D, 10_000D, 100_000D, 500_000D};
  private static final double[] COVERAGE_THRESHOLDS = {30D, 50D, 70D, 80D};
  private static final double[] SECURITY_REVIEW_RATING_THRESHOLDS = {30D, 50D, 70D, 80D};
  private static final double[] DUPLICATIONS_THRESHOLDS = {3D, 5D, 10D, 20D};
  private static final int SCROLL_SIZE = 5000;
  private static final TimeValue KEEP_ALIVE_SCROLL_DURATION = TimeValue.timeValueMinutes(1L);

  public enum Facet {
    NCLOC(new RangeMeasureFacet(NCLOC_KEY, LINES_THRESHOLDS)),
    NEW_LINES(new RangeMeasureFacet(NEW_LINES_KEY, LINES_THRESHOLDS)),
    DUPLICATED_LINES_DENSITY(new RangeWithNoDataMeasureFacet(DUPLICATED_LINES_DENSITY_KEY, DUPLICATIONS_THRESHOLDS)),
    NEW_DUPLICATED_LINES_DENSITY(new RangeWithNoDataMeasureFacet(NEW_DUPLICATED_LINES_DENSITY_KEY, DUPLICATIONS_THRESHOLDS)),
    COVERAGE(new RangeWithNoDataMeasureFacet(COVERAGE_KEY, COVERAGE_THRESHOLDS)),
    NEW_COVERAGE(new RangeWithNoDataMeasureFacet(NEW_COVERAGE_KEY, COVERAGE_THRESHOLDS)),

    //RuleType ratings
    SQALE_RATING(new RatingMeasureFacet(SQALE_RATING_KEY)),
    NEW_MAINTAINABILITY_RATING(new RatingMeasureFacet(NEW_MAINTAINABILITY_RATING_KEY)),
    RELIABILITY_RATING(new RatingMeasureFacet(RELIABILITY_RATING_KEY)),
    NEW_RELIABILITY_RATING(new RatingMeasureFacet(NEW_RELIABILITY_RATING_KEY)),
    SECURITY_RATING(new RatingMeasureFacet(SECURITY_RATING_KEY)),
    NEW_SECURITY_RATING(new RatingMeasureFacet(NEW_SECURITY_RATING_KEY)),
    SECURITY_REVIEW_RATING(new RatingMeasureFacet(SECURITY_REVIEW_RATING_KEY)),
    NEW_SECURITY_REVIEW_RATING(new RatingMeasureFacet(NEW_SECURITY_REVIEW_RATING_KEY)),

    //Software quality ratings
    SOFTWARE_QUALITY_MAINTAINABILITY_RATING(new RatingMeasureFacet(SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY)),
    NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING(new RatingMeasureFacet(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY)),
    SOFTWARE_QUALITY_RELIABILITY_RATING(new RatingMeasureFacet(SOFTWARE_QUALITY_RELIABILITY_RATING_KEY)),
    NEW_SOFTWARE_QUALITY_RELIABILITY_RATING(new RatingMeasureFacet(NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY)),
    SOFTWARE_QUALITY_SECURITY_RATING(new RatingMeasureFacet(SOFTWARE_QUALITY_SECURITY_RATING_KEY)),
    NEW_SOFTWARE_QUALITY_SECURITY_RATING(new RatingMeasureFacet(NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY)),

    SECURITY_HOTSPOTS_REVIEWED(new RangeMeasureFacet(SECURITY_HOTSPOTS_REVIEWED_KEY, SECURITY_REVIEW_RATING_THRESHOLDS)),
    NEW_SECURITY_HOTSPOTS_REVIEWED(new RangeMeasureFacet(NEW_SECURITY_HOTSPOTS_REVIEWED_KEY, SECURITY_REVIEW_RATING_THRESHOLDS)),
    ALERT_STATUS(new MeasureFacet(ALERT_STATUS_KEY, ProjectMeasuresIndex::buildAlertStatusFacet)),
    LANGUAGES(FILTER_LANGUAGES, FIELD_LANGUAGES, STICKY, ProjectMeasuresIndex::buildLanguageFacet),
    QUALIFIER(FILTER_QUALIFIER, FIELD_QUALIFIER, STICKY, ProjectMeasuresIndex::buildQualifierFacet),
    TAGS(FILTER_TAGS, FIELD_TAGS, STICKY, ProjectMeasuresIndex::buildTagsFacet);

    private final String name;
    private final TopAggregationDefinition<?> topAggregation;
    private final FacetBuilder facetBuilder;

    Facet(String name, String fieldName, boolean sticky, FacetBuilder facetBuilder) {
      this.name = name;
      this.topAggregation = new SimpleFieldTopAggregationDefinition(fieldName, sticky);
      this.facetBuilder = facetBuilder;
    }

    Facet(MeasureFacet measureFacet) {
      this.name = measureFacet.metricKey;
      this.topAggregation = measureFacet.topAggregation;
      this.facetBuilder = measureFacet.facetBuilder;
    }

    public String getName() {
      return name;
    }

    public TopAggregationDefinition<?> getTopAggregationDef() {
      return topAggregation;
    }

    public TopAggregationDefinition.FilterScope getFilterScope() {
      return topAggregation.getFilterScope();
    }

    public FacetBuilder getFacetBuilder() {
      return facetBuilder;
    }
  }

  private static final Map<String, Facet> FACETS_BY_NAME = Arrays.stream(Facet.values())
    .collect(Collectors.toMap(Facet::getName, Function.identity()));

  private final EsClient client;
  private final WebAuthorizationTypeSupport authorizationTypeSupport;
  private final System2 system2;

  public ProjectMeasuresIndex(EsClient client, WebAuthorizationTypeSupport authorizationTypeSupport, System2 system2) {
    this.client = client;
    this.authorizationTypeSupport = authorizationTypeSupport;
    this.system2 = system2;
  }

  public SearchIdResult<String> search(ProjectMeasuresQuery query, SearchOptions searchOptions) {
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .fetchSource(false)
      .trackTotalHits(true)
      .from(searchOptions.getOffset())
      .size(searchOptions.getLimit());

    AllFilters allFilters = createFilters(query);
    RequestFiltersComputer filtersComputer = createFiltersComputer(searchOptions, allFilters);
    addFacets(searchSourceBuilder, searchOptions, filtersComputer, query);
    addSort(query, searchSourceBuilder);

    filtersComputer.getQueryFilters().ifPresent(searchSourceBuilder::query);
    filtersComputer.getPostFilters().ifPresent(searchSourceBuilder::postFilter);
    SearchResponse response = client.search(EsClient.prepareSearch(TYPE_PROJECT_MEASURES.getMainType())
      .source(searchSourceBuilder));
    return new SearchIdResult<>(response, id -> id, system2.getDefaultTimeZone().toZoneId());
  }

  private static RequestFiltersComputer createFiltersComputer(SearchOptions searchOptions, AllFilters allFilters) {
    Collection<String> facetNames = searchOptions.getFacets();
    Set<TopAggregationDefinition<?>> facets = facetNames.stream()
      .map(FACETS_BY_NAME::get)
      .filter(Objects::nonNull)
      .map(Facet::getTopAggregationDef)
      .collect(Collectors.toSet());
    return new RequestFiltersComputer(allFilters, facets);
  }

  public ProjectMeasuresStatistics searchSupportStatistics() {
    SearchRequest projectMeasuresSearchRequest = buildProjectMeasureSearchRequest();
    SearchResponse projectMeasures = client.search(projectMeasuresSearchRequest);
    return buildProjectMeasuresStatistics(projectMeasures);
  }

  private static SearchRequest buildProjectMeasureSearchRequest() {
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .fetchSource(false)
      .size(0);

    BoolQueryBuilder esFilter = boolQuery()
      .filter(termQuery(FIELD_INDEX_TYPE, TYPE_PROJECT_MEASURES.getName()))
      .filter(termQuery(FIELD_QUALIFIER, ComponentQualifiers.PROJECT));
    searchSourceBuilder.query(esFilter);
    searchSourceBuilder.aggregation(AggregationBuilders.terms(FIELD_LANGUAGES)
      .field(FIELD_LANGUAGES)
      .size(MAX_PAGE_SIZE)
      .minDocCount(1)
      .order(BucketOrder.count(false)));
    searchSourceBuilder.aggregation(AggregationBuilders.nested(FIELD_NCLOC_DISTRIBUTION, FIELD_NCLOC_DISTRIBUTION)
      .subAggregation(AggregationBuilders.terms(FIELD_NCLOC_DISTRIBUTION + "_terms")
        .field(FIELD_NCLOC_DISTRIBUTION_LANGUAGE)
        .size(MAX_PAGE_SIZE)
        .minDocCount(1)
        .order(BucketOrder.count(false))
        .subAggregation(sum(FIELD_NCLOC_DISTRIBUTION_NCLOC).field(FIELD_NCLOC_DISTRIBUTION_NCLOC))));
    searchSourceBuilder.aggregation(AggregationBuilders.nested(NCLOC_KEY, FIELD_MEASURES)
      .subAggregation(AggregationBuilders.filter(NCLOC_KEY + "_filter", termQuery(FIELD_MEASURES_MEASURE_KEY, NCLOC_KEY))
        .subAggregation(sum(NCLOC_KEY + "_filter_sum").field(FIELD_MEASURES_MEASURE_VALUE))));
    searchSourceBuilder.size(SCROLL_SIZE);

    return EsClient.prepareSearch(TYPE_PROJECT_MEASURES.getMainType()).source(searchSourceBuilder).scroll(KEEP_ALIVE_SCROLL_DURATION);
  }

  private static ProjectMeasuresStatistics buildProjectMeasuresStatistics(SearchResponse response) {
    ProjectMeasuresStatistics.Builder statistics = ProjectMeasuresStatistics.builder();
    statistics.setProjectCount(getTotalHits(response.getHits().getTotalHits()).value);
    statistics.setProjectCountByLanguage(termsToMap(response.getAggregations().get(FIELD_LANGUAGES)));

    Function<Terms.Bucket, Long> bucketToNcloc = bucket -> Math.round(((Sum) bucket.getAggregations().get(FIELD_NCLOC_DISTRIBUTION_NCLOC)).getValue());
    Map<String, Long> nclocByLanguage = Stream.of((Nested) response.getAggregations().get(FIELD_NCLOC_DISTRIBUTION))
      .map(nested -> (Terms) nested.getAggregations().get(nested.getName() + "_terms"))
      .flatMap(terms -> terms.getBuckets().stream())
      .collect(Collectors.toMap(Bucket::getKeyAsString, bucketToNcloc));
    statistics.setNclocByLanguage(nclocByLanguage);

    return statistics.build();
  }

  private static TotalHits getTotalHits(@Nullable TotalHits totalHits) {
    return ofNullable(totalHits).orElseThrow(() -> new IllegalStateException("Could not get total hits of search results"));
  }

  private static void addSort(ProjectMeasuresQuery query, SearchSourceBuilder requestBuilder) {
    String sort = query.getSort();
    if (SORT_BY_NAME.equals(sort)) {
      requestBuilder.sort(DefaultIndexSettingsElement.SORTABLE_ANALYZER.subField(FIELD_NAME), query.isAsc() ? ASC : DESC);
    } else if (SORT_BY_LAST_ANALYSIS_DATE.equals(sort)) {
      requestBuilder.sort(FIELD_ANALYSED_AT, query.isAsc() ? ASC : DESC);
    } else if (SORT_BY_CREATION_DATE.equals(sort)) {
      requestBuilder.sort(FIELD_CREATED_AT, query.isAsc() ? ASC : DESC);
    } else if (ALERT_STATUS_KEY.equals(sort)) {
      requestBuilder.sort(FIELD_QUALITY_GATE_STATUS, query.isAsc() ? ASC : DESC);
      requestBuilder.sort(DefaultIndexSettingsElement.SORTABLE_ANALYZER.subField(FIELD_NAME), ASC);
    } else {
      addMetricSort(query, requestBuilder, sort);
      requestBuilder.sort(DefaultIndexSettingsElement.SORTABLE_ANALYZER.subField(FIELD_NAME), ASC);
    }
    // last sort is by key in order to be deterministic when same value
    requestBuilder.sort(FIELD_KEY, ASC);
  }

  private static void addMetricSort(ProjectMeasuresQuery query, SearchSourceBuilder requestBuilder, String sort) {
    requestBuilder.sort(
      new FieldSortBuilder(FIELD_MEASURES_MEASURE_VALUE)
        .setNestedSort(
          new NestedSortBuilder(FIELD_MEASURES)
            .setFilter(termQuery(FIELD_MEASURES_MEASURE_KEY, sort)))
        .order(query.isAsc() ? ASC : DESC));
  }

  private static void addFacets(SearchSourceBuilder esRequest, SearchOptions options, RequestFiltersComputer filtersComputer, ProjectMeasuresQuery query) {
    TopAggregationHelper topAggregationHelper = new TopAggregationHelper(filtersComputer, new SubAggregationHelper());
    options.getFacets().stream()
      .map(FACETS_BY_NAME::get)
      .filter(Objects::nonNull)
      .map(facet -> facet.getFacetBuilder().buildFacet(facet, query, topAggregationHelper))
      .forEach(esRequest::aggregation);
  }

  private static AbstractAggregationBuilder<?> createRangeFacet(String metricKey, double[] thresholds) {
    RangeAggregationBuilder rangeAgg = AggregationBuilders.range(metricKey)
      .field(FIELD_MEASURES_MEASURE_VALUE);
    final int lastIndex = thresholds.length - 1;
    IntStream.range(0, thresholds.length)
      .forEach(i -> {
        if (i == 0) {
          rangeAgg.addUnboundedTo(thresholds[0]);
          rangeAgg.addRange(thresholds[0], thresholds[1]);
        } else if (i == lastIndex) {
          rangeAgg.addUnboundedFrom(thresholds[lastIndex]);
        } else {
          rangeAgg.addRange(thresholds[i], thresholds[i + 1]);
        }
      });

    return AggregationBuilders.nested("nested_" + metricKey, FIELD_MEASURES)
      .subAggregation(
        AggregationBuilders.filter("filter_" + metricKey, termsQuery(FIELD_MEASURES_MEASURE_KEY, metricKey))
          .subAggregation(rangeAgg));
  }

  private static AbstractAggregationBuilder<?> createQualityGateFacet() {
    return filters(
      ALERT_STATUS_KEY,
      QUALITY_GATE_STATUS
        .entrySet()
        .stream()
        .map(entry -> new KeyedFilter(entry.getKey(), termQuery(FIELD_QUALITY_GATE_STATUS, entry.getValue())))
        .toArray(KeyedFilter[]::new));
  }

  private static AbstractAggregationBuilder<?> createQualifierFacet() {
    return filters(
      FILTER_QUALIFIER,
      Stream.of(ComponentQualifiers.APP, ComponentQualifiers.PROJECT)
        .map(qualifier -> new KeyedFilter(qualifier, termQuery(FIELD_QUALIFIER, qualifier)))
        .toArray(KeyedFilter[]::new));
  }

  private AllFilters createFilters(ProjectMeasuresQuery query) {
    AllFilters filters = RequestFiltersComputer.newAllFilters();
    filters.addFilter(
      "__indexType", new SimpleFieldFilterScope(FIELD_INDEX_TYPE),
      termQuery(FIELD_INDEX_TYPE, TYPE_PROJECT_MEASURES.getName()));
    if (!query.isIgnoreAuthorization()) {
      filters.addFilter("__authorization", new SimpleFieldFilterScope("parent"), authorizationTypeSupport.createQueryFilter());
    }
    Multimap<String, MetricCriterion> metricCriterionMultimap = ArrayListMultimap.create();
    query.getMetricCriteria()
      .forEach(metricCriterion -> metricCriterionMultimap.put(metricCriterion.getMetricKey(), metricCriterion));
    metricCriterionMultimap.asMap().forEach((key, value) -> {
      BoolQueryBuilder metricFilters = boolQuery();
      value
        .stream()
        .map(ProjectMeasuresIndex::toQuery)
        .forEach(metricFilters::must);
      filters.addFilter(key, new NestedFieldFilterScope<>(FIELD_MEASURES, SUB_FIELD_MEASURES_KEY, key), metricFilters);
    });

    query.getQualityGateStatus().ifPresent(qualityGateStatus -> filters.addFilter(
      ALERT_STATUS_KEY, ALERT_STATUS.getFilterScope(),
      termQuery(FIELD_QUALITY_GATE_STATUS, QUALITY_GATE_STATUS.get(qualityGateStatus.name()))));

    query.getProjectUuids().ifPresent(projectUuids -> filters.addFilter(
      "ids", new SimpleFieldFilterScope("_id"),
      termsQuery("_id", projectUuids)));

    query.getLanguages()
      .ifPresent(languages -> filters.addFilter(FILTER_LANGUAGES, LANGUAGES.getFilterScope(), termsQuery(FIELD_LANGUAGES, languages)));

    query.getTags().ifPresent(tags -> filters.addFilter(FIELD_TAGS, TAGS.getFilterScope(), termsQuery(FIELD_TAGS, tags)));

    query.getQualifiers()
      .ifPresent(qualifiers -> filters.addFilter(FIELD_QUALIFIER, new SimpleFieldFilterScope(FIELD_QUALIFIER), termsQuery(FIELD_QUALIFIER, qualifiers)));

    query.getQueryText()
      .map(ProjectsTextSearchQueryFactory::createQuery)
      .ifPresent(queryBuilder -> filters.addFilter("textQuery", new SimpleFieldFilterScope(FIELD_NAME), queryBuilder));
    return filters;
  }

  private static QueryBuilder toQuery(MetricCriterion criterion) {
    if (criterion.isNoData()) {
      return boolQuery().mustNot(
        nestedQuery(
          FIELD_MEASURES,
          termQuery(FIELD_MEASURES_MEASURE_KEY, criterion.getMetricKey()),
          ScoreMode.Avg));
    }
    return nestedQuery(
      FIELD_MEASURES,
      boolQuery()
        .filter(termQuery(FIELD_MEASURES_MEASURE_KEY, criterion.getMetricKey()))
        .filter(toValueQuery(criterion)),
      ScoreMode.Avg);
  }

  private static QueryBuilder toValueQuery(MetricCriterion criterion) {
    String fieldName = FIELD_MEASURES_MEASURE_VALUE;

    switch (criterion.getOperator()) {
      case GT:
        return rangeQuery(fieldName).gt(criterion.getValue());
      case GTE:
        return rangeQuery(fieldName).gte(criterion.getValue());
      case LT:
        return rangeQuery(fieldName).lt(criterion.getValue());
      case LTE:
        return rangeQuery(fieldName).lte(criterion.getValue());
      case EQ:
        return termQuery(fieldName, criterion.getValue());
      default:
        throw new IllegalStateException("Metric criteria non supported: " + criterion.getOperator().name());
    }
  }

  public List<String> searchTags(@Nullable String textQuery, int page, int size) {
    int maxPageSize = 100;
    int maxPage = 20;
    checkArgument(size <= maxPageSize, "Page size must be lower than or equals to " + maxPageSize);
    checkArgument(page > 0 && page <= maxPage, "Page must be between 0 and " + maxPage);

    if (size <= 0) {
      return emptyList();
    }

    TermsAggregationBuilder tagFacet = AggregationBuilders.terms(FIELD_TAGS)
      .field(FIELD_TAGS)
      .size(size * page)
      .minDocCount(1)
      .order(BucketOrder.key(true));
    if (textQuery != null) {
      tagFacet.includeExclude(new IncludeExclude(".*" + escapeSpecialRegexChars(textQuery) + ".*", null));
    }

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(authorizationTypeSupport.createQueryFilter())
      .fetchSource(false)
      .aggregation(tagFacet);

    SearchResponse response = client.search(EsClient.prepareSearch(TYPE_PROJECT_MEASURES.getMainType())
      .source(searchSourceBuilder));

    Terms aggregation = response.getAggregations().get(FIELD_TAGS);

    return aggregation.getBuckets().stream()
      .skip((page - 1) * size)
      .map(Bucket::getKeyAsString)
      .toList();
  }

  private interface FacetBuilder {
    FilterAggregationBuilder buildFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper);
  }

  /**
   * A sticky facet on field {@link ProjectMeasuresIndexDefinition#FIELD_MEASURES_MEASURE_KEY}.
   */
  private static class MeasureFacet {
    private final String metricKey;
    private final TopAggregationDefinition<?> topAggregation;
    private final FacetBuilder facetBuilder;

    private MeasureFacet(String metricKey, FacetBuilder facetBuilder) {
      this.metricKey = metricKey;
      this.topAggregation = new NestedFieldTopAggregationDefinition<>(FIELD_MEASURES_MEASURE_KEY, metricKey, STICKY);
      this.facetBuilder = facetBuilder;
    }
  }

  private static final class RangeMeasureFacet extends MeasureFacet {

    private RangeMeasureFacet(String metricKey, double[] thresholds) {
      super(metricKey, new MetricRangeFacetBuilder(metricKey, thresholds));
    }

    private static final class MetricRangeFacetBuilder implements FacetBuilder {
      private final String metricKey;
      private final double[] thresholds;

      private MetricRangeFacetBuilder(String metricKey, double[] thresholds) {
        this.metricKey = metricKey;
        this.thresholds = thresholds;
      }

      @Override
      public FilterAggregationBuilder buildFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
        return topAggregationHelper.buildTopAggregation(
          facet.getName(), facet.getTopAggregationDef(),
          NO_EXTRA_FILTER,
          t -> t.subAggregation(createRangeFacet(metricKey, thresholds)));
      }
    }
  }

  private static final class RangeWithNoDataMeasureFacet extends MeasureFacet {

    private RangeWithNoDataMeasureFacet(String metricKey, double[] thresholds) {
      super(metricKey, new MetricRangeWithNoDataFacetBuilder(metricKey, thresholds));
    }

    private static final class MetricRangeWithNoDataFacetBuilder implements FacetBuilder {
      private final String metricKey;
      private final double[] thresholds;

      private MetricRangeWithNoDataFacetBuilder(String metricKey, double[] thresholds) {
        this.metricKey = metricKey;
        this.thresholds = thresholds;
      }

      @Override
      public FilterAggregationBuilder buildFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
        return topAggregationHelper.buildTopAggregation(
          facet.getName(), facet.getTopAggregationDef(),
          NO_EXTRA_FILTER,
          t -> t.subAggregation(createRangeFacet(metricKey, thresholds))
            .subAggregation(createNoDataFacet(metricKey)));
      }

      private static AbstractAggregationBuilder<?> createNoDataFacet(String metricKey) {
        return AggregationBuilders.filter(
          "no_data_" + metricKey,
          boolQuery().mustNot(nestedQuery(FIELD_MEASURES, termQuery(FIELD_MEASURES_MEASURE_KEY, metricKey), ScoreMode.Avg)));
      }
    }
  }

  private static class RatingMeasureFacet extends MeasureFacet {

    private RatingMeasureFacet(String metricKey) {
      super(metricKey, new MetricRatingFacetBuilder(metricKey));
    }

    private static class MetricRatingFacetBuilder implements FacetBuilder {
      private final String metricKey;

      private MetricRatingFacetBuilder(String metricKey) {
        this.metricKey = metricKey;
      }

      @Override
      public FilterAggregationBuilder buildFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
        return topAggregationHelper.buildTopAggregation(
          facet.getName(), facet.getTopAggregationDef(),
          NO_EXTRA_FILTER,
          t -> t.subAggregation(createMeasureRatingFacet(metricKey)));
      }

      private static AbstractAggregationBuilder<?> createMeasureRatingFacet(String metricKey) {
        return AggregationBuilders.nested("nested_" + metricKey, FIELD_MEASURES)
          .subAggregation(
            AggregationBuilders.filter("filter_" + metricKey, termsQuery(FIELD_MEASURES_MEASURE_KEY, metricKey))
              .subAggregation(filters(metricKey,
                new KeyedFilter("1", termQuery(FIELD_MEASURES_MEASURE_VALUE, 1D)),
                new KeyedFilter("2", termQuery(FIELD_MEASURES_MEASURE_VALUE, 2D)),
                new KeyedFilter("3", termQuery(FIELD_MEASURES_MEASURE_VALUE, 3D)),
                new KeyedFilter("4", termQuery(FIELD_MEASURES_MEASURE_VALUE, 4D)),
                new KeyedFilter("5", termQuery(FIELD_MEASURES_MEASURE_VALUE, 5D)))));
      }
    }
  }

  private static FilterAggregationBuilder buildLanguageFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
    // optional selected languages sub-aggregation
    Consumer<FilterAggregationBuilder> extraSubAgg = t -> query.getLanguages()
      .flatMap(languages -> topAggregationHelper.getSubAggregationHelper()
        .buildSelectedItemsAggregation(FILTER_LANGUAGES, facet.getTopAggregationDef(), languages.toArray()))
      .ifPresent(t::subAggregation);
    return topAggregationHelper.buildTermTopAggregation(
      FILTER_LANGUAGES, facet.getTopAggregationDef(), FACET_DEFAULT_SIZE,
      NO_EXTRA_FILTER, extraSubAgg);
  }

  private static FilterAggregationBuilder buildAlertStatusFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
    return topAggregationHelper.buildTopAggregation(
      facet.getName(), facet.getTopAggregationDef(),
      NO_EXTRA_FILTER,
      t -> t.subAggregation(createQualityGateFacet()));
  }

  private static FilterAggregationBuilder buildTagsFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
    // optional selected tags sub-aggregation
    Consumer<FilterAggregationBuilder> extraSubAgg = t -> query.getTags()
      .flatMap(tags -> topAggregationHelper.getSubAggregationHelper()
        .buildSelectedItemsAggregation(FILTER_TAGS, facet.getTopAggregationDef(), tags.toArray()))
      .ifPresent(t::subAggregation);
    return topAggregationHelper.buildTermTopAggregation(
      FILTER_TAGS, facet.getTopAggregationDef(), FACET_DEFAULT_SIZE,
      NO_EXTRA_FILTER, extraSubAgg);
  }

  private static FilterAggregationBuilder buildQualifierFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
    return topAggregationHelper.buildTopAggregation(
      facet.getName(), facet.getTopAggregationDef(),
      NO_EXTRA_FILTER,
      t -> t.subAggregation(createQualifierFacet()));
  }

}
