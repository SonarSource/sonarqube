/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

  default boolean isShortLivingBranch() {
    return branchType() == BranchType.SHORT;
  }

  /**
   * The name of the branch.
   */
  @CheckForNull
  String branchName();

  /**
   * The name of the target branch to merge into.
   */
  @CheckForNull
  String branchTarget();

  /**
   * The name of the base branch to determine project repository and changed files.
   */
  @CheckForNull
  String branchBase();
}
