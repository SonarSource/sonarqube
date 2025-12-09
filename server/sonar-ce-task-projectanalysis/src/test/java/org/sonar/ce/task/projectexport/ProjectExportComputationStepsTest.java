/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectexport;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.container.TaskContainerImpl;
import org.sonar.ce.task.projectanalysis.step.ComplexityMeasuresStep;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.SpringComponentContainer;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectExportComputationStepsTest {
  private final TaskContainer container = mock(TaskContainer.class);
  private final ProjectExportComputationSteps underTest = new ProjectExportComputationSteps(container);

  @Test
  public void count_step_classes() {
    assertThat(copyOf(underTest.orderedStepClasses())).hasSize(19);
  }

  @Test
  public void instances_throws_ISE_if_steps_do_not_exist_in_container() {
    when(container.getComponentByType(any())).thenThrow(new IllegalStateException("Error"));
    Iterable<ComputationStep> instances = underTest.instances();
    assertThatThrownBy(() -> copyOf(instances))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Error");
  }

  @Test
  public void instances_throws_ISE_if_container_does_not_have_second_step() {
    ComplexityMeasuresStep reportExtractionStep = mock(ComplexityMeasuresStep.class);

    SpringComponentContainer componentContainer = new SpringComponentContainer() {
      {
        add(reportExtractionStep);
      }
    }.startComponents();
    TaskContainerImpl computeEngineContainer = new TaskContainerImpl(componentContainer, container -> {
      // do nothing
    });
    computeEngineContainer.startComponents();
    Iterable<ComputationStep> instances = new ProjectExportComputationSteps(computeEngineContainer).instances();
    assertThatThrownBy(() -> Lists.newArrayList(instances))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("class org.sonar.ce.task.projectexport.steps.LoadProjectStep");

  }
}
