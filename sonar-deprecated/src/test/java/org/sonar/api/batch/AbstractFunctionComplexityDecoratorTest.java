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
package org.sonar.api.batch;

import org.junit.Test;
import static org.mockito.Mockito.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Resource;

public class AbstractFunctionComplexityDecoratorTest {

  @Test
  public void calculateFunctionComplexity() {

    Resource directory = new Directory("fake");
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 200.0));
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 500.0));

    new AbstractFunctionComplexityDecorator(Java.INSTANCE) {
    }.decorate(directory, context);

    verify(context).saveMeasure(CoreMetrics.FUNCTION_COMPLEXITY, 2.5);
  }

  @Test
  public void noAverageIfMissingData() {

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 20.0));
    Resource directory = new Directory("fake");

    new AbstractFunctionComplexityDecorator(Java.INSTANCE) {
    }.decorate(directory, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.FUNCTION_COMPLEXITY), anyDouble());
  }

  @Test
  public void noAverageIfZeroFiles() {
    AbstractFunctionComplexityDecorator decorator = new AbstractFunctionComplexityDecorator(Java.INSTANCE) {
    };

    Resource directory = new Directory("fake");
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 0.0));
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 500.0));

    decorator.decorate(directory, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.FUNCTION_COMPLEXITY), anyDouble());
  }

  @Test
  public void doNotCalculateOnFiles() {

    Resource file = new File("fake");
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.FUNCTIONS)).thenReturn(new Measure(CoreMetrics.FUNCTIONS, 1.0));
    when(context.getMeasure(CoreMetrics.COMPLEXITY)).thenReturn(new Measure(CoreMetrics.COMPLEXITY, 25.0));

    new AbstractFunctionComplexityDecorator(Java.INSTANCE) {
    }.decorate(file, context);

    verify(context).saveMeasure(eq(CoreMetrics.FUNCTION_COMPLEXITY), anyDouble());
  }
}
