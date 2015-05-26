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
import java.util.List;
import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.computation.container.CEContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class ComputationStepsTest {

  @Test
  public void ordered_steps() {
    CEContainer ceContainer = new CEContainer(new ComponentContainer());
    ceContainer.add(
      // unordered
      mock(ApplyPermissionsStep.class),
      mock(ParseReportStep.class),
      mock(IndexSourceLinesStep.class),
      mock(PersistIssuesStep.class),
      mock(IndexIssuesStep.class),
      mock(SwitchSnapshotStep.class),
      mock(PurgeDatastoresStep.class),
      mock(SendIssueNotificationsStep.class),
      mock(IndexComponentsStep.class),
      mock(PersistProjectLinksStep.class),
      mock(PersistMeasuresStep.class),
      mock(PersistEventsStep.class),
      mock(PersistDuplicationsStep.class),
      mock(PersistNumberOfDaysSinceLastCommitStep.class),
      mock(PersistFileSourcesStep.class),
      mock(PersistTestsStep.class),
      mock(IndexTestsStep.class),
      mock(PopulateComponentsUuidAndKeyStep.class),
      mock(PersistComponentsStep.class),
      mock(QualityProfileEventsStep.class),
      mock(ValidateProjectStep.class)
      );
    ComputationSteps computationSteps = new ComputationSteps(ceContainer);

    List<ComputationStep> steps = Lists.newArrayList(computationSteps.instances());
    assertThat(steps).hasSize(21);
    assertThat(steps.get(0)).isInstanceOf(PopulateComponentsUuidAndKeyStep.class);
    assertThat(steps.get(20)).isInstanceOf(SendIssueNotificationsStep.class);
  }

  @Test
  public void fail_if_a_step_is_not_registered_in_picocontainer() {
    try {
      Lists.newArrayList(new ComputationSteps(new CEContainer(new ComponentContainer())).instances());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageContaining("Component not found");
    }
  }
}
