/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import static org.hamcrest.Matchers.equalTo;

import java.util.Collection;

import org.sonar.api.measures.MeasuresFilter;

import org.sonar.api.measures.Metric.Level;

import org.sonar.api.measures.MeasuresFilters;

import org.sonar.api.resources.Qualifiers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;

import java.util.Arrays;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class GlobalAlertTest {
  private GlobalAlertDecorator decorator;
  private DecoratorContext context;
  private Measure measureClasses;
  private Measure measureCoverage;
  private Resource<?> project;


  @Before
  public void setup() {
    context = mock(DecoratorContext.class);

    measureClasses = mock(Measure.class);
    measureCoverage = mock(Measure.class);

    decorator = new GlobalAlertDecorator();
    project = mock(Resource.class);
    when(project.getQualifier()).thenReturn(Qualifiers.PROJECT);
  }

  @Test
  public void shouldBeOkWhenNoAlerts() {
    when(measureClasses.getAlertStatus()).thenReturn(Level.OK);
    when(measureCoverage.getAlertStatus()).thenReturn(Level.OK);
    when(context.getMeasures(any(MeasuresFilter.class))).thenReturn(Arrays.asList(
        measureClasses,
        measureCoverage));

    decorator.decorate(project, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.ALERT_STATUS, Metric.Level.OK.toString())));
  }

  @Test
  public void checkRootProjectsOnly() {
    when(project.getQualifier()).thenReturn(Qualifiers.FILE);
    when(context.getMeasures(any(MeasuresFilter.class))).thenReturn(Arrays.asList(
        measureClasses,
        measureCoverage));

    decorator.decorate(project, context);

    verify(context, never()).saveMeasure((Measure) anyObject());
  }

  @Test
  public void shouldGenerateWarnings() {
    when(measureClasses.getAlertStatus()).thenReturn(Level.OK);
    when(measureCoverage.getAlertStatus()).thenReturn(Level.WARN);
    when(context.getMeasures(any(MeasuresFilter.class))).thenReturn(Arrays.asList(
        measureClasses,
        measureCoverage));
    decorator.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.WARN, null)));
  }

  @Test
  public void globalStatusShouldBeErrorIfWarningsAndErrors() {
    when(measureClasses.getAlertStatus()).thenReturn(Level.ERROR);
    when(measureCoverage.getAlertStatus()).thenReturn(Level.WARN);
    when(context.getMeasures(any(MeasuresFilter.class))).thenReturn(Arrays.asList(
        measureClasses,
        measureCoverage));
    decorator.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, null)));
  }

  @Test
  public void globalLabelShouldAggregateAllLabels() {
    when(measureClasses.getAlertStatus()).thenReturn(Level.ERROR);
    when(measureClasses.getAlertText()).thenReturn("error classes");
    when(measureCoverage.getAlertStatus()).thenReturn(Level.WARN);
    when(measureCoverage.getAlertText()).thenReturn("warning coverage");
    when(context.getMeasures(any(MeasuresFilter.class))).thenReturn(Arrays.asList(
        measureClasses,
        measureCoverage));

    decorator.decorate(project, context);

    verify(context).saveMeasure(argThat(matchesMetric(CoreMetrics.ALERT_STATUS, Metric.Level.ERROR, "error classes, warning coverage")));
  }

  private ArgumentMatcher<Measure> matchesMetric(final Metric metric, final Metric.Level alertStatus, final String alertText) {
    return new ArgumentMatcher<Measure>() {
      @Override
      public boolean matches(Object arg) {
        boolean result = ((Measure) arg).getMetric().equals(metric) && ((Measure) arg).getAlertStatus() == alertStatus;
        if (result && alertText != null) {
          result = alertText.equals(((Measure) arg).getAlertText());
        }
        return result;
      }
    };
  }

}
