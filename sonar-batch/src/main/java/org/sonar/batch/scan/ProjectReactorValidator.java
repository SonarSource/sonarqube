/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan;

import org.apache.commons.lang.StringUtils;
import org.sonar.core.component.ComponentKeys;

import com.google.common.base.Joiner;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;
import org.sonar.core.resource.ResourceDao;

import java.util.ArrayList;
import java.util.List;

/**
 * This class aims at validating project reactor
 * @since 3.6
 */
public class ProjectReactorValidator {

  private final Settings settings;
  private final ResourceDao resourceDao;

  public ProjectReactorValidator(Settings settings, ResourceDao resourceDao) {
    this.settings = settings;
    this.resourceDao = resourceDao;
  }

  public void validate(ProjectReactor reactor) {
    if (settings.getBoolean(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION)) {
      String projectKey = reactor.getRoot().getKey();
      if (resourceDao.findByKey(projectKey) == null) {
        throw new SonarException("Unable to scan non-existing project " + projectKey);
      }
    }

    List<String> validationMessages = new ArrayList<String>();
    for (ProjectDefinition moduleDef : reactor.getProjects()) {
      validateModule(moduleDef, validationMessages);
    }

    validateBranch(validationMessages);

    if (!validationMessages.isEmpty()) {
      throw new SonarException("Validation of project reactor failed:\n  o " + Joiner.on("\n  o ").join(validationMessages));
    }
  }

  private void validateModule(ProjectDefinition moduleDef, List<String> validationMessages) {
    validateKey(moduleDef, validationMessages);
  }

  private void validateKey(ProjectDefinition def, List<String> validationMessages) {
    if (!ComponentKeys.isValidModuleKey(def.getKey())) {
      validationMessages.add(String.format("%s is not a valid project or module key", def.getKey()));
    }
  }

  private void validateBranch(List<String> validationMessages) {
    String branch = settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
    if (StringUtils.isNotEmpty(branch) && !ComponentKeys.isValidModuleKey(branch)) {
      validationMessages.add(String.format("%s is not a valid branch name", branch));
    }
  }

}
