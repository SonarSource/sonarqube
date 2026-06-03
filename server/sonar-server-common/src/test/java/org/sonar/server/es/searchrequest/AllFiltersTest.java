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

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.FilterScope;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class AllFiltersTest {
  @Test
  public void newAllFilters_always_returns_a_new_instance() {
    int expected = 1 + new Random().nextInt(200);
    RequestFiltersComputer.AllFilters[] instances = IntStream.range(0, expected)
      .mapToObj(t -> RequestFiltersComputer.newAllFilters())
      .toArray(RequestFiltersComputer.AllFilters[]::new);

    assertThat(instances).hasSize(expected);
  }

  @Test
  public void addFilterV2_fails_if_name_is_null() {
    FilterScope filterScope = mock(FilterScope.class);
    RequestFiltersComputer.AllFilters allFilters = RequestFiltersComputer.newAllFilters();

    Query query = Query.of(q -> q.matchAll(m -> m));
    assertThatThrownBy(() -> allFilters.addFilterV2(null, filterScope, query))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("name can't be null");
  }

  @Test
  public void addFilterV2_fails_if_filterScope_is_null() {
    String name = secure().nextAlphabetic(12);
    RequestFiltersComputer.AllFilters allFilters = RequestFiltersComputer.newAllFilters();

    Query query = Query.of(q -> q.matchAll(m -> m));
    assertThatThrownBy(() -> allFilters.addFilterV2(name, null, query))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("filterScope can't be null");
  }

  @Test
  public void addFilterV2_fails_if_filter_with_same_name_already_exists() {
    String name = secure().nextAlphabetic(15);
    FilterScope filterScope1 = mock(FilterScope.class);
    FilterScope filterScope2 = mock(FilterScope.class);
    RequestFiltersComputer.AllFilters allFilters = RequestFiltersComputer.newAllFilters();
    Query query = Query.of(q -> q.matchAll(m -> m));
    allFilters.addFilterV2(name, filterScope1, query);

    Stream.<ThrowingCallable>of(
      () -> allFilters.addFilterV2(name, filterScope1, query),
      () -> allFilters.addFilterV2(name, filterScope2, query))
      .forEach(t -> assertThatThrownBy(t)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("A filter with name " + name + " has already been added"));
  }

  @Test
  public void addFilterV2_does_not_add_filter_if_query_is_null() {
    String name = secure().nextAlphabetic(12);
    String name2 = secure().nextAlphabetic(14);
    RequestFiltersComputer.AllFilters allFilters = RequestFiltersComputer.newAllFilters();
    Query query = Query.of(q -> q.matchAll(m -> m));
    allFilters.addFilterV2(name, mock(FilterScope.class), query)
      .addFilterV2(name2, mock(FilterScope.class), null);

    List<Query> all = allFilters.streamV2().toList();
    assertThat(all).hasSize(1);
    assertThat(all.iterator().next()).isSameAs(query);
  }

  @Test
  public void streamV2_is_empty_when_addFilterV2_never_called() {
    RequestFiltersComputer.AllFilters allFilters = RequestFiltersComputer.newAllFilters();

    assertThat(allFilters.streamV2()).isEmpty();
  }
}