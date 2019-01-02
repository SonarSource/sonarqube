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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.container.TaskContainerImpl;
import org.sonar.core.platform.ComponentContainer;

import static org.mockito.Mockito.mock;

public class ReportComputationStepsTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void instances_throws_ISE_if_container_does_not_have_any_step() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Component not found: " + ExtractReportStep.class);

    TaskContainerImpl computeEngineContainer = new TaskContainerImpl(new ComponentContainer(), container -> {
      // do nothing
    });

    Lists.newArrayList(new ReportComputationSteps(computeEngineContainer).instances());
  }

  @Test
  public void instances_throws_ISE_if_container_does_not_have_second_step() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Component not found: class org.sonar.ce.task.projectanalysis.step.PersistScannerContextStep");

    final ExtractReportStep reportExtractionStep = mock(ExtractReportStep.class);
    ComponentContainer componentContainer = new ComponentContainer() {
      {
        addSingleton(reportExtractionStep);
      }
    };
    TaskContainerImpl computeEngineContainer = new TaskContainerImpl(componentContainer, container -> {
      // do nothing
    });

    Lists.newArrayList(new ReportComputationSteps(computeEngineContainer).instances());
  }
}
