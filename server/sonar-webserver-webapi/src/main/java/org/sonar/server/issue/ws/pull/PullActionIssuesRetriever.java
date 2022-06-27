/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.issue.ws.pull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueQueryParams;

import static org.sonar.db.issue.IssueDao.DEFAULT_PAGE_SIZE;

public class PullActionIssuesRetriever {

  private final DbClient dbClient;
  private final IssueQueryParams issueQueryParams;

  public PullActionIssuesRetriever(DbClient dbClient, IssueQueryParams queryParams) {
    this.dbClient = dbClient;
    this.issueQueryParams = queryParams;
  }

  public void processIssuesByBatch(DbSession dbSession, Set<String> issueKeysSnapshot, Consumer<List<IssueDto>> listConsumer) {
    int nextPage = 1;
    boolean hasMoreIssues = true;

    while (hasMoreIssues) {
      List<IssueDto> issueDtos = nextOpenIssues(dbSession, nextPage);
      listConsumer.accept(filterDuplicateIssues(issueDtos, issueKeysSnapshot));
      nextPage++;
      if (issueDtos.isEmpty() || issueDtos.size() < DEFAULT_PAGE_SIZE) {
        hasMoreIssues = false;
      }
    }
  }

  private static List<IssueDto> filterDuplicateIssues(List<IssueDto> issues, Set<String> issueKeysSnapshot) {
    return issues
      .stream()
      .filter(issue -> isUniqueIssue(issue.getKee(), issueKeysSnapshot))
      .collect(Collectors.toList());
  }

  private static boolean isUniqueIssue(String issueKey, Set<String> issueKeysSnapshot) {
    boolean isUniqueIssue = issueKeysSnapshot.contains(issueKey);

    if (isUniqueIssue) {
      issueKeysSnapshot.remove(issueKey);
    }

    return isUniqueIssue;
  }

  public List<String> retrieveClosedIssues(DbSession dbSession) {
    return dbClient.issueDao().selectRecentlyClosedIssues(dbSession, issueQueryParams);
  }

  private List<IssueDto> nextOpenIssues(DbSession dbSession, int nextPage) {
    return dbClient.issueDao().selectByBranch(dbSession, issueQueryParams, nextPage);
  }
}
