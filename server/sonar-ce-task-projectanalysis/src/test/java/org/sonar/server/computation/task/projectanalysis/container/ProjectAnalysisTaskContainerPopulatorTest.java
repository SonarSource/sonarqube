/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.task.projectanalysis.container;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.Test;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.PicoContainer;
import org.sonar.ce.queue.CeTask;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.plugin.ce.ReportAnalysisComponentProvider;
import org.sonar.server.computation.task.container.TaskContainer;
import org.sonar.server.computation.task.step.StepsExplorer;
import org.sonar.server.computation.task.projectanalysis.step.PersistComponentsStep;
import org.sonar.server.computation.task.step.ComputationStep;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Sets.difference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectAnalysisTaskContainerPopulatorTest {
  private static final String PROJECTANALYSIS_STEP_PACKAGE = "org.sonar.server.computation.task.projectanalysis.step";

  private CeTask task = mock(CeTask.class);
  private ProjectAnalysisTaskContainerPopulator underTest;

  @Test
  public void item_is_added_to_the_container() {
    underTest = new ProjectAnalysisTaskContainerPopulator(task, null);
    AddedObjectsRecorderTaskContainer container = new AddedObjectsRecorderTaskContainer();
    underTest.populateContainer(container);

    assertThat(container.added).contains(task);
  }

  @Test
  public void all_computation_steps_are_added_in_order_to_the_container() {
    underTest = new ProjectAnalysisTaskContainerPopulator(task, null);
    AddedObjectsRecorderTaskContainer container = new AddedObjectsRecorderTaskContainer();
    underTest.populateContainer(container);

    Set<String> computationStepClassNames = from(container.added)
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
      .toSet();

    assertThat(difference(StepsExplorer.retrieveStepPackageStepsCanonicalNames(PROJECTANALYSIS_STEP_PACKAGE), computationStepClassNames)).isEmpty();
  }

  @Test
  public void at_least_one_core_step_is_added_to_the_container() {
    underTest = new ProjectAnalysisTaskContainerPopulator(task, null);
    AddedObjectsRecorderTaskContainer container = new AddedObjectsRecorderTaskContainer();
    underTest.populateContainer(container);

    assertThat(container.added).contains(PersistComponentsStep.class);
  }

  @Test
  public void Components_of_ReportAnalysisComponentProvider_are_added_to_the_container() {
    Object object = new Object();
    Class<MyClass> clazz = MyClass.class;
    ReportAnalysisComponentProvider componentProvider = mock(ReportAnalysisComponentProvider.class);
    when(componentProvider.getComponents()).thenReturn(ImmutableList.of(object, clazz));

    underTest = new ProjectAnalysisTaskContainerPopulator(task, new ReportAnalysisComponentProvider[] {componentProvider});
    AddedObjectsRecorderTaskContainer container = new AddedObjectsRecorderTaskContainer();
    container.add(componentProvider);
    underTest.populateContainer(container);

    assertThat(container.added).contains(object, clazz);
  }

  private static final class MyClass {

  }

  private enum IsComputationStep implements Predicate<Class<?>> {
    INSTANCE;

    @Override
    public boolean apply(Class<?> input) {
      return ComputationStep.class.isAssignableFrom(input);
    }
  }

  private static class AddedObjectsRecorderTaskContainer implements TaskContainer {
    private static final DefaultPicoContainer SOME_EMPTY_PICO_CONTAINER = new DefaultPicoContainer();

    private List<Object> added = new ArrayList<>();

    @Override
    public void bootup() {
      // no effect
    }

    @Override
    public ComponentContainer getParent() {
      throw new UnsupportedOperationException("getParent is not implemented");
    }

    @Override
    public void close() {
      throw new UnsupportedOperationException("cleanup is not implemented");
    }

    @Override
    public PicoContainer getPicoContainer() {
      return SOME_EMPTY_PICO_CONTAINER;
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
      for (Object add : added) {
        if (add.getClass().getSimpleName().contains(type.getSimpleName())) {
          return (T) add;
        }
      }
      return null;
    }

    @Override
    public <T> List<T> getComponentsByType(final Class<T> type) {
      return from(added)
        .filter(new Predicate<Object>() {
          @Override
          public boolean apply(@Nonnull Object input) {
            return input.getClass().getSimpleName().contains(type.getSimpleName());
          }
        }).transform(new Function<Object, T>() {
          @Override
          @Nonnull
          public T apply(@Nonnull Object input) {
            return (T) input;
          }
        }).toList();
    }
  }

}
