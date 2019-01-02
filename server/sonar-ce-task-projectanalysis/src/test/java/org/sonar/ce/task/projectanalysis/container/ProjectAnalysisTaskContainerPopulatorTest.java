/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.container;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.PicoContainer;
import org.reflections.Reflections;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.projectanalysis.step.PersistComponentsStep;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.util.stream.MoreCollectors;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Sets.difference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectAnalysisTaskContainerPopulatorTest {
  private static final String PROJECTANALYSIS_STEP_PACKAGE = "org.sonar.ce.task.projectanalysis.step";

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

    Set<String> computationStepClassNames = container.added.stream()
      .map(s -> {
        if (s instanceof Class) {
          return (Class<?>) s;
        }
        return null;
      })
      .filter(Objects::nonNull)
      .filter(ComputationStep.class::isAssignableFrom)
      .map(Class::getCanonicalName)
      .collect(MoreCollectors.toSet());

    assertThat(difference(retrieveStepPackageStepsCanonicalNames(PROJECTANALYSIS_STEP_PACKAGE), computationStepClassNames)).isEmpty();
  }

  /**
   * Compute set of canonical names of classes implementing ComputationStep in the specified package using reflection.
   */
  private static Set<Object> retrieveStepPackageStepsCanonicalNames(String packageName) {
    Reflections reflections = new Reflections(packageName);

    return reflections.getSubTypesOf(ComputationStep.class).stream()
      .filter(input -> !Modifier.isAbstract(input.getModifiers()))
      .map(Class::getCanonicalName)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
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
