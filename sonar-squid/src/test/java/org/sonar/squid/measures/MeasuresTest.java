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

package org.sonar.squid.measures;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MeasuresTest {

  private Measures measures = new Measures();

  @Test
  public void testGetValue() {
    assertEquals(0, measures.getValue(Metric.ACCESSORS), 0.1);
  }

  @Test
  public void testGetData() {
    assertNull(measures.getData(Metric.LCOM4_BLOCKS));
  }

  @Test
  public void testSetValue() {
    measures.setValue(Metric.ACCESSORS, 3);
    assertEquals(3, measures.getValue(Metric.ACCESSORS), 0.1);
  }

  @Test
  public void testSetData() {
    measures.setData(Metric.LCOM4_BLOCKS, "blocks detail");
    assertEquals("blocks detail", measures.getData(Metric.LCOM4_BLOCKS));
  }

}
