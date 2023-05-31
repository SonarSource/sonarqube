/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.MessageException;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.scan.branch.BranchParamsValidator;
import org.springframework.beans.factory.annotation.Autowired;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.sonar.core.component.ComponentKeys.ALLOWED_CHARACTERS_MESSAGE;
import static org.sonar.core.config.ScannerProperties.BRANCHES_DOC_LINK_SUFFIX;
import static org.sonar.core.config.ScannerProperties.BRANCH_NAME;
import static org.sonar.core.config.ScannerProperties.PULL_REQUEST_BASE;
import static org.sonar.core.config.ScannerProperties.PULL_REQUEST_BRANCH;
import static org.sonar.core.config.ScannerProperties.PULL_REQUEST_KEY;

/**
 * This class aims at validating project reactor
 *
 * @since 3.6
 */
public class ProjectReactorValidator {
  private final GlobalConfiguration settings;

  // null = branch plugin is not available
  @Nullable
  private final BranchParamsValidator branchParamsValidator;

  private final DocumentationLinkGenerator documentationLinkGenerator;

  @Autowired(required = false)
  public ProjectReactorValidator(GlobalConfiguration settings, @Nullable BranchParamsValidator branchParamsValidator, DocumentationLinkGenerator documentationLinkGenerator) {
    this.settings = settings;
    this.branchParamsValidator = branchParamsValidator;
    this.documentationLinkGenerator = documentationLinkGenerator;
  }

  @Autowired(required = false)
  public ProjectReactorValidator(GlobalConfiguration settings, DocumentationLinkGenerator documentationLinkGenerator) {
    this(settings, null, documentationLinkGenerator);
  }

  public void validate(ProjectReactor reactor) {
    List<String> validationMessages = new ArrayList<>();

    for (ProjectDefinition moduleDef : reactor.getProjects()) {
      validateModule(moduleDef, validationMessages);
    }

    if (isBranchFeatureAvailable()) {
      branchParamsValidator.validate(validationMessages);
    } else {
      validateBranchParamsWhenPluginAbsent(validationMessages);
      validatePullRequestParamsWhenPluginAbsent(validationMessages);
    }

    if (!validationMessages.isEmpty()) {
      throw MessageException.of("Validation of project failed:\n  o " +
        String.join("\n  o ", validationMessages));
    }
  }

  private void validateBranchParamsWhenPluginAbsent(List<String> validationMessages) {
    if (isNotEmpty(settings.get(BRANCH_NAME).orElse(null))) {
      validationMessages.add(format("To use the property \"%s\" and analyze branches, Developer Edition or above is required. "
        + "See %s for more information.", BRANCH_NAME, documentationLinkGenerator.getDocumentationLink(BRANCHES_DOC_LINK_SUFFIX)));
    }
  }

  private void validatePullRequestParamsWhenPluginAbsent(List<String> validationMessages) {
    Stream.of(PULL_REQUEST_KEY, PULL_REQUEST_BRANCH, PULL_REQUEST_BASE)
      .filter(param -> nonNull(settings.get(param).orElse(null)))
      .forEach(param -> validationMessages.add(format("To use the property \"%s\" and analyze pull requests, Developer Edition or above is required. "
        + "See %s for more information.", param, documentationLinkGenerator.getDocumentationLink(BRANCHES_DOC_LINK_SUFFIX))));
  }

  private static void validateModule(ProjectDefinition projectDefinition, List<String> validationMessages) {
    if (!ComponentKeys.isValidProjectKey(projectDefinition.getKey())) {
      validationMessages.add(format("\"%s\" is not a valid project key. %s.", projectDefinition.getKey(), ALLOWED_CHARACTERS_MESSAGE));
    }
  }

  private boolean isBranchFeatureAvailable() {
    return branchParamsValidator != null;
  }

}
