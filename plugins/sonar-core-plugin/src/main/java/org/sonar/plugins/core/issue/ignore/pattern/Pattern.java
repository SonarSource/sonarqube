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

package org.sonar.plugins.core.issue.ignore.pattern;

import com.google.common.collect.Sets;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WildcardPattern;

import java.util.Set;

public class Pattern {

  private WildcardPattern resourcePattern;
  private WildcardPattern rulePattern;
  private Set<Integer> lines = Sets.newLinkedHashSet();
  private Set<LineRange> lineRanges = Sets.newLinkedHashSet();
  private String beginBlockRegexp;
  private String endBlockRegexp;
  private String allFileRegexp;
  private boolean checkLines = true;

  public Pattern() {
  }

  public Pattern(String resourcePattern, String rulePattern) {
    this.resourcePattern = WildcardPattern.create(resourcePattern);
    this.rulePattern = WildcardPattern.create(rulePattern);
  }

  public Pattern(String resourcePattern, String rulePattern, Set<LineRange> lineRanges) {
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

  Pattern addLineRange(int fromLineId, int toLineId) {
    lineRanges.add(new LineRange(fromLineId, toLineId));
    return this;
  }

  Pattern addLine(int lineId) {
    lines.add(lineId);
    return this;
  }

  boolean isCheckLines() {
    return checkLines;
  }

  Pattern setCheckLines(boolean b) {
    this.checkLines = b;
    return this;
  }

  Pattern setBeginBlockRegexp(String beginBlockRegexp) {
    this.beginBlockRegexp = beginBlockRegexp;
    return this;
  }

  Pattern setEndBlockRegexp(String endBlockRegexp) {
    this.endBlockRegexp = endBlockRegexp;
    return this;
  }

  Pattern setAllFileRegexp(String allFileRegexp) {
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

  public boolean match(Issue violation) {
    boolean match = matchResource(violation.componentKey()) && matchRule(violation.ruleKey());
    if (checkLines && violation.line() != null) {
      match = match && matchLine(violation.line());
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
    System.out.printf("Matching rule {} against pattern {}", rule, rulePattern);
    if (rule == null) {
      return false;
    }

    String key = new StringBuilder().append(rule.repository()).append(':').append(rule.rule()).toString();
    return rulePattern.match(key);
  }

  boolean matchResource(String resource) {
    System.out.printf("Matching resource {} against pattern {}", resource, resourcePattern);
    return resource != null && resourcePattern.match(resource);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
