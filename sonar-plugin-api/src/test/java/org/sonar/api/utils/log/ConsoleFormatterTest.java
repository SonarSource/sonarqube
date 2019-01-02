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
package org.sonar.api.utils.log;

import org.junit.Test;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsoleFormatterTest {

  @Test
  public void format() {
    assertThat(ConsoleFormatter.format("foo")).isEqualTo("foo");
    assertThat(ConsoleFormatter.format("arg: {}", "foo")).isEqualTo("arg: foo");
    assertThat(ConsoleFormatter.format("two args: {} and {}", "foo", 42)).isEqualTo("two args: foo and 42");
    assertThat(ConsoleFormatter.format("args: {}, {} and {}", true, 42, 2L)).isEqualTo("args: true, 42 and 2");
    assertThat(ConsoleFormatter.format("args: {}, {} and {}", null, null, null)).isEqualTo("args: null, null and null");
  }

  @Test
  public void only_static_methods() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(ConsoleFormatter.class)).isTrue();
  }
}
