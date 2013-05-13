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
package org.sonar.server.util;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;

public class RubyUtilsTest {
  @Test
  public void should_parse_list_of_strings() {
    assertThat(RubyUtils.toStrings(null)).isNull();
    assertThat(RubyUtils.toStrings("")).isEmpty();
    assertThat(RubyUtils.toStrings("foo")).containsOnly("foo");
    assertThat(RubyUtils.toStrings("foo,bar")).containsOnly("foo", "bar");
    assertThat(RubyUtils.toStrings(asList("foo", "bar"))).containsOnly("foo", "bar");
  }

  @Test
  public void toBoolean() throws Exception {
    assertThat(RubyUtils.toBoolean(null)).isNull();
    assertThat(RubyUtils.toBoolean("true")).isTrue();
    assertThat(RubyUtils.toBoolean(true)).isTrue();
    assertThat(RubyUtils.toBoolean("false")).isFalse();
    assertThat(RubyUtils.toBoolean(false)).isFalse();

  }
}
