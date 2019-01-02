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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ShortBranchComponentsWithIssues;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.ShortBranchIssueDto;

import static org.sonar.api.utils.DateUtils.longToDate;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class ShortBranchIssuesLoader {

  private final ShortBranchComponentsWithIssues shortBranchComponentsWithIssues;
  private final DbClient dbClient;
  private final ComponentIssuesLoader componentIssuesLoader;

  public ShortBranchIssuesLoader(ShortBranchComponentsWithIssues shortBranchComponentsWithIssues, DbClient dbClient,
    ComponentIssuesLoader componentIssuesLoader) {
    this.shortBranchComponentsWithIssues = shortBranchComponentsWithIssues;
    this.dbClient = dbClient;
    this.componentIssuesLoader = componentIssuesLoader;
  }

  public Collection<ShortBranchIssue> loadCandidateIssuesForMergingInTargetBranch(Component component) {
    String componentKey = ComponentDto.removeBranchAndPullRequestFromKey(component.getDbKey());
    Set<String> uuids = shortBranchComponentsWithIssues.getUuids(componentKey);
    if (uuids.isEmpty()) {
      return Collections.emptyList();
    }

    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.issueDao().selectOpenByComponentUuids(session, uuids)
        .stream()
        .map(ShortBranchIssuesLoader::toShortBranchIssue)
        .collect(Collectors.toList());
    }
  }

  private static ShortBranchIssue toShortBranchIssue(ShortBranchIssueDto dto) {
    return new ShortBranchIssue(dto.getKey(), dto.getLine(), dto.getMessage(), dto.getChecksum(), dto.getRuleKey(), dto.getStatus(), dto.getBranchName(),
      longToDate(dto.getIssueCreationDate()));
  }

  public Map<ShortBranchIssue, DefaultIssue> loadDefaultIssuesWithChanges(Collection<ShortBranchIssue> lightIssues) {
    if (lightIssues.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, ShortBranchIssue> issuesByKey = lightIssues.stream().collect(Collectors.toMap(ShortBranchIssue::getKey, i -> i));
    try (DbSession session = dbClient.openSession(false)) {
      List<DefaultIssue> issues = dbClient.issueDao().selectByKeys(session, issuesByKey.keySet())
        .stream()
        .map(IssueDto::toDefaultIssue)
        .collect(toList(issuesByKey.size()));
      componentIssuesLoader.loadChanges(session, issues);
      return issues.stream()
        .collect(uniqueIndex(i -> issuesByKey.get(i.key()), i -> i, issues.size()));
    }
  }

}
