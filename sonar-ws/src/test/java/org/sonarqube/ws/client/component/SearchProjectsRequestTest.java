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
package org.sonarqube.ws.client.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class SearchProjectsRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SearchProjectsRequest.Builder underTest = SearchProjectsRequest.builder();

  @Test
  public void filter_parameter() throws Exception {
    SearchProjectsRequest result = underTest
      .setFilter("ncloc > 10")
      .build();

    assertThat(result.getFilter()).isEqualTo("ncloc > 10");
  }

  @Test
  public void set_facets() throws Exception {
    SearchProjectsRequest result = underTest
      .setFacets(singletonList("ncloc"))
      .build();

    assertThat(result.getFacets()).containsOnly("ncloc");
  }

  @Test
  public void facets_are_empty_by_default() throws Exception {
    SearchProjectsRequest result = underTest.build();

    assertThat(result.getFacets()).isEmpty();
  }

  @Test
  public void fail_if_facets_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.setFacets(null);
  }

  @Test
  public void default_page_values() {
    SearchProjectsRequest result = underTest.build();

    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(100);
  }

  @Test
  public void handle_paging_limit_values() {
    SearchProjectsRequest result = underTest.setPageSize(500).build();

    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(500);
  }

  @Test
  public void fail_if_page_size_greater_than_500() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page size must not be greater than 500");

    underTest.setPageSize(501).build();
  }
}
