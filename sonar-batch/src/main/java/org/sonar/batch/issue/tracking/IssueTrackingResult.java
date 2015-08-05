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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.protocol.output.BatchReport;

class IssueTrackingResult {
  private final Map<String, ServerIssue> unmatchedByKey = new HashMap<>();
  private final Map<RuleKey, Map<String, ServerIssue>> unmatchedByRuleAndKey = new HashMap<>();
  private final Map<RuleKey, Map<Integer, Multimap<String, ServerIssue>>> unmatchedByRuleAndLineAndChecksum = new HashMap<>();
  private final Map<BatchReport.Issue, ServerIssue> matched = Maps.newIdentityHashMap();

  Collection<ServerIssue> unmatched() {
    return unmatchedByKey.values();
  }

  Map<String, ServerIssue> unmatchedByKeyForRule(RuleKey ruleKey) {
    return unmatchedByRuleAndKey.containsKey(ruleKey) ? unmatchedByRuleAndKey.get(ruleKey) : Collections.<String, ServerIssue>emptyMap();
  }

  Collection<ServerIssue> unmatchedForRuleAndForLineAndForChecksum(RuleKey ruleKey, @Nullable Integer line, @Nullable String checksum) {
    if (!unmatchedByRuleAndLineAndChecksum.containsKey(ruleKey)) {
      return Collections.emptyList();
    }
    Map<Integer, Multimap<String, ServerIssue>> unmatchedForRule = unmatchedByRuleAndLineAndChecksum.get(ruleKey);
    Integer lineNotNull = line != null ? line : 0;
    if (!unmatchedForRule.containsKey(lineNotNull)) {
      return Collections.emptyList();
    }
    Multimap<String, ServerIssue> unmatchedForRuleAndLine = unmatchedForRule.get(lineNotNull);
    String checksumNotNull = StringUtils.defaultString(checksum, "");
    if (!unmatchedForRuleAndLine.containsKey(checksumNotNull)) {
      return Collections.emptyList();
    }
    return unmatchedForRuleAndLine.get(checksumNotNull);
  }

  Collection<BatchReport.Issue> matched() {
    return matched.keySet();
  }

  boolean isMatched(BatchReport.Issue issue) {
    return matched.containsKey(issue);
  }

  ServerIssue matching(BatchReport.Issue issue) {
    return matched.get(issue);
  }

  void addUnmatched(ServerIssue i) {
    unmatchedByKey.put(i.key(), i);
    RuleKey ruleKey = i.ruleKey();
    if (!unmatchedByRuleAndKey.containsKey(ruleKey)) {
      unmatchedByRuleAndKey.put(ruleKey, new HashMap<String, ServerIssue>());
      unmatchedByRuleAndLineAndChecksum.put(ruleKey, new HashMap<Integer, Multimap<String, ServerIssue>>());
    }
    unmatchedByRuleAndKey.get(ruleKey).put(i.key(), i);
    Map<Integer, Multimap<String, ServerIssue>> unmatchedForRule = unmatchedByRuleAndLineAndChecksum.get(ruleKey);
    Integer lineNotNull = lineNotNull(i);
    if (!unmatchedForRule.containsKey(lineNotNull)) {
      unmatchedForRule.put(lineNotNull, HashMultimap.<String, ServerIssue>create());
    }
    Multimap<String, ServerIssue> unmatchedForRuleAndLine = unmatchedForRule.get(lineNotNull);
    String checksumNotNull = StringUtils.defaultString(i.checksum(), "");
    unmatchedForRuleAndLine.put(checksumNotNull, i);
  }

  private static Integer lineNotNull(ServerIssue i) {
    Integer line = i.line();
    return line != null ? line : 0;
  }

  void setMatch(BatchReport.Issue issue, ServerIssue matching) {
    matched.put(issue, matching);
    RuleKey ruleKey = matching.ruleKey();
    unmatchedByRuleAndKey.get(ruleKey).remove(matching.key());
    unmatchedByKey.remove(matching.key());
    Integer lineNotNull = lineNotNull(matching);
    String checksumNotNull = StringUtils.defaultString(matching.checksum(), "");
    unmatchedByRuleAndLineAndChecksum.get(ruleKey).get(lineNotNull).get(checksumNotNull).remove(matching);
  }
}
