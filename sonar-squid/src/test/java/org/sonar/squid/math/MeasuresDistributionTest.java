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

package org.sonar.squid.math;

import org.junit.Before;
import org.junit.Test;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MeasuresDistributionTest {

  private MeasuresDistribution distribution;

  @Before
  public void setup() {
    SourceFile file0 = newFile("File0.java", 0);
    SourceFile file1 = newFile("File1.java", 1);
    SourceFile file8 = newFile("File8.java", 8);
    SourceFile file10 = newFile("File10.java", 10);
    SourceFile file20 = newFile("File20.java", 20);
    SourceFile file21 = newFile("File21.java", 21);
    SourceFile file30 = newFile("File3.java", 30);
    distribution = new MeasuresDistribution(Arrays.<SourceCode>asList(file0, file1, file8, file10, file20, file21, file30));
  }

  private SourceFile newFile(String filename, int complexity) {
    SourceFile file0 = new SourceFile(filename);
    file0.setMeasure(Metric.COMPLEXITY, complexity);
    return file0;
  }

  @Test
  public void testComplexityDistribution() {
    Map<Integer, Integer> intervals = distribution.distributeAccordingTo(Metric.COMPLEXITY, 1, 10, 18, 25);
    assertEquals(4, intervals.size());
    assertEquals(2, (int) intervals.get(1)); // between 1 included and 10 excluded
    assertEquals(1, (int) intervals.get(10));// between 10 included and 18 excluded
    assertEquals(2, (int) intervals.get(18));
    assertEquals(1, (int) intervals.get(25)); // >= 25
  }

}
