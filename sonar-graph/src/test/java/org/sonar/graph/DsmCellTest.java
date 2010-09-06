/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.graph;

import org.junit.Ignore;
import org.junit.Test;
import org.sonar.graph.DsmCell;
import org.sonar.graph.StringEdge;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class DsmCellTest {

  @Test
  @Ignore
  public void testEquals() {
    DsmCell cell1 = new DsmCell(new StringEdge("A", "B", 1), true);
    DsmCell cell1bis = new DsmCell(new StringEdge("A", "B", 1), false);
    DsmCell cell4 = new DsmCell(new StringEdge("B", "A", 4), true);

    assertThat(cell1, equalTo(cell1));
    assertThat(cell1, equalTo(cell1bis));
    assertThat(cell1, not(equalTo(cell4)));
  }

}
