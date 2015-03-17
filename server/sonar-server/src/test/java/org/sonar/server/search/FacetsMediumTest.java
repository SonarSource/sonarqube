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
package org.sonar.server.search;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram.Interval;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.NewIndex.NewIndexType;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FacetsMediumTest {

  private static final String INDEX = "facetstests";
  private static final String TYPE = "tagsdoc";
  private static final String FIELD_KEY = "key";
  private static final String FIELD_TAGS = "tags";
  private static final String FIELD_CREATED_AT = "createdAt";

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new FacetsTestDefinition());

  @Before
  public void setUp() throws Exception {
    esTester.truncateIndices();
  }

  @Test
  public void should_ignore_result_without_aggregations() throws Exception {
    Facets facets = new Facets(mock(SearchResponse.class));
    assertThat(facets.getFacets()).isEmpty();
    assertThat(facets.getFacetKeys("polop")).isEmpty();
    assertThat(facets.getFacetValues("polop")).isEmpty();
  }

  @Test
  public void should_ignore_unknown_aggregation_type() throws Exception {
    esTester.putDocuments(INDEX, TYPE,
      newTagsDocument("noTags"),
      newTagsDocument("oneTag", "tag1"),
      newTagsDocument("twoTags", "tag1", "tag2"),
      newTagsDocument("twoTags", "tag1", "tag2", "tag3"),
      newTagsDocument("twoTags", "tag1", "tag2", "tag3", "tag4"));
    SearchRequestBuilder search = esTester.client().prepareSearch(INDEX).setTypes(TYPE)
      .addAggregation(AggregationBuilders.cardinality(FIELD_TAGS).field(FIELD_TAGS));

    Facets facets = new Facets(search.get());
    assertThat(facets.getFacets()).isEmpty();
    assertThat(facets.getFacetKeys(FIELD_TAGS)).isEmpty();
  }

  @Test
  public void should_process_result_with_nested_missing_and_terms_aggregations() throws Exception {
    esTester.putDocuments(INDEX, TYPE,
      newTagsDocument("noTags"),
      newTagsDocument("oneTag", "tag1"),
      newTagsDocument("twoTags", "tag1", "tag2"),
      newTagsDocument("twoTags", "tag1", "tag2", "tag3"),
      newTagsDocument("twoTags", "tag1", "tag2", "tag3", "tag4"));

    SearchRequestBuilder search = esTester.client().prepareSearch(INDEX).setTypes(TYPE)
      .addAggregation(AggregationBuilders.global("tags__global")
        .subAggregation(AggregationBuilders.missing("tags_missing").field(FIELD_TAGS))
        .subAggregation(AggregationBuilders.terms("tags").field(FIELD_TAGS).size(2))
        .subAggregation(AggregationBuilders.terms("tags__selected").field(FIELD_TAGS).include("tag4"))
        .subAggregation(AggregationBuilders.terms("__ignored").field(FIELD_TAGS).include("tag3")));

    Facets facets = new Facets(search.get());
    assertThat(facets.getFacets()).isNotEmpty();
    assertThat(facets.getFacetKeys(FIELD_TAGS)).containsOnly("", "tag1", "tag2", "tag4");
    assertThat(facets.getFacetKeys(FIELD_CREATED_AT)).isEmpty();
    // ES internals use HashMap, so can't test the exact string for compatibility with both java 7 and java 8
    assertThat(facets.toString()).startsWith("{tags=[{")
      .contains("__ignored=[{tag3=1}]")
      .contains("{tag4=1}")
      .contains("{=1}")
      .contains("{tag1=2}")
      .contains("{tag2=1}");
  }

  @Test
  public void should_ignore_empty_missing_aggregation() throws Exception {
    esTester.putDocuments(INDEX, TYPE,
      newTagsDocument("oneTag", "tag1"),
      newTagsDocument("twoTags", "tag1", "tag2"),
      newTagsDocument("twoTags", "tag1", "tag2", "tag3"),
      newTagsDocument("twoTags", "tag1", "tag2", "tag3", "tag4"));

    SearchRequestBuilder search = esTester.client().prepareSearch(INDEX).setTypes(TYPE)
      .addAggregation(AggregationBuilders.global("tags__global")
        .subAggregation(AggregationBuilders.missing("tags_missing").field(FIELD_TAGS))
        .subAggregation(AggregationBuilders.terms("tags").field(FIELD_TAGS).size(2))
        .subAggregation(AggregationBuilders.terms("tags__selected").field(FIELD_TAGS).include("tag4"))
        .subAggregation(AggregationBuilders.terms("__ignored").field(FIELD_TAGS).include("tag3")));

    Facets facets = new Facets(search.get());
    assertThat(facets.getFacets()).isNotEmpty();
    assertThat(facets.getFacetKeys(FIELD_TAGS)).containsOnly("tag1", "tag2", "tag4");
    assertThat(facets.getFacetKeys(FIELD_CREATED_AT)).isEmpty();
  }

  @Test
  public void should_process_result_with_date_histogram() throws Exception {
    esTester.putDocuments(INDEX, TYPE,
      newTagsDocument("first"), newTagsDocument("second"), newTagsDocument("third"));

    SearchRequestBuilder search = esTester.client().prepareSearch(INDEX).setTypes(TYPE)
      .addAggregation(
        AggregationBuilders.dateHistogram(FIELD_CREATED_AT)
          .field(FIELD_CREATED_AT)
          .interval(Interval.MINUTE)
          .format(DateUtils.DATETIME_FORMAT));

    Facets facets = new Facets(search.get());
    assertThat(facets.getFacets()).isNotEmpty();
    assertThat(facets.getFacetKeys(FIELD_TAGS)).isEmpty();
    assertThat(facets.getFacetKeys(FIELD_CREATED_AT)).hasSize(1);
    FacetValue value = facets.getFacetValues(FIELD_CREATED_AT).iterator().next();
    assertThat(DateUtils.parseDateTime(value.getKey()).before(new Date()));
    assertThat(value.getValue()).isEqualTo(3L);
  }

  private static Map<String, Object> newTagsDocument(String key, String... tags) {
    ImmutableMap<String, Object> doc = ImmutableMap.<String, Object>of(
      FIELD_KEY, key,
      FIELD_TAGS, Arrays.asList(tags),
      FIELD_CREATED_AT, new Date());
    return doc;
  }

  static class FacetsTestDefinition implements org.sonar.server.es.IndexDefinition {

    @Override
    public void define(IndexDefinitionContext context) {
      NewIndexType newType = context.create(INDEX).createType(TYPE);
      newType.setAttribute("_id", ImmutableMap.of("path", FIELD_KEY));
      newType.stringFieldBuilder(FIELD_KEY).build();
      newType.stringFieldBuilder(FIELD_TAGS).build();
      newType.createDateTimeField(FIELD_CREATED_AT);
    }
  }
}
