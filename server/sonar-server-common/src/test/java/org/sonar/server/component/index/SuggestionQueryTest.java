/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class SuggestionQueryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void should_fail_with_IAE_if_query_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Query must be at least two characters long");

    SuggestionQuery.builder().setQuery("");
  }

  @Test
  public void should_fail_with_IAE_if_query_is_one_character_long() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Query must be at least two characters long");

    SuggestionQuery.builder().setQuery("a");
  }

  @Test
  public void should_support_query_with_two_characters_long() {
    SuggestionQuery query = SuggestionQuery.builder().setQuery("ab").build();

    assertThat(query.getQuery()).isEqualTo("ab");
  }

  @Test
  public void should_fail_with_IAE_if_limit_is_negative() {
    SuggestionQuery.Builder query = SuggestionQuery.builder().setQuery("ab");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Limit has to be strictly positive");

    query.setLimit(-1);
  }

  @Test
  public void should_fail_with_IAE_if_limit_is_zero() {
    SuggestionQuery.Builder query = SuggestionQuery.builder().setQuery("ab");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Limit has to be strictly positive");

    query.setLimit(0);
  }

  @Test
  public void should_support_positive_limit() {
    SuggestionQuery query = SuggestionQuery.builder().setQuery("ab")
      .setLimit(1).build();

    assertThat(query.getLimit()).isEqualTo(1);
  }
}
