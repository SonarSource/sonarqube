/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.step;

import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ExecuteStatelessOnFinishStepTest {

  @Test
  public void execute_whenNoExtensionsRegistered_shouldNotThrowExeption() {
    ExecuteStatelessOnFinishStep step = new ExecuteStatelessOnFinishStep();

    assertDoesNotThrow(() -> step.execute(mock(ComputationStep.Context.class)));
  }

  @Test
  public void execute_whenTwoExtensionsRegistered_shouldCallTwoOfThem() {
    StatelessFinishExtension firstMock = mock(StatelessFinishExtension.class);
    StatelessFinishExtension secondMock = mock(StatelessFinishExtension.class);
    StatelessFinishExtension[] extensions = new StatelessFinishExtension[]{firstMock, secondMock};

    ExecuteStatelessOnFinishStep step = new ExecuteStatelessOnFinishStep(extensions);

    step.execute(mock(ComputationStep.Context.class));

    verify(firstMock, Mockito.times(1)).onFinish();
    verify(secondMock, Mockito.times(1)).onFinish();
  }

  @Test
  public void getDescription_shouldReturnNotEmptyString() {
    ExecuteStatelessOnFinishStep step = new ExecuteStatelessOnFinishStep();

    assertThat(step.getDescription()).isNotEmpty();
  }
}
