/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.PathPattern;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public abstract class AbstractExclusionFilters {

  private static final Logger LOG = Loggers.get(AbstractExclusionFilters.class);
  private final String[] sourceInclusions;
  private final String[] testInclusions;
  private final String[] sourceExclusions;
  private final String[] testExclusions;

  private PathPattern[] mainInclusionsPattern;
  private PathPattern[] mainExclusionsPattern;
  private PathPattern[] testInclusionsPattern;
  private PathPattern[] testExclusionsPattern;

  public AbstractExclusionFilters(Function<String, String[]> configProvider) {
    this.sourceInclusions = inclusions(configProvider, CoreProperties.PROJECT_INCLUSIONS_PROPERTY);
    this.testInclusions = inclusions(configProvider, CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY);
    this.sourceExclusions = exclusions(configProvider, CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY, CoreProperties.PROJECT_EXCLUSIONS_PROPERTY);
    this.testExclusions = exclusions(configProvider, CoreProperties.GLOBAL_TEST_EXCLUSIONS_PROPERTY, CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY);
    this.mainInclusionsPattern = prepareMainInclusions(sourceInclusions);
    this.mainExclusionsPattern = prepareMainExclusions(sourceExclusions, testInclusions);
    this.testInclusionsPattern = prepareTestInclusions(testInclusions);
    this.testExclusionsPattern = prepareTestExclusions(testExclusions);
  }

  protected void log() {
    log("Included sources: ", mainInclusionsPattern);
    log("Excluded sources: ", mainExclusionsPattern);
    log("Included tests: ", testInclusionsPattern);
    log("Excluded tests: ", testExclusionsPattern);
  }

  private String[] inclusions(Function<String, String[]> configProvider, String propertyKey) {
    return Arrays.stream(configProvider.apply(propertyKey))
      .map(StringUtils::trim)
      .filter(s -> !"**/*".equals(s))
      .filter(s -> !"file:**/*".equals(s))
      .toArray(String[]::new);
  }

  private String[] exclusions(Function<String, String[]> configProvider, String globalExclusionsProperty, String exclusionsProperty) {
    String[] globalExclusions = configProvider.apply(globalExclusionsProperty);
    String[] exclusions = configProvider.apply(exclusionsProperty);
    return Stream.concat(Arrays.stream(globalExclusions), Arrays.stream(exclusions))
      .map(StringUtils::trim)
      .toArray(String[]::new);
  }

  public boolean hasPattern() {
    return mainInclusionsPattern.length > 0 || mainExclusionsPattern.length > 0 || testInclusionsPattern.length > 0 || testExclusionsPattern.length > 0;
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
      inclusionPatterns = mainInclusionsPattern;
      exclusionPatterns = mainExclusionsPattern;
    } else if (InputFile.Type.TEST == type) {
      inclusionPatterns = testInclusionsPattern;
      exclusionPatterns = testExclusionsPattern;
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

  private static PathPattern[] prepareMainInclusions(String[] sourceInclusions) {
    if (sourceInclusions.length > 0) {
      // User defined params
      return PathPattern.create(sourceInclusions);
    }
    return new PathPattern[0];
  }

  private static PathPattern[] prepareTestInclusions(String[] testInclusions) {
    return PathPattern.create(testInclusions);
  }

  static PathPattern[] prepareMainExclusions(String[] sourceExclusions, String[] testInclusions) {
    String[] patterns = (String[]) ArrayUtils.addAll(
      sourceExclusions, testInclusions);
    return PathPattern.create(patterns);
  }

  private static PathPattern[] prepareTestExclusions(String[] testExclusions) {
    return PathPattern.create(testExclusions);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AbstractExclusionFilters)) {
      return false;
    }
    AbstractExclusionFilters that = (AbstractExclusionFilters) o;
    return Arrays.equals(sourceInclusions, that.sourceInclusions) &&
      Arrays.equals(testInclusions, that.testInclusions) &&
      Arrays.equals(sourceExclusions, that.sourceExclusions) &&
      Arrays.equals(testExclusions, that.testExclusions);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(sourceInclusions);
    result = 31 * result + Arrays.hashCode(testInclusions);
    result = 31 * result + Arrays.hashCode(sourceExclusions);
    result = 31 * result + Arrays.hashCode(testExclusions);
    return result;
  }
}
