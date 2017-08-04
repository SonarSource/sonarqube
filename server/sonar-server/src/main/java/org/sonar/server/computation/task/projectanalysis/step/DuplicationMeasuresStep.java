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

import javax.annotation.Nullable;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.duplication.DuplicationMeasures;
import org.sonar.server.computation.task.projectanalysis.duplication.IncrementalDuplicationMeasures;
import org.sonar.server.computation.task.step.ComputationStep;

/**
 * Computes duplication measures on files and then aggregates them on higher components.
 * 
 * This step must be executed after {@link CommentMeasuresStep} as it depends on {@link CoreMetrics#COMMENT_LINES}
 */
public class DuplicationMeasuresStep implements ComputationStep {
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DuplicationMeasures defaultDuplicationMeasures;
  private final IncrementalDuplicationMeasures incrementalDuplicationsMeasures;

  public DuplicationMeasuresStep(AnalysisMetadataHolder analysisMetadataHolder, DuplicationMeasures defaultDuplicationMeasures,
    @Nullable IncrementalDuplicationMeasures incrementalDuplicationMeasures) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.defaultDuplicationMeasures = defaultDuplicationMeasures;
    this.incrementalDuplicationsMeasures = incrementalDuplicationMeasures;
  }

  /**
   * Constructor used by Pico in Views where no IncrementalDuplicationMeasures is available.
   */
  public DuplicationMeasuresStep(AnalysisMetadataHolder analysisMetadataHolder, DuplicationMeasures defaultDuplicationMeasures) {
    this(analysisMetadataHolder, defaultDuplicationMeasures, null);
  }

  @Override
  public String getDescription() {
    return "Compute duplication measures";
  }

  @Override
  public void execute() {
    if (analysisMetadataHolder.isIncrementalAnalysis()) {
      incrementalDuplicationsMeasures.execute();
    } else {
      defaultDuplicationMeasures.execute();
    }
  }
}
