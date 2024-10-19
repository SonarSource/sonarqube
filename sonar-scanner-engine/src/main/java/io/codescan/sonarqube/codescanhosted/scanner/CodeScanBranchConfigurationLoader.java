/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.MessageException;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationLoader;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.DefaultBranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

/**
 * This class is a loader to return a branch configuration to use for the current build...
 */
public class CodeScanBranchConfigurationLoader implements BranchConfigurationLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CodeScanBranchConfigurationLoader.class);

    private static final String BRANCH_TYPE = "sonar.branch.type";

    private static final Set<String> BRANCH_ANALYSIS_PARAMETERS =
            new HashSet<>(Collections.singletonList(ScannerProperties.BRANCH_NAME));

    private static final Set<String> PULL_REQUEST_ANALYSIS_PARAMETERS = new HashSet<>(
            Arrays.asList(ScannerProperties.PULL_REQUEST_BRANCH, ScannerProperties.PULL_REQUEST_KEY,
                    ScannerProperties.PULL_REQUEST_BASE));

    private static final Set<String> COMPARISON_BRANCH_ANALYSIS_PARAMETERS = new HashSet<>(
            Arrays.asList(ScannerProperties.COMPARISON_BRANCH, ScannerProperties.COMPARISON_BASE));

    @Override
    public BranchConfiguration load(Map<String, String> projectSettings, ProjectBranches branches) {

        if (BRANCH_ANALYSIS_PARAMETERS.stream().anyMatch(projectSettings::containsKey)) {
            return createBranchConfiguration(projectSettings, branches);
        } else if (PULL_REQUEST_ANALYSIS_PARAMETERS.stream().anyMatch(projectSettings::containsKey)) {
            return createPullRequestConfiguration(projectSettings, branches);
        } else if (COMPARISON_BRANCH_ANALYSIS_PARAMETERS.stream().anyMatch(projectSettings::containsKey)) {
            return createComparisonBranchConfiguration(projectSettings, branches);
        }

        return new DefaultBranchConfiguration();
    }

    private BranchConfiguration createBranchConfiguration(Map<String, String> projectSettings,
            ProjectBranches branches) {
        String branchName = StringUtils.trimToNull(projectSettings.get(ScannerProperties.BRANCH_NAME));
        String branchTarget = StringUtils.trimToNull(projectSettings.get(ScannerProperties.BRANCH_TARGET /* TODO */));
        String targetScmBranch = branchTarget;
        BranchType branchType = convertBranchType(projectSettings.get(BRANCH_TYPE));

        // No branch config. Use default settings.
        if (branchName == null && branchTarget == null) {
            return new DefaultBranchConfiguration();
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
        return new CodeScanBranchConfiguration(branchType, branchName, targetScmBranch, referenceBranchName, null);
    }

    private static BranchConfiguration createPullRequestConfiguration(Map<String, String> projectSettings,
            ProjectBranches branches) {
        String pullRequestKey = projectSettings.get(ScannerProperties.PULL_REQUEST_KEY);
        String pullRequestBranch = projectSettings.get(ScannerProperties.PULL_REQUEST_BRANCH);
        String pullRequestBase = projectSettings.get(ScannerProperties.PULL_REQUEST_BASE);

        if (null == pullRequestBase || pullRequestBase.isEmpty()) {
            return new CodeScanBranchConfiguration(BranchType.PULL_REQUEST, pullRequestBranch,
                    branches.defaultBranchName(), branches.defaultBranchName(), pullRequestKey);
        } else {
            return new CodeScanBranchConfiguration(BranchType.PULL_REQUEST, pullRequestBranch,
                    Optional.ofNullable(branches.get(pullRequestBase))
                            .map(b -> pullRequestBase)
                            .orElse(null),
                    pullRequestBase, pullRequestKey);
        }
    }

    private static BranchConfiguration createComparisonBranchConfiguration(Map<String, String> projectSettings,
            ProjectBranches branches) {
        String comparisonBranchName = projectSettings.get(ScannerProperties.COMPARISON_BRANCH);
        String comparisonBranchBase = projectSettings.get(ScannerProperties.COMPARISON_BASE);

        if (null == comparisonBranchBase || comparisonBranchBase.isEmpty()) {
            return new CodeScanBranchConfiguration(BranchType.PULL_REQUEST, comparisonBranchName,
                    branches.defaultBranchName(), branches.defaultBranchName(), comparisonBranchName);
        } else {
            return new CodeScanBranchConfiguration(BranchType.PULL_REQUEST, comparisonBranchName,
                    Optional.ofNullable(branches.get(comparisonBranchBase))
                            .map(b -> comparisonBranchBase)
                            .orElse(null),
                    comparisonBranchBase, comparisonBranchName);
        }
    }

    private BranchType convertBranchType(String branchType) {
        if ("pull_request".equalsIgnoreCase(branchType)) {
            return BranchType.PULL_REQUEST;
        } else if ("short".equalsIgnoreCase(branchType) || "long".equalsIgnoreCase(branchType)
                || "branch".equalsIgnoreCase(branchType)) {
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
