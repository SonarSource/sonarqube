/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.SonarException;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * This class aims at validating project reactor
 * @since 3.6
 */
public class ProjectReactorValidator {

  private static final String SONAR_PHASE = "sonar.phase";
  private final Settings settings;
  private final ResourceDao resourceDao;

  public ProjectReactorValidator(Settings settings, @Nullable ResourceDao resourceDao) {
    this.settings = settings;
    this.resourceDao = resourceDao;
  }

  public ProjectReactorValidator(Settings settings) {
    this(settings, null);
  }

  public void validate(ProjectReactor reactor) {
    preventAutomaticProjectCreationIfNeeded(reactor);

    String branch = settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
    String rootProjectKey = ComponentKeys.createKey(reactor.getRoot().getKey(), branch);

    List<String> validationMessages = new ArrayList<String>();
    checkDeprecatedProperties(validationMessages);

    for (ProjectDefinition moduleDef : reactor.getProjects()) {
      validateModule(moduleDef, validationMessages, branch, rootProjectKey);
    }

    validateBranch(validationMessages, branch);

    if (!validationMessages.isEmpty()) {
      throw new SonarException("Validation of project reactor failed:\n  o " + Joiner.on("\n  o ").join(validationMessages));
    }
  }

  private void preventAutomaticProjectCreationIfNeeded(ProjectReactor reactor) {
    if (resourceDao != null && settings.getBoolean(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION)) {
      String projectKey = reactor.getRoot().getKeyWithBranch();
      if (resourceDao.findByKey(projectKey) == null) {
        throw new SonarException(String.format("Unable to scan non-existing project \"%s\"", projectKey));
      }
    }
  }

  private void validateModule(ProjectDefinition moduleDef, List<String> validationMessages, @Nullable String branch, String rootProjectKey) {
    if (!ComponentKeys.isValidModuleKey(moduleDef.getKey())) {
      validationMessages.add(String.format("\"%s\" is not a valid project or module key. "
        + "Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.", moduleDef.getKey()));
    } else if (resourceDao != null && isSubProject(moduleDef)) {
      // SONAR-4692 Validate root project is the same than previous analysis to avoid module with same key in different projects
      String moduleKey = ComponentKeys.createKey(moduleDef.getKey(), branch);
      ResourceDto rootInDB = resourceDao.getRootProjectByComponentKey(moduleKey);
      if (rootInDB == null || Qualifiers.LIBRARY.equals(rootInDB.getQualifier())) {
        // This is a new module or previously a library so OK
        return;
      }
      if (rootInDB.getKey().equals(moduleKey)) {
        // SONAR-4245 current subproject is actually a root project in SQ DB
        throw new SonarException(
          String.format("The project '%s' is already defined in SonarQube but not as a module of project '%s'. "
            + "If you really want to stop directly analysing project '%s', please first delete it from SonarQube and then relaunch the analysis of project '%s'.",
            moduleKey, rootProjectKey, moduleKey, rootProjectKey));
      }
      if (!rootProjectKey.equals(rootInDB.getKey())) {
        // SONAR-4692 current subproject is already a subproject in another project
        throw new SonarException(String.format("Module \"%s\" is already part of project \"%s\"", moduleDef.getKey(), rootInDB.getKey()));
      }
    }
  }

  private void checkDeprecatedProperties(List<String> validationMessages) {
    if (settings.getString(SONAR_PHASE) != null) {
      validationMessages.add(String.format("Property \"%s\" is deprecated. Please remove it from your configuration.", SONAR_PHASE));
    }
  }

  private boolean isSubProject(ProjectDefinition moduleDef) {
    return moduleDef.getParent() != null;
  }

  private void validateBranch(List<String> validationMessages, @Nullable String branch) {
    if (StringUtils.isNotEmpty(branch) && !ComponentKeys.isValidBranch(branch)) {
      validationMessages.add(String.format("\"%s\" is not a valid branch name. "
        + "Allowed characters are alphanumeric, '-', '_', '.' and '/'.", branch));
    }
  }

}
