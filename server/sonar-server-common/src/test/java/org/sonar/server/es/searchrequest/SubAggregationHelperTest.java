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

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.util.NamedValue;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.es.searchrequest.TopAggregationHelperTest.DEFAULT_BUCKET_SIZE;

public class SubAggregationHelperTest {

  private static final Aggregation CUSTOM_SUB_AGG = Aggregation.of(a -> a.sum(s -> s.field("foo")));

  private SubAggregationHelper underTest = new SubAggregationHelper();
  private SubAggregationHelper underTestWithCustomSubAgg = new SubAggregationHelper(CUSTOM_SUB_AGG, null);
  private SubAggregationHelper underTestWithCustomSubAggAndOrder = new SubAggregationHelper(
    CUSTOM_SUB_AGG, List.of(NamedValue.of("_count", SortOrder.Asc)));

  @Test
  public void buildTermsAggregationV2_adds_term_subaggregation_with_minDoc_1_and_default_sort() {
    String aggName = secure().nextAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    Stream.of(underTest, underTestWithCustomSubAgg)
      .forEach(t -> {
        Aggregation agg = t.buildTermsAggregationV2(topAggregation, null);

        assertThat(agg.isTerms()).isTrue();
        TermsAggregation terms = agg.terms();
        assertThat(terms.field()).isEqualTo(topAggregation.getFilterScope().getFieldName());
        assertThat(terms.minDocCount()).isOne();
        assertThat(terms.order()).extracting(NamedValue::name, NamedValue::value)
          .containsExactly(tuple("_count", SortOrder.Desc));
      });
  }

  @Test
  public void buildTermsAggregationV2_adds_custom_order_from_constructor() {
    String aggName = secure().nextAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    Aggregation agg = underTestWithCustomSubAggAndOrder.buildTermsAggregationV2(topAggregation, null);

    assertThat(agg.isTerms()).isTrue();
    assertThat(agg.terms().order()).extracting(NamedValue::name, NamedValue::value)
      .containsExactly(tuple("_count", SortOrder.Asc));
  }

  @Test
  public void buildTermsAggregationV2_adds_custom_sub_agg_from_constructor() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    Stream.of(underTestWithCustomSubAgg, underTestWithCustomSubAggAndOrder)
      .forEach(t -> {
        Aggregation agg = t.buildTermsAggregationV2(topAggregation, null);

        assertThat(agg.isTerms()).isTrue();
        assertThat(agg.aggregations()).containsKey("subAggregation");
      });
  }

  @Test
  public void buildTermsAggregationV2_adds_custom_size_if_numberOfTerms_specified() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    int customSize = 1 + new Random().nextInt(400);

    Stream.of(underTest, underTestWithCustomSubAgg, underTestWithCustomSubAggAndOrder)
      .forEach(t -> {
        Aggregation agg = t.buildTermsAggregationV2(topAggregation, customSize);

        assertThat(agg.isTerms()).isTrue();
        assertThat(agg.terms().size()).isEqualTo(customSize);
      });
  }

  @Test
  public void buildTermsAggregationV2_does_not_set_size_when_numberOfTerms_is_null() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    Aggregation agg = underTest.buildTermsAggregationV2(topAggregation, null);

    assertThat(agg.isTerms()).isTrue();
    assertThat(agg.terms().size()).isNull();
  }

  @Test
  public void buildSelectedItemsAggregationV2_returns_empty_if_no_selected_item() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    Stream.of(underTest, underTestWithCustomSubAgg, underTestWithCustomSubAggAndOrder)
      .forEach(t -> assertThat(t.buildSelectedItemsAggregationV2(topAggregation, new Object[0])).isEmpty());
  }

  @Test
  public void buildSelectedItemsAggregationV2_returns_aggregation_with_field_and_regexp_include() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    String[] selected = randomNonEmptySelected();

    Aggregation agg = underTest.buildSelectedItemsAggregationV2(topAggregation, selected).get();

    assertThat(agg.isTerms()).isTrue();
    TermsAggregation terms = agg.terms();
    assertThat(terms.field()).isEqualTo(topAggregation.getFilterScope().getFieldName());
    assertThat(terms.include()).isNotNull();
    assertThat(terms.include().isRegexp()).isTrue();
  }

  @Test
  public void buildSelectedItemsAggregationV2_adds_custom_sub_agg_from_constructor() {
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    String[] selected = randomNonEmptySelected();

    Stream.of(underTestWithCustomSubAgg, underTestWithCustomSubAggAndOrder)
      .forEach(t -> {
        Aggregation agg = t.buildSelectedItemsAggregationV2(topAggregation, selected).get();

        assertThat(agg.isTerms()).isTrue();
        assertThat(agg.aggregations()).containsKey("subAggregation");
      });
  }

  private static String[] randomNonEmptySelected() {
    return IntStream.range(0, 1 + new Random().nextInt(22))
      .mapToObj(i -> "selected_" + i)
      .toArray(String[]::new);
  }
}
