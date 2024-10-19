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

import javax.annotation.CheckForNull;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;

/**
 * The branch configuration that we return from the loader.
 */
public class CodeScanBranchConfiguration implements BranchConfiguration {

    private final BranchType branchType;
    private final String branchName;
    private final String targetBranchName;
    private final String referenceBranchName;
    private final String pullRequestKey;

    public CodeScanBranchConfiguration(BranchType branchType, String branchName, String targetBranchName,
            String referenceBranchName, String pullRequestKey) {
        this.branchType = branchType;
        this.branchName = branchName;
        this.targetBranchName = targetBranchName;
        this.referenceBranchName = referenceBranchName;
        this.pullRequestKey = pullRequestKey;
    }

    @Override
    public BranchType branchType() {
        return branchType;
    }

    @Override
    public String branchName() {
        return branchName;
    }

    @CheckForNull
    @Override
    public String referenceBranchName() {
        return referenceBranchName;
    }

    @CheckForNull
    @Override
    public String targetBranchName() {
        return targetBranchName;
    }

    @Override
    public String pullRequestKey() {
        return pullRequestKey;
    }
}
