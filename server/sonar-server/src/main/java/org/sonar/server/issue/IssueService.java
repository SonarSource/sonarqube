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

package org.sonar.server.issue;

import com.google.common.collect.Multiset;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.persistence.DbSession;

import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;

public interface IssueService extends ServerComponent {
  List<String> listStatus();

  List<Transition> listTransitions(String issueKey);

  List<Transition> listTransitions(@Nullable Issue issue);

  Issue doTransition(String issueKey, String transitionKey);

  Issue assign(String issueKey, @Nullable String assignee);

  Issue plan(String issueKey, @Nullable String actionPlanKey);

  Issue setSeverity(String issueKey, String severity);

  DefaultIssue createManualIssue(String componentKey, RuleKey ruleKey, @Nullable Integer line, @Nullable String message, @Nullable String severity,
                                 @Nullable Double effortToFix);

  // TODO result should be replaced by an aggregation object in IssueIndex
  RulesAggregation findRulesByComponent(String componentKey, @Nullable Date periodDate, DbSession session);

  // TODO result should be replaced by an aggregation object in IssueIndex
  Multiset<String> findSeveritiesByComponent(String componentKey, @Nullable Date periodDate, DbSession session);

  DefaultIssue getIssueByKey(DbSession session, String key);

  DefaultIssue getIssueByKey(String key);

  List<Issue> search(List<String> issues);

}
