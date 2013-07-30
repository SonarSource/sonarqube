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
package org.sonar.plugins.core.issue.notification;

import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.issue.IssueCache;
import org.sonar.core.DryRunIncompatible;
import org.sonar.core.issue.IssueNotifications;

/**
 * @since 3.6
 */
@DryRunIncompatible
public class SendIssueNotificationsPostJob implements PostJob {

  private final IssueCache issueCache;
  private final IssueNotifications notifications;
  private final RuleFinder ruleFinder;

  public SendIssueNotificationsPostJob(IssueCache issueCache, IssueNotifications notifications, RuleFinder ruleFinder) {
    this.issueCache = issueCache;
    this.notifications = notifications;
    this.ruleFinder = ruleFinder;
  }

  @Override
  public void executeOn(Project project, SensorContext context) {
    sendNotifications(project);
  }

  private void sendNotifications(Project project) {
    int newIssues = 0;
    IssueChangeContext context = IssueChangeContext.createScan(project.getAnalysisDate());
    for (DefaultIssue issue : issueCache.all()) {
      if (issue.isNew() && issue.resolution() == null) {
        newIssues++;
      }
      if (!issue.isNew() && issue.isChanged() && issue.mustSendNotifications()) {
        Rule rule = ruleFinder.findByKey(issue.ruleKey());
        // TODO warning - rules with status REMOVED are currently ignored, but should not
        if (rule != null) {
          notifications.sendChanges(issue, context, rule, project, null);
        }
      }
    }
    if (newIssues > 0) {
      notifications.sendNewIssues(project, newIssues);
    }
  }
}
