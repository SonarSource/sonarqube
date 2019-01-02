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
package org.sonar.server.platform.monitoring.cluster;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeInfoTest {

  @Test
  public void test_equals_and_hashCode() {
    NodeInfo foo = new NodeInfo("foo");
    NodeInfo bar = new NodeInfo("bar");
    NodeInfo bar2 = new NodeInfo("bar");

    assertThat(foo.equals(foo)).isTrue();
    assertThat(foo.equals(bar)).isFalse();
    assertThat(bar.equals(bar2)).isTrue();

    assertThat(bar.hashCode()).isEqualTo(bar.hashCode());
    assertThat(bar.hashCode()).isEqualTo(bar2.hashCode());
  }

}
