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
package org.sonar.plugins.core.issue;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.db.IssueDto;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class IssueTrackingResult {
  private final Map<String, IssueDto> unmatchedByKey = new HashMap<String, IssueDto>();
  private final Map<RuleKey, Map<String, IssueDto>> unmatchedByRuleAndKey = new HashMap<RuleKey, Map<String, IssueDto>>();
  private final Map<RuleKey, Map<Integer, Multimap<String, IssueDto>>> unmatchedByRuleAndLineAndChecksum =
    new HashMap<RuleKey, Map<Integer, Multimap<String, IssueDto>>>();
  private final Map<DefaultIssue, IssueDto> matched = Maps.newIdentityHashMap();

  Collection<IssueDto> unmatched() {
    return unmatchedByKey.values();
  }

  Map<String, IssueDto> unmatchedByKeyForRule(RuleKey ruleKey) {
    return unmatchedByRuleAndKey.containsKey(ruleKey) ? unmatchedByRuleAndKey.get(ruleKey) : Collections.<String, IssueDto>emptyMap();
  }

  Collection<IssueDto> unmatchedForRuleAndForLineAndForChecksum(RuleKey ruleKey, @Nullable Integer line, @Nullable String checksum) {
    if (!unmatchedByRuleAndLineAndChecksum.containsKey(ruleKey)) {
      return Collections.emptyList();
    }
    Map<Integer, Multimap<String, IssueDto>> unmatchedForRule = unmatchedByRuleAndLineAndChecksum.get(ruleKey);
    Integer lineNotNull = line != null ? line : 0;
    if (!unmatchedForRule.containsKey(lineNotNull)) {
      return Collections.emptyList();
    }
    Multimap<String, IssueDto> unmatchedForRuleAndLine = unmatchedForRule.get(lineNotNull);
    String checksumNotNull = StringUtils.defaultString(checksum, "");
    if (!unmatchedForRuleAndLine.containsKey(checksumNotNull)) {
      return Collections.emptyList();
    }
    return unmatchedForRuleAndLine.get(checksumNotNull);
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
    unmatchedByKey.put(i.getKee(), i);
    RuleKey ruleKey = RuleKey.of(i.getRuleRepo(), i.getRule());
    if (!unmatchedByRuleAndKey.containsKey(ruleKey)) {
      unmatchedByRuleAndKey.put(ruleKey, new HashMap<String, IssueDto>());
      unmatchedByRuleAndLineAndChecksum.put(ruleKey, new HashMap<Integer, Multimap<String, IssueDto>>());
    }
    unmatchedByRuleAndKey.get(ruleKey).put(i.getKee(), i);
    Map<Integer, Multimap<String, IssueDto>> unmatchedForRule = unmatchedByRuleAndLineAndChecksum.get(ruleKey);
    Integer lineNotNull = lineNotNull(i);
    if (!unmatchedForRule.containsKey(lineNotNull)) {
      unmatchedForRule.put(lineNotNull, HashMultimap.<String, IssueDto>create());
    }
    Multimap<String, IssueDto> unmatchedForRuleAndLine = unmatchedForRule.get(lineNotNull);
    String checksumNotNull = StringUtils.defaultString(i.getChecksum(), "");
    unmatchedForRuleAndLine.put(checksumNotNull, i);
  }

  private Integer lineNotNull(IssueDto i) {
    Integer line = i.getLine();
    return line != null ? line : 0;
  }

  void setMatch(DefaultIssue issue, IssueDto matching) {
    matched.put(issue, matching);
    RuleKey ruleKey = RuleKey.of(matching.getRuleRepo(), matching.getRule());
    unmatchedByRuleAndKey.get(ruleKey).remove(matching.getKee());
    unmatchedByKey.remove(matching.getKee());
    Integer lineNotNull = lineNotNull(matching);
    String checksumNotNull = StringUtils.defaultString(matching.getChecksum(), "");
    unmatchedByRuleAndLineAndChecksum.get(ruleKey).get(lineNotNull).get(checksumNotNull).remove(matching);
  }
}
