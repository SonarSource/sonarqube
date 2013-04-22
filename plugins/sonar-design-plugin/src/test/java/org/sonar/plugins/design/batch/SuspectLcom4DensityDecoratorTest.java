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
package org.sonar.plugins.design.batch;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class SuspectLcom4DensityDecoratorTest {

  @Test
  public void shouldNotDecorateFiles() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.FILES)).thenReturn(new Measure(CoreMetrics.FILES, 1.0));
    when(context.getMeasure(CoreMetrics.LCOM4)).thenReturn(newLcom4(3));

    SuspectLcom4DensityDecorator decorator = new SuspectLcom4DensityDecorator();
    decorator.decorate(new JavaFile("org.foo.Bar"), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.SUSPECT_LCOM4_DENSITY), anyDouble());
  }

  @Test
  public void shouldComputeDensityOnPackages() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.FILES)).thenReturn(new Measure(CoreMetrics.FILES, 4.0));
    when(context.getChildrenMeasures(CoreMetrics.LCOM4)).thenReturn(Arrays.asList(newLcom4(1), newLcom4(3), newLcom4(5), newLcom4(1)));

    SuspectLcom4DensityDecorator decorator = new SuspectLcom4DensityDecorator();
    decorator.decorate(new JavaPackage("org.foo"), context);

    verify(context).saveMeasure(CoreMetrics.SUSPECT_LCOM4_DENSITY, 50.0);
  }

  @Test
  public void shouldConsolidateDensityOnProjects() {
    List<DecoratorContext> children = Arrays.asList(
        newContext(3, 20.0),
        newContext(0, 0.0),
        newContext(5, 50.0));

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getChildren()).thenReturn(children);

    SuspectLcom4DensityDecorator decorator = new SuspectLcom4DensityDecorator();
    decorator.decorate(new Project("Foo"), context);

    verify(context).saveMeasure(CoreMetrics.SUSPECT_LCOM4_DENSITY, (20.0*3 + 50.0*5) / (3.0+5.0));
  }

  @Test
  public void doNotComputeDensityWhenLcom4IsMissing() {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.FILES)).thenReturn(new Measure(CoreMetrics.FILES, 4.0));
    when(context.getChildrenMeasures(CoreMetrics.LCOM4)).thenReturn(Collections.<Measure>emptyList());

    SuspectLcom4DensityDecorator decorator = new SuspectLcom4DensityDecorator();
    decorator.decorate(new JavaPackage("org.foo"), context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.SUSPECT_LCOM4_DENSITY), anyDouble());
  }

  private Measure newLcom4(int lcom4) {
    return new Measure(CoreMetrics.LCOM4, (double) lcom4);
  }

  private DecoratorContext newContext(int files, double density) {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.FILES)).thenReturn(new Measure(CoreMetrics.FILES, (double) files));
    when(context.getMeasure(CoreMetrics.SUSPECT_LCOM4_DENSITY)).thenReturn(new Measure(CoreMetrics.SUSPECT_LCOM4_DENSITY, density));
    return context;
  }
}
