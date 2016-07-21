/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.collect.ImmutableSet;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.issue.IssueCache;
import org.sonar.server.computation.task.projectanalysis.issue.RuleRepository;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotificationFactory;
import org.sonar.server.issue.notification.NewIssuesStatistics;
import org.sonar.server.notification.NotificationService;

/**
 * Reads issues from disk cache and send related notifications. For performance reasons,
 * the standard notification DB queue is not used as a temporary storage. Notifications
 * are directly processed by {@link NotificationService}.
 */
public class SendIssueNotificationsStep implements ComputationStep {
  /**
   * Types of the notifications sent by this step
   */
  static final Set<String> NOTIF_TYPES = ImmutableSet.of(IssueChangeNotification.TYPE, NewIssuesNotification.TYPE, MyNewIssuesNotification.MY_NEW_ISSUES_NOTIF_TYPE);

  private final IssueCache issueCache;
  private final RuleRepository rules;
  private final TreeRootHolder treeRootHolder;
  private final NotificationService service;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private NewIssuesNotificationFactory newIssuesNotificationFactory;

  public SendIssueNotificationsStep(IssueCache issueCache, RuleRepository rules, TreeRootHolder treeRootHolder,
    NotificationService service, AnalysisMetadataHolder analysisMetadataHolder,
    NewIssuesNotificationFactory newIssuesNotificationFactory) {
    this.issueCache = issueCache;
    this.rules = rules;
    this.treeRootHolder = treeRootHolder;
    this.service = service;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.newIssuesNotificationFactory = newIssuesNotificationFactory;
  }

  @Override
  public void execute() {
    Component project = treeRootHolder.getRoot();
    if (service.hasProjectSubscribersForTypes(project.getUuid(), NOTIF_TYPES)) {
      doExecute(project);
    }
  }

  private void doExecute(Component project) {
    NewIssuesStatistics newIssuesStats = new NewIssuesStatistics();
    CloseableIterator<DefaultIssue> issues = issueCache.traverse();
    try {
      processIssues(newIssuesStats, issues, project);
    } finally {
      issues.close();
    }
    if (newIssuesStats.hasIssues()) {
      long analysisDate = analysisMetadataHolder.getAnalysisDate();
      sendNewIssuesNotification(newIssuesStats, project, analysisDate);
      sendNewIssuesNotificationToAssignees(newIssuesStats, project, analysisDate);
    }
  }

  private void processIssues(NewIssuesStatistics newIssuesStats, CloseableIterator<DefaultIssue> issues, Component project) {
    while (issues.hasNext()) {
      DefaultIssue issue = issues.next();
      if (issue.isNew() && issue.resolution() == null) {
        newIssuesStats.add(issue);
      } else if (issue.isChanged() && issue.mustSendNotifications()) {
        sendIssueChangeNotification(issue, project);
      }
    }
  }

  private void sendIssueChangeNotification(DefaultIssue issue, Component project) {
    IssueChangeNotification changeNotification = new IssueChangeNotification();
    changeNotification.setRuleName(rules.getByKey(issue.ruleKey()).getName());
    changeNotification.setIssue(issue);
    changeNotification.setProject(project.getKey(), project.getName());
    service.deliver(changeNotification);
  }

  private void sendNewIssuesNotification(NewIssuesStatistics statistics, Component project, long analysisDate) {
    NewIssuesStatistics.Stats globalStatistics = statistics.globalStatistics();
    NewIssuesNotification notification = newIssuesNotificationFactory
      .newNewIssuesNotication()
      .setProject(project.getKey(), project.getUuid(), project.getName())
      .setAnalysisDate(new Date(analysisDate))
      .setStatistics(project.getName(), globalStatistics)
      .setDebt(globalStatistics.debt());
    service.deliver(notification);
  }

  private void sendNewIssuesNotificationToAssignees(NewIssuesStatistics statistics, Component project, long analysisDate) {
    // send email to each user having issues
    for (Map.Entry<String, NewIssuesStatistics.Stats> assigneeAndStatisticsTuple : statistics.assigneesStatistics().entrySet()) {
      String assignee = assigneeAndStatisticsTuple.getKey();
      NewIssuesStatistics.Stats assigneeStatistics = assigneeAndStatisticsTuple.getValue();
      MyNewIssuesNotification myNewIssuesNotification = newIssuesNotificationFactory
        .newMyNewIssuesNotification()
        .setAssignee(assignee);
      myNewIssuesNotification
        .setProject(project.getKey(), project.getUuid(), project.getName())
        .setAnalysisDate(new Date(analysisDate))
        .setStatistics(project.getName(), assigneeStatistics)
        .setDebt(assigneeStatistics.debt());

      service.deliver(myNewIssuesNotification);
    }
  }

  @Override
  public String getDescription() {
    return "Send issue notifications";
  }

}
