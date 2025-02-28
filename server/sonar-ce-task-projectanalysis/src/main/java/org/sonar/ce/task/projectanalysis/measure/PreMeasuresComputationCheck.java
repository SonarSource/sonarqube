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
package org.sonar.ce.task.projectanalysis.measure;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.projectanalysis.analysis.Branch;


/**
 * Extension point that is called during processing of a task
 * by {@link PreMeasuresComputationChecksStep}.
 *
 * It is stateless, the same instance is reused for all tasks.
 * As a consequence Compute Engine task components can't be injected
 * as dependencies.
 */
@ComputeEngineSide
@ExtensionPoint
public interface PreMeasuresComputationCheck {

  /**
   * Throwing a {@link PreMeasuresComputationCheckException} will only produce an analysis warning.
   * Any other exception will fail the analysis.
   */
  void onCheck(Context context) throws PreMeasuresComputationCheckException;

  interface Context {

    String getProjectUuid();

    Branch getBranch();

    Configuration getConfiguration();

    ScannerReportReader getReportReader();
  }

  class PreMeasuresComputationCheckException extends RuntimeException {
    public PreMeasuresComputationCheckException(String message) {
      super(message);
    }

    public PreMeasuresComputationCheckException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
