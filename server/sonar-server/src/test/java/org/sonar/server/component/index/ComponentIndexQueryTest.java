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
package org.sonar.server.component.index;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.component.index.ComponentIndexQuery.Sort;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIndexQueryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_query() {
    ComponentIndexQuery query = new ComponentIndexQuery()
      .setQuery("SonarQube")
      .setQualifiers(asList("TRK", "FIL"))
      .setSort(Sort.BY_ASCENDING_NAME)
      .setLimit(5);

    assertThat(query.getQuery().get()).isEqualTo("SonarQube");
    assertThat(query.getQualifiers()).containsOnly("TRK", "FIL");
    assertThat(query.getSort()).isEqualTo(Sort.BY_ASCENDING_NAME);
    assertThat(query.getLimit().get()).isEqualTo(5);
  }

  @Test
  public void create_query_accepts_null_text_query() {
    ComponentIndexQuery query = new ComponentIndexQuery()
      .setQuery(null);

    assertThat(query.getQuery()).isNotPresent();
  }

  @Test
  public void test_default_values() throws Exception {
    ComponentIndexQuery query = new ComponentIndexQuery();

    assertThat(query.getQuery()).isNotPresent();
    assertThat(query.getQualifiers()).isEmpty();
    assertThat(query.getSort()).isEqualTo(Sort.BY_SCORE);
    assertThat(query.getLimit()).isNotPresent();
  }

  @Test
  public void should_fail_with_IAE_if_query_is_one_character_long() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Query must be at least two characters long");

    new ComponentIndexQuery().setQuery("a");
  }

  @Test
  public void should_support_query_with_two_characters_long() {
    ComponentIndexQuery query = new ComponentIndexQuery().setQuery("ab");

    assertThat(query.getQuery().get()).isEqualTo("ab");
  }

  @Test
  public void should_fail_with_IAE_if_limit_is_negative() {
    ComponentIndexQuery query = new ComponentIndexQuery().setQuery("ab");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Limit has to be strictly positive");

    query.setLimit(-1);
  }

  @Test
  public void should_fail_with_IAE_if_limit_is_zero() {
    ComponentIndexQuery query = new ComponentIndexQuery().setQuery("ab");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Limit has to be strictly positive");

    query.setLimit(0);
  }

  @Test
  public void should_support_positive_limit() {
    ComponentIndexQuery query = new ComponentIndexQuery().setQuery("ab")
      .setLimit(1);

    assertThat(query.getLimit()).isEqualTo(Optional.of(1));
  }

  @Test
  public void should_fail_with_NPE_if_sort_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Sort cannot be null");

    new ComponentIndexQuery().setSort(null);
  }
}
