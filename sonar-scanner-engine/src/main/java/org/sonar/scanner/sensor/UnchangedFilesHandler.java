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
package org.sonar.scanner.sensor;

import java.util.Objects;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.scan.branch.BranchConfiguration;

public class UnchangedFilesHandler {
  private static final Logger LOG = LoggerFactory.getLogger(UnchangedFilesHandler.class);
  private static final Set<SensorId> ENABLED_SENSORS = Set.of(
    new SensorId("cpp", "CFamily"),
    new SensorId("cobol", "CobolSquidSensor"),
    // for ITs
    new SensorId("xoo", "Mark As Unchanged Sensor"));
  private static final String ENABLE_PROPERTY_KEY = "sonar.unchangedFiles.optimize";
  private final boolean featureActive;
  private final ExecutingSensorContext executingSensorContext;

  public UnchangedFilesHandler(Configuration configuration, BranchConfiguration branchConfiguration, ExecutingSensorContext executingSensorContext) {
    this.executingSensorContext = executingSensorContext;
    this.featureActive = getFeatureActivationStatus(configuration, branchConfiguration);
  }

  private static boolean getFeatureActivationStatus(Configuration configuration, BranchConfiguration branchConfiguration) {
    boolean isPropertyEnabled = configuration.getBoolean(ENABLE_PROPERTY_KEY).orElse(false);
    if (!isPropertyEnabled) {
      return false;
    }
    if (branchConfiguration.isPullRequest() || !Objects.equals(branchConfiguration.branchName(), branchConfiguration.referenceBranchName())) {
      LOG.debug("Optimization for unchanged files not enabled because it's not an analysis of a branch with a previous analysis");
      return false;
    }
    LOG.info("Optimization for unchanged files enabled");
    return true;
  }

  public void markAsUnchanged(DefaultInputFile file) {
    if (isFeatureActive()) {
      if (file.status() != InputFile.Status.SAME) {
        LOG.error("File '{}' was marked as unchanged but its status is {}", file.getProjectRelativePath(), file.status());
      } else {
        LOG.debug("File '{}' marked as unchanged", file.getProjectRelativePath());
        file.setMarkedAsUnchanged(true);
      }
    }
  }

  private boolean isFeatureActive() {
    return featureActive && executingSensorContext.getSensorExecuting() != null && ENABLED_SENSORS.contains(executingSensorContext.getSensorExecuting());
  }
}
