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

package org.sonarqube.ws.client.measure;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.measure.SearchHistoryRequest.DEFAULT_PAGE_SIZE;
import static org.sonarqube.ws.client.measure.SearchHistoryRequest.MAX_PAGE_SIZE;

public class SearchHistoryRequestTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SearchHistoryRequest.Builder underTest = SearchHistoryRequest.builder();

  @Test
  public void full_example() {
    SearchHistoryRequest result = underTest
      .setComponent("C1")
      .setMetrics(singletonList("new_lines"))
      .setFrom("2017-01-15")
      .setTo("2017-01-20")
      .setPage(23)
      .setPageSize(42)
      .build();

    assertThat(result)
      .extracting(SearchHistoryRequest::getComponent, SearchHistoryRequest::getMetrics, SearchHistoryRequest::getFrom, SearchHistoryRequest::getTo,
        SearchHistoryRequest::getPage, SearchHistoryRequest::getPageSize)
      .containsExactly("C1", singletonList("new_lines"), "2017-01-15", "2017-01-20", 23, 42);
  }

  @Test
  public void default_values() {
    SearchHistoryRequest result = underTest.setComponent("C1").setMetrics(singletonList("new_lines")).build();

    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
  }

  @Test
  public void fail_if_no_component() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component key is required");

    underTest.setMetrics(singletonList("new_lines")).build();
  }

  @Test
  public void fail_if_empty_component() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component key is required");

    underTest.setComponent("").setMetrics(singletonList("new_lines")).build();
  }

  @Test
  public void fail_if_no_metric() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric keys are required");

    underTest.setComponent("C1").build();
  }

  @Test
  public void fail_if_empty_metrics() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric keys are required");

    underTest.setComponent("C1").setMetrics(emptyList()).build();
  }

  @Test
  public void fail_if_page_size_greater_than_max_authorized_size() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page size (1001) must be lower than or equal to 1000");

    underTest.setComponent("C1").setMetrics(singletonList("violations")).setPageSize(MAX_PAGE_SIZE + 1).build();
  }
}
