/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;

public class ProjectBranches {
  private final String longLivedBranchesRegex;
  private final Map<String, BranchInfo> branches;

  public ProjectBranches(Configuration settings, List<BranchInfo> branchInfos) {
    this.longLivedBranchesRegex = settings.get(CoreProperties.LONG_LIVED_BRANCHES_REGEX).orElse("");
    this.branches = branchInfos.stream().collect(Collectors.toMap(b -> b.name, Function.identity()));
  }

  public enum BranchType {
    SHORT, LONG
  }

  static class BranchInfo {
    private final String name;
    private final BranchType type;

    BranchInfo(String name, boolean isLong) {
      this.name = name;
      if (isLong) {
        this.type = BranchType.LONG;
      } else {
        this.type = BranchType.SHORT;
      }
    }
  }

  public BranchType getBranchType(String branchName) {
    BranchInfo branch = branches.get(branchName);
    if (branch != null) {
      return branch.type;
    }

    return branchName.matches(longLivedBranchesRegex)
      ? BranchType.LONG
      : BranchType.SHORT;
  }
}
