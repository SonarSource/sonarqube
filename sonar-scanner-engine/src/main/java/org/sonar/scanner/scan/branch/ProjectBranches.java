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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * Container class for information about the branches of a project.
 */
@Immutable
public class ProjectBranches {

  private final Map<String, BranchInfo> branches;

  public ProjectBranches(List<BranchInfo> branchInfos) {
    branches = branchInfos.stream().collect(Collectors.toMap(BranchInfo::name, Function.identity()));
  }

  @CheckForNull
  public BranchInfo get(String name) {
    return branches.get(name);
  }

  public boolean isEmpty() {
    return branches.isEmpty();
  }
}
