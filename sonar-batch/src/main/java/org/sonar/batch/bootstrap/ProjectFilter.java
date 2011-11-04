/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
      if (isExcluded(getArtifactId(p))) {
        return true;
      }
      p = p.getParent();
    }
    return false;
  }

  private boolean isExcluded(String artifactId) {
    String[] includedArtifactIds = settings.getStringArray("sonar.includedModules");

    if (includedArtifactIds.length > 0) {
      return !ArrayUtils.contains(includedArtifactIds, artifactId);
    }
    String[] excludedArtifactIds = settings.getStringArray("sonar.skippedModules");
    return ArrayUtils.contains(excludedArtifactIds, artifactId);
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
