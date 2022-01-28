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

import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonarqube.ws.NewCodePeriods;

public class ReferenceBranchSupplier {
  private static final Logger LOG = Loggers.get(ReferenceBranchSupplier.class);
  private static final String LOG_MSG_WS = "Load New Code definition";

  private final NewCodePeriodLoader newCodePeriodLoader;
  private final BranchConfiguration branchConfiguration;
  private final DefaultInputProject project;
  private final ProjectBranches branches;

  public ReferenceBranchSupplier(NewCodePeriodLoader newCodePeriodLoader, BranchConfiguration branchConfiguration, DefaultInputProject project, ProjectBranches branches) {
    this.newCodePeriodLoader = newCodePeriodLoader;
    this.branchConfiguration = branchConfiguration;
    this.project = project;
    this.branches = branches;
  }

  @CheckForNull
  public String get() {
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

    return referenceBranchName;
  }
}
