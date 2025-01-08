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

import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.MutableQualityGateHolder;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateService;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.server.project.Project;

/**
 * This step retrieves the QualityGate and stores it in
 * {@link MutableQualityGateHolder}.
 */
public class LoadQualityGateStep implements ComputationStep {

  private final QualityGateService qualityGateService;
  private final MutableQualityGateHolder qualityGateHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public LoadQualityGateStep(QualityGateService qualityGateService, MutableQualityGateHolder qualityGateHolder, AnalysisMetadataHolder analysisMetadataHolder) {
    this.qualityGateService = qualityGateService;
    this.qualityGateHolder = qualityGateHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    QualityGate qualityGate = getProjectQualityGate();

    if (analysisMetadataHolder.isPullRequest()) {
      qualityGate = filterQGForPR(qualityGate);
    }

    qualityGateHolder.setQualityGate(qualityGate);
  }

  private static QualityGate filterQGForPR(QualityGate qg) {
    return new QualityGate(qg.getUuid(), qg.getName(), qg.getConditions().stream().filter(Condition::useVariation).toList());
  }

  private QualityGate getProjectQualityGate() {
    Project project = analysisMetadataHolder.getProject();
    return qualityGateService.findEffectiveQualityGate(project);
  }

  @Override
  public String getDescription() {
    return "Load Quality gate";
  }
}
