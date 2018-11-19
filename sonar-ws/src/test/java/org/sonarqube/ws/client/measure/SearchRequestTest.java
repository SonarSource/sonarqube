/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.ws.client.measure;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class SearchRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SearchRequest.Builder underTest = SearchRequest.builder();

  @Test
  public void create_request() {
    SearchRequest result = underTest
      .setMetricKeys(singletonList("metric"))
      .setProjectKeys(singletonList("key"))
      .build();

    assertThat(result.getMetricKeys()).containsExactly("metric");
    assertThat(result.getProjectKeys()).containsExactly("key");
  }

  @Test
  public void create_request_with_100_keys() {
    SearchRequest result = underTest
      .setMetricKeys(singletonList("metric"))
      .setProjectKeys(IntStream.rangeClosed(1, 100).mapToObj(Integer::toString).collect(Collectors.toList()))
      .build();

    assertThat(result.getProjectKeys()).hasSize(100);
  }

  @Test
  public void fail_when_non_null_metric_keys() {
    expectExceptionOnMetricKeys();

    underTest.setMetricKeys(null).build();
  }

  @Test
  public void fail_when_non_empty_metric_keys() {
    expectExceptionOnMetricKeys();

    underTest.setMetricKeys(emptyList()).build();
  }

  @Test
  public void fail_when_unset_metric_keys() {
    expectExceptionOnMetricKeys();

    underTest.build();
  }

  @Test
  public void fail_when_component_keys_is_empty() {
    expectExceptionOnComponents();

    underTest
      .setMetricKeys(singletonList("metric"))
      .setProjectKeys(emptyList())
      .build();
  }

  @Test
  public void fail_when_component_keys_contains_more_than_100_keys() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("101 projects provided, more than maximum authorized (100)");

    underTest
      .setMetricKeys(singletonList("metric"))
      .setProjectKeys(IntStream.rangeClosed(1, 101).mapToObj(Integer::toString).collect(Collectors.toList()))
      .build();
  }

  private void expectExceptionOnMetricKeys() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric keys must be provided");
  }

  private void expectExceptionOnComponents() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Project keys must be provided");
  }
}
