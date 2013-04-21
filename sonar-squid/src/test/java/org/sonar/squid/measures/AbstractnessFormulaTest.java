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

package org.sonar.squid.measures;

import org.junit.Test;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.measures.AbstractnessFormula;
import org.sonar.squid.measures.Metric;

import static org.junit.Assert.assertEquals;

public class AbstractnessFormulaTest {

  AbstractnessFormula abstractness = new AbstractnessFormula();
  SourcePackage measurable = new SourcePackage("pac1");

  @Test
  public void testCalculate() {
    measurable.setMeasure(Metric.CLASSES, 10);
    measurable.setMeasure(Metric.INTERFACES, 1);
    measurable.setMeasure(Metric.ABSTRACT_CLASSES, 1);

    assertEquals(0.2, abstractness.calculate(measurable), 0);
  }

  @Test
  public void testCalculateOnEmptyProject() {
    measurable.setMeasure(Metric.CLASSES, 0);
    measurable.setMeasure(Metric.INTERFACES, 0);
    measurable.setMeasure(Metric.ABSTRACT_CLASSES, 0);

    assertEquals(0, abstractness.calculate(measurable), 0);
  }

}
