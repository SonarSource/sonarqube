/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.IncrementalMeasureTransition;
import org.sonar.server.computation.task.step.ComputationStep;

public class IncrementalMeasureTransitionStepTest extends BaseStepTest {
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  IncrementalMeasureTransitionStep underTest = new IncrementalMeasureTransitionStep(analysisMetadataHolder);

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void skip_if_not_incremental() {
    analysisMetadataHolder.setIncrementalAnalysis(false);
    underTest.execute();
  }

  @Test
  public void execute_if_incremental() {
    IncrementalMeasureTransition runnable = mock(IncrementalMeasureTransition.class);
    IncrementalMeasureTransitionStep underTest = new IncrementalMeasureTransitionStep(analysisMetadataHolder, runnable);
    analysisMetadataHolder.setIncrementalAnalysis(true);

    underTest.execute();
    verify(runnable).run();
  }

}
