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
package org.sonar.batch.phases;

import org.sonar.core.measure.MeasurementFilters;

import org.junit.Test;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.DefaultDecoratorContext;
import org.sonar.batch.events.EventBus;
import static org.hamcrest.number.OrderingComparisons.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparisons.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class DecoratorsExecutorTest {

  @Test
  public void shouldProfileExecutionTime() {
    Decorator1 decorator1 = new Decorator1();
    Decorator2 decorator2 = new Decorator2();
    PhasesTimeProfiler.DecoratorsProfiler profiler = new PhasesTimeProfiler.DecoratorsProfiler();

    profiler.start(decorator1);
    profiler.stop();
    profiler.start(decorator2);
    profiler.stop();

    assertThat(profiler.getMessage().indexOf("Decorator1"), greaterThanOrEqualTo(0));
    assertThat(profiler.getMessage().indexOf("Decorator2"), greaterThanOrEqualTo(0));

    // sequence of execution
    assertThat(profiler.getMessage().indexOf("Decorator1"), lessThan(profiler.getMessage().indexOf("Decorator2")));
  }

  @Test
  public void exceptionShouldIncludeResource() {
    Decorator decorator = mock(Decorator.class);
    doThrow(new SonarException()).when(decorator).decorate(any(Resource.class), any(DecoratorContext.class));

    DecoratorsExecutor executor = new DecoratorsExecutor(mock(BatchExtensionDictionnary.class), new Project("key"), mock(SonarIndex.class),
        mock(EventBus.class), mock(MeasurementFilters.class));
    try {
      executor.executeDecorator(decorator, mock(DefaultDecoratorContext.class), new File("org/foo/Bar.java"));
      fail("Exception has not been thrown");

    } catch (SonarException e) {
      assertThat(e.getMessage(), containsString("org/foo/Bar.java"));
    }
  }

  static class Decorator1 implements Decorator {
    public void decorate(Resource resource, DecoratorContext context) {
    }

    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }

  static class Decorator2 implements Decorator {
    public void decorate(Resource resource, DecoratorContext context) {
    }

    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }
}
