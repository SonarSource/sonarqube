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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ShortBranchComponentsWithIssues;

public class ResolvedShortBranchIssuesFactory {

  private final ShortBranchComponentsWithIssues shortBranchComponentsWithIssues;
  private final DbClient dbClient;

  public ResolvedShortBranchIssuesFactory(ShortBranchComponentsWithIssues shortBranchComponentsWithIssues, DbClient dbClient) {
    this.shortBranchComponentsWithIssues = shortBranchComponentsWithIssues;
    this.dbClient = dbClient;
  }

  public Collection<DefaultIssue> create(Component component) {
    Set<String> uuids = shortBranchComponentsWithIssues.getUuids(component.getKey());
    if (uuids.isEmpty()) {
      return Collections.emptyList();
    }
    try (DbSession session = dbClient.openSession(false)) {
      return uuids
        .stream()
        .flatMap(uuid -> dbClient.issueDao().selectResolvedOrConfirmedByComponentUuid(session, uuid).stream())
        .map(IssueDto::toDefaultIssue)
        .collect(Collectors.toList());
    }
  }
}
