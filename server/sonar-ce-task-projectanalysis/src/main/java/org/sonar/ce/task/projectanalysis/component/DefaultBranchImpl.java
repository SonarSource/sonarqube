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
package org.sonar.ce.task.projectanalysis.component;

import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

/**
 * Implementation of {@link Branch} for default/main branch. It is used
 * when no branch is provided as a scanner parameter or if the branch plugin is not installed.
 */
public class DefaultBranchImpl implements Branch {
  private final String branchName;

  public DefaultBranchImpl() {
    this.branchName = BranchDto.DEFAULT_MAIN_BRANCH_NAME;
  }

  @Override
  public BranchType getType() {
    return BranchType.LONG;
  }

  @Override
  public boolean isMain() {
    return true;
  }

  @Override
  public String getMergeBranchUuid() {
    throw new IllegalStateException("Not valid for the main branch");
  }

  @Override
  public String getName() {
    return branchName;
  }

  @Override
  public boolean supportsCrossProjectCpd() {
    return true;
  }

  @Override
  public String getPullRequestKey() {
    throw new IllegalStateException("Only a branch of type PULL_REQUEST can have a pull request id.");
  }

  @Override
  public String getTargetBranchName() {
    throw new IllegalStateException("Only on a short lived branch or pull request");
  }

  @Override
  public String generateKey(String projectKey, @Nullable String fileOrDirPath) {
    if (isEmpty(fileOrDirPath)) {
      return projectKey;
    }
    return ComponentKeys.createEffectiveKey(projectKey, trimToNull(fileOrDirPath));
  }
}
