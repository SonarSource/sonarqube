/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.es.searchrequest.TopAggregationHelperTest.DEFAULT_BUCKET_SIZE;

public class SubAggregationHelperTest {
  private static final BucketOrder ES_BUILTIN_TIE_BREAKER = BucketOrder.key(true);
  private static final BucketOrder SQ_DEFAULT_BUCKET_ORDER = BucketOrder.count(false);

  private AbstractAggregationBuilder<?> customSubAgg = AggregationBuilders.sum("foo");
  private SubAggregationHelper underTest = new SubAggregationHelper();
  private BucketOrder customOrder = BucketOrder.count(true);
  private SubAggregationHelper underTestWithCustomSubAgg = new SubAggregationHelper(customSubAgg);
  private SubAggregationHelper underTestWithCustomsSubAggAndOrder = new SubAggregationHelper(customSubAgg, customOrder);

  @Test
  public void buildTermsAggregation_adds_term_subaggregation_with_minDoc_1_and_default_sort() {
    String aggName = randomAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    Stream.of(
      underTest,
      underTestWithCustomSubAgg)
      .forEach(t -> {
        TermsAggregationBuilder agg = t.buildTermsAggregation(aggName, topAggregation, null);

        assertThat(agg.getName()).isEqualTo(aggName);
        assertThat(agg.field()).isEqualTo(topAggregation.getFilterScope().getFieldName());
        assertThat(agg.size()).isEqualTo(DEFAULT_BUCKET_SIZE);
        assertThat(agg.minDocCount()).isOne();
        assertThat(agg.order()).isEqualTo(BucketOrder.compound(SQ_DEFAULT_BUCKET_ORDER, ES_BUILTIN_TIE_BREAKER));
      });
  }

  @Test
  public void buildTermsAggregation_adds_custom_order_from_constructor() {
    String aggName = randomAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    TermsAggregationBuilder agg = underTestWithCustomsSubAggAndOrder.buildTermsAggregation(aggName, topAggregation, null);

    assertThat(agg.getName()).isEqualTo(aggName);
    assertThat(agg.field()).isEqualTo(topAggregation.getFilterScope().getFieldName());
    assertThat(agg.order()).isEqualTo(BucketOrder.compound(customOrder, ES_BUILTIN_TIE_BREAKER));
  }

  @Test
  public void buildTermsAggregation_adds_custom_sub_agg_from_constructor() {
    String aggName = randomAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    Stream.of(
      underTestWithCustomSubAgg,
      underTestWithCustomsSubAggAndOrder)
      .forEach(t -> {
        TermsAggregationBuilder agg = t.buildTermsAggregation(aggName, topAggregation, null);

        assertThat(agg.getName()).isEqualTo(aggName);
        assertThat(agg.field()).isEqualTo(topAggregation.getFilterScope().getFieldName());
        assertThat(agg.getSubAggregations()).hasSize(1);
        assertThat(agg.getSubAggregations().iterator().next()).isSameAs(customSubAgg);
      });
  }

  @Test
  public void buildTermsAggregation_adds_custom_size_if_TermTopAggregation_specifies_one() {
    String aggName = randomAlphabetic(10);
    int customSize = 1 + new Random().nextInt(400);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    Stream.of(
      underTest,
      underTestWithCustomSubAgg,
      underTestWithCustomsSubAggAndOrder)
      .forEach(t -> {
        TermsAggregationBuilder agg = t.buildTermsAggregation(aggName, topAggregation, customSize);

        assertThat(agg.getName()).isEqualTo(aggName);
        assertThat(agg.field()).isEqualTo(topAggregation.getFilterScope().getFieldName());
        assertThat(agg.size()).isEqualTo(customSize);
      });
  }

  @Test
  public void buildSelectedItemsAggregation_returns_empty_if_no_selected_item() {
    String aggName = randomAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);

    Stream.of(
      underTest,
      underTestWithCustomSubAgg,
      underTestWithCustomsSubAggAndOrder)
      .forEach(t -> assertThat(t.buildSelectedItemsAggregation(aggName, topAggregation, new Object[0])).isEmpty());
  }

  @Test
  public void buildSelectedItemsAggregation_does_not_add_custom_order_from_constructor() {
    String aggName = randomAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    String[] selected = randomNonEmptySelected();

    TermsAggregationBuilder agg = underTestWithCustomsSubAggAndOrder.buildSelectedItemsAggregation(aggName, topAggregation, selected)
      .get();

    assertThat(agg.getName()).isEqualTo(aggName + "_selected");
    assertThat(agg.field()).isEqualTo(topAggregation.getFilterScope().getFieldName());
    assertThat(agg.order()).isEqualTo(BucketOrder.compound(SQ_DEFAULT_BUCKET_ORDER, ES_BUILTIN_TIE_BREAKER));
  }

  @Test
  public void buildSelectedItemsAggregation_adds_custom_sub_agg_from_constructor() {
    String aggName = randomAlphabetic(10);
    SimpleFieldTopAggregationDefinition topAggregation = new SimpleFieldTopAggregationDefinition("bar", false);
    String[] selected = randomNonEmptySelected();

    Stream.of(
      underTestWithCustomSubAgg,
      underTestWithCustomsSubAggAndOrder)
      .forEach(t -> {
        TermsAggregationBuilder agg = t.buildSelectedItemsAggregation(aggName, topAggregation, selected).get();

        assertThat(agg.getName()).isEqualTo(aggName + "_selected");
        assertThat(agg.field()).isEqualTo(topAggregation.getFilterScope().getFieldName());
        assertThat(agg.getSubAggregations()).hasSize(1);
        assertThat(agg.getSubAggregations().iterator().next()).isSameAs(customSubAgg);
      });
  }

  private static String[] randomNonEmptySelected() {
    return IntStream.range(0, 1 + new Random().nextInt(22))
      .mapToObj(i -> "selected_" + i)
      .toArray(String[]::new);
  }

}
