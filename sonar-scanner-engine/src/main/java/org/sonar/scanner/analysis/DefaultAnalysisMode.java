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
package org.sonar.scanner.analysis;

import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.scan.branch.BranchConfiguration;

@Immutable
public class DefaultAnalysisMode implements AnalysisMode {
  private static final Logger LOG = Loggers.get(DefaultAnalysisMode.class);
  private static final String KEY_SCAN_ALL = "sonar.scanAllFiles";
  private static final String KEY_INCREMENTAL = "sonar.incremental";

  private final Map<String, String> analysisProps;
  private final GlobalAnalysisMode analysisMode;
  private final BranchConfiguration branchConfig;
  private final ProjectRepositories projectRepos;
  private final IncrementalScannerHandler incrementalScannerHandler;

  private boolean scanAllFiles;
  private boolean incremental;

  public DefaultAnalysisMode(AnalysisProperties props, BranchConfiguration branchConfig, GlobalAnalysisMode analysisMode, ProjectRepositories projectRepos) {
    this(props, branchConfig, analysisMode, projectRepos, null);
  }

  public DefaultAnalysisMode(AnalysisProperties props, BranchConfiguration branchConfig,
    GlobalAnalysisMode analysisMode, ProjectRepositories projectRepos, @Nullable IncrementalScannerHandler incrementalScannerHandler) {
    this.branchConfig = branchConfig;
    this.analysisMode = analysisMode;
    this.projectRepos = projectRepos;
    this.incrementalScannerHandler = incrementalScannerHandler;
    this.analysisProps = props.properties();
    load();
    printFlags();
  }

  @Override
  public boolean isIncremental() {
    return incremental;
  }

  public boolean scanAllFiles() {
    return scanAllFiles;
  }

  private void printFlags() {
    if (incremental) {
      LOG.info("Incremental mode");
    }
    if (!scanAllFiles) {
      LOG.info("Scanning only changed files");
    }
  }

  private void load() {
    String scanAllStr = analysisProps.get(KEY_SCAN_ALL);
    incremental = incremental();
    scanAllFiles = !incremental && !branchConfig.isShortLivingBranch() && (!analysisMode.isIssues() || "true".equals(scanAllStr));
  }

  private boolean incremental() {
    String inc = analysisProps.get(KEY_INCREMENTAL);
    if ("true".equals(inc)) {
      if (incrementalScannerHandler == null || !incrementalScannerHandler.execute()) {
        throw MessageException.of("Incremental mode is not available. Please contact your administrator.");
      }

      if (!analysisMode.isPublish()) {
        throw MessageException.of("Incremental analysis is only available in publish mode");
      }

      if (branchConfig.branchName() != null) {
        LOG.warn("Incremental analysis mode has been activated but it's not compatible with branches so a full analysis will be done.");
        return false;
      }

      if (!projectRepos.exists() || projectRepos.lastAnalysisDate() == null) {
        LOG.warn("Incremental analysis mode has been activated but the project was never analyzed before so a full analysis is about to be done.");
        return false;
      }

      LOG.debug("Reference analysis is {}", DateUtils.formatDateTime(projectRepos.lastAnalysisDate()));
      return true;
    }

    return false;
  }

  @Override
  public boolean isPreview() {
    return analysisMode.isPreview();
  }

  @Override
  public boolean isIssues() {
    return analysisMode.isIssues();
  }

  @Override
  public boolean isPublish() {
    return analysisMode.isPublish();
  }
}
