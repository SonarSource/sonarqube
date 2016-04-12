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
package org.sonar.api;

import java.util.Arrays;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.SonarQubeVersion.V5_5;

public class PluginTest {

  @Test
  public void test_context() {
    Plugin.Context context = new Plugin.Context(V5_5);

    assertThat(context.getSonarQubeVersion()).isEqualTo(V5_5);
    assertThat(context.getExtensions()).isEmpty();

    context.addExtension("foo");
    assertThat(context.getExtensions()).containsOnly("foo");

    context.addExtensions(Arrays.asList("bar", "baz"));
    assertThat(context.getExtensions()).containsOnly("foo", "bar", "baz");

    context.addExtensions("one", "two", "three", "four");
    assertThat(context.getExtensions()).containsOnly("foo", "bar", "baz", "one", "two", "three", "four");
  }
}
