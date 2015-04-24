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

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.*;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.bootstrap.BatchExtensionDictionnary;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DecoratorsSelectorTest {

  private Metric withFormula1 = new Metric("metric1").setFormula(new FakeFormula());
  private Metric withFormula2 = new Metric("metric2").setFormula(new FakeFormula());
  private Metric withoutFormula3 = new Metric("metric3");

  @Test
  public void selectAndSortFormulas() {
    Project project = new Project("key");
    BatchExtensionDictionnary batchExtDictionnary = newBatchDictionnary(withFormula1, withoutFormula3, withFormula2);

    Collection<Decorator> decorators = new DecoratorsSelector(batchExtDictionnary).select(project);
    assertThat(decorators).hasSize(2);
    assertThat(decorators).contains(new FormulaDecorator(withFormula1));
    assertThat(decorators).contains(new FormulaDecorator(withFormula2));
  }

  @Test
  public void decoratorsShouldBeExecutedBeforeFormulas() {
    Project project = new Project("key");
    Decorator metric1Decorator = new Metric1Decorator();
    BatchExtensionDictionnary batchExtDictionnary = newBatchDictionnary(withFormula1, metric1Decorator);

    Collection<Decorator> decorators = new DecoratorsSelector(batchExtDictionnary).select(project);

    Decorator firstDecorator = Iterables.get(decorators, 0);
    Decorator secondDecorator = Iterables.get(decorators, 1);

    assertThat(firstDecorator).isInstanceOf(Metric1Decorator.class);
    assertThat(secondDecorator).isInstanceOf(FormulaDecorator.class);

    FormulaDecorator formulaDecorator = (FormulaDecorator) secondDecorator;
    assertThat(formulaDecorator.dependsUponDecorators()).hasSize(1);
    assertThat(Iterables.get(formulaDecorator.dependsUponDecorators(), 0)).isEqualTo(firstDecorator);
  }

  private BatchExtensionDictionnary newBatchDictionnary(Object... extensions) {
    ComponentContainer ioc = new ComponentContainer();
    for (Object extension : extensions) {
      ioc.addSingleton(extension);
    }
    return new BatchExtensionDictionnary(ioc, null, null, null, null);
  }

  class FakeFormula implements Formula {
    public List<Metric> dependsUponMetrics() {
      return Arrays.asList();
    }

    public Measure calculate(FormulaData data, FormulaContext context) {
      return null;
    }
  }

  public class Metric1Decorator implements Decorator {
    @DependedUpon
    public Metric generatesMetric1Measure() {
      return withFormula1;
    }

    public void decorate(Resource resource, DecoratorContext context) {

    }

    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  public class FakeDecorator implements Decorator {
    public void decorate(Resource resource, DecoratorContext context) {

    }

    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }
}
