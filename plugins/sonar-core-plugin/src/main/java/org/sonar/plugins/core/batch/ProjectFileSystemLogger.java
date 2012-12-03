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
package org.sonar.plugins.core.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Initializer;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

import java.util.Arrays;
import java.util.List;

public class ProjectFileSystemLogger extends Initializer {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectFileSystemLogger.class);

  @Override
  public void execute(Project project) {
    logExclusionPatterns("Excluded sources: {}", project.getExclusionPatterns());
    logExclusionPatterns("Excluded tests: {}", project.getTestExclusionPatterns());

    ProjectFileSystem projectFileSystem = project.getFileSystem();
    logDirectories("Source directories:", projectFileSystem.getSourceDirs());
    logDirectories("Test directories:", projectFileSystem.getTestDirs());
  }

  private void logExclusionPatterns(String message, String[] exclusionPatterns) {
    if (exclusionPatterns != null && exclusionPatterns.length > 0) {
      LOG.info(message, Arrays.toString(exclusionPatterns));
    }
  }

  private void logDirectories(String name, List<java.io.File> dirs) {
    if (!dirs.isEmpty()) {
      LOG.info(name);
      for (java.io.File dir : dirs) {
        LOG.info("  {}", dir);
      }
    }
  }
}
