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
package org.sonar.batch.scan.filesystem;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.PathPattern;
import org.sonar.api.scan.filesystem.FileExclusions;

public class ExclusionFilters implements BatchComponent {
  private final FileExclusions exclusionSettings;

  private PathPattern[] mainInclusions;
  private PathPattern[] mainExclusions;
  private PathPattern[] testInclusions;
  private PathPattern[] testExclusions;

  public ExclusionFilters(FileExclusions exclusions) {
    this.exclusionSettings = exclusions;
  }

  public void prepare() {
    mainInclusions = prepareMainInclusions();
    mainExclusions = prepareMainExclusions();
    testInclusions = prepareTestInclusions();
    testExclusions = prepareTestExclusions();
    log("Included sources: ", mainInclusions);
    log("Excluded sources: ", mainExclusions);
    log("Included tests: ", testInclusions);
    log("Excluded tests: ", testExclusions);
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

  public boolean accept(InputFile inputFile, InputFile.Type type) {
    PathPattern[] inclusionPatterns = null;
    PathPattern[] exclusionPatterns = null;
    if (InputFile.Type.MAIN == type) {
      inclusionPatterns = mainInclusions;
      exclusionPatterns = mainExclusions;
    } else if (InputFile.Type.TEST == type) {
      inclusionPatterns = testInclusions;
      exclusionPatterns = testExclusions;
    }
    if (inclusionPatterns.length > 0) {
      boolean matchInclusion = false;
      for (PathPattern pattern : inclusionPatterns) {
        matchInclusion |= pattern.match(inputFile);
      }
      if (!matchInclusion) {
        return false;
      }
    }
    if (exclusionPatterns.length > 0) {
      for (PathPattern pattern : exclusionPatterns) {
        if (pattern.match(inputFile)) {
          return false;
        }
      }
    }
    return true;
  }

  PathPattern[] prepareMainInclusions() {
    if (exclusionSettings.sourceInclusions().length > 0) {
      // User defined params
      return PathPattern.create(exclusionSettings.sourceInclusions());
    }
    return new PathPattern[0];
  }

  PathPattern[] prepareTestInclusions() {
    return PathPattern.create(computeTestInclusions());
  }

  private String[] computeTestInclusions() {
    if (exclusionSettings.testInclusions().length > 0) {
      // User defined params
      return exclusionSettings.testInclusions();
    }
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  PathPattern[] prepareMainExclusions() {
    String[] patterns = (String[]) ArrayUtils.addAll(
      exclusionSettings.sourceExclusions(), computeTestInclusions());
    return PathPattern.create(patterns);
  }

  PathPattern[] prepareTestExclusions() {
    return PathPattern.create(exclusionSettings.testExclusions());
  }
}
