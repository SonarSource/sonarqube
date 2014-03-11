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

package org.sonar.squid.recognizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegexDetectorTest {

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeProbability() {
    new RegexDetector("toto", -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProbabilityHigherThan1() {
    new RegexDetector("toto", 1.2);
  }

  @Test
  public void testProbability() {
    RegexDetector pattern = new RegexDetector("toto", 0.3);
    assertEquals(0.3, pattern.recognition(" toto "), 0.01);
    assertEquals(0, pattern.recognition("sql"), 0.01);
    assertEquals(1 - Math.pow(0.7, 3), pattern.recognition(" toto toto toto "), 0.01);
  }

  @Test
  public void testSeveralMatches() {
    RegexDetector pattern = new RegexDetector("(\\S\\.\\S)", 0.3); // \S is non-whitespace character 
    assertEquals(0.0, pattern.recognition(" toto "), 0.001);
    assertEquals(0.3, pattern.recognition("abc.def ghi jkl"), 0.001);
    assertEquals(0.51, pattern.recognition("abc.def.ghi"), 0.001);
    assertEquals(0.51, pattern.recognition("abc.def ghi.jkl"), 0.001);
  }
}
