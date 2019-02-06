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
import org.sonar.api.config.Configuration;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.MutableQualityGateHolder;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateService;
import org.sonar.ce.task.step.ComputationStep;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * This step retrieves the QualityGate and stores it in
 * {@link MutableQualityGateHolder}.
 */
public class LoadQualityGateStep implements ComputationStep {
  private static final String PROPERTY_PROJECT_QUALITY_GATE = "sonar.qualitygate";

  private final ConfigurationRepository configRepository;
  private final QualityGateService qualityGateService;
  private final MutableQualityGateHolder qualityGateHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public LoadQualityGateStep(ConfigurationRepository settingsRepository, QualityGateService qualityGateService, MutableQualityGateHolder qualityGateHolder,
    AnalysisMetadataHolder analysisMetadataHolder) {
    this.configRepository = settingsRepository;
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
    Configuration config = configRepository.getConfiguration();
    String qualityGateSetting = config.get(PROPERTY_PROJECT_QUALITY_GATE).orElse(null);

    if (isBlank(qualityGateSetting)) {
      return Optional.empty();
    }

    try {
      long qualityGateId = Long.parseLong(qualityGateSetting);
      return qualityGateService.findById(qualityGateId);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
        String.format("Unsupported value (%s) in property %s", qualityGateSetting, PROPERTY_PROJECT_QUALITY_GATE), e);
    }
  }

  private QualityGate getOrganizationDefaultQualityGate() {
    return qualityGateService.findDefaultQualityGate(analysisMetadataHolder.getOrganization());
  }

  @Override
  public String getDescription() {
    return "Load Quality gate";
  }
}
