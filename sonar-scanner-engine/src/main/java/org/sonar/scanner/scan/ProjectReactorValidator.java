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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.MessageException;
import org.sonar.core.component.ComponentKeys;
import org.sonar.scanner.analysis.DefaultAnalysisMode;

import com.google.common.base.Joiner;

/**
 * This class aims at validating project reactor
 * @since 3.6
 */
public class ProjectReactorValidator {
  private final DefaultAnalysisMode mode;

  public ProjectReactorValidator(DefaultAnalysisMode mode) {
    this.mode = mode;
  }

  public void validate(ProjectReactor reactor) {
    String branch = reactor.getRoot().getBranch();

    List<String> validationMessages = new ArrayList<>();

    for (ProjectDefinition moduleDef : reactor.getProjects()) {
      if (mode.isIssues()) {
        validateModuleIssuesMode(moduleDef, validationMessages);
      } else {
        validateModule(moduleDef, validationMessages);
      }
    }

    validateBranch(validationMessages, branch);

    if (!validationMessages.isEmpty()) {
      throw MessageException.of("Validation of project reactor failed:\n  o " + Joiner.on("\n  o ").join(validationMessages));
    }
  }

  private static void validateModuleIssuesMode(ProjectDefinition moduleDef, List<String> validationMessages) {
    if (!ComponentKeys.isValidModuleKeyIssuesMode(moduleDef.getKey())) {
      validationMessages.add(String.format("\"%s\" is not a valid project or module key. "
        + "Allowed characters in issues mode are alphanumeric, '-', '_', '.', '/' and ':', with at least one non-digit.", moduleDef.getKey()));
    }
  }

  private static void validateModule(ProjectDefinition moduleDef, List<String> validationMessages) {
    if (!ComponentKeys.isValidModuleKey(moduleDef.getKey())) {
      validationMessages.add(String.format("\"%s\" is not a valid project or module key. "
        + "Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.", moduleDef.getKey()));
    }
  }

  private static void validateBranch(List<String> validationMessages, @Nullable String branch) {
    if (StringUtils.isNotEmpty(branch) && !ComponentKeys.isValidBranch(branch)) {
      validationMessages.add(String.format("\"%s\" is not a valid branch name. "
        + "Allowed characters are alphanumeric, '-', '_', '.' and '/'.", branch));
    }
  }

}
