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

package org.sonar.batch.issue.ignore.scanner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchSide;
import org.sonar.batch.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssuePattern;
import org.sonar.batch.issue.ignore.pattern.LineRange;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

@BatchSide
public class IssueExclusionsRegexpScanner {

  private static final Logger LOG = LoggerFactory.getLogger(IssueExclusionsRegexpScanner.class);

  private IssueExclusionPatternInitializer exclusionPatternInitializer;
  private List<java.util.regex.Pattern> allFilePatterns;
  private List<DoubleRegexpMatcher> blockMatchers;

  // fields to be reset at every new scan
  private DoubleRegexpMatcher currentMatcher;
  private int fileLength;
  private List<LineExclusion> lineExclusions;
  private LineExclusion currentLineExclusion;

  public IssueExclusionsRegexpScanner(IssueExclusionPatternInitializer patternsInitializer) {
    this.exclusionPatternInitializer = patternsInitializer;

    lineExclusions = Lists.newArrayList();
    allFilePatterns = Lists.newArrayList();
    blockMatchers = Lists.newArrayList();

    for (IssuePattern pattern : patternsInitializer.getAllFilePatterns()) {
      allFilePatterns.add(java.util.regex.Pattern.compile(pattern.getAllFileRegexp()));
    }
    for (IssuePattern pattern : patternsInitializer.getBlockPatterns()) {
      blockMatchers.add(new DoubleRegexpMatcher(
        java.util.regex.Pattern.compile(pattern.getBeginBlockRegexp()),
        java.util.regex.Pattern.compile(pattern.getEndBlockRegexp())));
    }

    init();
  }

  private void init() {
    currentMatcher = null;
    fileLength = 0;
    lineExclusions.clear();
    currentLineExclusion = null;
  }

  public void scan(String resource, File file, Charset sourcesEncoding) throws IOException {
    LOG.debug("Scanning {}", resource);
    init();

    List<String> lines = FileUtils.readLines(file, sourcesEncoding.name());
    int lineIndex = 0;
    for (String line : lines) {
      lineIndex++;
      if (line.trim().length() == 0) {
        continue;
      }

      // first check the single regexp patterns that can be used to totally exclude a file
      for (java.util.regex.Pattern pattern : allFilePatterns) {
        if (pattern.matcher(line).find()) {
          exclusionPatternInitializer.getPatternMatcher().addPatternToExcludeResource(resource);
          // nothing more to do on this file
          LOG.debug("- Exclusion pattern '{}': every violation in this file will be ignored.", pattern);
          return;
        }
      }

      // then check the double regexps if we're still here
      checkDoubleRegexps(line, lineIndex);
    }

    if (currentMatcher != null && !currentMatcher.hasSecondPattern()) {
      // this will happen when there is a start block regexp but no end block regexp
      endExclusion(lineIndex + 1);
    }

    // now create the new line-based pattern for this file if there are exclusions
    fileLength = lineIndex;
    if (!lineExclusions.isEmpty()) {
      Set<LineRange> lineRanges = convertLineExclusionsToLineRanges();
      LOG.debug("- Line exclusions found: {}", lineRanges);
      exclusionPatternInitializer.getPatternMatcher().addPatternToExcludeLines(resource, lineRanges);
    }
  }

  private Set<LineRange> convertLineExclusionsToLineRanges() {
    Set<LineRange> lineRanges = Sets.newHashSet();
    for (LineExclusion lineExclusion : lineExclusions) {
      lineRanges.add(lineExclusion.toLineRange());
    }
    return lineRanges;
  }

  private void checkDoubleRegexps(String line, int lineIndex) {
    if (currentMatcher == null) {
      for (DoubleRegexpMatcher matcher : blockMatchers) {
        if (matcher.matchesFirstPattern(line)) {
          startExclusion(lineIndex);
          currentMatcher = matcher;
          break;
        }
      }
    } else {
      if (currentMatcher.matchesSecondPattern(line)) {
        endExclusion(lineIndex);
        currentMatcher = null;
      }
    }
  }

  private void startExclusion(int lineIndex) {
    currentLineExclusion = new LineExclusion(lineIndex);
    lineExclusions.add(currentLineExclusion);
  }

  private void endExclusion(int lineIndex) {
    currentLineExclusion.setEnd(lineIndex);
    currentLineExclusion = null;
  }

  private class LineExclusion {

    private int start;
    private int end;

    LineExclusion(int start) {
      this.start = start;
      this.end = -1;
    }

    void setEnd(int end) {
      this.end = end;
    }

    public LineRange toLineRange() {
      return new LineRange(start, end == -1 ? fileLength : end);
    }

  }

  private static class DoubleRegexpMatcher {

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

}
