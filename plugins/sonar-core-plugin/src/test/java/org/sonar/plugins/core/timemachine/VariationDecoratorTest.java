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
package org.sonar.plugins.core.timemachine;

import org.junit.Test;
import org.mockito.Matchers;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.*;
import org.sonar.api.resources.*;
import org.sonar.api.rules.Rule;
import org.sonar.batch.components.PastMeasuresLoader;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.*;

public class VariationDecoratorTest extends AbstractDbUnitTestCase {

  public static final int NCLOC_ID = 12;
  public static final Metric NCLOC = new Metric("ncloc").setId(NCLOC_ID);

  public static final int COVERAGE_ID = 16;
  public static final Metric COVERAGE = new Metric("coverage").setId(COVERAGE_ID);

  public static final int VIOLATIONS_ID = 17;
  public static final Metric VIOLATIONS = new Metric("violations").setId(VIOLATIONS_ID);

  @Test
  public void shouldComputeVariations() {
    TimeMachineConfiguration conf = mock(TimeMachineConfiguration.class);
    when(conf.isFileVariationEnabled()).thenReturn(false);
    VariationDecorator decorator = new VariationDecorator(mock(PastMeasuresLoader.class), mock(MetricFinder.class), conf);

    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, new Project("foo")), is(true));
    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_DATE, new Project("foo")), is(true));
  }

  @Test
  public void shouldNotComputeFileVariations() {
    TimeMachineConfiguration conf = mock(TimeMachineConfiguration.class);
    when(conf.isFileVariationEnabled()).thenReturn(false);
    VariationDecorator decorator = new VariationDecorator(mock(PastMeasuresLoader.class), mock(MetricFinder.class), conf);

    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, new JavaFile("org.foo.Bar")), is(false));
    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_DATE, new JavaFile("org.foo.Bar")), is(false));
    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, new File("org/foo/Bar.php")), is(false));
    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_DATE, new File("org/foo/Bar.php")), is(false));
  }

  @Test
  public void shouldComputeFileVariationsIfExplictlyEnabled() {
    TimeMachineConfiguration conf = mock(TimeMachineConfiguration.class);
    when(conf.isFileVariationEnabled()).thenReturn(true);
    VariationDecorator decorator = new VariationDecorator(mock(PastMeasuresLoader.class), mock(MetricFinder.class), conf);

    // only for variation with reference analysis
    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, new JavaFile("org.foo.Bar")), is(true));
    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_DATE, new JavaFile("org.foo.Bar")), is(false));
    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, new File("org/foo/Bar.php")), is(true));
    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_DATE, new File("org/foo/Bar.php")), is(false));

    // no side-effect on other resources
    assertThat(decorator.shouldComputeVariation(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, new Project("foo")), is(true));
  }

  @Test
  public void shouldCompareAndSaveVariation() {
    Resource javaPackage = new JavaPackage("org.foo");

    PastMeasuresLoader pastMeasuresLoader = mock(PastMeasuresLoader.class);
    PastSnapshot pastSnapshot1 = new PastSnapshot("days", new Date()).setIndex(1);
    PastSnapshot pastSnapshot3 = new PastSnapshot("days", new Date()).setIndex(3);

    // first past analysis
    when(pastMeasuresLoader.getPastMeasures(javaPackage, pastSnapshot1)).thenReturn(Arrays.asList(
        new Object[] {NCLOC_ID, null, null, null, 180.0},
        new Object[] {COVERAGE_ID, null, null, null, 75.0}));

    // second past analysis
    when(pastMeasuresLoader.getPastMeasures(javaPackage, pastSnapshot3)).thenReturn(Arrays.<Object[]>asList(
        new Object[] {NCLOC_ID, null, null, null, 240.0}));

    // current analysis
    DecoratorContext context = mock(DecoratorContext.class);
    Measure currentNcloc = newMeasure(NCLOC, 200.0);
    Measure currentCoverage = newMeasure(COVERAGE, 80.0);
    when(context.getMeasures(Matchers.<MeasuresFilter>anyObject())).thenReturn(Arrays.asList(currentNcloc, currentCoverage));

    VariationDecorator decorator = new VariationDecorator(pastMeasuresLoader, mock(MetricFinder.class), Arrays.asList(pastSnapshot1, pastSnapshot3), false);
    decorator.decorate(javaPackage, context);

    // context updated for each variation : 2 times for ncloc and 1 time for coverage
    verify(context, times(3)).saveMeasure(Matchers.<Measure>anyObject());

    assertThat(currentNcloc.getVariation1(), is(20.0));
    assertThat(currentNcloc.getVariation2(), nullValue());
    assertThat(currentNcloc.getVariation3(), is(-40.0));

    assertThat(currentCoverage.getVariation1(), is(5.0));
    assertThat(currentCoverage.getVariation2(), nullValue());
    assertThat(currentCoverage.getVariation3(), nullValue());
  }

  @Test
  public void shouldComputeVariationOfRuleMeasures() {
    Rule rule1 = Rule.create();
    rule1.setId(1);
    Rule rule2 = Rule.create();
    rule2.setId(2);

    Resource javaPackage = new JavaPackage("org.foo");

    PastMeasuresLoader pastMeasuresLoader = mock(PastMeasuresLoader.class);
    PastSnapshot pastSnapshot1 = new PastSnapshot("days", new Date()).setIndex(1);

    // first past analysis
    when(pastMeasuresLoader.getPastMeasures(javaPackage, pastSnapshot1)).thenReturn(Arrays.asList(
        new Object[] {VIOLATIONS_ID, null, null, null, 180.0},// total
        new Object[] {VIOLATIONS_ID, null, null, rule1.getId(), 100.0},// rule 1
        new Object[] {VIOLATIONS_ID, null, null, rule2.getId(), 80.0})); // rule 2

    // current analysis
    DecoratorContext context = mock(DecoratorContext.class);
    Measure violations = newMeasure(VIOLATIONS, 200.0);
    Measure violationsRule1 = RuleMeasure.createForRule(VIOLATIONS, rule1, 130.0);
    Measure violationsRule2 = RuleMeasure.createForRule(VIOLATIONS, rule2, 70.0);
    when(context.getMeasures(Matchers.<MeasuresFilter>anyObject())).thenReturn(Arrays.asList(violations, violationsRule1, violationsRule2));

    VariationDecorator decorator = new VariationDecorator(pastMeasuresLoader, mock(MetricFinder.class), Arrays.asList(pastSnapshot1), false);
    decorator.decorate(javaPackage, context);

    // context updated for each variation
    verify(context, times(3)).saveMeasure(Matchers.<Measure>anyObject());

    assertThat(violations.getVariation1(), is(20.0));
  }

  private Measure newMeasure(Metric metric, double value) {
    return new Measure(metric, value);
  }
}
