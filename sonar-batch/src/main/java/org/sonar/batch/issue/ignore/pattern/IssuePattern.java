/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.issue.ignore.pattern;

import org.sonar.api.scan.issue.filter.FilterableIssue;

import com.google.common.collect.Sets;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WildcardPattern;

import java.util.Set;

public class IssuePattern {

  private WildcardPattern resourcePattern;
  private WildcardPattern rulePattern;
  private Set<Integer> lines = Sets.newLinkedHashSet();
  private Set<LineRange> lineRanges = Sets.newLinkedHashSet();
  private String beginBlockRegexp;
  private String endBlockRegexp;
  private String allFileRegexp;
  private boolean checkLines = true;

  public IssuePattern() {
  }

  public IssuePattern(String resourcePattern, String rulePattern) {
    this.resourcePattern = WildcardPattern.create(resourcePattern);
    this.rulePattern = WildcardPattern.create(rulePattern);
  }

  public IssuePattern(String resourcePattern, String rulePattern, Set<LineRange> lineRanges) {
    this(resourcePattern, rulePattern);
    this.lineRanges = lineRanges;
  }

  public WildcardPattern getResourcePattern() {
    return resourcePattern;
  }

  public WildcardPattern getRulePattern() {
    return rulePattern;
  }

  public String getBeginBlockRegexp() {
    return beginBlockRegexp;
  }

  public String getEndBlockRegexp() {
    return endBlockRegexp;
  }

  public String getAllFileRegexp() {
    return allFileRegexp;
  }

  IssuePattern addLineRange(int fromLineId, int toLineId) {
    lineRanges.add(new LineRange(fromLineId, toLineId));
    return this;
  }

  IssuePattern addLine(int lineId) {
    lines.add(lineId);
    return this;
  }

  boolean isCheckLines() {
    return checkLines;
  }

  IssuePattern setCheckLines(boolean b) {
    this.checkLines = b;
    return this;
  }

  IssuePattern setBeginBlockRegexp(String beginBlockRegexp) {
    this.beginBlockRegexp = beginBlockRegexp;
    return this;
  }

  IssuePattern setEndBlockRegexp(String endBlockRegexp) {
    this.endBlockRegexp = endBlockRegexp;
    return this;
  }

  IssuePattern setAllFileRegexp(String allFileRegexp) {
    this.allFileRegexp = allFileRegexp;
    return this;
  }

  Set<Integer> getAllLines() {
    Set<Integer> allLines = Sets.newLinkedHashSet(lines);
    for (LineRange lineRange : lineRanges) {
      allLines.addAll(lineRange.toLines());
    }
    return allLines;
  }

  public boolean match(FilterableIssue issue) {
    boolean match = matchResource(issue.componentKey())
      && matchRule(issue.ruleKey());
    if (checkLines) {
      Integer line = issue.line();
      if (line == null) {
        match = false;
      } else {
        match = match && matchLine(line);
      }
    }
    return match;
  }

  boolean matchLine(int lineId) {
    if (lines.contains(lineId)) {
      return true;
    }

    for (LineRange range : lineRanges) {
      if (range.in(lineId)) {
        return true;
      }
    }

    return false;
  }

  boolean matchRule(RuleKey rule) {
    if (rule == null) {
      return false;
    }

    String key = new StringBuilder().append(rule.repository()).append(':').append(rule.rule()).toString();
    return rulePattern.match(key);
  }

  boolean matchResource(String resource) {
    return resource != null && resourcePattern.match(resource);
  }

  public IssuePattern forResource(String resource) {
    return new IssuePattern(resource, rulePattern.toString(), lineRanges).setCheckLines(isCheckLines());
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
