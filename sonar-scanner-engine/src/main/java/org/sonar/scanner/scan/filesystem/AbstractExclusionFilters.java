/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.PathPattern;
import org.sonar.api.notifications.AnalysisWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.sonar.api.CoreProperties.PROJECT_TESTS_EXCLUSIONS_PROPERTY;
import static org.sonar.api.CoreProperties.PROJECT_TESTS_INCLUSIONS_PROPERTY;
import static org.sonar.api.CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY;
import static org.sonar.api.CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY;

public abstract class AbstractExclusionFilters {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractExclusionFilters.class);
  private static final String WARNING_ALIAS_PROPERTY_USAGE = "Use of %s detected. While being taken into account, the only supported property is %s." +
    " Consider updating your configuration.";

  private static final String WARNING_LEGACY_AND_ALIAS_PROPERTIES_USAGE = "Use of %s and %s at the same time. %s is taken into account. Consider updating your configuration";

  private final AnalysisWarnings analysisWarnings;
  private final String[] sourceInclusions;
  private final String[] testInclusions;
  private final String[] sourceExclusions;
  private final String[] testExclusions;

  private PathPattern[] mainInclusionsPattern;
  private PathPattern[] mainExclusionsPattern;
  private PathPattern[] testInclusionsPattern;
  private PathPattern[] testExclusionsPattern;

  protected AbstractExclusionFilters(AnalysisWarnings analysisWarnings, Function<String, String[]> configProvider) {
    this.analysisWarnings = analysisWarnings;

    this.sourceInclusions = inclusions(configProvider, CoreProperties.PROJECT_INCLUSIONS_PROPERTY);
    this.sourceExclusions = initSourceExclusions(configProvider);
    this.testInclusions = initTestInclusions(configProvider);
    this.testExclusions = initTestExclusions(configProvider);

    this.mainInclusionsPattern = prepareMainInclusions(this.sourceInclusions);
    this.mainExclusionsPattern = prepareMainExclusions(this.sourceExclusions, this.testInclusions);
    this.testInclusionsPattern = prepareTestInclusions(this.testInclusions);
    this.testExclusionsPattern = prepareTestExclusions(this.testExclusions);
  }

  private String[] initSourceExclusions(Function<String, String[]> configProvider) {
    String[] projectSourceExclusion = exclusions(configProvider, CoreProperties.PROJECT_EXCLUSIONS_PROPERTY);
    String[] globalSourceExclusion = exclusions(configProvider, CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY);
    return Stream.concat(Arrays.stream(projectSourceExclusion), Arrays.stream(globalSourceExclusion))
      .map(String::trim)
      .toArray(String[]::new);
  }

  private String[] initTestInclusions(Function<String, String[]> configProvider) {
    String[] testInclusionsFromLegacy = inclusions(configProvider, PROJECT_TEST_INCLUSIONS_PROPERTY);
    String[] testInclusionsFromAlias = inclusions(configProvider, PROJECT_TESTS_INCLUSIONS_PROPERTY);
    return keepInclusionTestBetweenLegacyAndAliasProperties(testInclusionsFromLegacy, testInclusionsFromAlias);
  }

  private String[] initTestExclusions(Function<String, String[]> configProvider) {
    String[] testExclusionsFromLegacy = exclusions(configProvider, CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY);
    String[] testExclusionsFromAlias = exclusions(configProvider, CoreProperties.PROJECT_TESTS_EXCLUSIONS_PROPERTY);
    String[] testExclusionsKept = keepExclusionTestBetweenLegacyAndAliasProperties(testExclusionsFromLegacy, testExclusionsFromAlias);

    String[] testExclusionsFromGlobal = exclusions(configProvider, CoreProperties.GLOBAL_TEST_EXCLUSIONS_PROPERTY);
    return Stream.concat(Arrays.stream(testExclusionsKept), Arrays.stream(testExclusionsFromGlobal))
      .map(String::trim)
      .toArray(String[]::new);
  }

  private String[] keepExclusionTestBetweenLegacyAndAliasProperties(String[] fromLegacyProperty, String[] fromAliasProperty) {
    if (fromAliasProperty.length == 0) {
      return fromLegacyProperty;
    }
    if (fromLegacyProperty.length == 0) {
      logWarningForAliasUsage(PROJECT_TEST_EXCLUSIONS_PROPERTY, PROJECT_TESTS_EXCLUSIONS_PROPERTY);
      return fromAliasProperty;
    }
    logWarningForLegacyAndAliasUsage(PROJECT_TEST_EXCLUSIONS_PROPERTY, PROJECT_TESTS_EXCLUSIONS_PROPERTY);
    return fromLegacyProperty;
  }

  private String[] keepInclusionTestBetweenLegacyAndAliasProperties(String[] fromLegacyProperty, String[] fromAliasProperty) {
    if (fromAliasProperty.length == 0) {
      return fromLegacyProperty;
    }
    if (fromLegacyProperty.length == 0) {
      logWarningForAliasUsage(PROJECT_TEST_INCLUSIONS_PROPERTY, PROJECT_TESTS_INCLUSIONS_PROPERTY);
      return fromAliasProperty;
    }
    logWarningForLegacyAndAliasUsage(PROJECT_TEST_INCLUSIONS_PROPERTY, PROJECT_TESTS_INCLUSIONS_PROPERTY);
    return fromLegacyProperty;
  }

  private void logWarningForAliasUsage(String legacyProperty, String aliasProperty) {
    logWarning(format(WARNING_ALIAS_PROPERTY_USAGE, aliasProperty, legacyProperty));
  }

  private void logWarningForLegacyAndAliasUsage(String legacyProperty, String aliasProperty) {
    logWarning(format(WARNING_LEGACY_AND_ALIAS_PROPERTIES_USAGE, legacyProperty, aliasProperty, legacyProperty));
  }

  private void logWarning(String warning) {
    LOG.warn(warning);
    analysisWarnings.addUnique(warning);
  }

  public void log(String indent) {
    log("Included sources:", mainInclusionsPattern, indent);
    log("Excluded sources:", mainExclusionsPattern, indent);
    log("Included tests:", testInclusionsPattern, indent);
    log("Excluded tests:", testExclusionsPattern, indent);
  }

  private String[] inclusions(Function<String, String[]> configProvider, String propertyKey) {
    return Arrays.stream(configProvider.apply(propertyKey))
      .map(StringUtils::trim)
      .filter(s -> !"**/*".equals(s))
      .filter(s -> !"file:**/*".equals(s))
      .toArray(String[]::new);
  }

  private String[] exclusions(Function<String, String[]> configProvider, String property) {
    String[] exclusions = configProvider.apply(property);
    return Arrays.stream(exclusions)
      .map(StringUtils::trim)
      .toArray(String[]::new);
  }

  public boolean hasPattern() {
    return mainInclusionsPattern.length > 0 || mainExclusionsPattern.length > 0 || testInclusionsPattern.length > 0 || testExclusionsPattern.length > 0;
  }

  private static void log(String title, PathPattern[] patterns, String indent) {
    if (patterns.length > 0) {
      LOG.info("{}{} {}", indent, title, Arrays.stream(patterns).map(PathPattern::toString).collect(Collectors.joining(", ")));
    }
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

  public String[] getInclusionsConfig(InputFile.Type type) {
    return type == InputFile.Type.MAIN ? sourceInclusions : testInclusions;
  }

  public String[] getExclusionsConfig(InputFile.Type type) {
    return type == InputFile.Type.MAIN ? sourceExclusions : testExclusions;
  }

  public boolean isIncluded(Path absolutePath, Path relativePath, InputFile.Type type) {
    PathPattern[] inclusionPatterns = InputFile.Type.MAIN == type ? mainInclusionsPattern : testInclusionsPattern;

    if (inclusionPatterns.length == 0) {
      return true;
    }

    for (PathPattern pattern : inclusionPatterns) {
      if (pattern.match(absolutePath, relativePath)) {
        return true;
      }
    }

    return false;
  }

  public boolean isExcluded(Path absolutePath, Path relativePath, InputFile.Type type) {
    PathPattern[] exclusionPatterns = InputFile.Type.MAIN == type ? mainExclusionsPattern : testExclusionsPattern;

    for (PathPattern pattern : exclusionPatterns) {
      if (pattern.match(absolutePath, relativePath)) {
        return true;
      }
    }

    return false;
  }

  /**
   * <p>Checks if the file should be excluded as a parent directory of excluded files and subdirectories.</p>
   *
   * @param absolutePath The full path of the file.
   * @param relativePath The relative path of the file.
   * @param baseDir      The base directory of the project.
   * @param type         The file type.
   * @return True if the file should be excluded, false otherwise.
   */
  public boolean isExcludedAsParentDirectoryOfExcludedChildren(Path absolutePath, Path relativePath, Path baseDir, InputFile.Type type) {
    PathPattern[] exclusionPatterns = InputFile.Type.MAIN == type ? mainExclusionsPattern : testExclusionsPattern;

    return Stream.of(exclusionPatterns)
      .map(PathPattern::toString)
      .filter(ps -> ps.endsWith("/**/*"))
      .map(ps -> ps.substring(0, ps.length() - 5))
      .map(baseDir::resolve)
      .anyMatch(exclusionRootPath -> absolutePath.startsWith(exclusionRootPath)
        || PathPattern.create(exclusionRootPath.toString()).match(absolutePath, relativePath));
  }
}
