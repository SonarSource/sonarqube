/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.step.ComputationStep;

/**
 * Execute {@link PreMeasuresComputationCheck} instances in no specific order.
 * If an extension fails (throws an exception), consecutive extensions
 * won't be called.
 */
@ComputeEngineSide
public class PreMeasuresComputationChecksStep implements ComputationStep {

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final PreMeasuresComputationCheck[] extensions;

  public PreMeasuresComputationChecksStep(AnalysisMetadataHolder analysisMetadataHolder, PreMeasuresComputationCheck... extensions) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.extensions = extensions;
  }

  @Override
  public void execute(Context context) {
    PreMeasuresComputationCheck.Context extensionContext = new ContextImpl();
    for (PreMeasuresComputationCheck extension : extensions) {
      extension.onCheck(extensionContext);
    }
  }

  @Override
  public String getDescription() {
    return "Checks executed before computation of measures";
  }

  private class ContextImpl implements PreMeasuresComputationCheck.Context {

    @Override
    public String getProjectUuid() {
      return analysisMetadataHolder.getProject().getUuid();
    }

    @Override
    public Branch getBranch() {
      return analysisMetadataHolder.getBranch();
    }

  }
}
