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

import org.sonar.api.utils.MessageException;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.analysis.Organization;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidationsProxy;

/**
 * Verify that organization can execute analysis
 */
public class VerifyBillingStep implements ComputationStep {

  private final AnalysisMetadataHolder analysisMetadata;
  private final BillingValidations billingValidations;

  public VerifyBillingStep(AnalysisMetadataHolder analysisMetadata, BillingValidationsProxy billingValidations) {
    this.analysisMetadata = analysisMetadata;
    this.billingValidations = billingValidations;
  }

  @Override
  public void execute() {
    try {
      Organization organization = analysisMetadata.getOrganization();
      BillingValidations.Organization billingOrganization = new BillingValidations.Organization(organization.getKey(), organization.getUuid());
      billingValidations.checkOnProjectAnalysis(billingOrganization);
    } catch (BillingValidations.BillingValidationsException e) {
      throw MessageException.of(e.getMessage());
    }
  }

  @Override
  public String getDescription() {
    return "Verify billing";
  }
}
