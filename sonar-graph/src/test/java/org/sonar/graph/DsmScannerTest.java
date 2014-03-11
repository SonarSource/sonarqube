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

import org.junit.Test;
import org.sonar.graph.Dsm;
import org.sonar.graph.DsmCell;
import org.sonar.graph.DsmScanner;

import static org.junit.Assert.*;

public class DsmScannerTest {

  @Test
  public void testScanString() {
    StringPrintWriter builder = new StringPrintWriter();
    builder.println("  | A | B |");
    builder.println("A |   | 1*|");
    builder.println("B | 3 |   |");
    Dsm dsm = DsmScanner.scan(builder.toString());

    assertEquals("A", dsm.getVertex(0).toString());
    assertEquals("B", dsm.getVertex(1).toString());

    assertEquals(2, dsm.getDimension());

    DsmCell ba = dsm.getCell(1, 0);
    assertEquals(1, ba.getWeight());
    assertTrue(ba.isFeedbackEdge());

    DsmCell ab = dsm.getCell(0, 1);
    assertEquals(3, ab.getWeight());
    assertFalse(ab.isFeedbackEdge());

    assertEquals(0, dsm.getCell(1, 1).getWeight());
  }

}
