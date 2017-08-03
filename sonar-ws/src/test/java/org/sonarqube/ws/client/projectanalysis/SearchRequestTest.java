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
package org.sonarqube.ws.client.projectanalysis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.projectanalysis.EventCategory.QUALITY_GATE;

public class SearchRequestTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SearchRequest.Builder underTest = SearchRequest.builder();

  @Test
  public void search_request() {
    SearchRequest result = underTest
      .setProject("P1")
      .setBranch("my_branch")
      .setCategory(QUALITY_GATE)
      .setPage(2)
      .setPageSize(500)
      .setFrom("2016-01-01")
      .setTo("2017-07-01")
      .build();

    assertThat(result.getProject()).isEqualTo("P1");
    assertThat(result.getBranch()).isEqualTo("my_branch");
    assertThat(result.getPage()).isEqualTo(2);
    assertThat(result.getPageSize()).isEqualTo(500);
    assertThat(result.getCategory()).isEqualTo(QUALITY_GATE);
    assertThat(result.getFrom()).isEqualTo("2016-01-01");
    assertThat(result.getTo()).isEqualTo("2017-07-01");
  }

  @Test
  public void page_default_values() {
    SearchRequest result = underTest.setProject("P1").build();

    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(100);
  }

  @Test
  public void fail_if_project_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.build();
  }

  @Test
  public void fail_if_page_size_greater_than_500() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page size must be lower than or equal to 500");

    underTest.setProject("P1")
      .setPageSize(501)
      .build();
  }
}
