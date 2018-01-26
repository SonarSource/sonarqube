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
import org.sonar.server.computation.task.projectanalysis.duplication.DuplicationMeasures;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DuplicationMeasuresStepTest extends BaseStepTest {

  private DuplicationMeasures defaultDuplicationMeasures = mock(DuplicationMeasures.class);
  private DuplicationMeasuresStep underTest = new DuplicationMeasuresStep(defaultDuplicationMeasures);
  
  @Test
  public void full_analysis_mode() {
    underTest.execute();
    verify(defaultDuplicationMeasures).execute();
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
