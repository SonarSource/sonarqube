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
package org.sonar.batch.deprecated.decorator;

import org.sonar.batch.deprecated.decorator.FormulaDecorator;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Formula;
import org.sonar.api.measures.FormulaContext;
import org.sonar.api.measures.FormulaData;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.test.IsMeasure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FormulaDecoratorTest {

  @Test
  public void doAlwaysExecute() {
    assertThat(new FormulaDecorator(CoreMetrics.LINES).shouldExecuteOnProject(null), is(true));
  }

  @Test
  public void declareDependencies() {
    Formula formula = new Formula() {
      public List<Metric> dependsUponMetrics() {
        return Arrays.<Metric>asList(CoreMetrics.COMPLEXITY, CoreMetrics.COVERAGE);
      }

      public Measure calculate(FormulaData data, FormulaContext context) {
        return null;
      }
    };
    Metric metric = new Metric("ncloc").setFormula(formula);
    List<Metric> dependencies = new FormulaDecorator(metric).dependsUponMetrics();
    assertThat(dependencies).containsOnly(CoreMetrics.COMPLEXITY, CoreMetrics.COVERAGE);
  }

  @Test
  public void saveMeasure() {
    FormulaDecorator decorator = new FormulaDecorator(new Metric("fake").setFormula(new FakeFormula()));

    DecoratorContext context = mock(DecoratorContext.class);
    decorator.decorate(null, context);

    verify(context).saveMeasure(argThat(new IsMeasure(new Metric("fake"), 50.0)));
  }

  @Test
  public void doNotExecuteIfExistingResult() {
    Metric fake = new Metric("fake");
    FormulaDecorator decorator = new FormulaDecorator(fake.setFormula(new FakeFormula()));

    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getMeasure(fake)).thenReturn(new Measure(fake, 10.0));
    decorator.decorate(null, context);

    verify(context, never()).saveMeasure(any(Measure.class));
  }

  class FakeFormula implements Formula {

    public List<Metric> dependsUponMetrics() {
      return Collections.emptyList();
    }

    public Measure calculate(FormulaData data, FormulaContext context) {
      return new Measure(new Metric("fake")).setValue(50.0);
    }
  }
}
