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

import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.computation.task.projectanalysis.qualitygate.MutableQualityGateHolder;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateService;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.qualitygate.ShortLivingBranchQualityGate;

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
  public void execute() {
    Optional<QualityGate> qualityGate = getShortLivingBranchQualityGate();
    if (!qualityGate.isPresent()) {
      // Not on a short living branch, let's retrieve the QG of the project
      qualityGate = getProjectQualityGate();
      if (!qualityGate.isPresent()) {
        // No QG defined for the project, let's retrieve the QG on the organization
        qualityGate = Optional.of(getOrganizationDefaultQualityGate());
      }
    }

    qualityGateHolder.setQualityGate(qualityGate.orElseThrow(() -> new IllegalStateException("Quality gate not present")));
  }

  private Optional<QualityGate> getShortLivingBranchQualityGate() {
    if (analysisMetadataHolder.isShortLivingBranch()) {
      Optional<QualityGate> qualityGate = qualityGateService.findById(ShortLivingBranchQualityGate.ID);
      if (qualityGate.isPresent()) {
        return qualityGate;
      } else {
        throw new IllegalStateException("Failed to retrieve hardcoded short living branch Quality Gate");
      }
    } else {
      return Optional.empty();
    }
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
