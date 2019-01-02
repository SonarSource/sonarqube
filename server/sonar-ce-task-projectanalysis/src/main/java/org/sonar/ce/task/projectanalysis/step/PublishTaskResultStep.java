/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Optional;
import javax.annotation.concurrent.Immutable;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.taskprocessor.MutableTaskResultHolder;

public class PublishTaskResultStep implements ComputationStep {
  private final MutableTaskResultHolder taskResultHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public PublishTaskResultStep(MutableTaskResultHolder taskResultHolder, AnalysisMetadataHolder analysisMetadataHolder) {
    this.taskResultHolder = taskResultHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public String getDescription() {
    return "Publish task results";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    taskResultHolder.setResult(new CeTaskResultImpl(analysisMetadataHolder.getUuid()));
  }

  @Immutable
  private static class CeTaskResultImpl implements CeTaskResult {
    private final String analysisUuid;

    public CeTaskResultImpl(String analysisUuid) {
      this.analysisUuid = analysisUuid;
    }

    @Override
    public Optional<String> getAnalysisUuid() {
      return Optional.of(analysisUuid);
    }
  }
}
