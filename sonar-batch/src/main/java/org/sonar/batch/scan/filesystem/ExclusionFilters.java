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
package org.sonar.batch.scan.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.ResourceFilter;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.api.scan.filesystem.FileSystemFilter;
import org.sonar.api.scan.filesystem.FileType;

import java.io.File;

public class ExclusionFilters implements FileSystemFilter, ResourceFilter, BatchComponent {
  private final FileExclusions exclusionSettings;

  public ExclusionFilters(FileExclusions exclusions) {
    this.exclusionSettings = exclusions;
    log("Included sources: ", sourceInclusions());
    log("Excluded sources: ", sourceExclusions());
    log("Included tests: ", testInclusions());
    log("Excluded tests: ", testExclusions());
  }


  private void log(String title, PathPattern[] patterns) {
    if (patterns.length > 0) {
      Logger log = LoggerFactory.getLogger(ExclusionFilters.class);
      log.info(title);
      for (PathPattern pattern : patterns) {
        log.info("  " + pattern);
      }
    }
  }

  public boolean accept(File file, Context context) {
    PathPattern[] inclusionPatterns = (context.type() == FileType.TEST ? testInclusions() : sourceInclusions());
    if (inclusionPatterns.length > 0) {
      boolean matchInclusion = false;
      for (PathPattern pattern : inclusionPatterns) {
        matchInclusion |= pattern.match(context);
      }
      if (!matchInclusion) {
        return false;
      }
    }
    PathPattern[] exclusionPatterns = (context.type() == FileType.TEST ? testExclusions() : sourceExclusions());
    for (PathPattern pattern : exclusionPatterns) {
      if (pattern.match(context)) {
        return false;
      }
    }
    return true;
  }

  public boolean isIgnored(Resource resource) {
    if (ResourceUtils.isFile(resource)) {
      PathPattern[] inclusionPatterns = (ResourceUtils.isUnitTestClass(resource) ? testInclusions() : sourceInclusions());
      if (isIgnoredByInclusions(resource, inclusionPatterns)) {
        return true;
      }
      PathPattern[] exclusionPatterns = (ResourceUtils.isUnitTestClass(resource) ? testExclusions() : sourceExclusions());
      return isIgnoredByExclusions(resource, exclusionPatterns);
    }
    return false;
  }

  private boolean isIgnoredByInclusions(Resource resource, PathPattern[] inclusionPatterns) {
    if (inclusionPatterns.length > 0) {
      boolean matchInclusion = false;
      boolean supportResource = false;
      for (PathPattern pattern : inclusionPatterns) {
        if (pattern.supportResource()) {
          supportResource = true;
          matchInclusion |= pattern.match(resource);
        }
      }
      if (supportResource && !matchInclusion) {
        return true;
      }
    }
    return false;
  }

  private boolean isIgnoredByExclusions(Resource resource, PathPattern[] exclusionPatterns) {
    for (PathPattern pattern : exclusionPatterns) {
      if (pattern.supportResource() && pattern.match(resource)) {
        return true;
      }
    }
    return false;
  }

  PathPattern[] sourceInclusions() {
    return PathPattern.create(exclusionSettings.sourceInclusions());
  }

  PathPattern[] testInclusions() {
    return PathPattern.create(exclusionSettings.testInclusions());
  }

  PathPattern[] sourceExclusions() {
    return PathPattern.create(exclusionSettings.sourceExclusions());
  }

  PathPattern[] testExclusions() {
    return PathPattern.create(exclusionSettings.testExclusions());
  }
}
