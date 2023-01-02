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
package org.sonar.scanner.repository;

import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonarqube.ws.NewCodePeriods;

import static java.lang.String.format;

public class ReferenceBranchSupplier {
  private static final Logger LOG = Loggers.get(ReferenceBranchSupplier.class);
  private static final String LOG_MSG_WS = "Load New Code definition";
  private static final String NEW_CODE_PARAM_KEY = "sonar.newCode.referenceBranch";

  private final Configuration configuration;
  private final NewCodePeriodLoader newCodePeriodLoader;
  private final BranchConfiguration branchConfiguration;
  private final DefaultInputProject project;
  private final ProjectBranches branches;

  public ReferenceBranchSupplier(Configuration configuration, NewCodePeriodLoader newCodePeriodLoader, BranchConfiguration branchConfiguration, DefaultInputProject project,
    ProjectBranches branches) {
    this.configuration = configuration;
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

    return Optional.ofNullable(getFromProperties()).orElseGet(this::loadWs);
  }

  private String loadWs() {
    String branchName = getBranchName();
    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG_WS);
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

  @CheckForNull
  public String getFromProperties() {
    // branches will be empty in CE
    if (branchConfiguration.isPullRequest() || branches.isEmpty()) {
      return null;
    }

    Optional<String> value = configuration.get(NEW_CODE_PARAM_KEY);
    if (value.isPresent()) {
      String referenceBranchName = value.get();
      if (referenceBranchName.equals(getBranchName())) {
        throw new IllegalStateException(format("Reference branch set with '%s' points to the current branch '%s'", NEW_CODE_PARAM_KEY, referenceBranchName));
      }
      return referenceBranchName;
    }
    return null;
  }

  private String getBranchName() {
    return branchConfiguration.branchName() != null ? branchConfiguration.branchName() : branches.defaultBranchName();
  }
}
