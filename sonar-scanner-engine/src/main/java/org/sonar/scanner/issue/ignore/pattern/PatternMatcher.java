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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;

public class PatternMatcher {

  private Multimap<String, IssuePattern> excludePatternByComponent = LinkedHashMultimap.create();

  @CheckForNull
  public IssuePattern getMatchingPattern(String componentKey, RuleKey ruleKey, @Nullable Integer line) {
    for (IssuePattern pattern : getPatternsForComponent(componentKey)) {
      if (pattern.match(componentKey, ruleKey, line)) {
        return pattern;
      }
    }
    return null;
  }

  @VisibleForTesting
  public Collection<IssuePattern> getPatternsForComponent(String componentKey) {
    return excludePatternByComponent.get(componentKey);
  }

  public void addPatternForComponent(String componentKey, IssuePattern pattern) {
    excludePatternByComponent.put(componentKey, pattern.forResource(componentKey));
  }

  public void addPatternToExcludeResource(String componentKey) {
    addPatternForComponent(componentKey, new IssuePattern(componentKey, "*"));
  }

  public void addPatternToExcludeLines(String componentKey, Set<LineRange> lineRanges) {
    addPatternForComponent(componentKey, new IssuePattern(componentKey, "*", lineRanges));
  }

}
