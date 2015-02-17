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
package org.sonar.plugins.core.timemachine;

import org.sonar.batch.components.TimeMachineConfiguration;

import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.PastMeasuresLoader;
import org.junit.Test;
import org.mockito.Matchers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VariationDecoratorTest extends AbstractDbUnitTestCase {

  public static final int NCLOC_ID = 12;
  public static final Metric NCLOC = new Metric("ncloc").setId(NCLOC_ID);

  public static final int COVERAGE_ID = 16;
  public static final Metric COVERAGE = new Metric("coverage").setId(COVERAGE_ID);

  public static final int VIOLATIONS_ID = 17;
  public static final Metric VIOLATIONS = new Metric("violations").setId(VIOLATIONS_ID);

  @Test
  public void shouldComputeVariations() {
    TimeMachineConfiguration timeMachineConfiguration = mock(TimeMachineConfiguration.class);
    VariationDecorator decorator = new VariationDecorator(mock(PastMeasuresLoader.class), mock(MetricFinder.class), timeMachineConfiguration, mock(RuleFinder.class));

    assertThat(decorator.shouldComputeVariation(new Project("foo"))).isTrue();
    assertThat(decorator.shouldComputeVariation(File.create("foo/bar.c"))).isFalse();
  }

  @Test
  public void shouldCompareAndSaveVariation() {
    Resource dir = Directory.create("org/foo");

    PastMeasuresLoader pastMeasuresLoader = mock(PastMeasuresLoader.class);
    PastSnapshot pastSnapshot1 = new PastSnapshot("days", new Date()).setIndex(1);
    PastSnapshot pastSnapshot3 = new PastSnapshot("days", new Date()).setIndex(3);

    // first past analysis
    when(pastMeasuresLoader.getPastMeasures(dir, pastSnapshot1)).thenReturn(Arrays.asList(
      new Object[] {NCLOC_ID, null, null, null, 180.0},
      new Object[] {COVERAGE_ID, null, null, null, 75.0}));

    // second past analysis
    when(pastMeasuresLoader.getPastMeasures(dir, pastSnapshot3)).thenReturn(Arrays.<Object[]>asList(
      new Object[] {NCLOC_ID, null, null, null, 240.0}));

    // current analysis
    DecoratorContext context = mock(DecoratorContext.class);
    Measure currentNcloc = newMeasure(NCLOC, 200.0);
    Measure currentCoverage = newMeasure(COVERAGE, 80.0);
    when(context.getMeasures(Matchers.<MeasuresFilter>anyObject())).thenReturn(Arrays.asList(currentNcloc, currentCoverage));

    VariationDecorator decorator = new VariationDecorator(pastMeasuresLoader, mock(MetricFinder.class), Arrays.asList(pastSnapshot1, pastSnapshot3), mock(RuleFinder.class));
    decorator.decorate(dir, context);

    // context updated for each variation : 2 times for ncloc and 1 time for coverage
    verify(context, times(3)).saveMeasure(Matchers.<Measure>anyObject());

    assertThat(currentNcloc.getVariation1()).isEqualTo(20.0);
    assertThat(currentNcloc.getVariation2()).isNull();
    assertThat(currentNcloc.getVariation3()).isEqualTo(-40.0);

    assertThat(currentCoverage.getVariation1()).isEqualTo(5.0);
    assertThat(currentCoverage.getVariation2()).isNull();
    assertThat(currentCoverage.getVariation3()).isNull();
  }

  @Test
  public void shouldComputeVariationOfRuleMeasures() {
    RuleFinder ruleFinder = mock(RuleFinder.class);

    Rule rule1 = Rule.create("repo", "rule1");
    rule1.setId(1);
    Rule rule2 = Rule.create("repo", "rule2");
    rule2.setId(2);

    when(ruleFinder.findByKey(rule1.ruleKey())).thenReturn(rule1);
    when(ruleFinder.findByKey(rule2.ruleKey())).thenReturn(rule2);

    Resource dir = Directory.create("org/foo");

    PastMeasuresLoader pastMeasuresLoader = mock(PastMeasuresLoader.class);
    PastSnapshot pastSnapshot1 = new PastSnapshot("days", new Date()).setIndex(1);

    // first past analysis
    when(pastMeasuresLoader.getPastMeasures(dir, pastSnapshot1)).thenReturn(Arrays.asList(
      new Object[] {VIOLATIONS_ID, null, null, null, 180.0},// total
      new Object[] {VIOLATIONS_ID, null, null, rule1.getId(), 100.0},// rule 1
      new Object[] {VIOLATIONS_ID, null, null, rule2.getId(), 80.0})); // rule 2

    // current analysis
    DecoratorContext context = mock(DecoratorContext.class);
    Measure violations = newMeasure(VIOLATIONS, 200.0);
    Measure violationsRule1 = RuleMeasure.createForRule(VIOLATIONS, rule1, 130.0);
    Measure violationsRule2 = RuleMeasure.createForRule(VIOLATIONS, rule2, 70.0);
    when(context.getMeasures(Matchers.<MeasuresFilter>anyObject())).thenReturn(Arrays.asList(violations, violationsRule1, violationsRule2));

    VariationDecorator decorator = new VariationDecorator(pastMeasuresLoader, mock(MetricFinder.class), Arrays.asList(pastSnapshot1), ruleFinder);
    decorator.decorate(dir, context);

    // context updated for each variation
    verify(context, times(3)).saveMeasure(Matchers.<Measure>anyObject());

    assertThat(violations.getVariation1()).isEqualTo(20.0);
  }

  private Measure newMeasure(Metric metric, double value) {
    return new Measure(metric, value);
  }
}
