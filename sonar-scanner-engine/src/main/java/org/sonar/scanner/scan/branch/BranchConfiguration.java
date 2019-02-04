/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
   * The type of the branch we're on, determined by:
   *
   * - If the specified branch exists on the server, then its type
   * - If the branch name matches the pattern of long-lived branches, then it's long-lived
   * - Otherwise it's short-lived
   *
   * @return type of the current branch
   */
  BranchType branchType();

  default boolean isShortOrPullRequest() {
    return branchType() == BranchType.PULL_REQUEST || branchType() == BranchType.SHORT;
  }

  /**
   * For long/short living branches, this is the value of sonar.branch.name, and fallback on the default branch name configured in SQ
   * For PR: the name of the branch containing PR changes (sonar.pullrequest.branch)
   * Only @null if the branch feature is not available.
   */
  @CheckForNull
  String branchName();

  /**
   * The long living server branch from which we should load project settings/quality profiles/compare changed files/...
   * For long living branches, this is the sonar.branch.target (default to default branch) in case of first analysis,
   * otherwise it's the branch itself.
   * For short living branches, we look at sonar.branch.target (default to default branch). If it exists but is a short living branch or PR, we will
   * transitively use its own target.
   * For PR, we look at sonar.pullrequest.base (default to default branch). If it exists but is a short living branch or PR, we will
   * transitively use its own target. If base is not analyzed, we will use default branch.
   * Only @null if the branch feature is not available.
   */
  @CheckForNull
  String longLivingSonarReferenceBranch();

  /**
   * Raw value of sonar.branch.target or sonar.pullrequest.base (fallback to the default branch), will be used by the SCM to compute changed files and changed lines.
   * @null for long living branches and if the branch feature is not available
   */
  @CheckForNull
  String targetScmBranch();

  /**
   * The key of the pull request.
   *
   * @throws IllegalStateException if this branch configuration is not a pull request.
   */
  String pullRequestKey();

}
