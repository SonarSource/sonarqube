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
package org.sonar.scanner.issue.ignore.pattern;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WildcardPattern;

@Immutable
public class IssuePattern {

  private final WildcardPattern resourcePattern;
  private final WildcardPattern rulePattern;
  private final Set<Integer> lines;
  private final Set<LineRange> lineRanges;
  private final boolean checkLines;

  public IssuePattern(String resourcePattern, String rulePattern) {
    this(resourcePattern, rulePattern, Collections.emptySet());
  }

  public IssuePattern(String resourcePattern, String rulePattern, Set<LineRange> lineRanges) {
    this.resourcePattern = WildcardPattern.create(resourcePattern);
    this.rulePattern = WildcardPattern.create(rulePattern);
    this.checkLines = !lineRanges.isEmpty();
    Set<Integer> modifiableLines = new LinkedHashSet<>();
    Set<LineRange> modifiableLineRanges = new LinkedHashSet<>();

    for (LineRange range : lineRanges) {
      if (range.from() == range.to()) {
        modifiableLines.add(range.from());
      } else {
        modifiableLineRanges.add(range);
      }
    }

    this.lines = Collections.unmodifiableSet(modifiableLines);
    this.lineRanges = Collections.unmodifiableSet(modifiableLineRanges);
  }

  public WildcardPattern getResourcePattern() {
    return resourcePattern;
  }

  public WildcardPattern getRulePattern() {
    return rulePattern;
  }

  boolean isCheckLines() {
    return checkLines;
  }

  Set<Integer> getAllLines() {
    Set<Integer> allLines = new LinkedHashSet<>(lines);
    for (LineRange lineRange : lineRanges) {
      allLines.addAll(lineRange.toLines());
    }
    return allLines;
  }

  public boolean match(@Nullable String componentKey, RuleKey ruleKey, @Nullable Integer line) {
    if (checkLines) {
      if (line == null) {
        return false;
      } else {
        return matchResource(componentKey) && matchRule(ruleKey) && matchLine(line);
      }
    }
    return matchResource(componentKey) && matchRule(ruleKey);
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
    String key = new StringBuilder().append(rule.repository()).append(':').append(rule.rule()).toString();
    return rulePattern.match(key);
  }

  public boolean matchResource(@Nullable String resource) {
    return resource != null && resourcePattern.match(resource);
  }

  public IssuePattern forResource(String resource) {
    return new IssuePattern(resource, rulePattern.toString(), lineRanges);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
