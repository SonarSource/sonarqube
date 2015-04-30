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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class ComputationStepsTest {

  @Test
  public void ordered_steps() throws Exception {
    ComputationSteps registry = new ComputationSteps(
      // unordered
      mock(ApplyPermissionsStep.class),
      mock(ParseReportStep.class),
      mock(IndexSourceLinesStep.class),
      mock(IndexViewsStep.class),
      mock(PurgeRemovedViewsStep.class),
      mock(PersistIssuesStep.class),
      mock(IndexIssuesStep.class),
      mock(SwitchSnapshotStep.class),
      mock(PurgeDatastoresStep.class),
      mock(SendIssueNotificationsStep.class),
      mock(IndexComponentsStep.class),
      mock(PersistComponentLinksStep.class),
      mock(PersistMeasuresStep.class),
      mock(PersistEventsStep.class),
      mock(PersistDuplicationMeasuresStep.class),
      mock(PersistNumberOfDaysSinceLastCommitStep.class),
      mock(PersistFileSourcesStep.class),
      mock(PersistFileDependenciesStep.class)
      );

    assertThat(registry.orderedSteps()).hasSize(18);
    assertThat(registry.orderedSteps().get(0)).isInstanceOf(ParseReportStep.class);
    assertThat(registry.orderedSteps().get(17)).isInstanceOf(SendIssueNotificationsStep.class);
  }

  @Test
  public void fail_if_a_step_is_not_registered_in_picocontainer() throws Exception {
    try {
      new ComputationSteps(mock(ParseReportStep.class));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageContaining("Component not found");
    }
  }
}
