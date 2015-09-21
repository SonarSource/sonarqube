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
package org.sonar.server.computation.container;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.computation.queue.CeTask;
import org.sonar.server.computation.step.ComputationStep;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ReportComputeEngineContainerPopulatorTest {
  private CeTask task = mock(CeTask.class);
  private ReportComputeEngineContainerPopulator underTest = new ReportComputeEngineContainerPopulator(task);

  @Test
  public void item_is_added_to_the_container() {
    AddedObjectsRecorderComputeEngineContainer container = new AddedObjectsRecorderComputeEngineContainer();
    underTest.populateContainer(container);

    assertThat(container.added).contains(task);
  }

  @Test
  public void all_computation_steps_are_added_in_order_to_the_container() {
    AddedObjectsRecorderComputeEngineContainer container = new AddedObjectsRecorderComputeEngineContainer();
    underTest.populateContainer(container);

    assertThat(from(container.added)
        .transform(new Function<Object, Class<?>>() {
          @Nullable
          @Override
          public Class<?> apply(Object input) {
            if (input instanceof Class) {
              return (Class<?>) input;
            }
            return null;
          }
        })
        .filter(notNull())
        .filter(IsComputationStep.INSTANCE)
        .transform(StepsExplorer.toCanonicalName())
        .toSet())
        .isEqualTo(StepsExplorer.retrieveStepPackageStepsCanonicalNames());
  }

  private enum IsComputationStep implements Predicate<Class<?>> {
    INSTANCE;

    @Override
    public boolean apply(Class<?> input) {
      return ComputationStep.class.isAssignableFrom(input);
    }
  }

  private static class AddedObjectsRecorderComputeEngineContainer implements ComputeEngineContainer {
    private List<Object> added = new ArrayList<>();

    @Override
    public ComponentContainer getParent() {
      throw new UnsupportedOperationException("getParent is not implemented");
    }

    @Override
    public void cleanup() {
      throw new UnsupportedOperationException("cleanup is not implemented");
    }

    @Override
    public ComponentContainer add(Object... objects) {
      added.addAll(Arrays.asList(objects));
      return null; // not used anyway
    }

    @Override
    public ComponentContainer addSingletons(Iterable<?> components) {
      for (Object component : components) {
        added.add(component);
      }
      return null; // not used anyway
    }

    @Override
    public <T> T getComponentByType(Class<T> type) {
      throw new UnsupportedOperationException("getComponentByType is not implemented");
    }
  }
}
