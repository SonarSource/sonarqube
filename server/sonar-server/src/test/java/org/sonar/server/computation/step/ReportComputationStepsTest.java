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
package org.sonar.server.computation.step;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.ContainerPopulator;
import org.sonar.server.computation.container.ComputeEngineContainer;
import org.sonar.server.computation.container.ComputeEngineContainerImpl;

import static org.mockito.Mockito.mock;

public class ReportComputationStepsTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void instances_throws_ISE_if_container_does_not_have_any_step() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Component not found: " + ExtractReportStep.class);

    ComputeEngineContainerImpl computeEngineContainer = new ComputeEngineContainerImpl(new ComponentContainer(), new ContainerPopulator<ComputeEngineContainer>() {
      @Override
      public void populateContainer(ComputeEngineContainer container) {
        // do nothing
      }
    });

    Lists.newArrayList(new ReportComputationSteps(computeEngineContainer).instances());
  }

  @Test
  public void instances_throws_ISE_if_container_does_not_have_second_step() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Component not found: class org.sonar.server.computation.step.LogScannerContextStep");

    final ExtractReportStep reportExtractionStep = mock(ExtractReportStep.class);
    ComponentContainer componentContainer = new ComponentContainer() {
      {
        addSingleton(reportExtractionStep);
      }
    };
    ComputeEngineContainerImpl computeEngineContainer = new ComputeEngineContainerImpl(componentContainer, new ContainerPopulator<ComputeEngineContainer>() {
      @Override
      public void populateContainer(ComputeEngineContainer container) {
        // do nothing
      }
    });

    Lists.newArrayList(new ReportComputationSteps(computeEngineContainer).instances());
  }
}
