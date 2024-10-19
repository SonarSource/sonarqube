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
package org.sonar.scanner.scan.branch;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class DefaultBranchConfiguration implements BranchConfiguration {
  @Override
  public BranchType branchType() {
    return BranchType.BRANCH;
  }

  @CheckForNull
  @Override
  public String branchName() {
    return null;
  }

  @CheckForNull
  @Override
  public String targetBranchName() {
    return null;
  }

  @CheckForNull
  @Override
  public String referenceBranchName() {
    return null;
  }

  @Override
  public String pullRequestKey() {
    throw new IllegalStateException("Only a branch of type PULL_REQUEST can have a pull request id.");
  }
}
