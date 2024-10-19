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
package org.sonar.server.projectanalysis.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_ANALYSIS;

public class DeleteAction implements ProjectAnalysesWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;

  public DeleteAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("delete")
      .setDescription("Delete a project analysis.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer System'</li>" +
        "  <li>'Administer' rights on the project of the specified analysis</li>" +
        "</ul>")
      .setSince("6.3")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_ANALYSIS)
      .setDescription("Analysis key")
      .setExampleValue(Uuids.UUID_EXAMPLE_04)
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String analysisUuid = request.mandatoryParam(PARAM_ANALYSIS);

    try (DbSession dbSession = dbClient.openSession(false)) {
      SnapshotDto analysis = dbClient.snapshotDao().selectByUuid(dbSession, analysisUuid)
        .orElseThrow(() -> analysisNotFoundException(analysisUuid));
      if (STATUS_UNPROCESSED.equals(analysis.getStatus())) {
        throw analysisNotFoundException(analysisUuid);
      }
      userSession.checkComponentUuidPermission(UserRole.ADMIN, analysis.getRootComponentUuid());

      checkArgument(!analysis.getLast(), "The last analysis '%s' cannot be deleted", analysisUuid);
      checkNotUsedInNewCodePeriod(dbSession, analysis);

      analysis.setStatus(STATUS_UNPROCESSED);
      dbClient.snapshotDao().update(dbSession, analysis);
      dbSession.commit();
    }
    response.noContent();
  }

  private void checkNotUsedInNewCodePeriod(DbSession dbSession, SnapshotDto analysis) {
    boolean isSetAsBaseline = dbClient.newCodePeriodDao().existsByProjectAnalysisUuid(dbSession, analysis.getUuid());
    checkArgument(!isSetAsBaseline,
      "The analysis '%s' can not be deleted because it is set as a new code period baseline", analysis.getUuid());

  }

  private static NotFoundException analysisNotFoundException(String analysis) {
    return new NotFoundException(format("Analysis '%s' not found", analysis));
  }
}
