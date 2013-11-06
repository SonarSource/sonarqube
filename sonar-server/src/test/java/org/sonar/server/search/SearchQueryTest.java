/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.search;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class SearchQueryTest {

  @Test
  public void should_return_empty_query() {
    assertThat(SearchQuery.create().getQueryString()).isEmpty();
  }

  @Test
  public void should_handle_custom_query() {
    assertThat(SearchQuery.create("polop").getQueryString()).isEqualTo("polop");
  }

  @Test
  public void should_add_fields() {
    assertThat(SearchQuery.create()
      .field("field1", "value1")
      .field("field2", "value2")
      .getQueryString()).isEqualTo("field1:value1 AND field2:value2");
  }

  @Test
  public void should_add_fields_to_custom_query() {
    assertThat(SearchQuery.create("polop")
      .field("field1", "value1")
      .field("field2", "value2")
      .getQueryString()).isEqualTo("polop AND field1:value1 AND field2:value2");
  }

}
