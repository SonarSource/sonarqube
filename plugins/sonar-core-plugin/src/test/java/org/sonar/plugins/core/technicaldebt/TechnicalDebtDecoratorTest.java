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

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.test.IsMeasure;
import org.sonar.core.technicaldebt.TechnicalDebtCalculator;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class TechnicalDebtDecoratorTest {

  @Test
  public void generates_metrics() throws Exception {
    TechnicalDebtDecorator decorator = new TechnicalDebtDecorator(null);
    assertThat(decorator.generatesMetrics()).hasSize(1);
  }

  @Test
  public void execute_on_project() throws Exception {
    TechnicalDebtDecorator decorator = new TechnicalDebtDecorator(null);
    assertThat(decorator.shouldExecuteOnProject(null)).isTrue();
  }

  @Test
  public void execute_on_source_file() throws Exception {
    TechnicalDebtCalculator costCalculator = mock(TechnicalDebtCalculator.class);
    File resource = mock(File.class);
    DecoratorContext context = mock(DecoratorContext.class);

    TechnicalDebtDecorator decorator = new TechnicalDebtDecorator(costCalculator);
    decorator.decorate(resource, context);

    verify(costCalculator, times(1)).compute(context);
  }

  @Test
  public void not_execute_on_unit_test() throws Exception {
    TechnicalDebtCalculator costCalculator = mock(TechnicalDebtCalculator.class);
    File resource = mock(File.class);
    when(resource.getQualifier()).thenReturn(Qualifiers.UNIT_TEST_FILE);
    DecoratorContext context = mock(DecoratorContext.class);

    TechnicalDebtDecorator decorator = new TechnicalDebtDecorator(costCalculator);
    decorator.decorate(resource, context);

    verify(costCalculator, never()).compute(context);
  }

  @Test
  public void save_cost_measures() {
    TechnicalDebtCalculator costCalulator = mock(TechnicalDebtCalculator.class);
    when(costCalulator.getTotal()).thenReturn(60.0);

    TechnicalDebtDecorator decorator = new TechnicalDebtDecorator(costCalulator);
    DecoratorContext context = mock(DecoratorContext.class);

    decorator.saveCostMeasures(context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.TECHNICAL_DEBT, 60.0)));
  }

  @Test
  public void always_save_cost_for_positive_values() throws Exception {
    TechnicalDebtDecorator decorator = new TechnicalDebtDecorator(null);

    // for a project
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));
    decorator.saveCost(context, null, 12.0, false);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));

    // or for a file
    context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new File("foo"));
    decorator.saveCost(context, null, 12.0, false);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));
  }

  @Test
  public void always_save_cost_for_project_if_top_characteristic() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));
    // this is a top characteristic
    Characteristic topCharacteristic = Characteristic.create();

    TechnicalDebtDecorator decorator = new TechnicalDebtDecorator(null);

    decorator.saveCost(context, topCharacteristic, 0.0, true);
    verify(context, times(1)).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT).setCharacteristic(topCharacteristic));
  }

  /**
   * SQALE-147
   */
  @Test
  public void never_save_cost_for_project_if_not_top_characteristic() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new Project("foo"));
    Characteristic topCharacteristic = Characteristic.create();
    Characteristic childCharacteristic = Characteristic.create();
    topCharacteristic.addChild(childCharacteristic);

    TechnicalDebtDecorator decorator = new TechnicalDebtDecorator(null);

    decorator.saveCost(context, childCharacteristic, 0.0, true);
    verify(context, never()).saveMeasure(any(Measure.class));
  }

  @Test
  public void not_save_cost_for_file_if_zero() throws Exception {
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getResource()).thenReturn(new File("foo"));

    TechnicalDebtDecorator decorator = new TechnicalDebtDecorator(null);

    decorator.saveCost(context, null, 0.0, true);
    verify(context, never()).saveMeasure(new Measure(CoreMetrics.TECHNICAL_DEBT));
  }

  @Test
  public void check_definitions() {
    assertThat(TechnicalDebtDecorator.definitions()).hasSize(1);
  }

}
