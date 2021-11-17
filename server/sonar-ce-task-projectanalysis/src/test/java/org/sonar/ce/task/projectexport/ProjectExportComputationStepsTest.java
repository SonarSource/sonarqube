/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import org.sonar.ce.task.projectexport.steps.LoadProjectStep;
import org.sonar.core.platform.ComponentContainer;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class ProjectExportComputationStepsTest {

  private TaskContainer container = mock(TaskContainer.class);
  private ProjectExportComputationSteps underTest = new ProjectExportComputationSteps(container);

  @Test
  public void count_step_classes() {
    assertThat(copyOf(underTest.orderedStepClasses()).size()).isEqualTo(20);
  }

  @Test
  public void instances_throws_ISE_if_steps_do_not_exist_in_container() {
    assertThatThrownBy(() -> copyOf(underTest.instances()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Component not found: " + LoadProjectStep.class);
  }

  @Test
  public void instances_throws_ISE_if_container_does_not_have_second_step() {
    assertThatThrownBy(() -> {
      final ComplexityMeasuresStep reportExtractionStep = mock(ComplexityMeasuresStep.class);

      ComponentContainer componentContainer = new ComponentContainer() {
        {
          addSingleton(reportExtractionStep);
        }
      };
      TaskContainerImpl computeEngineContainer = new TaskContainerImpl(componentContainer, container -> {
        // do nothing
      });

      Lists.newArrayList(new ProjectExportComputationSteps(computeEngineContainer).instances());
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Component not found: class org.sonar.ce.task.projectexport.steps.LoadProjectStep");

  }
}
