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
package org.sonar.server.es.searchrequest;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.Test;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.es.searchrequest.TopAggregationHelper.NO_EXTRA_FILTER_V2;
import static org.sonar.server.es.searchrequest.TopAggregationHelper.NO_OTHER_SUBAGGREGATION_V2;

public class TopAggregationHelperTest {

  public static final int DEFAULT_BUCKET_SIZE = 10;
  private RequestFiltersComputer filtersComputer = mock(RequestFiltersComputer.class);
  private SubAggregationHelper subAggregationHelper = mock(SubAggregationHelper.class);
  private TopAggregationHelper underTest = new TopAggregationHelper(filtersComputer, subAggregationHelper);

  @Test
  public void buildTopAggregationV2_fails_with_ISE_if_no_subaggregation_added_by_lambda() {
    String aggregationName = "name";
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    assertThatThrownBy(() -> underTest.buildTopAggregationV2(aggregationName, topAggregation, NO_EXTRA_FILTER_V2, NO_OTHER_SUBAGGREGATION_V2))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("no sub-aggregation has been added to top-aggregation " + aggregationName);
  }

  @Test
  public void buildTopAggregationV2_adds_subAggregation_from_lambda_parameter() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    String[] subAggNames = IntStream.range(0, 5)
      .mapToObj(i -> "subAgg_" + i)
      .toArray(String[]::new);
    String topAggregationName = secure().nextAlphabetic(10);

    Aggregation aggregation = underTest.buildTopAggregationV2(topAggregationName, topAggregation,
      NO_EXTRA_FILTER_V2, subAggs -> {
        for (String name : subAggNames) {
          subAggs.put(name, Aggregation.of(a -> a.min(m -> m.field("f"))));
        }
      });

    assertThat(aggregation.isFilter()).isTrue();
    assertThat(aggregation.aggregations()).hasSize(subAggNames.length);
    assertThat(aggregation.aggregations().keySet()).containsExactlyInAnyOrder(subAggNames);
  }

  @Test
  public void buildTopAggregationV2_adds_filter_from_FiltersComputer_for_TopAggregation() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    Query computerFilter = Query.of(q -> q.term(t -> t.field("f").value("v")));
    when(filtersComputer.getTopAggregationFilterV2(topAggregation)).thenReturn(Optional.of(computerFilter));
    Aggregation subAgg = Aggregation.of(a -> a.min(m -> m.field("donut")));
    String topAggregationName = secure().nextAlphabetic(10);

    Aggregation aggregation = underTest.buildTopAggregationV2(topAggregationName, topAggregation,
      NO_EXTRA_FILTER_V2, subAggs -> subAggs.put("sub", subAgg));

    assertThat(aggregation.isFilter()).isTrue();
    Query filter = aggregation.filter();
    assertThat(filter.isBool()).isTrue();
    assertThat(filter.bool().must()).hasSize(1);
  }

  @Test
  public void buildTopAggregationV2_has_empty_bool_filter_when_FiltersComputer_returns_empty() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    when(filtersComputer.getTopAggregationFilterV2(topAggregation)).thenReturn(Optional.empty());
    String topAggregationName = secure().nextAlphabetic(10);

    Aggregation aggregation = underTest.buildTopAggregationV2(topAggregationName, topAggregation,
      NO_EXTRA_FILTER_V2, subAggs -> subAggs.put("sub", Aggregation.of(a -> a.min(m -> m.field("f")))));

    assertThat(aggregation.isFilter()).isTrue();
    Query filter = aggregation.filter();
    assertThat(filter.isBool()).isTrue();
  }

  @Test
  public void buildTopAggregationV2_adds_filter_and_extra_filter_from_lambda() {
    String topAggregationName = secure().nextAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    Query computerFilter = Query.of(q -> q.term(t -> t.field("f").value("v")));
    Query extraFilter = Query.of(q -> q.term(t -> t.field("extra").value("x")));
    when(filtersComputer.getTopAggregationFilterV2(topAggregation)).thenReturn(Optional.of(computerFilter));

    Aggregation aggregation = underTest.buildTopAggregationV2(topAggregationName, topAggregation,
      b -> b.must(extraFilter),
      subAggs -> subAggs.put("sub", Aggregation.of(a -> a.min(m -> m.field("f")))));

    assertThat(aggregation.isFilter()).isTrue();
    Query filter = aggregation.filter();
    assertThat(filter.isBool()).isTrue();
    assertThat(filter.bool().must()).hasSize(2);
  }

  @Test
  public void buildTopAggregationV2_does_not_add_subaggregation_from_subAggregationHelper() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    when(filtersComputer.getTopAggregationFilterV2(topAggregation)).thenReturn(Optional.empty());
    String topAggregationName = secure().nextAlphabetic(10);

    underTest.buildTopAggregationV2(topAggregationName, topAggregation, NO_EXTRA_FILTER_V2,
      subAggs -> subAggs.put("sub", Aggregation.of(a -> a.min(m -> m.field("f")))));

    verifyNoInteractions(subAggregationHelper);
  }

  @Test
  public void buildTermTopAggregationV2_adds_term_subaggregation_from_subAggregationHelper() {
    String topAggregationName = secure().nextAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    Aggregation termSubAgg = Aggregation.of(a -> a.terms(t -> t.field("foo")));
    when(subAggregationHelper.buildTermsAggregationV2(topAggregation, null)).thenReturn(termSubAgg);

    Aggregation aggregation = underTest.buildTermTopAggregationV2(
      topAggregationName, topAggregation, null,
      NO_EXTRA_FILTER_V2, NO_OTHER_SUBAGGREGATION_V2);

    assertThat(aggregation.isFilter()).isTrue();
    assertThat(aggregation.aggregations()).containsKey(topAggregationName);
    assertThat(aggregation.aggregations().get(topAggregationName)).isSameAs(termSubAgg);
  }

  @Test
  public void buildTermTopAggregationV2_adds_additional_subAggregations_from_lambda() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    String[] extraAggNames = IntStream.range(0, 5)
      .mapToObj(i -> "subAgg_" + i)
      .toArray(String[]::new);
    String topAggregationName = secure().nextAlphabetic(10);
    Aggregation termSubAgg = Aggregation.of(a -> a.terms(t -> t.field("foo")));
    when(subAggregationHelper.buildTermsAggregationV2(topAggregation, null)).thenReturn(termSubAgg);

    Aggregation aggregation = underTest.buildTermTopAggregationV2(
      topAggregationName, topAggregation, null,
      NO_EXTRA_FILTER_V2, subAggs -> {
        for (String name : extraAggNames) {
          subAggs.put(name, Aggregation.of(a -> a.min(m -> m.field("f"))));
        }
      });

    assertThat(aggregation.isFilter()).isTrue();
    assertThat(aggregation.aggregations()).hasSize(extraAggNames.length + 1);
    assertThat(aggregation.aggregations()).containsKey(topAggregationName);
  }

  @Test
  public void buildTermTopAggregationV2_adds_filter_from_FiltersComputer_for_TopAggregation() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    Query computerFilter = Query.of(q -> q.term(t -> t.field("f").value("v")));
    when(filtersComputer.getTopAggregationFilterV2(topAggregation)).thenReturn(Optional.of(computerFilter));
    String topAggregationName = secure().nextAlphabetic(10);
    Aggregation termSubAgg = Aggregation.of(a -> a.terms(t -> t.field("foo")));
    when(subAggregationHelper.buildTermsAggregationV2(topAggregation, null)).thenReturn(termSubAgg);

    Aggregation aggregation = underTest.buildTermTopAggregationV2(
      topAggregationName, topAggregation, null,
      NO_EXTRA_FILTER_V2, NO_OTHER_SUBAGGREGATION_V2);

    assertThat(aggregation.isFilter()).isTrue();
    Query filter = aggregation.filter();
    assertThat(filter.isBool()).isTrue();
    assertThat(filter.bool().must()).hasSize(1);
  }

  @Test
  public void buildTermTopAggregationV2_has_empty_bool_filter_when_FiltersComputer_returns_empty() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    when(filtersComputer.getTopAggregationFilterV2(topAggregation)).thenReturn(Optional.empty());
    String topAggregationName = secure().nextAlphabetic(10);
    Aggregation termSubAgg = Aggregation.of(a -> a.terms(t -> t.field("foo")));
    when(subAggregationHelper.buildTermsAggregationV2(topAggregation, null)).thenReturn(termSubAgg);

    Aggregation aggregation = underTest.buildTermTopAggregationV2(
      topAggregationName, topAggregation, null,
      NO_EXTRA_FILTER_V2, NO_OTHER_SUBAGGREGATION_V2);

    assertThat(aggregation.isFilter()).isTrue();
    assertThat(aggregation.filter().isBool()).isTrue();
  }

  @Test
  public void buildTermTopAggregationV2_adds_filter_and_extra_filter_from_lambda() {
    String topAggregationName = secure().nextAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    Query computerFilter = Query.of(q -> q.term(t -> t.field("f").value("v")));
    Query extraFilter = Query.of(q -> q.term(t -> t.field("extra").value("x")));
    when(filtersComputer.getTopAggregationFilterV2(topAggregation)).thenReturn(Optional.of(computerFilter));
    Aggregation termSubAgg = Aggregation.of(a -> a.terms(t -> t.field("foo")));
    when(subAggregationHelper.buildTermsAggregationV2(topAggregation, null)).thenReturn(termSubAgg);

    Aggregation aggregation = underTest.buildTermTopAggregationV2(
      topAggregationName, topAggregation, null,
      b -> b.must(extraFilter), NO_OTHER_SUBAGGREGATION_V2);

    assertThat(aggregation.isFilter()).isTrue();
    Query filter = aggregation.filter();
    assertThat(filter.isBool()).isTrue();
    assertThat(filter.bool().must()).hasSize(2);
  }
}
