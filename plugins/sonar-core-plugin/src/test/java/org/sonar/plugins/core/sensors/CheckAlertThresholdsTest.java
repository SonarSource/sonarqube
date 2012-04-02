/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.sonar.api.resources.Qualifiers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class CheckAlertThresholdsTest {
  private CheckAlertThresholds decorator;
  private DecoratorContext context;
  private RulesProfile profile;
  private Measure measureClasses;
  private Measure measureCoverage;
  private Resource<?> project;


  @Before
  public void setup() {
    context = mock(DecoratorContext.class);

    measureClasses = new Measure(CoreMetrics.CLASSES, 20d);
    measureCoverage = new Measure(CoreMetrics.COVERAGE, 35d);

    when(context.getMeasure(CoreMetrics.CLASSES)).thenReturn(measureClasses);
    when(context.getMeasure(CoreMetrics.COVERAGE)).thenReturn(measureCoverage);

    profile = mock(RulesProfile.class);
    decorator = new CheckAlertThresholds(profile);
    project = mock(Resource.class);
    when(project.getQualifier()).thenReturn(Qualifiers.PROJECT);
  }

  @Test
  public void shouldNotCreateAlertsWhenNoThresholds() {
    when(profile.getAlerts()).thenReturn(new ArrayList<Alert>());
    assertFalse(decorator.shouldExecuteOnProject(new Project("key")));
  }

  @Test
  public void shouldBeOkWhenNoAlerts() {
    when(profile.getAlerts()).thenReturn(Arrays.asList(
        new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "20"),
        new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_GREATER, null, "35.0")));

    decorator.decorate(project, context);

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.OK)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.OK)));
  }

  @Test
  public void checkRootProjectsOnly() {
    when(project.getQualifier()).thenReturn(Qualifiers.FILE);
    when(profile.getAlerts()).thenReturn(Arrays.asList(
        new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "20"),
        new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_GREATER, null, "35.0")));

    decorator.decorate(project, context);

    verify(context, never()).saveMeasure((Measure) anyObject());
  }

  @Test
  public void shouldGenerateWarnings() {
    when(profile.getAlerts()).thenReturn(Arrays.asList(
        new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_GREATER, null, "100"),
        new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_SMALLER, null, "95.0"))); // generates warning because coverage 35% < 95%

    decorator.decorate(project, context);

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.OK)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.WARN)));

  }

  @Test
  public void shouldGenerateWarningAndError() {
    when(profile.getAlerts()).thenReturn(Arrays.asList(
        new Alert(null, CoreMetrics.CLASSES, Alert.OPERATOR_SMALLER, null, "100"), // generates warning because classes 20 < 100
        new Alert(null, CoreMetrics.COVERAGE, Alert.OPERATOR_SMALLER, "50.0", "80.0"))); // generates error because coverage 35% < 50%

    decorator.decorate(project, context);

    verify(context).saveMeasure(argThat(hasLevel(measureClasses, Metric.Level.WARN)));
    verify(context).saveMeasure(argThat(hasLevel(measureCoverage, Metric.Level.ERROR)));
  }

  private ArgumentMatcher<Measure> hasLevel(final Measure measure, final Metric.Level alertStatus) {
    return new ArgumentMatcher<Measure>() {
      @Override
      public boolean matches(Object arg) {
        return arg == measure && ((Measure) arg).getAlertStatus().equals(alertStatus);
      }
    };
  }

}
