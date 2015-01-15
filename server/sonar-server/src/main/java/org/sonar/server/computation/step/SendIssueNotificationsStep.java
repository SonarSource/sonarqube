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
package org.sonar.server.computation.step;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.issue.FinalIssues;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.issue.notification.IssueNotifications;
import org.sonar.server.util.CloseableIterator;

/**
 * Reads issues from disk cache and send related notifications. For performance reasons,
 * the standard notification DB queue is not used as a temporary storage. Notifications
 * are directly processed by {@link org.sonar.server.notifications.NotificationService}.
 */
public class SendIssueNotificationsStep implements ComputationStep {

  private final FinalIssues finalIssues;
  private final RuleCache rules;
  private final IssueNotifications service;

  public SendIssueNotificationsStep(FinalIssues finalIssues, RuleCache rules,
    IssueNotifications service) {
    this.finalIssues = finalIssues;
    this.rules = rules;
    this.service = service;
  }

  @Override
  public void execute(ComputationContext context) {
    NewIssuesStatistics newIssuesStatistics = new NewIssuesStatistics();
    CloseableIterator<DefaultIssue> issues = finalIssues.traverse();
    try {
      while (issues.hasNext()) {
        DefaultIssue issue = issues.next();
        if (issue.isNew() && issue.resolution() == null) {
          newIssuesStatistics.add(issue);
        } else if (issue.isChanged() && issue.mustSendNotifications()) {
          service.sendChanges(issue, null, rules.ruleName(issue.ruleKey()),
            context.getProject(), /* TODO */null, null, true);
        }
      }

    } finally {
      issues.close();
    }
    sendNewIssuesStatistics(context, newIssuesStatistics);
  }

  private void sendNewIssuesStatistics(ComputationContext context, NewIssuesStatistics newIssuesStatistics) {
    if (!newIssuesStatistics.isEmpty()) {
      ComponentDto project = context.getProject();
      Notification notification = new Notification("new-issues")
        .setFieldValue("projectName", project.longName())
        .setFieldValue("projectKey", project.key())
        .setDefaultMessage(newIssuesStatistics.size() + " new issues on " + project.longName() + ".\n")
        .setFieldValue("projectDate", DateUtils.formatDateTime(context.getAnalysisDate()))
        .setFieldValue("projectUuid", project.uuid())
        .setFieldValue("count", String.valueOf(newIssuesStatistics.size()));
      for (String severity : Severity.ALL) {
        notification.setFieldValue("count-" + severity, String.valueOf(newIssuesStatistics.issuesWithSeverity(severity)));
      }
      service.send(notification, true);
    }
  }

  @Override
  public String getDescription() {
    return "Send issue notifications";
  }

  static class NewIssuesStatistics {
    private final Multiset<String> set = HashMultiset.create();

    void add(Issue issue) {
      set.add(issue.severity());
    }

    int issuesWithSeverity(String severity) {
      return set.count(severity);
    }

    int size() {
      return set.size();
    }

    boolean isEmpty() {
      return set.isEmpty();
    }
  }

}
