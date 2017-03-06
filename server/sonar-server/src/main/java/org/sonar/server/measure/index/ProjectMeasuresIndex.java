/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.Optional;
import java.util.stream.IntStream;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filters.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.DefaultIndexSettingsElement;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.StickyFacetBuilder;
import org.sonar.server.es.textsearch.ComponentTextSearchFeature;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.permission.index.AuthorizationTypeSupport;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filters;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresDoc.QUALITY_GATE_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_LANGUAGES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NAME;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ORGANIZATION_UUID;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALITY_GATE_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.INDEX_TYPE_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGE;

public class ProjectMeasuresIndex extends BaseIndex {

  public static final List<String> SUPPORTED_FACETS = ImmutableList.of(
    NCLOC_KEY,
    DUPLICATED_LINES_DENSITY_KEY,
    COVERAGE_KEY,
    SQALE_RATING_KEY,
    RELIABILITY_RATING_KEY,
    SECURITY_RATING_KEY,
    ALERT_STATUS_KEY,
    FILTER_LANGUAGE,
    FIELD_TAGS);

  private static final String FIELD_MEASURES_KEY = FIELD_MEASURES + "." + ProjectMeasuresIndexDefinition.FIELD_MEASURES_KEY;
  private static final String FIELD_MEASURES_VALUE = FIELD_MEASURES + "." + ProjectMeasuresIndexDefinition.FIELD_MEASURES_VALUE;

  private static final String FIELD_LANGUAGES_KEY = FIELD_LANGUAGES + "." + ProjectMeasuresIndexDefinition.FIELD_LANGUAGES_KEY;
  private static final String FIELD_LANGUAGES_VALUE = FIELD_LANGUAGES + "." + ProjectMeasuresIndexDefinition.FIELD_LANGUAGES_VALUE;

  private static final Map<String, FacetSetter> FACET_FACTORIES = ImmutableMap.<String, FacetSetter>builder()
    .put(NCLOC_KEY, (esSearch, filters) -> addRangeFacet(esSearch, NCLOC_KEY, ImmutableList.of(1_000d, 10_000d, 100_000d, 500_000d), filters))
    .put(DUPLICATED_LINES_DENSITY_KEY, (esSearch, filters) -> addRangeFacet(esSearch, DUPLICATED_LINES_DENSITY_KEY, ImmutableList.of(3d, 5d, 10d, 20d), filters))
    .put(COVERAGE_KEY, (esSearch, filters) -> addRangeFacet(esSearch, COVERAGE_KEY, ImmutableList.of(30d, 50d, 70d, 80d), filters))
    .put(SQALE_RATING_KEY, (esSearch, filters) -> addRatingFacet(esSearch, SQALE_RATING_KEY, filters))
    .put(RELIABILITY_RATING_KEY, (esSearch, filters) -> addRatingFacet(esSearch, RELIABILITY_RATING_KEY, filters))
    .put(SECURITY_RATING_KEY, (esSearch, filters) -> addRatingFacet(esSearch, SECURITY_RATING_KEY, filters))
    .put(ALERT_STATUS_KEY, (esSearch, filters) -> esSearch.addAggregation(createStickyFacet(ALERT_STATUS_KEY, filters, createQualityGateFacet())))
    .put(FILTER_LANGUAGE, (esSearch, filters) -> esSearch.addAggregation(createStickyFacet(FILTER_LANGUAGE, filters, createLanguagesFacet())))
    .put(FIELD_TAGS, (esSearch, filters) -> esSearch.addAggregation(createStickyFacet(FIELD_TAGS, filters, createTagsFacet())))
    .build();

  private final AuthorizationTypeSupport authorizationTypeSupport;

  public ProjectMeasuresIndex(EsClient client, AuthorizationTypeSupport authorizationTypeSupport) {
    super(client);
    this.authorizationTypeSupport = authorizationTypeSupport;
  }

  public SearchIdResult<String> search(ProjectMeasuresQuery query, SearchOptions searchOptions) {
    SearchRequestBuilder requestBuilder = getClient()
      .prepareSearch(INDEX_TYPE_PROJECT_MEASURES)
      .setFetchSource(false)
      .setFrom(searchOptions.getOffset())
      .setSize(searchOptions.getLimit());

    BoolQueryBuilder esFilter = boolQuery();
    Map<String, QueryBuilder> filters = createFilters(query);
    filters.values().forEach(esFilter::must);
    requestBuilder.setQuery(esFilter);

    addFacets(query, requestBuilder, searchOptions, filters);
    addSort(query, requestBuilder);
    return new SearchIdResult<>(requestBuilder.get(), id -> id);
  }

  private static void addSort(ProjectMeasuresQuery query, SearchRequestBuilder requestBuilder) {
    String sort = query.getSort();
    if (SORT_BY_NAME.equals(sort)) {
      requestBuilder.addSort(DefaultIndexSettingsElement.SORTABLE_ANALYZER.subField(FIELD_NAME), query.isAsc() ? ASC : DESC);
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
        .setNestedPath(FIELD_MEASURES)
        .setNestedFilter(termQuery(FIELD_MEASURES_KEY, sort))
        .order(query.isAsc() ? ASC : DESC));
  }

  private static void addFacets(ProjectMeasuresQuery query, SearchRequestBuilder esSearch, SearchOptions options, Map<String, QueryBuilder> filters) {
    options.getFacets().stream()
      .filter(FACET_FACTORIES::containsKey)
      .map(FACET_FACTORIES::get)
      .forEach(factory -> factory.addFacet(esSearch, filters));
  }

  private static void addRangeFacet(SearchRequestBuilder esSearch, String metricKey, List<Double> thresholds, Map<String, QueryBuilder> filters) {
    esSearch.addAggregation(createStickyFacet(metricKey, filters, createRangeFacet(metricKey, thresholds)));
  }

  private static void addRatingFacet(SearchRequestBuilder esSearch, String metricKey, Map<String, QueryBuilder> filters) {
    esSearch.addAggregation(createStickyFacet(metricKey, filters, createRatingFacet(metricKey)));
  }

  private static AbstractAggregationBuilder createStickyFacet(String facetKey, Map<String, QueryBuilder> filters, AbstractAggregationBuilder aggregationBuilder) {
    StickyFacetBuilder facetBuilder = new StickyFacetBuilder(matchAllQuery(), filters);
    BoolQueryBuilder facetFilter = facetBuilder.getStickyFacetFilter(facetKey);
    return AggregationBuilders
      .global(facetKey)
      .subAggregation(AggregationBuilders.filter("facet_filter_" + facetKey)
        .filter(facetFilter)
        .subAggregation(aggregationBuilder));
  }

  private static AbstractAggregationBuilder createRangeFacet(String metricKey, List<Double> thresholds) {
    RangeBuilder rangeAgg = AggregationBuilders.range(metricKey)
      .field(FIELD_MEASURES_VALUE);
    final int lastIndex = thresholds.size() - 1;
    IntStream.range(0, thresholds.size())
      .forEach(i -> {
        if (i == 0) {
          rangeAgg.addUnboundedTo(thresholds.get(0));
          rangeAgg.addRange(thresholds.get(0), thresholds.get(1));
        } else if (i == lastIndex) {
          rangeAgg.addUnboundedFrom(thresholds.get(lastIndex));
        } else {
          rangeAgg.addRange(thresholds.get(i), thresholds.get(i + 1));
        }
      });

    return AggregationBuilders.nested("nested_" + metricKey)
      .path(FIELD_MEASURES)
      .subAggregation(
        AggregationBuilders.filter("filter_" + metricKey)
          .filter(termsQuery(FIELD_MEASURES_KEY, metricKey))
          .subAggregation(rangeAgg));
  }

  private static AbstractAggregationBuilder createRatingFacet(String metricKey) {
    return AggregationBuilders.nested("nested_" + metricKey)
      .path(FIELD_MEASURES)
      .subAggregation(
        AggregationBuilders.filter("filter_" + metricKey)
          .filter(termsQuery(FIELD_MEASURES_KEY, metricKey))
          .subAggregation(filters(metricKey)
            .filter("1", termQuery(FIELD_MEASURES_VALUE, 1d))
            .filter("2", termQuery(FIELD_MEASURES_VALUE, 2d))
            .filter("3", termQuery(FIELD_MEASURES_VALUE, 3d))
            .filter("4", termQuery(FIELD_MEASURES_VALUE, 4d))
            .filter("5", termQuery(FIELD_MEASURES_VALUE, 5d))));
  }

  private static AbstractAggregationBuilder createQualityGateFacet() {
    FiltersAggregationBuilder qualityGateStatusFilter = AggregationBuilders.filters(ALERT_STATUS_KEY);
    QUALITY_GATE_STATUS.entrySet().forEach(entry -> qualityGateStatusFilter.filter(entry.getKey(), termQuery(FIELD_QUALITY_GATE_STATUS, entry.getValue())));
    return qualityGateStatusFilter;
  }

  private static AbstractAggregationBuilder createLanguagesFacet() {
    return AggregationBuilders.nested("nested_" + FILTER_LANGUAGE)
      .path(FIELD_LANGUAGES)
      .subAggregation(
        AggregationBuilders.terms(FILTER_LANGUAGE)
          .field(FIELD_LANGUAGES_KEY)
          .subAggregation(AggregationBuilders.sum("size_" + FILTER_LANGUAGE)
            .field(FIELD_LANGUAGES_VALUE)));
  }

  private static AbstractAggregationBuilder createTagsFacet() {
    return AggregationBuilders.terms(FIELD_TAGS).field(FIELD_TAGS);
  }

  private Map<String, QueryBuilder> createFilters(ProjectMeasuresQuery query) {
    Map<String, QueryBuilder> filters = new HashMap<>();
    filters.put("__authorization", authorizationTypeSupport.createQueryFilter());
    Multimap<String, MetricCriterion> metricCriterionMultimap = ArrayListMultimap.create();
    query.getMetricCriteria().forEach(metricCriterion -> metricCriterionMultimap.put(metricCriterion.getMetricKey(), metricCriterion));
    metricCriterionMultimap.asMap().entrySet().forEach(entry -> {
      BoolQueryBuilder metricFilters = boolQuery();
      entry.getValue()
        .stream()
        .map(criterion -> nestedQuery(FIELD_MEASURES, boolQuery()
          .filter(termQuery(FIELD_MEASURES_KEY, criterion.getMetricKey()))
          .filter(toValueQuery(criterion))))
        .forEach(metricFilters::must);
      filters.put(entry.getKey(), metricFilters);
    });

    query.getQualityGateStatus()
      .ifPresent(qualityGateStatus -> filters.put(ALERT_STATUS_KEY, termQuery(FIELD_QUALITY_GATE_STATUS, QUALITY_GATE_STATUS.get(qualityGateStatus.name()))));

    query.getProjectUuids()
      .ifPresent(projectUuids -> filters.put("ids", termsQuery("_id", projectUuids)));

    query.getLanguages()
      .ifPresent(languages -> filters.put(FILTER_LANGUAGE,
        nestedQuery(FIELD_LANGUAGES, boolQuery().filter(termsQuery(FIELD_LANGUAGES_KEY, languages)))));

    query.getOrganizationUuid()
      .ifPresent(organizationUuid -> filters.put(FIELD_ORGANIZATION_UUID, termQuery(FIELD_ORGANIZATION_UUID, organizationUuid)));

    query.getTags()
      .ifPresent(tags -> filters.put(FIELD_TAGS, termsQuery(FIELD_TAGS, tags)));

    createTextQueryFilter(query).ifPresent(queryBuilder -> filters.put("textQuery", queryBuilder));
    return filters;
  }

  private static Optional<QueryBuilder> createTextQueryFilter(ProjectMeasuresQuery query) {
    Optional<String> queryText = query.getQueryText();
    if (!queryText.isPresent()) {
      return Optional.empty();
    }
    ComponentTextSearchQueryFactory.ComponentTextSearchQuery componentTextSearchQuery = ComponentTextSearchQueryFactory.ComponentTextSearchQuery.builder()
      .setQueryText(queryText.get())
      .setFieldKey(FIELD_KEY)
      .setFieldName(FIELD_NAME)
      .build();
    return Optional.of(ComponentTextSearchQueryFactory.createQuery(componentTextSearchQuery, ComponentTextSearchFeature.values()));
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

  @FunctionalInterface
  private interface FacetSetter {
    void addFacet(SearchRequestBuilder esSearch, Map<String, QueryBuilder> filters);
  }

}
