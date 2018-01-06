/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan.filesystem;

import java.nio.file.Path;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.PathPattern;
import org.sonar.api.scan.filesystem.FileExclusions;

@ScannerSide
public class ExclusionFilters {

  private static final Logger LOG = LoggerFactory.getLogger(ExclusionFilters.class);

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

  public boolean hasPattern() {
    return mainInclusions.length > 0 || mainExclusions.length > 0 || testInclusions.length > 0 || testExclusions.length > 0;
  }

  private static void log(String title, PathPattern[] patterns) {
    if (patterns.length > 0) {
      LOG.info(title);
      for (PathPattern pattern : patterns) {
        LOG.info("  {}", pattern);
      }
    }
  }

  public boolean accept(Path absolutePath, Path relativePath, InputFile.Type type) {
    PathPattern[] inclusionPatterns;
    PathPattern[] exclusionPatterns;
    if (InputFile.Type.MAIN == type) {
      inclusionPatterns = mainInclusions;
      exclusionPatterns = mainExclusions;
    } else if (InputFile.Type.TEST == type) {
      inclusionPatterns = testInclusions;
      exclusionPatterns = testExclusions;
    } else {
      throw new IllegalArgumentException("Unknown file type: " + type);
    }

    if (inclusionPatterns.length > 0) {
      boolean matchInclusion = false;
      for (PathPattern pattern : inclusionPatterns) {
        matchInclusion |= pattern.match(absolutePath, relativePath);
      }
      if (!matchInclusion) {
        return false;
      }
    }
    if (exclusionPatterns.length > 0) {
      for (PathPattern pattern : exclusionPatterns) {
        if (pattern.match(absolutePath, relativePath)) {
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
