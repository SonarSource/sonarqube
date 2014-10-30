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

package org.sonar.server.computation;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ComputationStepRegistryTest {

  private ComputationStepRegistry sut;
  private SynchronizeProjectPermissionsStep synchronizeProjectPermissionsStep;
  private IndexProjectIssuesStep indexProjectIssuesStep;
  private SwitchSnapshotStep switchSnapshotStep;
  private DataCleanerStep dataCleanerStep;
  private InvalidatePreviewCacheStep invalidatePreviewCacheStep;

  @Before
  public void before() {
    synchronizeProjectPermissionsStep = mock(SynchronizeProjectPermissionsStep.class);
    indexProjectIssuesStep = mock(IndexProjectIssuesStep.class);
    switchSnapshotStep = mock(SwitchSnapshotStep.class);
    dataCleanerStep = mock(DataCleanerStep.class);
    invalidatePreviewCacheStep = mock(InvalidatePreviewCacheStep.class);

    sut = new ComputationStepRegistry(synchronizeProjectPermissionsStep, indexProjectIssuesStep, switchSnapshotStep, dataCleanerStep, invalidatePreviewCacheStep);
  }

  @Test
  public void steps_returned_in_the_right_order() throws Exception {
    assertThat(sut.steps()).containsExactly(synchronizeProjectPermissionsStep, switchSnapshotStep, invalidatePreviewCacheStep, dataCleanerStep, indexProjectIssuesStep);
  }
}
