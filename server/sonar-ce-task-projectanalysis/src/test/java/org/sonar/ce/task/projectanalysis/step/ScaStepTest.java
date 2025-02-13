/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.sonar.ce.common.sca.ScaHolder;
import org.sonar.ce.common.sca.ScaStepProvider;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScaStepTest {

  @Test
  void shouldSkipScaStepIfNotAvailable() {
    var underTest = new ScaStep(mock(ScannerReportReader.class), mock(CeTaskMessages.class), null, null);

    assertThat(underTest.getDescription()).isEqualTo("Software composition analysis unavailable");

    assertThatNoException().isThrownBy(() -> underTest.execute(mock(ComputationStep.Context.class)));
  }

  @Test
  void shouldWrapScaStepIfAvailable() {
    var wrappedStep = mock(ComputationStep.class);
    when(wrappedStep.getDescription()).thenReturn("wrapped step");

    ScaStepProvider scaStepProvider = (reportReader, ceTaskMessages, scaHolder) -> wrappedStep;
    var underTest = new ScaStep(mock(ScannerReportReader.class), mock(CeTaskMessages.class), scaStepProvider, mock(ScaHolder.class));

    assertThat(underTest.getDescription()).isEqualTo("wrapped step");

    var context = mock(ComputationStep.Context.class);
    underTest.execute(context);

    verify(wrappedStep).execute(context);
  }

}
