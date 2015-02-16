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

import static org.assertj.core.api.Assertions.assertThat;

public class DuplicationTest {

  @Test
  public void testBlockEqualsAndCo() {
    Duplication.Block b1 = new Duplication.Block("foo", 1, 10);
    Duplication.Block b2 = new Duplication.Block("foo", 1, 10);
    assertThat(b1).isEqualTo(b1);
    assertThat(b1).isEqualTo(b2);
    assertThat(b1).isNotEqualTo("");
    assertThat(b1).isNotEqualTo(new Duplication.Block("foo1", 1, 10));
    assertThat(b1).isNotEqualTo(new Duplication.Block("foo", 2, 10));
    assertThat(b1).isNotEqualTo(new Duplication.Block("foo", 1, 11));

    assertThat(b1.hashCode()).isEqualTo(188843970);
    assertThat(b1.toString()).isEqualTo("Duplication.Block[resourceKey=foo,startLine=1,length=10]");
  }

}
