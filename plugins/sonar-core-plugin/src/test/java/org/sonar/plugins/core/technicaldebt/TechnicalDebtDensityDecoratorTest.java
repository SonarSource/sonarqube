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
package org.sonar.plugins.core.technicaldebt;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class TechnicalDebtDensityDecoratorTest {

  private TechnicalDebtDensityDecorator decorator;

  private Resource resource;
  private DecoratorContext context;

  @Before
  public void before() {
    decorator = new TechnicalDebtDensityDecorator();
    resource = mock(Resource.class);
    context = mock(DecoratorContext.class);
  }

  @Test
  public void generates_metrics() throws Exception {
    assertThat(decorator.generatesMetrics()).hasSize(1);
  }

  @Test
  public void depends_upon_technical_debt_metric() throws Exception {
    assertThat(decorator.dependsUponTechnicalDebt()).isEqualTo(CoreMetrics.TECHNICAL_DEBT);
  }

  @Test
  public void execute_on_project() throws Exception {
    assertThat(decorator.shouldExecuteOnProject(null)).isTrue();
  }

  @Test
  public void do_nothing_if_measure_already_computed() throws Exception {
    when(context.getMeasure(CoreMetrics.TECHNICAL_DEBT_DENSITY)).thenReturn(new Measure().setValue(5d).setMetric(CoreMetrics.TECHNICAL_DEBT_DENSITY));

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void do_nothing_if_no_ncloc() throws Exception {
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(null);

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void do_nothing_if_ncloc_is_zero() throws Exception {
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure().setValue(0d).setMetric(CoreMetrics.NCLOC));

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void do_nothing_if_no_technical_debt() throws Exception {
    when(context.getMeasure(CoreMetrics.TECHNICAL_DEBT)).thenReturn(null);

    decorator.decorate(resource, context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void compute_technical_debt_density() throws Exception {
    when(context.getMeasure(CoreMetrics.NCLOC)).thenReturn(new Measure().setValue(48d).setMetric(CoreMetrics.NCLOC));
    when(context.getMeasure(CoreMetrics.TECHNICAL_DEBT)).thenReturn(new Measure().setValue(1.48d).setMetric(CoreMetrics.TECHNICAL_DEBT));

    decorator.decorate(resource, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.TECHNICAL_DEBT_DENSITY, 31.3d)));
  }

}
