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
package com.sonarsource.branch;

import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.scan.BranchConfigurationValidator;

public class BranchConfigurationValidatorImpl implements BranchConfigurationValidator {

  private final GlobalConfiguration settings;

  BranchConfigurationValidatorImpl(GlobalConfiguration settings) {
    this.settings = settings;
  }

  @Override
  public void validate(List<String> validationMessages, @Nullable String deprecatedBranch) {
    String branchName = settings.get(ScannerProperties.BRANCH_NAME).orElse(null);
    validateBranchName(validationMessages, branchName);
    validateBranchParams(validationMessages, deprecatedBranch, branchName);
  }

  private static void validateBranchName(List<String> validationMessages, @Nullable String branch) {
    if (StringUtils.isNotEmpty(branch) && !ComponentKeys.isValidBranch(branch)) {
      validationMessages.add(String.format("\"%s\" is not a valid branch name. "
        + "Allowed characters are alphanumeric, '-', '_', '.' and '/'.", branch));
    }
  }

  private static void validateBranchParams(List<String> validationMessages, @Nullable String deprecatedBranch, @Nullable String branchName) {
    if (StringUtils.isNotEmpty(deprecatedBranch) && StringUtils.isNotEmpty(branchName)) {
      validationMessages.add(String.format("The %s parameter must not be used together with the deprecated sonar.branch parameter", ScannerProperties.BRANCH_NAME));
    }
  }
}
