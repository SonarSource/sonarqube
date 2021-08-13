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

package io.codescan.sonarqube.codescanhosted.ce;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.annotations.Nullable;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;

/**
 * Dto for the branch name.
 */
class CodeScanBranch implements Branch {

    private final String branchName;
    private final String referenceBranchUuid;
    private final String targetBranchName;
    private final BranchType branchType;
    private final boolean isMain;

    CodeScanBranch(final String branchName, @Nullable final String referenceBranchUuid, final BranchType branchType,
            final boolean isMain, final String targetBranchName) {
        this.branchName = branchName;
        this.referenceBranchUuid = referenceBranchUuid;
        this.branchType = branchType;
        this.isMain = isMain;
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

    public BranchType getType() {
        return this.branchType;
    }

    @Override
    public String getReferenceBranchUuid() {
        return this.referenceBranchUuid;
    }

    @Override
    public String getTargetBranchName() {
        return targetBranchName;
    }

    public boolean isMain() {
        return this.isMain;
    }

    public String getName() {
        return this.branchName;
    }

    public boolean supportsCrossProjectCpd() {
        return this.isMain;
    }

    public String generateKey(String projectKey, @javax.annotation.Nullable String fileOrDirPath) {
        String key;
        if (isEmpty(fileOrDirPath)) {
            key = projectKey;
        } else {
            key = ComponentKeys.createEffectiveKey(projectKey, trimToNull(fileOrDirPath));
        }
        return generateKey(key);

    }

    public String generateKey(String projectKey) {
        if (this.isMain) {
            return projectKey;
        }
        return ComponentDto.generateBranchKey(projectKey, this.branchName);
    }

    @Override
    public String getPullRequestKey() {
        return null;
    }

    public String toString() {
        return "CodeScanBranch{branchName=" + branchName + ", targetBranchUuid=" + referenceBranchUuid + ",branchType="
                + branchType + ",isMain=" + isMain + '}';
    }

}
