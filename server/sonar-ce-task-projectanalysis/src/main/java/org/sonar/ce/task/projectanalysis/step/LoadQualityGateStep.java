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
import java.util.stream.Collectors;
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

  public LoadQualityGateStep(QualityGateService qualityGateService, MutableQualityGateHolder qualityGateHolder,
                             AnalysisMetadataHolder analysisMetadataHolder) {
    this.qualityGateService = qualityGateService;
    this.qualityGateHolder = qualityGateHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    Optional<QualityGate> qualityGate = getProjectQualityGate();
    if (!qualityGate.isPresent()) {
      // No QG defined for the project, let's retrieve the QG on the organization
      qualityGate = Optional.of(getOrganizationDefaultQualityGate());
    }

    if (analysisMetadataHolder.isSLBorPR()) {
      qualityGate = filterQGForSLB(qualityGate);
    }

    qualityGateHolder.setQualityGate(qualityGate.orElseThrow(() -> new IllegalStateException("Quality gate not present")));
  }

  private static Optional<QualityGate> filterQGForSLB(Optional<QualityGate> qualityGate) {
    return qualityGate.map(qg -> new QualityGate(qg.getId(), qg.getName(),
      qg.getConditions().stream().filter(Condition::useVariation).collect(Collectors.toList())));
  }

  private Optional<QualityGate> getProjectQualityGate() {
    Project project = analysisMetadataHolder.getProject();
    return qualityGateService.findQualityGate(project);
  }

  private QualityGate getOrganizationDefaultQualityGate() {
    return qualityGateService.findDefaultQualityGate(analysisMetadataHolder.getOrganization());
  }

  @Override
  public String getDescription() {
    return "Load Quality gate";
  }
}
