/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.api.batch.sensor.duplication;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class DuplicationGroupTest {

  @Test
  public void testBlockEqualsAndCo() {
    DuplicationGroup.Block b1 = new DuplicationGroup.Block("foo", 1, 10);
    DuplicationGroup.Block b2 = new DuplicationGroup.Block("foo", 1, 10);
    assertThat(b1).isEqualTo(b1);
    assertThat(b1).isEqualTo(b2);
    assertThat(b1).isNotEqualTo("");
    assertThat(b1).isNotEqualTo(new DuplicationGroup.Block("foo1", 1, 10));
    assertThat(b1).isNotEqualTo(new DuplicationGroup.Block("foo", 2, 10));
    assertThat(b1).isNotEqualTo(new DuplicationGroup.Block("foo", 1, 11));

    assertThat(b1.hashCode()).isEqualTo(188843970);
    assertThat(b1.toString()).isEqualTo("DuplicationGroup.Block[resourceKey=foo,startLine=1,length=10]");
  }

  @Test
  public void testDuplicationGroupEqualsAndCo() {
    DuplicationGroup d1 = new DuplicationGroup(new DuplicationGroup.Block("foo", 1, 10));
    d1.setDuplicates(Arrays.asList(new DuplicationGroup.Block("foo", 20, 10), new DuplicationGroup.Block("foo2", 1, 10)));
    DuplicationGroup d2 = new DuplicationGroup(new DuplicationGroup.Block("foo", 1, 10));
    d2.setDuplicates(Arrays.asList(new DuplicationGroup.Block("foo", 20, 10), new DuplicationGroup.Block("foo2", 1, 10)));
    assertThat(d1).isEqualTo(d1);
    assertThat(d1).isEqualTo(d2);
    assertThat(d1).isNotEqualTo("");
    assertThat(d1).isNotEqualTo(new DuplicationGroup(new DuplicationGroup.Block("foo", 1, 10)));

    assertThat(d1.hashCode()).isEqualTo(578909124);
    assertThat(d1.toString()).contains("origin=DuplicationGroup.Block[resourceKey=foo,startLine=1,length=10]");
    assertThat(d1.toString()).contains(
      "duplicates=[DuplicationGroup.Block[resourceKey=foo,startLine=20,length=10], DuplicationGroup.Block[resourceKey=foo2,startLine=1,length=10]]");
  }

}
