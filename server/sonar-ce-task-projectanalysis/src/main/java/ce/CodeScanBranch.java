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
package ce;

import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.component.BranchType;

/**
 * Dto for the branch name.
 */
class CodeScanBranch implements Branch {

    private final String branchName;
    private final BranchType branchType;
    private final boolean isMain;
    private final String referenceBranchUuid;
    private final String pullRequestKey;
    private final String targetBranchName;

    CodeScanBranch(final String branchName, final BranchType branchType, final boolean isMain,
            @Nullable final String referenceBranchUuid, final String pullRequestKey, final String targetBranchName) {
        this.branchName = branchName;
        this.branchType = branchType;
        this.isMain = isMain;
        this.referenceBranchUuid = referenceBranchUuid;
        this.pullRequestKey = pullRequestKey;
        this.targetBranchName = targetBranchName;

        //kee is 255 long...
        if (branchName.length() > 255) {
            throw MessageException.of(String
                    .format("Illegal branch name: '%s'. Branch name must be less than 255 characters.", branchName));
        }

        //:BRANCH: is used to separate the project key, so it can't contain it
        if (StringUtils.contains(branchName, ":BRANCH:")) {
            throw MessageException
                    .of(String.format("Illegal branch name: '%s'. Branch name cannot contain ':BRANCH:'.", branchName));
        }
    }

    public String getName() {
        return this.branchName;
    }

    public BranchType getType() {
        return this.branchType;
    }

    public boolean isMain() {
        return this.isMain;
    }

    @Override
    public String getReferenceBranchUuid() {
        return this.referenceBranchUuid;
    }

    @Override
    public boolean supportsCrossProjectCpd() {
        return isMain;
    }

    @Override
    public String getPullRequestKey() {
        if (BranchType.PULL_REQUEST != branchType) {
            throw new IllegalStateException("Only a branch of type PULL_REQUEST can have a pull request ID");
        }
        return pullRequestKey;
    }

    @Override
    public String getTargetBranchName() {
        return targetBranchName;
    }

    public String toString() {
        return "CodeScanBranch{branchName=" + branchName + ", targetBranchUuid=" + referenceBranchUuid + ",branchType="
                + branchType + ",isMain=" + isMain + '}';
    }

}
