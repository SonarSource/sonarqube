/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;

/**
 * Exclude the sub-projects as defined by the properties sonar.skippedModules and sonar.includedModules
 *
 * @since 2.12
 */
@InstantiationStrategy(InstantiationStrategy.BOOTSTRAP)
public class ProjectExclusions implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectExclusions.class);

  private Settings settings;
  private ProjectReactor reactor;

  public ProjectExclusions(Settings settings, ProjectReactor reactor) {
    this.settings = settings;
    this.reactor = reactor;
  }

  public void start() {
    LOG.debug("Apply project exclusions");
    for (ProjectDefinition project : reactor.getProjects()) {
      if (isExcluded(key(project), project == reactor.getRoot())) {
        exclude(project);
      }
    }
  }

  private boolean isExcluded(String projectKey, boolean isRoot) {
    String[] includedKeys = settings.getStringArray("sonar.includedModules");
    boolean excluded = false;
    if (!isRoot && includedKeys.length > 0) {
      excluded = !ArrayUtils.contains(includedKeys, projectKey);
    }
    if (!excluded) {
      String[] excludedKeys = settings.getStringArray("sonar.skippedModules");
      excluded = ArrayUtils.contains(excludedKeys, projectKey);
    }
    if (excluded && isRoot) {
      throw new IllegalArgumentException("The root project can't be excluded. Please check the parameters sonar.skippedModules and sonar.includedModules.");
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
