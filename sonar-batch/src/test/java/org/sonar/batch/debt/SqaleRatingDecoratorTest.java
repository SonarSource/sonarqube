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

package org.sonar.batch.debt;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.test.IsMeasure;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SqaleRatingDecoratorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  static final Long ONE_DAY_IN_MINUTES = 8L * 60;

  Settings settings;
  Metric[] metrics = {CoreMetrics.NCLOC, CoreMetrics.COMPLEXITY};

  @Mock
  DecoratorContext context;

  DefaultFileSystem fs;

  File file = File.create("src/main/java/Foo.java");

  SqaleRatingDecorator decorator;

  @Before
  public void setUp() throws Exception {
    settings = new Settings();

    fs = new DefaultFileSystem(temp.newFolder().toPath());
    fs.add(new DefaultInputFile("foo", file.getPath())
      .setLanguage("java"));

    decorator = new SqaleRatingDecorator(new SqaleRatingSettings(settings), metrics, fs);
  }

  @Test
  public void generates_metrics() throws Exception {
    SqaleRatingDecorator decorator = new SqaleRatingDecorator();
    assertThat(decorator.generatesMetrics()).hasSize(3);
  }

  @Test
  public void depends_on_metrics() {
    SqaleRatingDecorator decorator = new SqaleRatingDecorator();
    assertThat(decorator.dependsOnMetrics()).containsOnly(CoreMetrics.TECHNICAL_DEBT, CoreMetrics.NCLOC, CoreMetrics.COMPLEXITY);
  }

  @Test
  public void execute_on_project() throws Exception {
    SqaleRatingDecorator decorator = new SqaleRatingDecorator();
    assertThat(decorator.shouldExecuteOnProject(null)).isTrue();
  }

  @Test
  public void not_execute_on_unit_test() throws Exception {
    File resource = mock(File.class);
    when(resource.getQualifier()).thenReturn(Qualifiers.UNIT_TEST_FILE);
    DecoratorContext context = mock(DecoratorContext.class);

    SqaleRatingDecorator decorator = new SqaleRatingDecorator();
    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void save_total_rating_c() {
    settings.setProperty(CoreProperties.DEVELOPMENT_COST, 2 * ONE_DAY_IN_MINUTES);
    settings.setProperty(CoreProperties.SIZE_METRIC, "ncloc");
    settings.setProperty(CoreProperties.RATING_GRID, "1, 10,20,50");

    when(context.getResource()).thenReturn(file);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 10.0));
    when(context.getMeasure(CoreMetrics.TECHNICAL_DEBT)).thenReturn(new Measure(CoreMetrics.TECHNICAL_DEBT, 300.0 * ONE_DAY_IN_MINUTES));

    decorator.decorate(file, context);
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.SQALE_RATING, 3.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.DEVELOPMENT_COST, "9600")));
    verify(context).saveMeasure(CoreMetrics.SQALE_DEBT_RATIO, 1500d);

    verify(context).getMeasure(CoreMetrics.NCLOC);
  }

  @Test
  public void save_total_rating_a() {
    settings.setProperty(CoreProperties.DEVELOPMENT_COST, 2 * ONE_DAY_IN_MINUTES);
    settings.setProperty(CoreProperties.SIZE_METRIC, "ncloc");
    settings.setProperty(CoreProperties.RATING_GRID, "1, 10,20,50");

    when(context.getResource()).thenReturn(file);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 10.0));
    when(context.getMeasure(CoreMetrics.TECHNICAL_DEBT)).thenReturn(new Measure(CoreMetrics.TECHNICAL_DEBT, 0.0));

    decorator.decorate(file, context);
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.SQALE_RATING, 1.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.DEVELOPMENT_COST, "9600")));
    verify(context).saveMeasure(CoreMetrics.SQALE_DEBT_RATIO, 0d);

    verify(context).getMeasure(CoreMetrics.NCLOC);
  }

  @Test
  public void save_total_rating_e() {
    settings.setProperty(CoreProperties.DEVELOPMENT_COST, 2 * ONE_DAY_IN_MINUTES);
    settings.setProperty(CoreProperties.SIZE_METRIC, "ncloc");
    settings.setProperty(CoreProperties.RATING_GRID, "1, 10,20,50");

    when(context.getResource()).thenReturn(file);
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure(CoreMetrics.NCLOC, 10.0));
    when(context.getMeasure(CoreMetrics.TECHNICAL_DEBT)).thenReturn(new Measure(CoreMetrics.TECHNICAL_DEBT, 960000.0));

    decorator.decorate(file, context);
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.SQALE_RATING, 5.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.DEVELOPMENT_COST, "9600")));
    verify(context).saveMeasure(CoreMetrics.SQALE_DEBT_RATIO, 10000d);

    verify(context).getMeasure(CoreMetrics.NCLOC);
  }

  @Test
  public void save_total_rating_on_project() {
    settings.setProperty(CoreProperties.RATING_GRID, "1, 10,20,50");

    when(context.getResource()).thenReturn(new Project("Sample"));
    when(context.getMeasure(CoreMetrics.TECHNICAL_DEBT)).thenReturn(new Measure(CoreMetrics.TECHNICAL_DEBT, 300.0 * ONE_DAY_IN_MINUTES));
    when(context.getChildrenMeasures(CoreMetrics.DEVELOPMENT_COST)).thenReturn(newArrayList(new Measure(CoreMetrics.DEVELOPMENT_COST, Double.toString(20.0 * ONE_DAY_IN_MINUTES))));

    decorator.decorate(mock(File.class), context);
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.SQALE_RATING, 3.0)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.DEVELOPMENT_COST, "9600")));
    verify(context).saveMeasure(CoreMetrics.SQALE_DEBT_RATIO, 1500d);

    verify(context, never()).getMeasure(CoreMetrics.NCLOC);
  }

  @Test
  public void translate_rating_to_letter() {
    assertThat(SqaleRatingDecorator.toRatingLetter(null)).isNull();
    assertThat(SqaleRatingDecorator.toRatingLetter(1)).isEqualTo("A");
    assertThat(SqaleRatingDecorator.toRatingLetter(4)).isEqualTo("D");
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_rating_out_of_range() {
    SqaleRatingDecorator.toRatingLetter(89);
  }

}
