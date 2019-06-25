/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator.KeyedFilter;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.StickyFacetBuilder;
import org.sonar.server.es.newindex.DefaultIndexSettingsElement;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
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
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.server.es.EsUtils.escapeSpecialRegexChars;
import static org.sonar.server.es.EsUtils.termsToMap;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.measure.index.ProjectMeasuresDoc.QUALITY_GATE_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_LANGUAGES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NAME;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NCLOC_LANGUAGE_DISTRIBUTION;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ORGANIZATION_UUID;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALITY_GATE_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_LAST_ANALYSIS_DATE;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGES;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_TAGS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.MAX_PAGE_SIZE;

@ServerSide
public class ProjectMeasuresIndex {

  public static final List<String> SUPPORTED_FACETS = ImmutableList.of(
    NCLOC_KEY,
    NEW_LINES_KEY,
    DUPLICATED_LINES_DENSITY_KEY,
    NEW_DUPLICATED_LINES_DENSITY_KEY,
    COVERAGE_KEY,
    NEW_COVERAGE_KEY,
    SQALE_RATING_KEY,
    NEW_MAINTAINABILITY_RATING_KEY,
    RELIABILITY_RATING_KEY,
    NEW_RELIABILITY_RATING_KEY,
    SECURITY_RATING_KEY,
    NEW_SECURITY_RATING_KEY,
    ALERT_STATUS_KEY,
    FILTER_LANGUAGES,
    FILTER_TAGS);

  private static final Double[] LINES_THRESHOLDS = new Double[] {1_000d, 10_000d, 100_000d, 500_000d};
  private static final Double[] COVERAGE_THRESHOLDS = new Double[] {30d, 50d, 70d, 80d};
  private static final Double[] DUPLICATIONS_THRESHOLDS = new Double[] {3d, 5d, 10d, 20d};

  private static final String FIELD_MEASURES_KEY = FIELD_MEASURES + "." + ProjectMeasuresIndexDefinition.FIELD_MEASURES_KEY;
  private static final String FIELD_MEASURES_VALUE = FIELD_MEASURES + "." + ProjectMeasuresIndexDefinition.FIELD_MEASURES_VALUE;
  private static final String FIELD_DISTRIB_LANGUAGE = FIELD_NCLOC_LANGUAGE_DISTRIBUTION + "." + ProjectMeasuresIndexDefinition.FIELD_DISTRIB_LANGUAGE;
  private static final String FIELD_DISTRIB_NCLOC = FIELD_NCLOC_LANGUAGE_DISTRIBUTION + "." + ProjectMeasuresIndexDefinition.FIELD_DISTRIB_NCLOC;

  private static final Map<String, FacetSetter> FACET_FACTORIES = ImmutableMap.<String, FacetSetter>builder()
    .put(NCLOC_KEY, (esSearch, query, facetBuilder) -> addRangeFacet(esSearch, NCLOC_KEY, facetBuilder, LINES_THRESHOLDS))
    .put(NEW_LINES_KEY, (esSearch, query, facetBuilder) -> addRangeFacet(esSearch, NEW_LINES_KEY, facetBuilder, LINES_THRESHOLDS))
    .put(DUPLICATED_LINES_DENSITY_KEY,
      (esSearch, query, facetBuilder) -> addRangeFacetIncludingNoData(esSearch, DUPLICATED_LINES_DENSITY_KEY, facetBuilder, DUPLICATIONS_THRESHOLDS))
    .put(NEW_DUPLICATED_LINES_DENSITY_KEY,
      (esSearch, query, facetBuilder) -> addRangeFacetIncludingNoData(esSearch, NEW_DUPLICATED_LINES_DENSITY_KEY, facetBuilder, DUPLICATIONS_THRESHOLDS))
    .put(COVERAGE_KEY, (esSearch, query, facetBuilder) -> addRangeFacetIncludingNoData(esSearch, COVERAGE_KEY, facetBuilder, COVERAGE_THRESHOLDS))
    .put(NEW_COVERAGE_KEY, (esSearch, query, facetBuilder) -> addRangeFacetIncludingNoData(esSearch, NEW_COVERAGE_KEY, facetBuilder, COVERAGE_THRESHOLDS))
    .put(SQALE_RATING_KEY, (esSearch, query, facetBuilder) -> addRatingFacet(esSearch, SQALE_RATING_KEY, facetBuilder))
    .put(NEW_MAINTAINABILITY_RATING_KEY, (esSearch, query, facetBuilder) -> addRatingFacet(esSearch, NEW_MAINTAINABILITY_RATING_KEY, facetBuilder))
    .put(RELIABILITY_RATING_KEY, (esSearch, query, facetBuilder) -> addRatingFacet(esSearch, RELIABILITY_RATING_KEY, facetBuilder))
    .put(NEW_RELIABILITY_RATING_KEY, (esSearch, query, facetBuilder) -> addRatingFacet(esSearch, NEW_RELIABILITY_RATING_KEY, facetBuilder))
    .put(SECURITY_RATING_KEY, (esSearch, query, facetBuilder) -> addRatingFacet(esSearch, SECURITY_RATING_KEY, facetBuilder))
    .put(NEW_SECURITY_RATING_KEY, (esSearch, query, facetBuilder) -> addRatingFacet(esSearch, NEW_SECURITY_RATING_KEY, facetBuilder))
    .put(ALERT_STATUS_KEY, (esSearch, query, facetBuilder) -> esSearch.addAggregation(createStickyFacet(ALERT_STATUS_KEY, facetBuilder, createQualityGateFacet(query))))
    .put(FILTER_LANGUAGES, ProjectMeasuresIndex::addLanguagesFacet)
    .put(FIELD_TAGS, ProjectMeasuresIndex::addTagsFacet)
    .build();

  private final EsClient client;
  private final WebAuthorizationTypeSupport authorizationTypeSupport;
  private final System2 system2;

  public ProjectMeasuresIndex(EsClient client, WebAuthorizationTypeSupport authorizationTypeSupport, System2 system2) {
    this.client = client;
    this.authorizationTypeSupport = authorizationTypeSupport;
    this.system2 = system2;
  }

  public SearchIdResult<String> search(ProjectMeasuresQuery query, SearchOptions searchOptions) {
    SearchRequestBuilder requestBuilder = client
      .prepareSearch(TYPE_PROJECT_MEASURES.getMainType())
      .setFetchSource(false)
      .setFrom(searchOptions.getOffset())
      .setSize(searchOptions.getLimit());

    BoolQueryBuilder esFilter = boolQuery();
    Map<String, QueryBuilder> filters = createFilters(query);
    filters.values().forEach(esFilter::must);
    requestBuilder.setQuery(esFilter);

    addFacets(requestBuilder, searchOptions, filters, query);
    addSort(query, requestBuilder);
    return new SearchIdResult<>(requestBuilder.get(), id -> id, system2.getDefaultTimeZone());
  }

  public ProjectMeasuresStatistics searchTelemetryStatistics() {
    SearchRequestBuilder request = client
      .prepareSearch(TYPE_PROJECT_MEASURES.getMainType())
      .setFetchSource(false)
      .setSize(0);

    BoolQueryBuilder esFilter = boolQuery();
    request.setQuery(esFilter);
    request.addAggregation(AggregationBuilders.terms(FIELD_LANGUAGES)
      .field(FIELD_LANGUAGES)
      .size(MAX_PAGE_SIZE)
      .minDocCount(1)
      .order(BucketOrder.count(false)));
    request.addAggregation(AggregationBuilders.nested(FIELD_NCLOC_LANGUAGE_DISTRIBUTION, FIELD_NCLOC_LANGUAGE_DISTRIBUTION)
      .subAggregation(AggregationBuilders.terms(FIELD_NCLOC_LANGUAGE_DISTRIBUTION + "_terms")
        .field(FIELD_DISTRIB_LANGUAGE)
        .size(MAX_PAGE_SIZE)
        .minDocCount(1)
        .order(BucketOrder.count(false))
        .subAggregation(sum(FIELD_DISTRIB_NCLOC).field(FIELD_DISTRIB_NCLOC))));

    request.addAggregation(AggregationBuilders.nested(NCLOC_KEY, FIELD_MEASURES)
      .subAggregation(AggregationBuilders.filter(NCLOC_KEY + "_filter", termQuery(FIELD_MEASURES_KEY, NCLOC_KEY))
        .subAggregation(sum(NCLOC_KEY + "_filter_sum").field(FIELD_MEASURES_VALUE))));

    ProjectMeasuresStatistics.Builder statistics = ProjectMeasuresStatistics.builder();

    SearchResponse response = request.get();
    statistics.setProjectCount(response.getHits().getTotalHits());
    Stream.of(NCLOC_KEY)
      .map(metric -> (Nested) response.getAggregations().get(metric))
      .map(nested -> (Filter) nested.getAggregations().get(nested.getName() + "_filter"))
      .map(filter -> (Sum) filter.getAggregations().get(filter.getName() + "_sum"))
      .forEach(sum -> {
        String metric = sum.getName().replace("_filter_sum", "");
        long value = Math.round(sum.getValue());
        statistics.setSum(metric, value);
      });
    statistics.setProjectCountByLanguage(termsToMap(response.getAggregations().get(FIELD_LANGUAGES)));
    Function<Terms.Bucket, Long> bucketToNcloc = bucket -> Math.round(((Sum) bucket.getAggregations().get(FIELD_DISTRIB_NCLOC)).getValue());
    Map<String, Long> nclocByLanguage = Stream.of((Nested) response.getAggregations().get(FIELD_NCLOC_LANGUAGE_DISTRIBUTION))
      .map(nested -> (Terms) nested.getAggregations().get(nested.getName() + "_terms"))
      .flatMap(terms -> terms.getBuckets().stream())
      .collect(MoreCollectors.uniqueIndex(Bucket::getKeyAsString, bucketToNcloc));
    statistics.setNclocByLanguage(nclocByLanguage);

    return statistics.build();
  }

  private static void addSort(ProjectMeasuresQuery query, SearchRequestBuilder requestBuilder) {
    String sort = query.getSort();
    if (SORT_BY_NAME.equals(sort)) {
      requestBuilder.addSort(DefaultIndexSettingsElement.SORTABLE_ANALYZER.subField(FIELD_NAME), query.isAsc() ? ASC : DESC);
    } else if (SORT_BY_LAST_ANALYSIS_DATE.equals(sort)) {
      requestBuilder.addSort(FIELD_ANALYSED_AT, query.isAsc() ? ASC : DESC);
    } else if (ALERT_STATUS_KEY.equals(sort)) {
      requestBuilder.addSort(FIELD_QUALITY_GATE_STATUS, query.isAsc() ? ASC : DESC);
      requestBuilder.addSort(DefaultIndexSettingsElement.SORTABLE_ANALYZER.subField(FIELD_NAME), ASC);
    } else {
      addMetricSort(query, requestBuilder, sort);
      requestBuilder.addSort(DefaultIndexSettingsElement.SORTABLE_ANALYZER.subField(FIELD_NAME), ASC);
    }
    // last sort is by key in order to be deterministic when same value
    requestBuilder.addSort(FIELD_KEY, ASC);
  }

  private static void addMetricSort(ProjectMeasuresQuery query, SearchRequestBuilder requestBuilder, String sort) {
    requestBuilder.addSort(
      new FieldSortBuilder(FIELD_MEASURES_VALUE)
        .setNestedSort(
          new NestedSortBuilder(FIELD_MEASURES)
            .setFilter(termQuery(FIELD_MEASURES_KEY, sort)))
        .order(query.isAsc() ? ASC : DESC));
  }

  private static void addRangeFacet(SearchRequestBuilder esSearch, String metricKey, StickyFacetBuilder facetBuilder, Double... thresholds) {
    esSearch.addAggregation(createStickyFacet(metricKey, facetBuilder, createRangeFacet(metricKey, thresholds)));
  }

  private static void addRangeFacetIncludingNoData(SearchRequestBuilder esSearch, String metricKey, StickyFacetBuilder facetBuilder, Double... thresholds) {
    esSearch.addAggregation(createStickyFacet(metricKey, facetBuilder,
      AggregationBuilders.filter("combined_" + metricKey, matchAllQuery())
        .subAggregation(createRangeFacet(metricKey, thresholds))
        .subAggregation(createNoDataFacet(metricKey))));
  }

  private static void addRatingFacet(SearchRequestBuilder esSearch, String metricKey, StickyFacetBuilder facetBuilder) {
    esSearch.addAggregation(createStickyFacet(metricKey, facetBuilder, createRatingFacet(metricKey)));
  }

  private static void addLanguagesFacet(SearchRequestBuilder esSearch, ProjectMeasuresQuery query, StickyFacetBuilder facetBuilder) {
    esSearch.addAggregation(facetBuilder.buildStickyFacet(FIELD_LANGUAGES, FILTER_LANGUAGES, query.getLanguages().map(Set::toArray).orElseGet(() -> new Object[] {})));
  }

  private static void addTagsFacet(SearchRequestBuilder esSearch, ProjectMeasuresQuery query, StickyFacetBuilder facetBuilder) {
    esSearch.addAggregation(facetBuilder.buildStickyFacet(FIELD_TAGS, FILTER_TAGS, query.getTags().map(Set::toArray).orElseGet(() -> new Object[] {})));
  }

  private static void addFacets(SearchRequestBuilder esSearch, SearchOptions options, Map<String, QueryBuilder> filters, ProjectMeasuresQuery query) {
    StickyFacetBuilder facetBuilder = new StickyFacetBuilder(matchAllQuery(), filters);
    options.getFacets().stream()
      .filter(FACET_FACTORIES::containsKey)
      .map(FACET_FACTORIES::get)
      .forEach(factory -> factory.addFacet(esSearch, query, facetBuilder));
  }

  private static AbstractAggregationBuilder createStickyFacet(String facetKey, StickyFacetBuilder facetBuilder, AbstractAggregationBuilder aggregationBuilder) {
    BoolQueryBuilder facetFilter = facetBuilder.getStickyFacetFilter(facetKey);
    return AggregationBuilders
      .global(facetKey)
      .subAggregation(
        AggregationBuilders
          .filter("facet_filter_" + facetKey, facetFilter)
          .subAggregation(aggregationBuilder));
  }

  private static AbstractAggregationBuilder createRangeFacet(String metricKey, Double... thresholds) {
    RangeAggregationBuilder rangeAgg = AggregationBuilders.range(metricKey)
      .field(FIELD_MEASURES_VALUE);
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
        AggregationBuilders.filter("filter_" + metricKey, termsQuery(FIELD_MEASURES_KEY, metricKey))
          .subAggregation(rangeAgg));
  }

  private static AbstractAggregationBuilder createNoDataFacet(String metricKey) {
    return AggregationBuilders.filter(
      "no_data_" + metricKey,
      boolQuery().mustNot(nestedQuery(FIELD_MEASURES, termQuery(FIELD_MEASURES_KEY, metricKey), ScoreMode.Avg)));
  }

  private static AbstractAggregationBuilder createRatingFacet(String metricKey) {
    return AggregationBuilders.nested("nested_" + metricKey, FIELD_MEASURES)
      .subAggregation(
        AggregationBuilders.filter("filter_" + metricKey, termsQuery(FIELD_MEASURES_KEY, metricKey))
          .subAggregation(filters(metricKey,
            new KeyedFilter("1", termQuery(FIELD_MEASURES_VALUE, 1d)),
            new KeyedFilter("2", termQuery(FIELD_MEASURES_VALUE, 2d)),
            new KeyedFilter("3", termQuery(FIELD_MEASURES_VALUE, 3d)),
            new KeyedFilter("4", termQuery(FIELD_MEASURES_VALUE, 4d)),
            new KeyedFilter("5", termQuery(FIELD_MEASURES_VALUE, 5d)))));
  }

  private static AbstractAggregationBuilder createQualityGateFacet(ProjectMeasuresQuery projectMeasuresQuery) {
    return filters(
      ALERT_STATUS_KEY,
      QUALITY_GATE_STATUS
        .entrySet()
        .stream()
        .filter(qgs -> !(projectMeasuresQuery.isIgnoreWarning() && qgs.getKey().equals(Metric.Level.WARN.name())))
        .map(entry -> new KeyedFilter(entry.getKey(), termQuery(FIELD_QUALITY_GATE_STATUS, entry.getValue())))
        .toArray(KeyedFilter[]::new));
  }

  private Map<String, QueryBuilder> createFilters(ProjectMeasuresQuery query) {
    Map<String, QueryBuilder> filters = new HashMap<>();
    filters.put("__indexType", termQuery(FIELD_INDEX_TYPE, TYPE_PROJECT_MEASURES.getName()));
    if (!query.isIgnoreAuthorization()) {
      filters.put("__authorization", authorizationTypeSupport.createQueryFilter());
    }
    Multimap<String, MetricCriterion> metricCriterionMultimap = ArrayListMultimap.create();
    query.getMetricCriteria().forEach(metricCriterion -> metricCriterionMultimap.put(metricCriterion.getMetricKey(), metricCriterion));
    metricCriterionMultimap.asMap().forEach((key, value) -> {
      BoolQueryBuilder metricFilters = boolQuery();
      value
        .stream()
        .map(ProjectMeasuresIndex::toQuery)
        .forEach(metricFilters::must);
      filters.put(key, metricFilters);
    });

    query.getQualityGateStatus()
      .ifPresent(qualityGateStatus -> filters.put(ALERT_STATUS_KEY, termQuery(FIELD_QUALITY_GATE_STATUS, QUALITY_GATE_STATUS.get(qualityGateStatus.name()))));

    query.getProjectUuids()
      .ifPresent(projectUuids -> filters.put("ids", termsQuery("_id", projectUuids)));

    query.getLanguages()
      .ifPresent(languages -> filters.put(FILTER_LANGUAGES, termsQuery(FIELD_LANGUAGES, languages)));

    query.getOrganizationUuid()
      .ifPresent(organizationUuid -> filters.put(FIELD_ORGANIZATION_UUID, termQuery(FIELD_ORGANIZATION_UUID, organizationUuid)));

    query.getTags()
      .ifPresent(tags -> filters.put(FIELD_TAGS, termsQuery(FIELD_TAGS, tags)));

    query.getQueryText()
      .map(ProjectsTextSearchQueryFactory::createQuery)
      .ifPresent(queryBuilder -> filters.put("textQuery", queryBuilder));
    return filters;
  }

  private static QueryBuilder toQuery(MetricCriterion criterion) {
    if (criterion.isNoData()) {
      return boolQuery().mustNot(
        nestedQuery(
          FIELD_MEASURES,
          termQuery(FIELD_MEASURES_KEY, criterion.getMetricKey()),
          ScoreMode.Avg));
    }
    return nestedQuery(
      FIELD_MEASURES,
      boolQuery()
        .filter(termQuery(FIELD_MEASURES_KEY, criterion.getMetricKey()))
        .filter(toValueQuery(criterion)),
      ScoreMode.Avg);
  }

  private static QueryBuilder toValueQuery(MetricCriterion criterion) {
    String fieldName = FIELD_MEASURES_VALUE;

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

  public List<String> searchTags(@Nullable String textQuery, int size) {
    int maxPageSize = 500;
    checkArgument(size <= maxPageSize, "Page size must be lower than or equals to " + maxPageSize);
    if (size <= 0) {
      return emptyList();
    }

    TermsAggregationBuilder tagFacet = AggregationBuilders.terms(FIELD_TAGS)
      .field(FIELD_TAGS)
      .size(size)
      .minDocCount(1)
      .order(BucketOrder.key(true));
    if (textQuery != null) {
      tagFacet.includeExclude(new IncludeExclude(".*" + escapeSpecialRegexChars(textQuery) + ".*", null));
    }

    SearchRequestBuilder searchQuery = client
      .prepareSearch(TYPE_PROJECT_MEASURES.getMainType())
      .setQuery(authorizationTypeSupport.createQueryFilter())
      .setFetchSource(false)
      .setSize(0)
      .addAggregation(tagFacet);

    Terms aggregation = searchQuery.get().getAggregations().get(FIELD_TAGS);

    return aggregation.getBuckets().stream()
      .map(Bucket::getKeyAsString)
      .collect(MoreCollectors.toList());
  }

  @FunctionalInterface
  private interface FacetSetter {
    void addFacet(SearchRequestBuilder esSearch, ProjectMeasuresQuery query, StickyFacetBuilder facetBuilder);
  }

}
