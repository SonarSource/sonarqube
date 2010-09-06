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
package org.sonar.plugins.core.sensors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ViolationsDensityDecoratorTest {

  @Test
  public void calculateDensity() {
    assertThat(ViolationsDensityDecorator.calculate(4000, 200), is(0.0));
    assertThat(ViolationsDensityDecorator.calculate(200, 200), is(0.0));
    assertThat(ViolationsDensityDecorator.calculate(50, 200), is(75.0));
    assertThat(ViolationsDensityDecorator.calculate(0, 200), is(100.0));
  }


  @Test
  public void decorateDensity() {
    Resource resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 200.0));
    when(context.getMeasure(CoreMetrics.WEIGHTED_VIOLATIONS)).thenReturn(new Measure(CoreMetrics.WEIGHTED_VIOLATIONS, 50.0));

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator(new HashMap());
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.VIOLATIONS_DENSITY, 75.0);
  }

  @Test
  public void noDensityIfNoNcloc() {
    Resource resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 0.0));
    when(context.getMeasure(CoreMetrics.WEIGHTED_VIOLATIONS)).thenReturn(new Measure(CoreMetrics.WEIGHTED_VIOLATIONS, 50.0));

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator(new HashMap());
    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.VIOLATIONS_DENSITY), anyDouble());
  }

  @Test
  public void saveDensityIfValueIsZero() {
    Resource resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 200.0));
    when(context.getMeasure(CoreMetrics.WEIGHTED_VIOLATIONS)).thenReturn(new Measure(CoreMetrics.WEIGHTED_VIOLATIONS, 5000.0));

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator(new HashMap());
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.VIOLATIONS_DENSITY, 0.0);
  }

  @Test
  public void densityIsHundredWhenNoDebt() {
    Resource resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 200.0));

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator(new HashMap());
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.VIOLATIONS_DENSITY, 100.0);
  }

  @Test
  public void densityIsHundredWhenDebtIsZero() {
    Resource resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 200.0));
    when(context.getMeasure(CoreMetrics.WEIGHTED_VIOLATIONS)).thenReturn(new Measure(CoreMetrics.WEIGHTED_VIOLATIONS, 0.0));

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator(new HashMap());
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.VIOLATIONS_DENSITY, 100.0);
  }

  @Test
  public void densityByCategory() {
    Map<Integer, Metric> metricByCategoryId = new HashMap<Integer, Metric>();
    metricByCategoryId.put(3, CoreMetrics.USABILITY);
    metricByCategoryId.put(5, CoreMetrics.EFFICIENCY);
    metricByCategoryId.put(6, CoreMetrics.RELIABILITY);

    Resource resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 200.0));
    when(context.getMeasures((MeasuresFilter) anyObject()))
        .thenReturn(Arrays.asList(
            new RuleMeasure(CoreMetrics.WEIGHTED_VIOLATIONS, new Rule(), null, 3).setValue(50.0),
            new RuleMeasure(CoreMetrics.WEIGHTED_VIOLATIONS, new Rule(), null, 5).setValue(0.0)));

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator(metricByCategoryId);
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.USABILITY, 75.0);
    verify(context).saveMeasure(CoreMetrics.EFFICIENCY, 100.0);
    verify(context).saveMeasure(CoreMetrics.RELIABILITY, 100.0);
  }

  @Test
  public void doNotCalculateDensityByCategoryOnEntities() {
    Map<Integer, Metric> metricByCategoryId = new HashMap<Integer, Metric>();
    metricByCategoryId.put(3, CoreMetrics.USABILITY);
    metricByCategoryId.put(5, CoreMetrics.EFFICIENCY);
    metricByCategoryId.put(6, CoreMetrics.RELIABILITY);

    Resource resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Resource.SCOPE_ENTITY);

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 200.0));
    when(context.getMeasure(CoreMetrics.WEIGHTED_VIOLATIONS)).thenReturn(new Measure(CoreMetrics.WEIGHTED_VIOLATIONS, 50.0));
    when(context.getMeasures((MeasuresFilter) anyObject()))
        .thenReturn(Arrays.asList(
            new RuleMeasure(CoreMetrics.WEIGHTED_VIOLATIONS, new Rule(), null, 3).setValue(50.0),
            new RuleMeasure(CoreMetrics.WEIGHTED_VIOLATIONS, new Rule(), null, 5).setValue(0.0)));

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator(metricByCategoryId);
    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.USABILITY), anyDouble());
    verify(context, never()).saveMeasure(eq(CoreMetrics.EFFICIENCY), anyDouble());
    verify(context, never()).saveMeasure(eq(CoreMetrics.RELIABILITY), anyDouble());
    verify(context, never()).saveMeasure(eq(CoreMetrics.MAINTAINABILITY), anyDouble());
    verify(context).saveMeasure(CoreMetrics.VIOLATIONS_DENSITY, 75.0);
  }
}
