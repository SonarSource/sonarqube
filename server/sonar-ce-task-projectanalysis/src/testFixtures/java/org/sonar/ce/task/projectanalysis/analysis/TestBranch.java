/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.analysis;

import javax.annotation.Nullable;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.component.BranchType;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class TestBranch implements Branch {
  private final String name;

  public TestBranch(String name) {
    this.name = name;
  }

  @Override
  public BranchType getType() {
    return BranchType.BRANCH;
  }

  @Override
  public boolean isMain() {
    return false;
  }

  @Override
  public String getReferenceBranchUuid() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean supportsCrossProjectCpd() {
    return false;
  }

  @Override
  public String getPullRequestKey() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getTargetBranchName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String generateKey(String projectKey, @Nullable String fileOrDirPath) {
    if (isEmpty(fileOrDirPath)) {
      return projectKey;
    }
    return ComponentKeys.createEffectiveKey(projectKey, trimToNull(fileOrDirPath));
  }
}
