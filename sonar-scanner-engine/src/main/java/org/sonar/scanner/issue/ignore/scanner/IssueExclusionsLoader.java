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
package org.sonar.scanner.issue.ignore.scanner;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.charhandler.CharHandler;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.issue.ignore.IgnoreIssuesFilter;
import org.sonar.scanner.issue.ignore.pattern.BlockIssuePattern;
import org.sonar.scanner.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;

public final class IssueExclusionsLoader {
  private static final Logger LOG = Loggers.get(IssueExclusionsLoader.class);

  private final List<java.util.regex.Pattern> allFilePatterns;
  private final List<DoubleRegexpMatcher> blockMatchers;
  private final IgnoreIssuesFilter ignoreIssuesFilter;
  private final AnalysisWarnings analysisWarnings;
  private final IssueExclusionPatternInitializer patternsInitializer;
  private final boolean enableCharHandler;
  private boolean warnDeprecatedIssuePatternAlreadyLogged;

  public IssueExclusionsLoader(IssueExclusionPatternInitializer patternsInitializer, IgnoreIssuesFilter ignoreIssuesFilter, AnalysisWarnings analysisWarnings) {
    this.patternsInitializer = patternsInitializer;
    this.ignoreIssuesFilter = ignoreIssuesFilter;
    this.analysisWarnings = analysisWarnings;
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

  public void addMulticriteriaPatterns(DefaultInputFile inputFile) {
    for (IssuePattern pattern : patternsInitializer.getMulticriteriaPatterns()) {
      if (pattern.matchFile(inputFile.getProjectRelativePath())) {
        ignoreIssuesFilter.addRuleExclusionPatternForComponent(inputFile, pattern.getRulePattern());
      } else if (pattern.matchFile(inputFile.getModuleRelativePath())) {
        warnOnceDeprecatedIssuePattern(
          "Specifying module-relative paths at project level in property '" + IssueExclusionPatternInitializer.CONFIG_KEY + "' is deprecated. " +
            "To continue matching files like '" + inputFile.getProjectRelativePath() + "', update this property so that patterns refer to project-relative paths.");
        ignoreIssuesFilter.addRuleExclusionPatternForComponent(inputFile, pattern.getRulePattern());
      }
    }
  }

  private void warnOnceDeprecatedIssuePattern(String msg) {
    if (!warnDeprecatedIssuePatternAlreadyLogged) {
      LOG.warn(msg);
      analysisWarnings.addUnique(msg);
      warnDeprecatedIssuePatternAlreadyLogged = true;
    }
  }

  @CheckForNull
  public CharHandler createCharHandlerFor(DefaultInputFile inputFile) {
    if (enableCharHandler) {
      return new IssueExclusionsRegexpScanner(inputFile, allFilePatterns, blockMatchers);
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
