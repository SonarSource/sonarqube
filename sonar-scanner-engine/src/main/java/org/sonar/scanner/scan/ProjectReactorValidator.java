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
package org.sonar.scanner.scan;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.MessageException;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.scan.branch.BranchParamsValidator;

/**
 * This class aims at validating project reactor
 * @since 3.6
 */
public class ProjectReactorValidator {
  private final AnalysisMode mode;
  private final GlobalConfiguration settings;

  // null = branch plugin is not available
  @Nullable
  private final BranchParamsValidator branchParamsValidator;

  public ProjectReactorValidator(AnalysisMode mode, GlobalConfiguration settings, @Nullable BranchParamsValidator branchParamsValidator) {
    this.mode = mode;
    this.settings = settings;
    this.branchParamsValidator = branchParamsValidator;
  }

  public ProjectReactorValidator(AnalysisMode mode, GlobalConfiguration settings) {
    this(mode, settings, null);
  }

  public void validate(ProjectReactor reactor) {
    List<String> validationMessages = new ArrayList<>();

    for (ProjectDefinition moduleDef : reactor.getProjects()) {
      if (mode.isIssues()) {
        validateModuleIssuesMode(moduleDef, validationMessages);
      } else {
        validateModule(moduleDef, validationMessages);
      }
    }

    String deprecatedBranchName = reactor.getRoot().getBranch();

    if (branchParamsValidator != null) {
      // branch plugin is present
      branchParamsValidator.validate(validationMessages, deprecatedBranchName);
    } else {
      validateBranchParamsWhenPluginAbsent(validationMessages);
    }

    validateBranch(validationMessages, deprecatedBranchName);

    if (!validationMessages.isEmpty()) {
      throw MessageException.of("Validation of project reactor failed:\n  o " + Joiner.on("\n  o ").join(validationMessages));
    }
  }

  private void validateBranchParamsWhenPluginAbsent(List<String> validationMessages) {
    for (String param : Arrays.asList(ScannerProperties.BRANCH_NAME, ScannerProperties.BRANCH_TARGET)) {
      if (StringUtils.isNotEmpty(settings.get(param).orElse(null))) {
        validationMessages.add(String.format("To use the property \"%s\", the branch plugin is required but not installed. "
          + "See the documentation of branch support: %s.", param, ScannerProperties.BRANCHES_DOC_LINK));
      }
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
    String originalVersion = moduleDef.getOriginalVersion();
    if (originalVersion != null && originalVersion.length() > 100) {
      validationMessages.add(String.format("\"%s\" is not a valid version name for module \"%s\". " +
        "The maximum length for version numbers is 100 characters.", originalVersion, moduleDef.getKey()));
    }
  }

  private static void validateBranch(List<String> validationMessages, @Nullable String branch) {
    if (StringUtils.isNotEmpty(branch) && !ComponentKeys.isValidBranch(branch)) {
      validationMessages.add(String.format("\"%s\" is not a valid branch name. "
        + "Allowed characters are alphanumeric, '-', '_', '.' and '/'.", branch));
    }
  }

}
