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
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.core.config.ScannerProperties;

public class ProjectBranches {

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

  private final BranchType branchType;

  @Nullable
  private final String branchName;

  @Nullable
  private final String branchTarget;

  private ProjectBranches(BranchType branchType, @Nullable String branchName, @Nullable String branchTarget) {
    this.branchType = branchType;
    this.branchName = branchName;
    this.branchTarget = branchTarget;
  }

  static ProjectBranches create(Configuration settings, List<BranchInfo> branchInfos) {
    String branchName = settings.get(ScannerProperties.BRANCH_NAME).orElse("");
    if (branchName.isEmpty()) {
      return new ProjectBranches(BranchType.LONG, null, null);
    }

    Map<String, BranchInfo> branches = branchInfos.stream().collect(Collectors.toMap(b -> b.name, Function.identity()));

    BranchType branchType;

    BranchInfo info = branches.get(branchName);
    if (info != null) {
      branchType = info.type;
    } else {
      String longLivedBranchesRegex = settings.get(CoreProperties.LONG_LIVED_BRANCHES_REGEX)
        .orElseThrow(() -> new IllegalStateException("Property must exist: " + CoreProperties.LONG_LIVED_BRANCHES_REGEX));

      if (branchName.matches(longLivedBranchesRegex)) {
        branchType = BranchType.LONG;
      } else {
        branchType = BranchType.SHORT;
      }
    }

    String branchTarget = StringUtils.trimToNull(settings.get(ScannerProperties.BRANCH_TARGET).orElse(null));
    if (branchTarget != null && !branches.containsKey(branchTarget)) {
      throw new IllegalStateException("Target branch does not exist on server: " + branchTarget);
    }

    return new ProjectBranches(branchType, branchName, branchTarget);
  }

  public BranchType branchType() {
    return branchType;
  }

  public String branchName() {
    return branchName;
  }

  public String branchTarget() {
    return branchTarget;
  }
}
