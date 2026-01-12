/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.purge;

import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.DisabledComponentsHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

public class PurgeDatastoresStep implements ComputationStep {

  private final ProjectCleaner projectCleaner;
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final ConfigurationRepository configRepository;
  private final DisabledComponentsHolder disabledComponentsHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TelemetryQGOnMergedPRProvider telemetryQGOnMergedPRProvider;

  public PurgeDatastoresStep(DbClient dbClient, ProjectCleaner projectCleaner, TreeRootHolder treeRootHolder,
    ConfigurationRepository configRepository, DisabledComponentsHolder disabledComponentsHolder,
    AnalysisMetadataHolder analysisMetadataHolder, TelemetryQGOnMergedPRProvider telemetryQGOnMergedPRProvider) {
    this.projectCleaner = projectCleaner;
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.configRepository = configRepository;
    this.disabledComponentsHolder = disabledComponentsHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.telemetryQGOnMergedPRProvider = telemetryQGOnMergedPRProvider;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    try (DbSession dbSession = dbClient.openSession(true)) {
      // applies to views and projects
      String projectUuid = analysisMetadataHolder.getProject().getUuid();
      projectCleaner.purge(dbSession, treeRootHolder.getRoot().getUuid(), projectUuid, configRepository.getConfiguration(),
        disabledComponentsHolder.getUuids());
      dbSession.commit();
    }
    telemetryQGOnMergedPRProvider.sendTelemetry();
  }

  @Override
  public String getDescription() {
    return "Purge db";
  }
}
