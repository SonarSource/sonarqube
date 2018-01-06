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
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.issue.IssueCache;
import org.sonar.server.computation.task.projectanalysis.issue.RuleRepository;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotificationFactory;
import org.sonar.server.issue.notification.NewIssuesStatistics;
import org.sonar.server.notification.NotificationService;

import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

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
  private final NewIssuesNotificationFactory newIssuesNotificationFactory;
  private Map<String, Component> componentsByDbKey;

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
    long analysisDate = analysisMetadataHolder.getAnalysisDate();
    Predicate<DefaultIssue> isOnLeakPredicate = i -> i.isNew() && i.creationDate().getTime() >= truncateToSeconds(analysisDate);
    NewIssuesStatistics newIssuesStats = new NewIssuesStatistics(isOnLeakPredicate);
    try (CloseableIterator<DefaultIssue> issues = issueCache.traverse()) {
      processIssues(newIssuesStats, issues, project);
    }
    if (newIssuesStats.hasIssuesOnLeak()) {
      sendNewIssuesNotification(newIssuesStats, project, analysisDate);
      sendNewIssuesNotificationToAssignees(newIssuesStats, project, analysisDate);
    }
  }

  /**
   * Truncated the analysis date to seconds before comparing it to {@link Issue#creationDate()} is required because
   * {@link DefaultIssue#setCreationDate(Date)} does it.
   */
  private static long truncateToSeconds(long analysisDate) {
    Instant instant = new Date(analysisDate).toInstant();
    instant = instant.truncatedTo(ChronoUnit.SECONDS);
    return Date.from(instant).getTime();
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
    changeNotification.setProject(project.getPublicKey(), project.getName(), getBranchName());
    getComponentKey(issue).ifPresent(c -> changeNotification.setComponent(c.getPublicKey(), c.getName()));
    service.deliver(changeNotification);
  }

  private void sendNewIssuesNotification(NewIssuesStatistics statistics, Component project, long analysisDate) {
    NewIssuesStatistics.Stats globalStatistics = statistics.globalStatistics();
    NewIssuesNotification notification = newIssuesNotificationFactory
      .newNewIssuesNotication()
      .setProject(project.getPublicKey(), project.getName(), getBranchName())
      .setProjectVersion(project.getReportAttributes().getVersion())
      .setAnalysisDate(new Date(analysisDate))
      .setStatistics(project.getName(), globalStatistics)
      .setDebt(Duration.create(globalStatistics.effort().getOnLeak()));
    service.deliver(notification);
  }

  private void sendNewIssuesNotificationToAssignees(NewIssuesStatistics statistics, Component project, long analysisDate) {
    statistics.getAssigneesStatistics().entrySet()
      .stream()
      .filter(e -> e.getValue().hasIssuesOnLeak())
      .forEach(e -> {
        String assignee = e.getKey();
        NewIssuesStatistics.Stats assigneeStatistics = e.getValue();
        MyNewIssuesNotification myNewIssuesNotification = newIssuesNotificationFactory
          .newMyNewIssuesNotification()
          .setAssignee(assignee);
        myNewIssuesNotification
          .setProject(project.getPublicKey(), project.getName(), getBranchName())
          .setProjectVersion(project.getReportAttributes().getVersion())
          .setAnalysisDate(new Date(analysisDate))
          .setStatistics(project.getName(), assigneeStatistics)
          .setDebt(Duration.create(assigneeStatistics.effort().getOnLeak()));

        service.deliver(myNewIssuesNotification);
      });
  }

  private Optional<Component> getComponentKey(DefaultIssue issue) {
    if (componentsByDbKey == null) {
      final ImmutableMap.Builder<String, Component> builder = ImmutableMap.builder();
      new DepthTraversalTypeAwareCrawler(
        new TypeAwareVisitorAdapter(CrawlerDepthLimit.LEAVES, POST_ORDER) {
          @Override
          public void visitAny(Component component) {
            builder.put(component.getKey(), component);
          }
        }).visit(this.treeRootHolder.getRoot());
      this.componentsByDbKey = builder.build();
    }
    return Optional.ofNullable(componentsByDbKey.get(issue.componentKey()));
  }

  @Override
  public String getDescription() {
    return "Send issue notifications";
  }

  private String getBranchName() {
    Branch branch = analysisMetadataHolder.getBranch();
    return branch.isMain() ? null : branch.getName();
  }

}
