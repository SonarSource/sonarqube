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
package org.sonar.plugins.core.issue;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.test.IsMeasure;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;


public class WeightedIssuesDecoratorTest {

  @Test
  public void test_weighted_issues() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY, "BLOCKER=10;CRITICAL=5;MAJOR=2;MINOR=1;INFO=0");
    WeightedIssuesDecorator decorator = new WeightedIssuesDecorator(settings);
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.INFO_ISSUES)).thenReturn(new Measure(CoreMetrics.INFO_ISSUES, 50.0));
    when(context.getMeasure(CoreMetrics.CRITICAL_ISSUES)).thenReturn(new Measure(CoreMetrics.CRITICAL_ISSUES, 80.0));
    when(context.getMeasure(CoreMetrics.BLOCKER_ISSUES)).thenReturn(new Measure(CoreMetrics.BLOCKER_ISSUES, 100.0));

    decorator.start();
    decorator.decorate(context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.WEIGHTED_ISSUES, (double) (100 * 10 + 80 * 5 + 50 * 0))));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.WEIGHTED_ISSUES, "INFO=50;CRITICAL=80;BLOCKER=100")));
  }

  // SONAR-3092
  @Test
  public void do_save_zero() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY, "BLOCKER=10;CRITICAL=5;MAJOR=2;MINOR=1;INFO=0");
    DecoratorContext context = mock(DecoratorContext.class);

    WeightedIssuesDecorator decorator = new WeightedIssuesDecorator(settings);
    decorator.start();
    decorator.decorate(context);

    verify(context).saveMeasure(any(Measure.class));
  }

  @Test
  public void should_load_severity_weights_at_startup() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY, "BLOCKER=2;CRITICAL=1;MAJOR=0;MINOR=0;INFO=0");

    WeightedIssuesDecorator decorator = new WeightedIssuesDecorator(settings);
    decorator.start();

    assertThat(decorator.getWeightsBySeverity().get(RulePriority.BLOCKER)).isEqualTo(2);
    assertThat(decorator.getWeightsBySeverity().get(RulePriority.CRITICAL)).isEqualTo(1);
    assertThat(decorator.getWeightsBySeverity().get(RulePriority.MAJOR)).isEqualTo(0);
  }

  @Test
  public void weights_setting_should_be_optional() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY, "BLOCKER=2");

    WeightedIssuesDecorator decorator = new WeightedIssuesDecorator(settings);
    decorator.start();

    assertThat(decorator.getWeightsBySeverity().get(RulePriority.MAJOR)).isEqualTo(1);
  }
}