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

package org.sonar.server.component;

import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentQueryTest {

  @Test
  public void should_build_query() {
    ComponentQuery query = ComponentQuery.builder()
      .keys(newArrayList("org.codehaus"))
      .names(newArrayList("Sona"))
      .qualifiers(newArrayList("TRK"))
      .pageSize(10)
      .pageIndex(2)
      .sort(ComponentQuery.SORT_BY_NAME)
      .asc(true)
      .build();
    assertThat(query.keys()).containsOnly("org.codehaus");
    assertThat(query.names()).containsOnly("Sona");
    assertThat(query.qualifiers()).containsOnly("TRK");
    assertThat(query.sort()).isEqualTo(ComponentQuery.SORT_BY_NAME);
    assertThat(query.asc()).isTrue();
    assertThat(query.pageSize()).isEqualTo(10);
    assertThat(query.pageIndex()).isEqualTo(2);
  }

  @Test
  public void should_accept_null_sort() {
    ComponentQuery query = ComponentQuery.builder().sort(null).build();
    assertThat(query.sort()).isNull();
  }

  @Test
  public void should_sort_by_name_asc_by_default() {
    ComponentQuery query = ComponentQuery.builder().build();
    assertThat(query.sort()).isEqualTo(ComponentQuery.SORT_BY_NAME);
    assertThat(query.asc()).isTrue();
  }

  @Test
  public void should_throw_exception_if_sort_is_not_valid() {
    try {
      ComponentQuery.builder()
        .sort("UNKNOWN")
        .build();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Bad sort field: UNKNOWN");
    }
  }

  @Test
  public void test_default_page_index_and_size() throws Exception {
    ComponentQuery query = ComponentQuery.builder().build();
    assertThat(query.pageSize()).isEqualTo(ComponentQuery.DEFAULT_PAGE_SIZE);
    assertThat(query.pageIndex()).isEqualTo(ComponentQuery.DEFAULT_PAGE_INDEX);
  }

  @Test
  public void should_build_non_paginated_query() {
    ComponentQuery query = ComponentQuery.builder().pageSize(ComponentQuery.NO_PAGINATION).build();
    assertThat(query.pageSize()).isEqualTo(ComponentQuery.NO_PAGINATION);
    assertThat(query.pageIndex()).isEqualTo(ComponentQuery.DEFAULT_PAGE_INDEX);
  }
}
