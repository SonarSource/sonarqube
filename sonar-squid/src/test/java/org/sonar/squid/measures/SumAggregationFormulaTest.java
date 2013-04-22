/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.sonar.squid.api.SourceClass;


public class SumAggregationFormulaTest {
  
  SumAggregationFormula formula = new SumAggregationFormula();

  @Test
  public void testAggregate() {
    List<Measurable> measurables = new ArrayList<Measurable>();
    SourceClass class1 = new SourceClass("com.My");
    class1.setMeasure(Metric.COMPLEXITY, 2);
    measurables.add(class1);
    SourceClass class2 = new SourceClass("com.My");
    class2.setMeasure(Metric.COMPLEXITY, 3);
    measurables.add(class2);
    
    assertEquals(5, formula.aggregate(Metric.COMPLEXITY, measurables), 0.01);
  }
  
  @Test
  public void testAggregateEmptyCollections() {
    List<Measurable> measurables = new ArrayList<Measurable>();
    assertEquals(0, formula.aggregate(Metric.COMPLEXITY, measurables), 0.01);
  }

}
