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
package org.sonar.server.computation.task.projectanalysis.component;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.MessageException;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.component.BranchType;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

/**
 * The default (and legacy) implementation of {@link Branch}. It is used
 * when scanner is configured with parameter "sonar.branch".
 * A legacy branch is implemented as a fork of the project, so any branch
 * is considered as "main".
 */
public class MainBranchImpl implements Branch {

  @Nullable
  private final String branchName;

  public MainBranchImpl(@Nullable String name) {
    this.branchName = name;
    if (name != null && !ComponentKeys.isValidBranch(name)) {
      throw MessageException.of(format("\"%s\" is not a valid branch name. "
        + "Allowed characters are alphanumeric, '-', '_', '.' and '/'.", name));
    }
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
  public Optional<String> getName() {
    return Optional.ofNullable(branchName);
  }

  @Override
  public boolean supportsCrossProjectCpd() {
    // only on regular project, not on branches
    return branchName == null;
  }

  @Override
  public String generateKey(ScannerReport.Component module, @Nullable ScannerReport.Component fileOrDir) {
    String moduleWithBranch =  ComponentKeys.createKey(module.getKey(), branchName);
    if (fileOrDir == null || isEmpty(fileOrDir.getPath())) {
      return moduleWithBranch;
    }
    return ComponentKeys.createEffectiveKey(moduleWithBranch, trimToNull(fileOrDir.getPath()));
  }
}
