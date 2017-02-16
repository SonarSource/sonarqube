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
package org.sonarqube.ws.client.project;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class SearchWsRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_request() throws Exception {
    SearchWsRequest underTest = SearchWsRequest.builder()
      .setOrganization("orga")
      .setQuery("project")
      .setQualifiers(asList("TRK", "VW"))
      .setPage(5)
      .setPageSize(10)
      .build();

    assertThat(underTest.getOrganization()).isEqualTo("orga");
    assertThat(underTest.getQuery()).isEqualTo("project");
    assertThat(underTest.getQualifiers()).containsOnly("TRK", "VW");
    assertThat(underTest.getPage()).isEqualTo(5);
    assertThat(underTest.getPageSize()).isEqualTo(10);
  }

  @Test
  public void fail_when_page_size_is_greather_then_500() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page size must not be greater than 500");

    SearchWsRequest.builder()
      .setPageSize(10000)
      .build();
  }
}
