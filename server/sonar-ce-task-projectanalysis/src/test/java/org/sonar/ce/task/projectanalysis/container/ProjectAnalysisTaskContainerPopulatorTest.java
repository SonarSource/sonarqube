/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.reflections.Reflections;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.projectanalysis.step.PersistComponentsStep;
import org.sonar.ce.task.projectanalysis.step.ReportComputationSteps;
import org.sonar.ce.task.projectanalysis.task.ListTaskContainer;
import org.sonar.ce.task.step.ComputationStep;

import static com.google.common.collect.Sets.difference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

public class ProjectAnalysisTaskContainerPopulatorTest {
  private static final String PROJECTANALYSIS_STEP_PACKAGE = "org.sonar.ce.task.projectanalysis.step";

  private final CeTask task = mock(CeTask.class);
  private final ProjectAnalysisTaskContainerPopulator underTest = new ProjectAnalysisTaskContainerPopulator(task, null);

  @Test
  public void item_is_added_to_the_container() {
    ListTaskContainer container = new ListTaskContainer();
    underTest.populateContainer(container);

    assertThat(container.getAddedComponents()).contains(task);
  }

  @Test
  public void all_computation_steps_are_added_in_order_to_the_container() {
    ListTaskContainer container = new ListTaskContainer();
    underTest.populateContainer(container);

    Set<String> computationStepClassNames = container.getAddedComponents().stream()
      .map(s -> {
        if (s instanceof Class) {
          return (Class<?>) s;
        }
        return null;
      })
      .filter(Objects::nonNull)
      .filter(ComputationStep.class::isAssignableFrom)
      .map(Class::getCanonicalName)
      .collect(Collectors.toSet());

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
    ListTaskContainer container = new ListTaskContainer();
    underTest.populateContainer(container);

    assertThat(container.getAddedComponents()).contains(PersistComponentsStep.class);
  }

  @Test
  public void Components_of_ReportAnalysisComponentProvider_are_added_to_the_container() {
    Object object = new Object();
    Class<MyClass> clazz = MyClass.class;
    ReportAnalysisComponentProvider componentProvider = mock(ReportAnalysisComponentProvider.class);
    when(componentProvider.getComponents()).thenReturn(ImmutableList.of(object, clazz));

    ProjectAnalysisTaskContainerPopulator populator = new ProjectAnalysisTaskContainerPopulator(task, new ReportAnalysisComponentProvider[] {componentProvider});
    ListTaskContainer container = new ListTaskContainer();
    container.add(componentProvider);
    populator.populateContainer(container);

    assertThat(container.getAddedComponents()).contains(object, clazz);
  }

  @Test
  public void populateContainer_includesReportComputationStepClasses() {
    Class<MyClass> clazz = MyClass.class;
    ListTaskContainer container = new ListTaskContainer();

    try (var ignored = mockConstruction(ReportComputationSteps.class,
      (mock, context) -> {
        when(mock.orderedStepClasses()).thenReturn(List.of(clazz));
      })) {
      underTest.populateContainer(container);

      assertThat(container.getAddedComponents()).contains(clazz);
    }
  }

  @Test
  public void populateContainer_doesNotIncludeReportComputationStepInterfaces() {
    Class<MyInterface> iface = MyInterface.class;
    ListTaskContainer container = new ListTaskContainer();

    try (var ignored = mockConstruction(ReportComputationSteps.class,
      (mock, context) -> {
        when(mock.orderedStepClasses()).thenReturn(List.of(iface));
      })) {
      underTest.populateContainer(container);

      assertThat(container.getAddedComponents()).doesNotContain(iface);
    }
  }

  private interface MyInterface extends ComputationStep {
  }
  private static final class MyClass implements ComputationStep {
    @Override
    public void execute(Context context) {
      // do nothing
    }

    @Override
    public String getDescription() {
      return "";
    }
  }

}
