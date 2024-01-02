/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.ce.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueSyncProgress;
import org.sonarqube.ws.Ce.IndexationStatusWsResponse;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class IndexationStatusAction implements CeWsAction {
  private final IssueIndexSyncProgressChecker issueIndexSyncChecker;
  private final DbClient dbClient;

  public IndexationStatusAction(DbClient dbClient, IssueIndexSyncProgressChecker issueIndexSyncChecker) {
    this.dbClient = dbClient;
    this.issueIndexSyncChecker = issueIndexSyncChecker;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("indexation_status")
      .setDescription("Returns the count of projects with completed issue indexation.")
      .setResponseExample(getClass().getResource("indexation_status-example.json"))
      .setChangelog(new Change("10.2", "Project count is returned instead of branch percentage."))
      .setHandler(this)
      .setInternal(true)
      .setSince("8.4");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    IndexationStatusWsResponse activityResponse = doHandle();
    writeProtobuf(activityResponse, wsRequest, wsResponse);
  }

  private IndexationStatusWsResponse doHandle() {
    IssueSyncProgress issueSyncProgress;
    try (DbSession dbSession = dbClient.openSession(false)) {
      issueSyncProgress = issueIndexSyncChecker.getIssueSyncProgress(dbSession);
    }

    return IndexationStatusWsResponse.newBuilder()
      .setIsCompleted(issueSyncProgress.isCompleted())
      .setHasFailures(issueSyncProgress.hasFailures())
      .setCompletedCount(issueSyncProgress.getCompletedCount())
      .setTotal(issueSyncProgress.getTotal())
      .build();
  }

}
