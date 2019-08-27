/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.api.issue.Issue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.IssueCache;
import org.sonar.ce.task.projectanalysis.notification.NotificationFactory;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchType;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.notification.IssuesChangesNotification;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesStatistics;
import org.sonar.server.notification.NotificationService;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.BranchType.SHORT;

/**
 * Reads issues from disk cache and send related notifications. For performance reasons,
 * the standard notification DB queue is not used as a temporary storage. Notifications
 * are directly processed by {@link NotificationService}.
 */
public class SendIssueNotificationsStep implements ComputationStep {
  /**
   * Types of the notifications sent by this step
   */
  static final Set<Class<? extends Notification>> NOTIF_TYPES = ImmutableSet.of(NewIssuesNotification.class, MyNewIssuesNotification.class, IssuesChangesNotification.class);

  private final IssueCache issueCache;
  private final TreeRootHolder treeRootHolder;
  private final NotificationService service;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final NotificationFactory notificationFactory;
  private final DbClient dbClient;

  public SendIssueNotificationsStep(IssueCache issueCache, TreeRootHolder treeRootHolder,
    NotificationService service, AnalysisMetadataHolder analysisMetadataHolder,
    NotificationFactory notificationFactory, DbClient dbClient) {
    this.issueCache = issueCache;
    this.treeRootHolder = treeRootHolder;
    this.service = service;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.notificationFactory = notificationFactory;
    this.dbClient = dbClient;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    BranchType branchType = analysisMetadataHolder.getBranch().getType();
    if (branchType == PULL_REQUEST || branchType == SHORT) {
      return;
    }

    Component project = treeRootHolder.getRoot();
    NotificationStatistics notificationStatistics = new NotificationStatistics();
    if (service.hasProjectSubscribersForTypes(analysisMetadataHolder.getProject().getUuid(), NOTIF_TYPES)) {
      doExecute(notificationStatistics, project);
    }
    notificationStatistics.dumpTo(context);
  }

  private void doExecute(NotificationStatistics notificationStatistics, Component project) {
    long analysisDate = analysisMetadataHolder.getAnalysisDate();
    Predicate<DefaultIssue> onCurrentAnalysis = i -> i.isNew() && i.creationDate().getTime() >= truncateToSeconds(analysisDate);
    NewIssuesStatistics newIssuesStats = new NewIssuesStatistics(onCurrentAnalysis);
    Map<String, UserDto> assigneesByUuid;
    try (DbSession dbSession = dbClient.openSession(false)) {
      Iterable<DefaultIssue> iterable = issueCache::traverse;
      Set<String> assigneeUuids = stream(iterable.spliterator(), false).map(DefaultIssue::assignee).filter(Objects::nonNull).collect(Collectors.toSet());
      assigneesByUuid = dbClient.userDao().selectByUuids(dbSession, assigneeUuids).stream().collect(toMap(UserDto::getUuid, dto -> dto));
    }

    try (CloseableIterator<DefaultIssue> issues = issueCache.traverse()) {
      processIssues(newIssuesStats, issues, assigneesByUuid, notificationStatistics);
    }
    if (newIssuesStats.hasIssuesOnCurrentAnalysis()) {
      sendNewIssuesNotification(newIssuesStats, project, assigneesByUuid, analysisDate, notificationStatistics);
      sendMyNewIssuesNotification(newIssuesStats, project, assigneesByUuid, analysisDate, notificationStatistics);
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

  private void processIssues(NewIssuesStatistics newIssuesStats, CloseableIterator<DefaultIssue> issues,
    Map<String, UserDto> assigneesByUuid, NotificationStatistics notificationStatistics) {
    int batchSize = 1000;
    Set<DefaultIssue> changedIssuesToNotify = new HashSet<>(batchSize);
    while (issues.hasNext()) {
      DefaultIssue issue = issues.next();
      if (issue.type() != RuleType.SECURITY_HOTSPOT) {
        if (issue.isNew() && issue.resolution() == null) {
          newIssuesStats.add(issue);
        } else if (issue.isChanged() && issue.mustSendNotifications()) {
          changedIssuesToNotify.add(issue);
        }
      }

      if (changedIssuesToNotify.size() >= batchSize) {
        sendIssuesChangesNotification(changedIssuesToNotify, assigneesByUuid, notificationStatistics);
        changedIssuesToNotify.clear();
      }
    }

    if (!changedIssuesToNotify.isEmpty()) {
      sendIssuesChangesNotification(changedIssuesToNotify, assigneesByUuid, notificationStatistics);
    }
  }

  private void sendIssuesChangesNotification(Set<DefaultIssue> issues, Map<String, UserDto> assigneesByUuid, NotificationStatistics notificationStatistics) {
    IssuesChangesNotification notification = notificationFactory.newIssuesChangesNotification(issues, assigneesByUuid);

    notificationStatistics.issueChangesDeliveries += service.deliverEmails(singleton(notification));
    notificationStatistics.issueChanges++;

    // compatibility with old API
    notificationStatistics.issueChangesDeliveries += service.deliver(notification);
  }

  private void sendNewIssuesNotification(NewIssuesStatistics statistics, Component project, Map<String, UserDto> assigneesByUuid,
    long analysisDate, NotificationStatistics notificationStatistics) {
    NewIssuesStatistics.Stats globalStatistics = statistics.globalStatistics();
    NewIssuesNotification notification = notificationFactory
      .newNewIssuesNotification(assigneesByUuid)
      .setProject(project.getKey(), project.getName(), getBranchName(), getPullRequest())
      .setProjectVersion(project.getProjectAttributes().getProjectVersion())
      .setAnalysisDate(new Date(analysisDate))
      .setStatistics(project.getName(), globalStatistics)
      .setDebt(Duration.create(globalStatistics.effort().getOnCurrentAnalysis()));
    notificationStatistics.newIssuesDeliveries += service.deliverEmails(singleton(notification));
    notificationStatistics.newIssues++;

    // compatibility with old API
    notificationStatistics.newIssuesDeliveries += service.deliver(notification);
  }

  private void sendMyNewIssuesNotification(NewIssuesStatistics statistics, Component project, Map<String, UserDto> assigneesByUuid, long analysisDate,
    NotificationStatistics notificationStatistics) {
    Map<String, UserDto> userDtoByUuid = loadUserDtoByUuid(statistics);
    Set<MyNewIssuesNotification> myNewIssuesNotifications = statistics.getAssigneesStatistics().entrySet()
      .stream()
      .filter(e -> e.getValue().hasIssuesOnCurrentAnalysis())
      .map(e -> {
        String assigneeUuid = e.getKey();
        NewIssuesStatistics.Stats assigneeStatistics = e.getValue();
        MyNewIssuesNotification myNewIssuesNotification = notificationFactory
          .newMyNewIssuesNotification(assigneesByUuid)
          .setAssignee(userDtoByUuid.get(assigneeUuid));
        myNewIssuesNotification
          .setProject(project.getKey(), project.getName(), getBranchName(), getPullRequest())
          .setProjectVersion(project.getProjectAttributes().getProjectVersion())
          .setAnalysisDate(new Date(analysisDate))
          .setStatistics(project.getName(), assigneeStatistics)
          .setDebt(Duration.create(assigneeStatistics.effort().getOnCurrentAnalysis()));

        return myNewIssuesNotification;
      })
      .collect(toSet(statistics.getAssigneesStatistics().size()));

    notificationStatistics.myNewIssuesDeliveries += service.deliverEmails(myNewIssuesNotifications);
    notificationStatistics.myNewIssues += myNewIssuesNotifications.size();

    // compatibility with old API
    myNewIssuesNotifications
      .forEach(e -> notificationStatistics.myNewIssuesDeliveries += service.deliver(e));
  }

  private Map<String, UserDto> loadUserDtoByUuid(NewIssuesStatistics statistics) {
    List<Map.Entry<String, NewIssuesStatistics.Stats>> entriesWithIssuesOnLeak = statistics.getAssigneesStatistics().entrySet()
      .stream().filter(e -> e.getValue().hasIssuesOnCurrentAnalysis()).collect(toList());
    List<String> assigneeUuids = entriesWithIssuesOnLeak.stream().map(Map.Entry::getKey).collect(toList());
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.userDao().selectByUuids(dbSession, assigneeUuids).stream().collect(toMap(UserDto::getUuid, u -> u));
    }
  }

  @Override
  public String getDescription() {
    return "Send issue notifications";
  }

  @CheckForNull
  private String getBranchName() {
    Branch branch = analysisMetadataHolder.getBranch();
    return branch.isMain() || branch.getType() == PULL_REQUEST ? null : branch.getName();
  }

  @CheckForNull
  private String getPullRequest() {
    Branch branch = analysisMetadataHolder.getBranch();
    return branch.getType() == PULL_REQUEST ? analysisMetadataHolder.getPullRequestKey() : null;
  }

  private static class NotificationStatistics {
    private int issueChanges = 0;
    private int issueChangesDeliveries = 0;
    private int newIssues = 0;
    private int newIssuesDeliveries = 0;
    private int myNewIssues = 0;
    private int myNewIssuesDeliveries = 0;

    private void dumpTo(ComputationStep.Context context) {
      context.getStatistics()
        .add("newIssuesNotifs", newIssues)
        .add("newIssuesDeliveries", newIssuesDeliveries)
        .add("myNewIssuesNotifs", myNewIssues)
        .add("myNewIssuesDeliveries", myNewIssuesDeliveries)
        .add("changesNotifs", issueChanges)
        .add("changesDeliveries", issueChangesDeliveries);
    }
  }
}
