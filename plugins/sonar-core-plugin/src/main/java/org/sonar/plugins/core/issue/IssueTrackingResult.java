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
package org.sonar.plugins.core.issue;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.db.IssueDto;

import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;

class IssueTrackingResult {
  private final HashSet<IssueDto> unmatched = Sets.newHashSet();
  private final Multimap<RuleKey, IssueDto> unmatchedByRule = LinkedHashMultimap.create();
  private final IdentityHashMap<DefaultIssue, IssueDto> matched = Maps.newIdentityHashMap();

  Collection<IssueDto> unmatched() {
    return unmatched;
  }

  Collection<IssueDto> unmatchedForRule(RuleKey ruleKey) {
    return unmatchedByRule.get(ruleKey);
  }

  Collection<DefaultIssue> matched() {
    return matched.keySet();
  }

  boolean isMatched(DefaultIssue issue) {
    return matched.containsKey(issue);
  }

  IssueDto matching(DefaultIssue issue) {
    return matched.get(issue);
  }

  void addUnmatched(IssueDto i) {
    unmatched.add(i);
    unmatchedByRule.put(RuleKey.of(i.getRuleRepo(), i.getRule()), i);
  }

  void setMatch(DefaultIssue issue, IssueDto matching) {
    matched.put(issue, matching);
    unmatchedByRule.remove(RuleKey.of(matching.getRuleRepo(), matching.getRule()), matching);
    unmatched.remove(matching);
  }
}
