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
package org.sonar.wsclient.issue;

import javax.annotation.Nullable;

import java.util.List;

/**
 * This client is a wrapper over the web services related to issues
 *
 * @since 3.6
 */
public interface IssueClient {

  /**
   * Wrap the web service /api/issues/search in order to search for issues.
   */
  Issues find(IssueQuery query);

  /**
   * Assign an existing issue to a user. A null assignee removes the assignee.
   *
   * @return the updated issue
   */
  Issue assign(String issueKey, @Nullable String assignee);

  /**
   * Assign an existing issue to current user.
   *
   * @return the updated issue
   */
  Issue assignToMe(String issueKey);


  /**
   * Change the severity of an existing issue. Supported values are "INFO", "MINOR",
   * "MAJOR", "CRITICAL" and "BLOCKER".
   *
   * @return the updated issue
   */
  Issue setSeverity(String issueKey, String severity);

  /**
   * Link an existing issue to an action plan. A null action plan unlinks the issue.
   */
  Issue plan(String issueKey, @Nullable String actionPlan);

  IssueComment addComment(String issueKey, String markdownText);

  Issue create(NewIssue issue);

  List<String> transitions(String issueKey);

  Issue doTransition(String issueKey, String transition);

  List<String> actions(String issueKey);

  Issue doAction(String issueKey, String action);

  /**
   * Execute bulk change on a list of issues
   */
  BulkChange bulkChange(BulkChangeQuery query);

  /**
   * @since 4.1
   *
   * @return the list of changes of an issue
   */
  List<IssueChange> changes(String issueKey);

}
