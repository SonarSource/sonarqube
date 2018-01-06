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
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Container class for information about a branch.
 */
@Immutable
public class BranchInfo {
  private final String name;
  private final BranchType type;
  private final boolean isMain;
  @Nullable
  private final String branchTargetName;

  public BranchInfo(String name, BranchType type, boolean isMain, @Nullable String branchTargetName) {
    this.name = name;
    this.type = type;
    this.isMain = isMain;
    this.branchTargetName = branchTargetName;
  }

  @CheckForNull
  public String branchTargetName() {
    return branchTargetName;
  }

  public String name() {
    return name;
  }

  public BranchType type() {
    return type;
  }

  public boolean isMain() {
    return isMain;
  }
}
