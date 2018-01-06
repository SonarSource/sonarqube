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
package org.sonar.scanner.issue.ignore.scanner;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.internal.charhandler.CharHandler;
import org.sonar.scanner.issue.ignore.pattern.BlockIssuePattern;
import org.sonar.scanner.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;
import org.sonar.scanner.issue.ignore.pattern.PatternMatcher;

public final class IssueExclusionsLoader {
  private final List<java.util.regex.Pattern> allFilePatterns;
  private final List<DoubleRegexpMatcher> blockMatchers;
  private final PatternMatcher patternMatcher;
  private final IssueExclusionPatternInitializer patternsInitializer;
  private final boolean enableCharHandler;

  public IssueExclusionsLoader(IssueExclusionPatternInitializer patternsInitializer, PatternMatcher patternMatcher) {
    this.patternsInitializer = patternsInitializer;
    this.patternMatcher = patternMatcher;
    this.allFilePatterns = new ArrayList<>();
    this.blockMatchers = new ArrayList<>();

    for (String pattern : patternsInitializer.getAllFilePatterns()) {
      allFilePatterns.add(java.util.regex.Pattern.compile(pattern));
    }
    for (BlockIssuePattern pattern : patternsInitializer.getBlockPatterns()) {
      blockMatchers.add(new DoubleRegexpMatcher(
        java.util.regex.Pattern.compile(pattern.getBeginBlockRegexp()),
        java.util.regex.Pattern.compile(pattern.getEndBlockRegexp())));
    }
    enableCharHandler = !allFilePatterns.isEmpty() || !blockMatchers.isEmpty();
  }

  public boolean shouldExecute() {
    return patternsInitializer.hasMulticriteriaPatterns();
  }

  public void addMulticriteriaPatterns(String relativePath, String componentKey) {
    for (IssuePattern pattern : patternsInitializer.getMulticriteriaPatterns()) {
      if (pattern.matchResource(relativePath)) {
        patternMatcher.addPatternForComponent(componentKey, pattern);
      }
    }
  }

  @CheckForNull
  public CharHandler createCharHandlerFor(String componentKey) {
    if (enableCharHandler) {
      return new IssueExclusionsRegexpScanner(componentKey, allFilePatterns, blockMatchers, patternMatcher);
    }
    return null;
  }

  public static class DoubleRegexpMatcher {

    private java.util.regex.Pattern firstPattern;
    private java.util.regex.Pattern secondPattern;

    DoubleRegexpMatcher(java.util.regex.Pattern firstPattern, java.util.regex.Pattern secondPattern) {
      this.firstPattern = firstPattern;
      this.secondPattern = secondPattern;
    }

    boolean matchesFirstPattern(String line) {
      return firstPattern.matcher(line).find();
    }

    boolean matchesSecondPattern(String line) {
      return hasSecondPattern() && secondPattern.matcher(line).find();
    }

    boolean hasSecondPattern() {
      return StringUtils.isNotEmpty(secondPattern.toString());
    }
  }

  @Override
  public String toString() {
    return "Issues Exclusions - Source Scanner";
  }
}
