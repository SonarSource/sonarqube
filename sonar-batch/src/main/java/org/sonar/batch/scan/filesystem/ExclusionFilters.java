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
package org.sonar.batch.scan.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.ResourceFilter;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.InputFileFilter;

public class ExclusionFilters implements InputFileFilter, ResourceFilter, BatchComponent {
  private final FileExclusions exclusionSettings;

  public ExclusionFilters(FileExclusions exclusions) {
    this.exclusionSettings = exclusions;
  }

  public void start() {
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

  @Override
  public boolean accept(InputFile inputFile) {
    String type = inputFile.attribute(InputFile.ATTRIBUTE_TYPE);
    PathPattern[] inclusionPatterns = null;
    PathPattern[] exclusionPatterns = null;
    if (InputFile.TYPE_SOURCE.equals(type)) {
      inclusionPatterns = sourceInclusions();
      exclusionPatterns = sourceExclusions();
    } else if (InputFile.TYPE_TEST.equals(type)) {
      inclusionPatterns = testInclusions();
      exclusionPatterns = testExclusions();
    }
    if (inclusionPatterns != null && inclusionPatterns.length > 0) {
      boolean matchInclusion = false;
      for (PathPattern pattern : inclusionPatterns) {
        matchInclusion |= pattern.match(inputFile);
      }
      if (!matchInclusion) {
        return false;
      }
    }
    if (exclusionPatterns != null && exclusionPatterns.length > 0) {
      for (PathPattern pattern : exclusionPatterns) {
        if (pattern.match(inputFile)) {
          return false;
        }
      }
    }
    return true;
  }


  public boolean isIgnored(Resource resource) {
    if (ResourceUtils.isFile(resource)) {
      PathPattern[] inclusionPatterns = ResourceUtils.isUnitTestClass(resource) ? testInclusions() : sourceInclusions();
      if (isIgnoredByInclusions(resource, inclusionPatterns)) {
        return true;
      }
      PathPattern[] exclusionPatterns = ResourceUtils.isUnitTestClass(resource) ? testExclusions() : sourceExclusions();
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
