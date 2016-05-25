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

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.NewIndex.NewIndexType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FacetsMediumTest {

  private static final String INDEX = "facetstests";
  private static final String TYPE = "tagsdoc";
  private static final String FIELD_KEY = "key";
  private static final String FIELD_TAGS = "tags";
  private static final String FIELD_CREATED_AT = "createdAt";

  @Rule
  public EsTester esTester = new EsTester(new FacetsTestDefinition());

  @Test
  public void should_ignore_result_without_aggregations() {
    Facets facets = new Facets(mock(SearchResponse.class));
    assertThat(facets.getFacets()).isEmpty();
    assertThat(facets.getFacetKeys("polop")).isEmpty();
    assertThat(facets.getFacetValues("polop")).isEmpty();
  }

  @Test
  public void should_ignore_unknown_aggregation_type() throws Exception {
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("noTags"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("oneTag", "tag1"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("twoTags", "tag1", "tag2"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("threeTags", "tag1", "tag2", "tag3"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("fourTags", "tag1", "tag2", "tag3", "tag4"));
    SearchRequestBuilder search = esTester.client().prepareSearch(INDEX).setTypes(TYPE)
      .addAggregation(AggregationBuilders.cardinality(FIELD_TAGS).field(FIELD_TAGS));

    Facets facets = new Facets(search.get());
    assertThat(facets.getFacets()).isEmpty();
    assertThat(facets.getFacetKeys(FIELD_TAGS)).isEmpty();
  }

  @Test
  public void should_process_result_with_nested_missing_and_terms_aggregations() throws Exception {
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("noTags"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("oneTag", "tag1"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("fourTags", "tag1", "tag2", "tag3", "tag4"));

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
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("oneTag", "tag1"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("twoTags", "tag1", "tag2"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("threeTags", "tag1", "tag2", "tag3"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("fourTags", "tag1", "tag2", "tag3", "tag4"));

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
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("first"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("second"));
    esTester.putDocuments(INDEX, TYPE, newTagsDocument("third"));

    SearchRequestBuilder search = esTester.client().prepareSearch(INDEX).setTypes(TYPE)
      .addAggregation(
        AggregationBuilders.dateHistogram(FIELD_CREATED_AT)
          .minDocCount(0L)
          .field(FIELD_CREATED_AT)
          .interval(DateHistogramInterval.MINUTE)
          .format(DateUtils.DATETIME_FORMAT));

    Facets facets = new Facets(search.get());
    assertThat(facets.getFacets()).isNotEmpty();
    assertThat(facets.getFacetKeys(FIELD_TAGS)).isEmpty();
    assertThat(facets.getFacetKeys(FIELD_CREATED_AT)).hasSize(1);
    FacetValue value = facets.getFacetValues(FIELD_CREATED_AT).iterator().next();
    assertThat(DateUtils.parseDateTime(value.getKey()).before(new Date())).isTrue();
    assertThat(value.getValue()).isEqualTo(3L);
  }

  private static TagsDoc newTagsDocument(String key, String... tags) {
    return new TagsDoc().setKey(key).setTags(Arrays.asList(tags)).setCreatedAt(new Date());
  }

  private static class FacetsTestDefinition implements org.sonar.server.es.IndexDefinition {

    @Override
    public void define(IndexDefinitionContext context) {
      NewIndexType newType = context.create(INDEX).createType(TYPE);
      newType.stringFieldBuilder(FIELD_KEY).build();
      newType.stringFieldBuilder(FIELD_TAGS).build();
      newType.createDateTimeField(FIELD_CREATED_AT);
    }
  }

  private static class TagsDoc extends BaseDoc {
    public TagsDoc() {
      super(Maps.<String, Object>newHashMap());
    }

    @Override
    public String getId() {
      return getKey();
    }

    @Override
    public String getRouting() {
      return null;
    }

    @Override
    public String getParent() {
      return null;
    }

    public String getKey() {
      return getField(FIELD_KEY);
    }

    public List<String> getTags() {
      return (List<String>) getField(FIELD_TAGS);
    }

    public TagsDoc setKey(String s) {
      setField(FIELD_KEY, s);
      return this;
    }

    public TagsDoc setTags(List<String> tags) {
      setField(FIELD_TAGS, tags);
      return this;
    }

    public TagsDoc setCreatedAt(Date date) {
      setField(FIELD_CREATED_AT, date);
      return this;
    }
  }
}
