/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.project.Project;
import org.sonar.server.computation.task.projectanalysis.view.TriggerViewRefreshDelegate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TriggerViewRefreshStepTest {
  private AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);


  @Test
  public void execute_has_no_effect_if_constructor_without_delegate() {
    TriggerViewRefreshStep underTest = new TriggerViewRefreshStep(analysisMetadataHolder);

    underTest.execute();

    verifyZeroInteractions(analysisMetadataHolder);
  }

  @Test
  public void execute_has_no_effect_if_constructor_with_null_delegate() {
    TriggerViewRefreshStep underTest = new TriggerViewRefreshStep(analysisMetadataHolder, null);

    underTest.execute();

    verifyZeroInteractions(analysisMetadataHolder);
  }

  @Test
  public void execute_calls_delegate_with_project_from_holder_if_passed_to_constructor() {
    TriggerViewRefreshDelegate delegate = mock(TriggerViewRefreshDelegate.class);
    Project project = mock(Project.class);
    when(analysisMetadataHolder.getProject()).thenReturn(project);
    TriggerViewRefreshStep underTest = new TriggerViewRefreshStep(analysisMetadataHolder, delegate);

    underTest.execute();

    verify(analysisMetadataHolder).getProject();
    verify(delegate).accept(project);
  }
}
