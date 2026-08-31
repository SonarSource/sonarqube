/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.issue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.telemetry.core.event.AnalyticsEventPublisher;
import org.sonar.telemetry.core.event.workflow.IssueTelemetryStatus;
import org.sonar.telemetry.core.event.workflow.IssueUpdatedBatchEvent;
import org.sonar.telemetry.core.event.workflow.IssueUpdatedBatchEvent.IssueUpdate;

import static java.util.function.Function.identity;

@ServerSide
public class IssueUpdatedTelemetryPublisher {

  private static final Logger LOG = LoggerFactory.getLogger(IssueUpdatedTelemetryPublisher.class);

  private final DbClient dbClient;
  private final AnalyticsEventPublisher analyticsEventPublisher;

  public IssueUpdatedTelemetryPublisher(DbClient dbClient, AnalyticsEventPublisher analyticsEventPublisher) {
    this.dbClient = dbClient;
    this.analyticsEventPublisher = analyticsEventPublisher;
  }

  public void publish(DbSession dbSession, Collection<DefaultIssue> issues) {
    if (!analyticsEventPublisher.isTelemetryEnabled()) {
      return;
    }
    try {
      List<DefaultIssue> changedIssues = issues.stream()
        .filter(IssueUpdatedTelemetryPublisher::hasStatusOrResolutionDiff)
        .toList();
      if (changedIssues.isEmpty()) {
        return;
      }

      Set<String> branchUuids = changedIssues.stream().map(DefaultIssue::getBranchUuid).collect(Collectors.toSet());
      Map<String, BranchDto> branchesByUuid = dbClient.branchDao().selectByUuids(dbSession, branchUuids).stream()
        .filter(branch -> branch.getBranchType() != BranchType.PULL_REQUEST)
        .collect(Collectors.toMap(BranchDto::getUuid, identity()));

      List<IssueUpdatedBatchEvent> events = changedIssues.stream()
        .filter(issue -> branchesByUuid.containsKey(issue.getBranchUuid()))
        .collect(Collectors.groupingBy(DefaultIssue::getBranchUuid, LinkedHashMap::new, Collectors.toList()))
        .entrySet().stream()
        .flatMap(entry -> toEvents(branchesByUuid.get(entry.getKey()), entry.getValue()).stream())
        .toList();
      if (events.isEmpty()) {
        return;
      }

      analyticsEventPublisher.publishAll(IssueUpdatedBatchEvent.TYPE, events);
    } catch (RuntimeException e) {
      LOG.warn("Failed to send issue update telemetry", e);
    }
  }

  private static List<IssueUpdatedBatchEvent> toEvents(BranchDto branch, List<DefaultIssue> branchIssues) {
    String branchType = branch.getBranchType().name();

    List<IssueUpdate> issueUpdates = branchIssues.stream()
      .map(IssueUpdatedTelemetryPublisher::toIssueUpdate)
      .toList();

    List<IssueUpdatedBatchEvent> events = new ArrayList<>();
    for (int fromIndex = 0; fromIndex < issueUpdates.size(); fromIndex += IssueUpdatedBatchEvent.MAX_ISSUES_PER_EVENT) {
      int toIndex = Math.min(fromIndex + IssueUpdatedBatchEvent.MAX_ISSUES_PER_EVENT, issueUpdates.size());
      events.add(new IssueUpdatedBatchEvent(branch.getProjectUuid(), branch.getUuid(), branchType, List.copyOf(issueUpdates.subList(fromIndex, toIndex))));
    }
    return events;
  }

  private static IssueUpdate toIssueUpdate(DefaultIssue issue) {
    Long issueResolvedAt = issue.closeDate() != null ? issue.closeDate().getTime() : null;
    return new IssueUpdate(
      issue.key(),
      issue.ruleKey().toString(),
      issue.creationDate().getTime(),
      IssueTelemetryStatus.of(issue.status(), issue.resolution()),
      issueResolvedAt);
  }

  private static boolean hasStatusOrResolutionDiff(DefaultIssue issue) {
    FieldDiffs currentChange = issue.currentChange();
    return currentChange != null
      && (currentChange.get(IssueFieldsSetter.STATUS) != null || currentChange.get(IssueFieldsSetter.RESOLUTION) != null);
  }
}
