/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualitygate.changeevent;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.qualitygate.LiveQualityGateFactory;
import org.sonar.server.settings.ProjectConfigurationLoader;

import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class IssueChangeTriggerImpl implements IssueChangeTrigger {
  private static final Set<String> MEANINGFUL_TRANSITIONS = ImmutableSet.of(
    DefaultTransitions.RESOLVE, DefaultTransitions.FALSE_POSITIVE, DefaultTransitions.WONT_FIX, DefaultTransitions.REOPEN);
  private final DbClient dbClient;
  private final ProjectConfigurationLoader projectConfigurationLoader;
  private final QGChangeEventListeners qgEventListeners;
  private final LiveQualityGateFactory liveQualityGateFactory;

  public IssueChangeTriggerImpl(DbClient dbClient, ProjectConfigurationLoader projectConfigurationLoader,
    QGChangeEventListeners qgEventListeners, LiveQualityGateFactory liveQualityGateFactory) {
    this.dbClient = dbClient;
    this.projectConfigurationLoader = projectConfigurationLoader;
    this.qgEventListeners = qgEventListeners;
    this.liveQualityGateFactory = liveQualityGateFactory;
  }

  @Override
  public void onChange(IssueChangeData issueChangeData, IssueChange issueChange, IssueChangeContext context) {
    if (isEmpty(issueChangeData) || !isUserChangeContext(context) || !isRelevant(issueChange) || qgEventListeners.isEmpty()) {
      return;
    }

    broadcastToListeners(issueChangeData);
  }

  private static boolean isRelevant(IssueChange issueChange) {
    return issueChange.getTransitionKey().map(IssueChangeTriggerImpl::isMeaningfulTransition).orElse(true);
  }

  private static boolean isEmpty(IssueChangeData issueChangeData) {
    return issueChangeData.getIssues().isEmpty();
  }

  private static boolean isUserChangeContext(IssueChangeContext context) {
    return context.login() != null;
  }

  private static boolean isMeaningfulTransition(String transitionKey) {
    return MEANINGFUL_TRANSITIONS.contains(transitionKey);
  }

  private void broadcastToListeners(IssueChangeData issueChangeData) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, ComponentDto> branchesByUuid = getBranchComponents(dbSession, issueChangeData);
      if (branchesByUuid.isEmpty()) {
        return;
      }

      Set<String> branchProjectUuids = branchesByUuid.values().stream()
        .map(ComponentDto::uuid)
        .collect(toSet(branchesByUuid.size()));
      Set<BranchDto> shortBranches = dbClient.branchDao().selectByUuids(dbSession, branchProjectUuids)
        .stream()
        .filter(branchDto -> branchDto.getBranchType() == BranchType.SHORT)
        .collect(toSet(branchesByUuid.size()));
      if (shortBranches.isEmpty()) {
        return;
      }

      Map<String, Configuration> configurationByUuid = projectConfigurationLoader.loadProjectConfigurations(dbSession,
        shortBranches.stream().map(shortBranch -> branchesByUuid.get(shortBranch.getUuid())).collect(Collectors.toSet()));
      Set<String> shortBranchesComponentUuids = shortBranches.stream().map(BranchDto::getUuid).collect(toSet(shortBranches.size()));
      Map<String, SnapshotDto> analysisByProjectUuid = dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(
        dbSession,
        shortBranchesComponentUuids)
        .stream()
        .collect(uniqueIndex(SnapshotDto::getComponentUuid));

      List<QGChangeEvent> qgChangeEvents = shortBranches
        .stream()
        .map(shortBranch -> {
          ComponentDto branch = branchesByUuid.get(shortBranch.getUuid());
          SnapshotDto analysis = analysisByProjectUuid.get(shortBranch.getUuid());
          if (branch != null && analysis != null) {
            Configuration configuration = configurationByUuid.get(shortBranch.getUuid());

            return new QGChangeEvent(branch, shortBranch, analysis, configuration,
              () -> Optional.of(liveQualityGateFactory.buildForShortLivedBranch(branch)));
          }
          return null;
        })
        .filter(Objects::nonNull)
        .collect(MoreCollectors.toList(shortBranches.size()));
      qgEventListeners.broadcast(Trigger.ISSUE_CHANGE, qgChangeEvents);
    }
  }

  private Map<String, ComponentDto> getBranchComponents(DbSession dbSession, IssueChangeData issueChangeData) {
    Set<String> projectUuids = issueChangeData.getIssues().stream()
      .map(DefaultIssue::projectUuid)
      .collect(toSet());
    Set<String> missingProjectUuids = ImmutableSet.copyOf(Sets.difference(
      projectUuids,
      issueChangeData.getComponents()
        .stream()
        .map(ComponentDto::uuid)
        .collect(Collectors.toSet())));
    if (missingProjectUuids.isEmpty()) {
      return issueChangeData.getComponents()
        .stream()
        .filter(c -> projectUuids.contains(c.uuid()))
        .filter(componentDto -> componentDto.getMainBranchProjectUuid() != null)
        .collect(uniqueIndex(ComponentDto::uuid));
    }
    return Stream.concat(
      issueChangeData.getComponents().stream().filter(c -> projectUuids.contains(c.uuid())),
      dbClient.componentDao().selectByUuids(dbSession, missingProjectUuids).stream())
      .filter(componentDto -> componentDto.getMainBranchProjectUuid() != null)
      .collect(uniqueIndex(ComponentDto::uuid));
  }
}
