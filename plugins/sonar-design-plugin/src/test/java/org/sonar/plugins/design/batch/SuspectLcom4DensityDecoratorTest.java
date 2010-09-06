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
package org.sonar.plugins.design.batch;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;

import java.util.Arrays;

import static org.mockito.Mockito.*;

public class SuspectLcom4DensityDecoratorTest {

  @Test
  public void doNotDecorateFiles() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.FILES)).thenReturn(new Measure(CoreMetrics.FILES, 1.0));
    when(context.getMeasure(CoreMetrics.LCOM4)).thenReturn(newLcom4(3));

    SuspectLcom4DensityDecorator decorator = new SuspectLcom4DensityDecorator();
    decorator.decorate(new JavaFile("org.foo.Bar"), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.SUSPECT_LCOM4_DENSITY), anyDouble());
  }

  @Test
  public void decoratePackages() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.FILES)).thenReturn(new Measure(CoreMetrics.FILES, 4.0));
    when(context.getChildrenMeasures(CoreMetrics.LCOM4)).thenReturn(Arrays.asList(newLcom4(1), newLcom4(3), newLcom4(5), newLcom4(1)));

    SuspectLcom4DensityDecorator decorator = new SuspectLcom4DensityDecorator();
    decorator.decorate(new JavaPackage("org.foo"), context);

    verify(context).saveMeasure(CoreMetrics.SUSPECT_LCOM4_DENSITY, 50.0);
  }

  private Measure newLcom4(int lcom4) {
    return new Measure(CoreMetrics.LCOM4, (double)lcom4);
  }
}
