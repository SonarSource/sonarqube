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
package org.sonar.ce.task.projectanalysis.step;

import org.junit.Test;
import org.sonar.ce.task.container.TaskContainerImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.SpringComponentContainer;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class ReportComputationStepsTest {

  @Test
  public void instances_throws_ISE_if_container_does_not_have_any_step() {
    TaskContainerImpl computeEngineContainer = new TaskContainerImpl(new SpringComponentContainer(), container -> {
      // do nothing
    });
    Iterable<ComputationStep> instances = new ReportComputationSteps(computeEngineContainer).instances();
    assertThatThrownBy(() -> newArrayList(instances))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(ExtractReportStep.class.getName());
  }

  @Test
  public void instances_throws_ISE_if_container_does_not_have_second_step() {
    ExtractReportStep reportExtractionStep = mock(ExtractReportStep.class);
    SpringComponentContainer componentContainer = new SpringComponentContainer() {
      {
        add(reportExtractionStep);
      }
    }.startComponents();
    TaskContainerImpl computeEngineContainer = new TaskContainerImpl(componentContainer, container -> {
      // do nothing
    });
    computeEngineContainer.startComponents();
    Iterable<ComputationStep> instances = new ReportComputationSteps(computeEngineContainer).instances();
    assertThatThrownBy(() -> newArrayList(instances))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("org.sonar.ce.task.projectanalysis.step.PersistScannerContextStep");
  }
}
