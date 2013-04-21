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
package org.sonar.plugins.core.timemachine;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;
import org.sonar.batch.components.TimeMachineConfiguration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.*;

public class TendencyDecoratorTest {

  @Test
  public void initQuery() throws ParseException {
    Project project = mock(Project.class);
    when(project.getAnalysisDate()).thenReturn(date("2009-12-25"));

    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.findAll()).thenReturn(Arrays.asList(CoreMetrics.LINES, CoreMetrics.COVERAGE, CoreMetrics.COVERAGE_LINE_HITS_DATA, CoreMetrics.PROFILE));

    TendencyDecorator decorator = new TendencyDecorator(null, metricFinder, newConf());

    TimeMachineQuery query = decorator.initQuery(project);
    assertThat(query.getMetrics().size(), is(2));
    assertThat(query.getMetrics(), hasItems(CoreMetrics.LINES, CoreMetrics.COVERAGE));
    assertThat(query.getFrom(), is(date("2009-11-25")));
    assertThat(query.isToCurrentAnalysis(), is(true));
  }

  private TimeMachineConfiguration newConf() {
    TimeMachineConfiguration configuration = mock(TimeMachineConfiguration.class);
    when(configuration.getTendencyPeriodInDays()).thenReturn(30);
    return configuration;
  }

  @Test
  public void includeCurrentMeasures() throws ParseException {
    TendencyAnalyser analyser = mock(TendencyAnalyser.class);
    TimeMachineQuery query = new TimeMachineQuery(null).setMetrics(CoreMetrics.LINES, CoreMetrics.COVERAGE);
    TimeMachine timeMachine = mock(TimeMachine.class);

    when(timeMachine.getMeasuresFields(query)).thenReturn(Arrays.<Object[]>asList(
        new Object[]{date("2009-12-01"), CoreMetrics.LINES, 1200.0},
        new Object[]{date("2009-12-01"), CoreMetrics.COVERAGE, 80.5},
        new Object[]{date("2009-12-02"), CoreMetrics.LINES, 1300.0},
        new Object[]{date("2009-12-02"), CoreMetrics.COVERAGE, 79.6},
        new Object[]{date("2009-12-15"), CoreMetrics.LINES, 1150.0}
    ));

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(CoreMetrics.LINES)).thenReturn(new Measure(CoreMetrics.LINES, 1400.0));
    when(context.getMeasure(CoreMetrics.COVERAGE)).thenReturn(new Measure(CoreMetrics.LINES, 90.0));

    TendencyDecorator decorator = new TendencyDecorator(timeMachine, query, analyser, newConf());
    decorator.decorate(new JavaPackage("org.foo"), context);

    verify(analyser).analyseLevel(Arrays.asList(1200.0, 1300.0, 1150.0, 1400.0));
    verify(analyser).analyseLevel(Arrays.asList(80.5, 79.6, 90.0));
  }

  @Test
  public void noTendencyIfNoCurrentMeasures() throws ParseException {
    TendencyAnalyser analyser = mock(TendencyAnalyser.class);
    TimeMachineQuery query = new TimeMachineQuery(null).setMetrics(CoreMetrics.LINES, CoreMetrics.COVERAGE);
    TimeMachine timeMachine = mock(TimeMachine.class);

    when(timeMachine.getMeasuresFields(query)).thenReturn(Arrays.<Object[]>asList(
        new Object[]{date("2009-12-01"), CoreMetrics.LINES, 1200.0},
        new Object[]{date("2009-12-02"), CoreMetrics.LINES, 1300.0}
    ));

    DecoratorContext context = mock(DecoratorContext.class);
    TendencyDecorator decorator = new TendencyDecorator(timeMachine, query, analyser, newConf());
    decorator.decorate(new JavaPackage("org.foo"), context);

    verify(analyser, never()).analyseLevel(anyList());
  }

  private Date date(String date) throws ParseException {
    return new SimpleDateFormat("yyyy-MM-dd").parse(date);
  }
}
