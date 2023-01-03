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
package org.sonar.scanner.scan.branch;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

@Immutable
public interface BranchConfiguration {

  /**
   * @return type of the current branch
   */
  BranchType branchType();

  default boolean isPullRequest() {
    return branchType() == BranchType.PULL_REQUEST;
  }

  /**
   * For branches, this is the value of sonar.branch.name, and fallback on the default branch name configured in SQ
   * For PR: the name of the branch containing PR changes (sonar.pullrequest.branch)
   *
   * @return null if the branch feature is not available or no branch was specified.
   */
  @CheckForNull
  String branchName();

  /**
   * The branch from which we should load project settings/quality profiles/compare changed files/...
   * For branches, it's the default branch in case of first analysis, otherwise it's the branch itself.
   * For PR, we look at sonar.pullrequest.base (default to default branch). If it exists and is not a PR we use it. If it exists but is a PR, we will
   * transitively use its own target. If base is not analyzed, we will use default branch.
   *
   * @return null if the branch feature is not available or no branch was specified.
   */
  @CheckForNull
  String referenceBranchName();

  /**
   * For P/Rs, it's the raw value of 'sonar.pullrequest.base'.
   * For branches it's always null.
   * In the scanner side, it will be used by the SCM to compute changed files and changed lines.
   *
   * @return null if the branch feature is not available or if it's not a P/R.
   */
  @CheckForNull
  String targetBranchName();

  /**
   * The key of the pull request.
   *
   * @throws IllegalStateException if this branch configuration is not a pull request.
   */
  String pullRequestKey();

}
