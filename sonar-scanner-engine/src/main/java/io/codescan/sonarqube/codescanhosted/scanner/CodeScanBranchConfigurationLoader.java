/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

package io.codescan.sonarqube.codescanhosted.scanner;

import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationLoader;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.DefaultBranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectPullRequests;

/**
 * This class is a loader to return a branch configuration to use for the current build...
 */
public class CodeScanBranchConfigurationLoader implements BranchConfigurationLoader {

    private static final Logger LOG = Loggers.get(CodeScanBranchConfigurationLoader.class);

    private static final String BRANCH_TYPE = "sonar.branch.type";

    @Override
    public BranchConfiguration load(Map<String, String> projectSettings, ProjectBranches branches,
            ProjectPullRequests pullRequests) {
        String branchName = StringUtils.trimToNull(projectSettings.get(ScannerProperties.BRANCH_NAME));
        String branchTarget = StringUtils.trimToNull(projectSettings.get(ScannerProperties.BRANCH_TARGET));
        String targetScmBranch = branchTarget;
        BranchType branchType = convertBranchType(projectSettings.get(BRANCH_TYPE));

        // No branch config. Use default settings.
        if (branchName == null && branchTarget == null) {
            return new DefaultBranchConfiguration();
        }

        String pullRequestKey = null;
        if (branchType == BranchType.PULL_REQUEST) {
            pullRequestKey = projectSettings.get(ScannerProperties.PULL_REQUEST_KEY);
        }

        // Else we are using branch feature... always need a branch name.
        if (branchName == null) {
            throw MessageException.of("'sonar.branch.name' is required for a branch analysis");
        }

        // Basic sanity checks.
        if (branchName.equals(branchTarget)) {
            throw MessageException.of("'sonar.branch.name' cannot be the same as the 'sonar.branch.target'");
        }

        // Infer branch target.
        if (branchTarget == null) {
            String defaultBranchName = branches.defaultBranchName();
            // Use default branch.
            if (defaultBranchName != null && !branchName.equals(defaultBranchName)) {
                branchTarget = defaultBranchName;
                LOG.debug("missing sonar.branch.target set to {}", branchTarget);
            }
        }

        // Fetch existing target.
        if (branchTarget != null) {
            BranchInfo branchTargetInfo = getBranchInfo(branches, branchTarget);

            if (branchTargetInfo.type() == BranchType.PULL_REQUEST) {
                // If we're merging into a non-master type branch...
                if (branchTargetInfo.branchTargetName() == null) {
                    // We need to have a target for that one.
                    throw MessageException.of("Target branch is short-lived and has a short-lived branch as a target: "
                            + branchTarget);
                } else {
                    // Use the parent of the target.
                    branchTarget = branchTargetInfo.branchTargetName();
                    LOG.debug("sonar.branch.target set to parent of target: {}", branchTarget);
                }
            }
        } else if (branches.defaultBranchName() == null && !branchName.equals("master")) {
            throw MessageException.of("First time run! Please run main branch on master for the first time");
        } else if (branches.defaultBranchName() == null && branchName.equals("master")) {
            return new DefaultBranchConfiguration();
        }

        /*
          The long living server branch from which we should load project settings/quality profiles/compare changed files/...
          For long living branches, this is the sonar.branch.target (default to default branch) in case of first analysis,
          otherwise it's the branch itself.
          For short living branches, we look at sonar.branch.target (default to default branch). If it exists but is
          a short living branch or PR, we will transitively use its own target.
         */
        String referenceBranchName = branchTarget;

        // Get info of existing branch.
        BranchInfo branchInfo = branches.get(branchName);
        if (branchInfo != null) {
            // Can't merge main branch onto something else.
            if (branchTarget != null && branchInfo.isMain()) {
                throw MessageException.of("Cannot pass a branch target to the main branch");
            }

            // Check that branch type doesn't change.
            if (branchType != null && branchType != branchInfo.type()) {
                throw MessageException.of("Cannot change branch type as branch already exists");
            } else if (branchType == null) {
                branchType = branchInfo.type();
                LOG.debug("sonar.branch.type set to existing type of: {}", branchType);
            }

            // See above.
            if (branchType == BranchType.BRANCH) {
                referenceBranchName = branchName;
            }
        } else if (branchType == null) {
            branchType = BranchType.BRANCH;
            LOG.debug("sonar.branch.type set: {}", branchType);
        }
        return new CodeScanBranchConfiguration(branchType, branchName, targetScmBranch, referenceBranchName, pullRequestKey);
    }

    private BranchType convertBranchType(String branchType) {
        if ("pull_request".equalsIgnoreCase(branchType)) {
            return BranchType.PULL_REQUEST;
        } else if ("short".equalsIgnoreCase(branchType) || "long".equalsIgnoreCase(branchType) || "branch".equalsIgnoreCase(branchType)) {
            return BranchType.BRANCH;
        } else if (branchType != null && !"".equals(branchType)) {
            throw MessageException.of("'sonar.branch.type' is invalid. Must be 'short' or 'long' or 'branch'");
        }
        return null;
    }

    private static BranchInfo getBranchInfo(ProjectBranches branches, String branchTarget) {
        BranchInfo ret = branches.get(branchTarget);
        if (ret == null) {
            throw MessageException
                    .of("Target branch does not exist on server. Run a regular analysis before running a branch analysis");
        } else {
            return ret;
        }
    }
}
