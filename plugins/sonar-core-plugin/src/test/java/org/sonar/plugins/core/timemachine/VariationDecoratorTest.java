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
package org.sonar.plugins.core.timemachine;

import org.junit.Test;
import org.mockito.Matchers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.*;
import org.sonar.batch.components.PastMeasuresLoader;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.*;

public class VariationDecoratorTest extends AbstractDbUnitTestCase {

  public static final Metric NCLOC = new Metric("ncloc").setId(12);
  public static final Metric COVERAGE = new Metric("coverage").setId(16);

  @Test
  public void shouldNotCalculateVariationsOnFiles() {
    assertThat(VariationDecorator.shouldCalculateVariations(new Project("foo")), is(true));
    assertThat(VariationDecorator.shouldCalculateVariations(new JavaPackage("org.foo")), is(true));
    assertThat(VariationDecorator.shouldCalculateVariations(new Directory("org/foo")), is(true));

    assertThat(VariationDecorator.shouldCalculateVariations(new JavaFile("org.foo.Bar")), is(false));
    assertThat(VariationDecorator.shouldCalculateVariations(new JavaFile("org.foo.Bar", true)), is(false));
    assertThat(VariationDecorator.shouldCalculateVariations(new File("org/foo/Bar.php")), is(false));
  }

  @Test
  public void shouldCompareAndSaveVariation() {
    Resource javaPackage = new JavaPackage("org.foo");

    PastMeasuresLoader pastMeasuresLoader = mock(PastMeasuresLoader.class);
    PastSnapshot pastSnapshot1 = new PastSnapshot("days", new Date()).setIndex(1);
    PastSnapshot pastSnapshot3 = new PastSnapshot("days", new Date()).setIndex(3);

    // first past analysis
    when(pastMeasuresLoader.getPastMeasures(javaPackage, pastSnapshot1)).thenReturn(Arrays.asList(
        newMeasureModel(NCLOC, 180.0),
        newMeasureModel(COVERAGE, 75.0)));

    // second past analysis
    when(pastMeasuresLoader.getPastMeasures(javaPackage, pastSnapshot3)).thenReturn(Arrays.asList(
        newMeasureModel(NCLOC, 240.0)));

    // current analysis
    DecoratorContext context = mock(DecoratorContext.class);
    Measure currentNcloc = newMeasure(NCLOC, 200.0);
    Measure currentCoverage = newMeasure(COVERAGE, 80.0);
    when(context.getMeasures(Matchers.<MeasuresFilter>anyObject())).thenReturn(Arrays.asList(currentNcloc, currentCoverage));

    VariationDecorator decorator = new VariationDecorator(pastMeasuresLoader, mock(MetricFinder.class), Arrays.asList(pastSnapshot1, pastSnapshot3));
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

  private Measure newMeasure(Metric metric, double value) {
    return new Measure(metric, value);
  }

  private MeasureModel newMeasureModel(Metric metric, double value) {
    return new MeasureModel(metric.getId(), value);
  }
}
