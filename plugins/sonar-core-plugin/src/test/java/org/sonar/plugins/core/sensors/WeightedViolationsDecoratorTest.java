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
package org.sonar.plugins.core.sensors;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.test.IsMeasure;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WeightedViolationsDecoratorTest {

  @Test
  public void testWeightedViolations() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY, "BLOCKER=10;CRITICAL=5;MAJOR=2;MINOR=1;INFO=0");
    WeightedViolationsDecorator decorator = new WeightedViolationsDecorator(settings);
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.INFO_VIOLATIONS)).thenReturn(new Measure(CoreMetrics.INFO_VIOLATIONS, 50.0));
    when(context.getMeasure(CoreMetrics.CRITICAL_VIOLATIONS)).thenReturn(new Measure(CoreMetrics.CRITICAL_VIOLATIONS, 80.0));
    when(context.getMeasure(CoreMetrics.BLOCKER_VIOLATIONS)).thenReturn(new Measure(CoreMetrics.BLOCKER_VIOLATIONS, 100.0));

    decorator.start();
    decorator.decorate(context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.WEIGHTED_VIOLATIONS, (double) (100 * 10 + 80 * 5 + 50 * 0))));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.WEIGHTED_VIOLATIONS, "INFO=50;CRITICAL=80;BLOCKER=100")));
  }

  // SONAR-3092
  @Test
  public void doSaveZero() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY, "BLOCKER=10;CRITICAL=5;MAJOR=2;MINOR=1;INFO=0");
    DecoratorContext context = mock(DecoratorContext.class);

    WeightedViolationsDecorator decorator = new WeightedViolationsDecorator(settings);
    decorator.start();
    decorator.decorate(context);

    verify(context).saveMeasure(any(Measure.class));
  }

  @Test
  public void shouldLoadSeverityWeightsAtStartup() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY, "BLOCKER=2;CRITICAL=1;MAJOR=0;MINOR=0;INFO=0");
    
    WeightedViolationsDecorator decorator = new WeightedViolationsDecorator(settings);
    decorator.start();

    assertThat(decorator.getWeightsBySeverity().get(RulePriority.BLOCKER), Is.is(2));
    assertThat(decorator.getWeightsBySeverity().get(RulePriority.CRITICAL), Is.is(1));
    assertThat(decorator.getWeightsBySeverity().get(RulePriority.MAJOR), Is.is(0));
  }

  @Test
  public void weightsSettingShouldBeOptional() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY, "BLOCKER=2");

    WeightedViolationsDecorator decorator = new WeightedViolationsDecorator(settings);
    decorator.start();

    assertThat(decorator.getWeightsBySeverity().get(RulePriority.MAJOR), Is.is(1));
  }
}