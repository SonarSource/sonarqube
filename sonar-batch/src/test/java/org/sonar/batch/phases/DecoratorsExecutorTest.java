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
package org.sonar.batch.phases;

import org.junit.Test;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.number.OrderingComparisons.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparisons.lessThan;
import static org.junit.Assert.assertThat;

public class DecoratorsExecutorTest {

  @Test
  public void shouldProfileExecutionTime() {
    Decorator1 decorator1 = new Decorator1();
    Decorator2 decorator2 = new Decorator2();
    List<Decorator> decorators = Arrays.asList(decorator1, decorator2);
    DecoratorsExecutor.DecoratorsProfiler profiler = new DecoratorsExecutor.DecoratorsProfiler(decorators);

    profiler.start(decorator1);
    profiler.stop();
    profiler.start(decorator2);
    profiler.stop();

    assertThat(profiler.getMessage().indexOf("Decorator1"), greaterThanOrEqualTo(0));
    assertThat(profiler.getMessage().indexOf("Decorator2"), greaterThanOrEqualTo(0));

    // sequence of execution
    assertThat(profiler.getMessage().indexOf("Decorator1"), lessThan(profiler.getMessage().indexOf("Decorator2")));
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
