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
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

/**
 * Filter projects to analyze by using the properties sonar.skippedModules and sonar.includedModules
 *
 * @since 2.12
 */
public class ProjectFilter {

  private Settings settings;

  public ProjectFilter(Settings settings) {
    this.settings = settings;
  }

  public boolean isExcluded(Project project) {
    Project p = project;
    while (p != null) {
      if (isExcludedModule(getArtifactId(p), p.isRoot())) {
        return true;
      }
      p = p.getParent();
    }
    return false;
  }

  private boolean isExcludedModule(String artifactId, boolean isRoot) {
    String[] includedArtifactIds = settings.getStringArray("sonar.includedModules");
    boolean excluded = false;
    if (!isRoot && includedArtifactIds.length > 0) {
      excluded = !ArrayUtils.contains(includedArtifactIds, artifactId);
    }
    if (!excluded) {
      String[] excludedArtifactIds = settings.getStringArray("sonar.skippedModules");
      excluded = ArrayUtils.contains(excludedArtifactIds, artifactId);
    }
    if (excluded && isRoot) {
      throw new IllegalArgumentException("The root module can't be skipped. Please check the parameter sonar.skippedModules.");
    }
    return excluded;
  }

  // TODO see http://jira.codehaus.org/browse/SONAR-2324
  static String getArtifactId(Project project) {
    String key = project.getKey();
    if (StringUtils.isNotBlank(project.getBranch())) {
      // remove branch part
      key = StringUtils.removeEnd(project.getKey(), ":" + project.getBranch());
    }
    if (key.contains(":")) {
      return StringUtils.substringAfter(key, ":");
    }
    return key;
  }
}
