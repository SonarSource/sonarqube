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
package org.sonar.graph;

import org.junit.Before;
import org.junit.Test;
import org.sonar.graph.Dsm;
import org.sonar.graph.DsmScanner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DsmTest {

  private Dsm<String> dsm;

  @Before
  public void init() {
    StringPrintWriter textDsm = new StringPrintWriter();
    textDsm.println("  | A | B | C | D | E |");
    textDsm.println("A |   | 1 |   |   |   |");
    textDsm.println("B | 2 |   |   |   |   |");
    textDsm.println("C | 4 |   |   |   |   |");
    textDsm.println("D |   | 7 |   |   | 5 |");
    textDsm.println("E |   |   |   |   |   |");
    dsm = DsmScanner.scan(textDsm.toString());
  }

  @Test
  public void testGetVertex() {
    assertEquals("A", dsm.getVertex(0));
    assertEquals("B", dsm.getVertex(1));
    assertEquals("C", dsm.getVertex(2));
    assertEquals("D", dsm.getVertex(3));
    assertEquals("E", dsm.getVertex(4));
  }

  @Test
  public void testPermute() {
    assertEquals(2, dsm.getCell(0, 1).getWeight());
    assertEquals(4, dsm.getCell(0, 2).getWeight());

    dsm.permute(0, 1);
    assertEquals(1, dsm.getCell(0, 1).getWeight());
    assertEquals(0, dsm.getCell(0, 2).getWeight());
  }

  @Test
  public void testGetNumberOfOutgoingEdges() {
    assertEquals(0, dsm.getNumberOfOutgoingEdges(3, 0, 4));
    assertEquals(2, dsm.getNumberOfOutgoingEdges(0, 0, 4));
  }

  @Test
  public void testGetNumberOfIncomingEdges() {
    assertThat(dsm.getNumberOfIncomingEdges(0, 0, 4), equalTo(1));
    assertThat(dsm.getNumberOfIncomingEdges(4, 0, 4), equalTo(0));
  }
}
