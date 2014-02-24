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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.PathPattern;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExclusionFilters implements BatchComponent {
  private final FileExclusions exclusionSettings;

  private PathPattern[] sourceInclusions;
  private PathPattern[] sourceExclusions;
  private PathPattern[] testInclusions;
  private PathPattern[] testExclusions;

  public ExclusionFilters(FileExclusions exclusions) {
    this.exclusionSettings = exclusions;
  }

  public void prepare(ModuleFileSystem fs) {
    sourceInclusions = computeSourceInclusions(fs);
    sourceExclusions = computeSourceExclusions();
    testInclusions = computeTestInclusions(fs);
    testExclusions = computeTestExclusions();
    log("Included sources: ", sourceInclusions);
    log("Excluded sources: ", sourceExclusions);
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
    if (InputFile.Type.MAIN==type) {
      inclusionPatterns = sourceInclusions;
      exclusionPatterns = sourceExclusions;
    } else if (InputFile.Type.TEST==type) {
      inclusionPatterns = testInclusions;
      exclusionPatterns = testExclusions;
    }
    boolean matchInclusion = false;
    if (inclusionPatterns != null && inclusionPatterns.length > 0) {
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
    return matchInclusion;
  }

  PathPattern[] computeSourceInclusions(ModuleFileSystem fs) {
    if (exclusionSettings.sourceInclusions().length > 0) {
      // User defined params
      return PathPattern.create(exclusionSettings.sourceInclusions());
    }
    // Convert source directories to inclusions
    List<String> sourcePattern = new ArrayList<String>();
    for (File src : fs.sourceDirs()) {
      String path = new PathResolver().relativePath(fs.baseDir(), src);
      sourcePattern.add(path + "/**");
    }
    return PathPattern.create(sourcePattern.toArray(new String[sourcePattern.size()]));
  }

  PathPattern[] computeTestInclusions(ModuleFileSystem fs) {
    if (exclusionSettings.testInclusions().length > 0) {
      // User defined params
      return PathPattern.create(exclusionSettings.testInclusions());
    }
    // Convert source directories to inclusions
    List<String> testPatterns = new ArrayList<String>();
    for (File test : fs.testDirs()) {
      String path = new PathResolver().relativePath(fs.baseDir(), test);
      testPatterns.add(path + "/**");
    }
    return PathPattern.create(testPatterns.toArray(new String[testPatterns.size()]));
  }

  PathPattern[] computeSourceExclusions() {
    return PathPattern.create(exclusionSettings.sourceExclusions());
  }

  PathPattern[] computeTestExclusions() {
    return PathPattern.create(exclusionSettings.testExclusions());
  }
}
