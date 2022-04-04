/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.scanner.repository;

import java.time.Instant;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.report.ChangedLinesPublisher;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonarqube.ws.NewCodePeriods;

public class ForkDateSupplier {
  private static final Logger LOG = Loggers.get(ChangedLinesPublisher.class);
  private static final String LOG_MSG_WS = "Load New Code definition";

  private final NewCodePeriodLoader newCodePeriodLoader;
  private final BranchConfiguration branchConfiguration;
  private final DefaultInputProject project;
  private final ScmConfiguration scmConfiguration;
  private final ProjectBranches branches;
  private final AnalysisWarnings analysisWarnings;

  public ForkDateSupplier(NewCodePeriodLoader newCodePeriodLoader, BranchConfiguration branchConfiguration,
    DefaultInputProject project, ScmConfiguration scmConfiguration, ProjectBranches branches, AnalysisWarnings analysisWarnings) {
    this.newCodePeriodLoader = newCodePeriodLoader;
    this.branchConfiguration = branchConfiguration;
    this.project = project;
    this.scmConfiguration = scmConfiguration;
    this.branches = branches;
    this.analysisWarnings = analysisWarnings;
  }

  @CheckForNull
  public Instant get() {
    // branches will be empty in CE
    if (branchConfiguration.isPullRequest() || branches.isEmpty()) {
      return null;
    }

    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG_WS);
    String branchName = branchConfiguration.branchName() != null ? branchConfiguration.branchName() : branches.defaultBranchName();
    NewCodePeriods.ShowWSResponse newCode = newCodePeriodLoader.load(project.key(), branchName);
    profiler.stopInfo();
    if (newCode.getType() != NewCodePeriods.NewCodePeriodType.REFERENCE_BRANCH) {
      return null;
    }

    String referenceBranchName = newCode.getValue();
    if (branchName.equals(referenceBranchName)) {
      LOG.warn("New Code reference branch is set to the branch being analyzed. Skipping the computation of New Code");
      return null;
    }

    LOG.info("Computing New Code since fork with '{}'", referenceBranchName);
    if (scmConfiguration.isDisabled() || scmConfiguration.provider() == null) {
      LOG.warn("SCM provider is disabled. No New Code will be computed.");
      analysisWarnings.addUnique("The scanner failed to compute New Code because no SCM provider was found. Please check your scanner logs.");
      return null;
    }

    Instant forkdate = scmConfiguration.provider().forkDate(referenceBranchName, project.getBaseDir());
    if (forkdate != null) {
      LOG.debug("Fork detected at '{}'", referenceBranchName, forkdate);
    } else {
      analysisWarnings.addUnique("The scanner failed to compute New Code. Please check your scanner logs.");
      LOG.warn("Failed to detect fork date. No New Code will be computed.", referenceBranchName);
    }
    return forkdate;
  }
}
