/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.component.index;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SuggestionQueryTest {


  @Test
  public void should_fail_with_IAE_if_query_is_empty() {
    assertThatThrownBy(() -> SuggestionQuery.builder().setQuery(""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Query must be at least two characters long");
  }

  @Test
  public void should_fail_with_IAE_if_query_is_one_character_long() {
    assertThatThrownBy(() -> SuggestionQuery.builder().setQuery("a"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Query must be at least two characters long");
  }

  @Test
  public void should_support_query_with_two_characters_long() {
    SuggestionQuery query = SuggestionQuery.builder().setQuery("ab").build();

    assertThat(query.getQuery()).isEqualTo("ab");
  }

  @Test
  public void should_fail_with_IAE_if_limit_is_negative() {
    SuggestionQuery.Builder query = SuggestionQuery.builder().setQuery("ab");

    assertThatThrownBy(() -> query.setLimit(-1))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Limit has to be strictly positive");
  }

  @Test
  public void should_fail_with_IAE_if_limit_is_zero() {
    SuggestionQuery.Builder query = SuggestionQuery.builder().setQuery("ab");

    assertThatThrownBy(() ->  query.setLimit(0))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Limit has to be strictly positive");
  }

  @Test
  public void should_support_positive_limit() {
    SuggestionQuery query = SuggestionQuery.builder().setQuery("ab")
      .setLimit(1).build();

    assertThat(query.getLimit()).isOne();
  }
}
