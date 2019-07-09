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

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import static java.util.stream.Collectors.toList;

@Immutable
public abstract class AbstractCoverageAndDuplicationExclusions {
  private static final Logger LOG = Loggers.get(AbstractCoverageAndDuplicationExclusions.class);
  private final Function<DefaultInputFile, String> pathExtractor;
  private final String[] coverageExclusionConfig;
  private final String[] duplicationExclusionConfig;

  private final Collection<WildcardPattern> coverageExclusionPatterns;
  private final Collection<WildcardPattern> duplicationExclusionPatterns;

  public AbstractCoverageAndDuplicationExclusions(Function<String, String[]> configProvider, Function<DefaultInputFile, String> pathExtractor) {
    this.pathExtractor = pathExtractor;
    coverageExclusionConfig = configProvider.apply(CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY);
    coverageExclusionPatterns = Stream.of(coverageExclusionConfig).map(WildcardPattern::create).collect(toList());
    duplicationExclusionConfig = configProvider.apply(CoreProperties.CPD_EXCLUSIONS);
    duplicationExclusionPatterns = Stream.of(duplicationExclusionConfig).map(WildcardPattern::create).collect(toList());
  }

  public String[] getCoverageExclusionConfig() {
    return coverageExclusionConfig;
  }

  public String[] getDuplicationExclusionConfig() {
    return duplicationExclusionConfig;
  }

  void log(String indent) {
    if (!coverageExclusionPatterns.isEmpty()) {
      log("Excluded sources for coverage:", coverageExclusionPatterns, indent);
    }
    if (!duplicationExclusionPatterns.isEmpty()) {
      log("Excluded sources for duplication:", duplicationExclusionPatterns, indent);
    }
  }

  public boolean isExcludedForCoverage(DefaultInputFile file) {
    return isExcluded(file, coverageExclusionPatterns);
  }

  public boolean isExcludedForDuplication(DefaultInputFile file) {
    return isExcluded(file, duplicationExclusionPatterns);
  }

  private boolean isExcluded(DefaultInputFile file, Collection<WildcardPattern> patterns) {
    if (patterns.isEmpty()) {
      return false;
    }
    final String path = pathExtractor.apply(file);
    return patterns
      .stream()
      .anyMatch(p -> p.match(path));
  }

  private static void log(String title, Collection<WildcardPattern> patterns, String ident) {
    if (!patterns.isEmpty()) {
      LOG.info("{}{} {}", ident, title, patterns.stream().map(WildcardPattern::toString).collect(Collectors.joining(", ")));
    }
  }
}
