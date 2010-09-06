/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.batch;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.mock;
import org.picocontainer.containers.TransientPicoContainer;
import org.sonar.api.batch.*;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DecoratorsSelectorTest {

  private Metric withFormula1 = new Metric("metric1").setFormula(new FakeFormula());
  private Metric withFormula2 = new Metric("metric2").setFormula(new FakeFormula());
  private Metric withoutFormula = new Metric("metric3");

  @Test
  public void selectAndSortFormulas() {
    Project project = new Project("key");
    BatchExtensionDictionnary dictionnary = newDictionnary(withFormula1, withoutFormula, withFormula2);

    Collection<Decorator> decorators = new DecoratorsSelector(dictionnary).select(project);
    assertThat(decorators.size(), is(2));
    assertThat(decorators, hasItem((Decorator) new FormulaDecorator(withFormula1)));
    assertThat(decorators, hasItem((Decorator) new FormulaDecorator(withFormula2)));
  }

  @Test
  public void pluginDecoratorsCanOverrideFormulas() {
    Project project = new Project("key");
    Decorator fakeDecorator = new FakeDecorator();
    Decorator metric1Decorator = new Metric1Decorator();
    BatchExtensionDictionnary dictionnary = newDictionnary(fakeDecorator, metric1Decorator, withFormula1, withoutFormula, withFormula2);

    Collection<Decorator> decorators = new DecoratorsSelector(dictionnary).select(project);

    assertThat(decorators.size(), is(3));
    assertThat(decorators, hasItem(fakeDecorator));
    assertThat(decorators, hasItem(metric1Decorator));
    assertThat(decorators, hasItem((Decorator) new FormulaDecorator(withFormula2)));
  }

  private BatchExtensionDictionnary newDictionnary(Object... extensions) {
    TransientPicoContainer ioc = new TransientPicoContainer();
    int index = 0;
    for (Object extension : extensions) {
      ioc.addComponent("" + index, extension);
      index++;
    }
    return new BatchExtensionDictionnary(ioc);
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
