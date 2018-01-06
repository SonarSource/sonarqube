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
package org.sonar.server.projectanalysis.ws;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.server.projectanalysis.ws.EventValidator.checkModifiable;
import static org.sonar.server.projectanalysis.ws.EventCategory.OTHER;
import static org.sonar.server.projectanalysis.ws.EventCategory.VERSION;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_EVENT;

public class DeleteEventAction implements ProjectAnalysesWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;

  public DeleteEventAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("delete_event")
      .setDescription("Delete a project analysis event.<br>" +
        "Only event of category '%s' and '%s' can be deleted.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer System'</li>" +
        "  <li>'Administer' rights on the specified project</li>" +
        "</ul>",
        VERSION.name(), OTHER.name())
      .setPost(true)
      .setSince("6.3")
      .setHandler(this);

    action.createParam(PARAM_EVENT)
      .setDescription("Event key")
      .setExampleValue(Uuids.UUID_EXAMPLE_02)
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String eventP = request.mandatoryParam(PARAM_EVENT);
    try (DbSession dbSession = dbClient.openSession(false)) {
      Stream.of(getEvent(dbSession, eventP))
              .peek(checkPermissions())
              .peek(checkModifiable())
              .forEach(event -> deleteEvent(dbSession, event));
    }
    response.noContent();
  }

  private EventDto getEvent(DbSession dbSession, String event) {
    return dbClient.eventDao().selectByUuid(dbSession, event)
      .orElseThrow(() -> new NotFoundException(format("Event '%s' not found", event)));
  }

  private void deleteEvent(DbSession dbSession, EventDto dbEvent) {
    if (VERSION.getLabel().equals(dbEvent.getCategory())) {
      SnapshotDto analysis = dbClient.snapshotDao().selectByUuid(dbSession, dbEvent.getAnalysisUuid())
        .orElseThrow(() -> new IllegalStateException(format("Analysis '%s' not found", dbEvent.getAnalysisUuid())));
      checkArgument(!analysis.getLast(), "Cannot delete the version event of last analysis");
      analysis.setVersion(null);
      dbClient.snapshotDao().update(dbSession, analysis);
    }
    dbClient.eventDao().delete(dbSession, dbEvent.getUuid());
    dbSession.commit();
  }

  private Consumer<EventDto> checkPermissions() {
    return event -> userSession.checkComponentUuidPermission(UserRole.ADMIN, event.getComponentUuid());
  }
}
