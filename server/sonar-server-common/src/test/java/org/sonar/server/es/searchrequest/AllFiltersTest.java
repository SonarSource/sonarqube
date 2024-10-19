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

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.FilterScope;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.mockito.Mockito.mock;

public class AllFiltersTest {
  @Test
  public void newalways_returns_a_new_instance() {
    int expected = 1 + new Random().nextInt(200);
    RequestFiltersComputer.AllFilters[] instances = IntStream.range(0, expected)
      .mapToObj(t -> RequestFiltersComputer.newAllFilters())
      .toArray(RequestFiltersComputer.AllFilters[]::new);

    assertThat(instances).hasSize(expected);
  }

  @Test
  public void addFilter_fails_if_name_is_null() {
    FilterScope filterScope = mock(FilterScope.class);
    RequestFiltersComputer.AllFilters allFilters = RequestFiltersComputer.newAllFilters();

    BoolQueryBuilder boolQuery = boolQuery();
    assertThatThrownBy(() -> allFilters.addFilter(null, filterScope, boolQuery))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("name can't be null");
  }

  @Test
  public void addFilter_fails_if_fieldname_is_null() {
    String name = randomAlphabetic(12);
    RequestFiltersComputer.AllFilters allFilters = RequestFiltersComputer.newAllFilters();

    BoolQueryBuilder boolQuery = boolQuery();
    assertThatThrownBy(() -> allFilters.addFilter(name, null, boolQuery))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("filterScope can't be null");
  }

  @Test
  public void addFilter_fails_if_field_with_name_already_exists() {
    String name1 = randomAlphabetic(12);
    String name2 = randomAlphabetic(15);
    FilterScope filterScope1 = mock(FilterScope.class);
    FilterScope filterScope2 = mock(FilterScope.class);
    RequestFiltersComputer.AllFilters allFilters = RequestFiltersComputer.newAllFilters();
    allFilters.addFilter(name2, filterScope1, boolQuery());

    Stream.<ThrowingCallable>of(
      // exact same call
      () -> allFilters.addFilter(name2, filterScope1, boolQuery()),
      // call with a different fieldName
      () -> allFilters.addFilter(name2, filterScope2, boolQuery()))
      .forEach(t -> assertThatThrownBy(t)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("A filter with name " + name2 + " has already been added"));
  }

  @Test
  public void addFilter_does_not_add_filter_if_QueryBuilder_is_null() {
    String name = randomAlphabetic(12);
    String name2 = randomAlphabetic(14);
    RequestFiltersComputer.AllFilters allFilters = RequestFiltersComputer.newAllFilters();
    BoolQueryBuilder query = boolQuery();
    allFilters.addFilter(name, mock(FilterScope.class), query)
      .addFilter(name2, mock(FilterScope.class), null);

    List<QueryBuilder> all = allFilters.stream().toList();
    assertThat(all).hasSize(1);
    assertThat(all.iterator().next()).isSameAs(query);
  }

  @Test
  public void stream_is_empty_when_addFilter_never_called() {
    RequestFiltersComputer.AllFilters allFilters = RequestFiltersComputer.newAllFilters();

    assertThat(allFilters.stream()).isEmpty();
  }
}
