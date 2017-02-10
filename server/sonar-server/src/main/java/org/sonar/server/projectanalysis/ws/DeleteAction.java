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
package org.sonar.server.projectanalysis.ws;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
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
import org.sonarqube.ws.client.projectanalysis.DeleteRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_ANALYSIS;

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
    Stream.of(request)
      .map(toWsRequest())
      .forEach(deleteAnalysis());

    response.noContent();

  }

  private static Function<Request, DeleteRequest> toWsRequest() {
    return request -> new DeleteRequest(request.mandatoryParam(PARAM_ANALYSIS));
  }

  private Consumer<DeleteRequest> deleteAnalysis() {
    return request -> {
      try (DbSession dbSession = dbClient.openSession(false)) {
        SnapshotDto analysis = dbClient.snapshotDao().selectByUuid(dbSession, request.getAnalysis())
          .orElseThrow(() -> analysisNotFoundException(request.getAnalysis()));
        if (STATUS_UNPROCESSED.equals(analysis.getStatus())) {
          throw analysisNotFoundException(request.getAnalysis());
        }
        userSession.checkComponentUuidPermission(UserRole.ADMIN, analysis.getComponentUuid());
        checkArgument(!analysis.getLast(), "The last analysis '%s' cannot be deleted", request.getAnalysis());
        analysis.setStatus(STATUS_UNPROCESSED);
        dbClient.snapshotDao().update(dbSession, analysis);
        dbSession.commit();
      }
    };
  }

  private static NotFoundException analysisNotFoundException(String analysis) {
    return new NotFoundException(format("Analysis '%s' not found", analysis));
  }
}
