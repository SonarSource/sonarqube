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
package org.sonar.batch.issue.tracking;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class IssueTrackingResult {
  private final Map<String, PreviousIssue> unmatchedByKey = new HashMap<>();
  private final Map<RuleKey, Map<String, PreviousIssue>> unmatchedByRuleAndKey = new HashMap<>();
  private final Map<RuleKey, Map<Integer, Multimap<String, PreviousIssue>>> unmatchedByRuleAndLineAndChecksum = new HashMap<>();
  private final Map<DefaultIssue, PreviousIssue> matched = Maps.newIdentityHashMap();

  Collection<PreviousIssue> unmatched() {
    return unmatchedByKey.values();
  }

  Map<String, PreviousIssue> unmatchedByKeyForRule(RuleKey ruleKey) {
    return unmatchedByRuleAndKey.containsKey(ruleKey) ? unmatchedByRuleAndKey.get(ruleKey) : Collections.<String, PreviousIssue>emptyMap();
  }

  Collection<PreviousIssue> unmatchedForRuleAndForLineAndForChecksum(RuleKey ruleKey, @Nullable Integer line, @Nullable String checksum) {
    if (!unmatchedByRuleAndLineAndChecksum.containsKey(ruleKey)) {
      return Collections.emptyList();
    }
    Map<Integer, Multimap<String, PreviousIssue>> unmatchedForRule = unmatchedByRuleAndLineAndChecksum.get(ruleKey);
    Integer lineNotNull = line != null ? line : 0;
    if (!unmatchedForRule.containsKey(lineNotNull)) {
      return Collections.emptyList();
    }
    Multimap<String, PreviousIssue> unmatchedForRuleAndLine = unmatchedForRule.get(lineNotNull);
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

  PreviousIssue matching(DefaultIssue issue) {
    return matched.get(issue);
  }

  void addUnmatched(PreviousIssue i) {
    unmatchedByKey.put(i.key(), i);
    RuleKey ruleKey = i.ruleKey();
    if (!unmatchedByRuleAndKey.containsKey(ruleKey)) {
      unmatchedByRuleAndKey.put(ruleKey, new HashMap<String, PreviousIssue>());
      unmatchedByRuleAndLineAndChecksum.put(ruleKey, new HashMap<Integer, Multimap<String, PreviousIssue>>());
    }
    unmatchedByRuleAndKey.get(ruleKey).put(i.key(), i);
    Map<Integer, Multimap<String, PreviousIssue>> unmatchedForRule = unmatchedByRuleAndLineAndChecksum.get(ruleKey);
    Integer lineNotNull = lineNotNull(i);
    if (!unmatchedForRule.containsKey(lineNotNull)) {
      unmatchedForRule.put(lineNotNull, HashMultimap.<String, PreviousIssue>create());
    }
    Multimap<String, PreviousIssue> unmatchedForRuleAndLine = unmatchedForRule.get(lineNotNull);
    String checksumNotNull = StringUtils.defaultString(i.checksum(), "");
    unmatchedForRuleAndLine.put(checksumNotNull, i);
  }

  private Integer lineNotNull(PreviousIssue i) {
    Integer line = i.line();
    return line != null ? line : 0;
  }

  void setMatch(DefaultIssue issue, PreviousIssue matching) {
    matched.put(issue, matching);
    RuleKey ruleKey = matching.ruleKey();
    unmatchedByRuleAndKey.get(ruleKey).remove(matching.key());
    unmatchedByKey.remove(matching.key());
    Integer lineNotNull = lineNotNull(matching);
    String checksumNotNull = StringUtils.defaultString(matching.checksum(), "");
    unmatchedByRuleAndLineAndChecksum.get(ruleKey).get(lineNotNull).get(checksumNotNull).remove(matching);
  }
}
