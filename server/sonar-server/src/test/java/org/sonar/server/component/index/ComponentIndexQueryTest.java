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
package org.sonar.server.component.index;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIndexQueryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void should_fail_with_IAE_if_query_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Query must be at least two characters long");

    new ComponentIndexQuery("");
  }

  @Test
  public void should_fail_with_IAE_if_query_is_one_character_long() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Query must be at least two characters long");

    new ComponentIndexQuery("a");
  }

  @Test
  public void should_support_query_with_two_characters_long() {
    ComponentIndexQuery query = new ComponentIndexQuery("ab");

    assertThat(query.getQuery()).isEqualTo("ab");
  }

  @Test
  public void should_fail_with_IAE_if_limit_is_negative() {
    ComponentIndexQuery query = new ComponentIndexQuery("ab");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Limit has to be strictly positive");

    query.setLimit(-1);
  }

  @Test
  public void should_fail_with_IAE_if_limit_is_zero() {
    ComponentIndexQuery query = new ComponentIndexQuery("ab");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Limit has to be strictly positive");

    query.setLimit(0);
  }

  @Test
  public void should_support_positive_limit() {
    ComponentIndexQuery query = new ComponentIndexQuery("ab")
      .setLimit(1);

    assertThat(query.getLimit()).isEqualTo(Optional.of(1));
  }
}
