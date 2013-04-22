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
package org.sonar.plugins.core.sensors;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator();
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

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator();
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

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator();
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.VIOLATIONS_DENSITY, 0.0);
  }

  @Test
  public void densityIsHundredWhenNoDebt() {
    Resource resource = mock(Resource.class);
    when(resource.getScope()).thenReturn(Resource.SCOPE_SET);

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 200.0));

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator();
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

    ViolationsDensityDecorator decorator = new ViolationsDensityDecorator();
    decorator.decorate(resource, context);

    verify(context).saveMeasure(CoreMetrics.VIOLATIONS_DENSITY, 100.0);
  }
}
