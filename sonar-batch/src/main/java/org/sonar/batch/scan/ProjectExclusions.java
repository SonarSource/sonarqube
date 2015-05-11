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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;

/**
 * Exclude the sub-projects as defined by the properties sonar.skippedModules and sonar.includedModules
 *
 * @since 2.12
 */
public class ProjectExclusions {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectExclusions.class);

  private Settings settings;

  public ProjectExclusions(Settings settings) {
    this.settings = settings;
  }

  public void apply(ProjectReactor reactor) {
    if (!reactor.getProjects().isEmpty() && StringUtils.isNotBlank(reactor.getProjects().get(0).getKey())) {
      LOG.info("Apply project exclusions");

      if (settings.hasKey(CoreProperties.CORE_INCLUDED_MODULES_PROPERTY)) {
        LOG.warn("'sonar.includedModules' property is deprecated since version 4.3 and should not be used anymore.");
      }
      if (settings.hasKey(CoreProperties.CORE_SKIPPED_MODULES_PROPERTY)) {
        LOG.warn("'sonar.skippedModules' property is deprecated since version 4.3 and should not be used anymore.");
      }

      for (ProjectDefinition project : reactor.getProjects()) {
        if (isExcluded(key(project), project == reactor.getRoot())) {
          exclude(project);
        }
      }
    }
  }

  private boolean isExcluded(String projectKey, boolean isRoot) {
    String[] includedKeys = settings.getStringArray(CoreProperties.CORE_INCLUDED_MODULES_PROPERTY);
    boolean excluded = false;
    if (!isRoot && includedKeys.length > 0) {
      excluded = !ArrayUtils.contains(includedKeys, projectKey);
    }
    String skippedModulesProperty = CoreProperties.CORE_SKIPPED_MODULES_PROPERTY;
    if (!excluded) {
      String[] excludedKeys = settings.getStringArray(skippedModulesProperty);
      excluded = ArrayUtils.contains(excludedKeys, projectKey);
    }
    if (excluded && isRoot) {
      throw new IllegalArgumentException("The root project can't be excluded. Please check the parameters " + skippedModulesProperty + " and sonar.includedModules.");
    }
    return excluded;
  }

  private void exclude(ProjectDefinition project) {
    LOG.info(String.format("Exclude project: %s [%s]", project.getName(), project.getKey()));
    project.remove();
  }

  static String key(ProjectDefinition project) {
    String key = project.getKey();
    if (key.contains(":")) {
      return StringUtils.substringAfter(key, ":");
    }
    return key;
  }
}
