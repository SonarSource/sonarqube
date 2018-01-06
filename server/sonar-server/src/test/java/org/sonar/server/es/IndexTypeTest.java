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
 package org.sonar.server.es;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexTypeTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void format_and_parse() {
    IndexType type1 = new IndexType("foo", "bar");
    assertThat(type1.format()).isEqualTo("foo/bar");

    IndexType type2 = IndexType.parse(type1.format());
    assertThat(type2.equals(type1)).isTrue();
  }

  @Test
  public void parse_throws_IAE_if_invalid_format() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unsupported IndexType value: foo");

    IndexType.parse("foo");
  }

  @Test
  public void equals_and_hashCode() {
    IndexType type1 = new IndexType("foo", "bar");
    IndexType type1b = new IndexType("foo", "bar");
    IndexType type2 = new IndexType("foo", "baz");

    assertThat(type1.equals(type1)).isTrue();
    assertThat(type1.equals(type1b)).isTrue();
    assertThat(type1.equals(type2)).isFalse();

    assertThat(type1.hashCode()).isEqualTo(type1.hashCode());
    assertThat(type1.hashCode()).isEqualTo(type1b.hashCode());
  }
}
