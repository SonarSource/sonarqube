/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.squid.text;

import org.junit.Test;
import org.sonar.squid.measures.Metric;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LineTest {

  @Test
  public void testIsThereCode() {
    Line line = new Line("//comment");
    line.setComment("//comment");
    assertFalse(line.isThereCode());
    line.setComment(null, false);
    assertTrue(line.isThereCode());
  }

  @Test
  public void testIsThereComment() {
    Line line = new Line("  //comment");
    line.setComment("//comment");
    assertTrue(line.isThereComment());
  }

  @Test
  public void testIsThereBlankComment() {
    Line line = new Line("//");
    line.setComment("//");
    assertTrue(line.isThereBlankComment());
  }

  @Test(expected = IllegalStateException.class)
  public void testUnexpectedMetric() {
    Line line = new Line("  //comment");
    line.getInt(Metric.CA);
  }

  @Test
  public void testIsBlank() {
    Line line = new Line("    ");
    assertTrue(line.isBlank());
    line.setComment("");
    assertFalse(line.isBlank());
  }

  @Test
  public void testIsThereCodeWithBlankLinesBeforeComment() {
    Line line = new Line("   //comment");
    line.setComment("//comment");
    assertFalse(line.isThereCode());
  }

  @Test
  public void testIsThereCodeWithBlankLinesAfterComment() {
    Line line = new Line("   //comment");
    line.setComment("//comment");
    assertFalse(line.isThereCode());
  }

  @Test
  public void testIsThereNoSonarTag() {
    Line line = new Line("   //NOSONAR");
    line.setComment("//NOSONAR");
    assertTrue(line.isThereNoSonarTag());
    line.setComment(null);
    assertFalse(line.isThereNoSonarTag());
  }

}
