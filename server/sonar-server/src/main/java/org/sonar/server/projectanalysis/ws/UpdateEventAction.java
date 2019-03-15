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
package org.sonar.server.projectanalysis.ws;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
import org.sonarqube.ws.ProjectAnalyses.Event;
import org.sonarqube.ws.ProjectAnalyses.UpdateEventResponse;

import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.server.projectanalysis.ws.EventValidator.checkModifiable;
import static org.sonar.server.projectanalysis.ws.EventValidator.checkVersionName;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonar.server.projectanalysis.ws.EventCategory.VERSION;
import static org.sonar.server.projectanalysis.ws.EventCategory.fromLabel;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_EVENT;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_NAME;

public class UpdateEventAction implements ProjectAnalysesWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;

  public UpdateEventAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("update_event")
      .setDescription("Update a project analysis event.<br>" +
        "Only events of category '%s' and '%s' can be updated.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer System'</li>" +
        "  <li>'Administer' rights on the specified project</li>" +
        "</ul>",
        EventCategory.VERSION.name(), EventCategory.OTHER.name())
      .setSince("6.3")
      .setPost(true)
      .setResponseExample(getClass().getResource("update_event-example.json"))
      .setHandler(this);

    action.createParam(PARAM_EVENT)
      .setDescription("Event key")
      .setExampleValue(Uuids.UUID_EXAMPLE_08)
      .setRequired(true);

    action.createParam(PARAM_NAME)
      .setMaximumLength(org.sonar.db.event.EventValidator.MAX_NAME_LENGTH)
      .setDescription("New name")
      .setExampleValue("5.6")
      .setRequired(true);
  }

  @Override
  public void handle(Request httpRequest, Response httpResponse) throws Exception {
    Stream.of(httpRequest)
      .map(toUpdateEventRequest())
      .map(this::doHandle)
      .forEach(wsResponse -> writeProtobuf(wsResponse, httpRequest, httpResponse));
  }

  private UpdateEventResponse doHandle(UpdateEventRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return Stream
        .of(getDbEvent(dbSession, request))
        .peek(checkPermissions())
        .peek(checkModifiable())
        .peek(checkVersionNameLength(request))
        .map(updateNameAndDescription(request))
        .peek(checkNonConflictingOtherEvents(dbSession))
        .peek(updateInDb(dbSession))
        .map(toWsResponse())
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Event not found"));
    }
  }

  private Consumer<EventDto> updateInDb(DbSession dbSession) {
    return event -> {
      dbClient.eventDao().update(dbSession, event.getUuid(), event.getName(), event.getDescription());
      if (VERSION.getLabel().equals(event.getCategory())) {
        SnapshotDto analysis = getAnalysis(dbSession, event);
        analysis.setProjectVersion(event.getName());
        dbClient.snapshotDao().update(dbSession, analysis);
      }
      dbSession.commit();
    };
  }

  private EventDto getDbEvent(DbSession dbSession, UpdateEventRequest request) {
    checkArgument(isNotBlank(request.getName()), "A non empty name is required");
    return dbClient.eventDao().selectByUuid(dbSession, request.getEvent())
      .orElseThrow(() -> new NotFoundException(format("Event '%s' not found", request.getEvent())));
  }

  private Consumer<EventDto> checkPermissions() {
    return event -> userSession.checkComponentUuidPermission(UserRole.ADMIN, event.getComponentUuid());
  }

  private Consumer<EventDto> checkNonConflictingOtherEvents(DbSession dbSession) {
    return candidateEvent -> {
      List<EventDto> dbEvents = dbClient.eventDao().selectByAnalysisUuid(dbSession, candidateEvent.getAnalysisUuid());
      Predicate<EventDto> otherEventWithSameName = otherEvent -> !candidateEvent.getUuid().equals(otherEvent.getUuid()) && otherEvent.getName().equals(candidateEvent.getName());
      dbEvents.stream()
        .filter(otherEventWithSameName)
        .findAny()
        .ifPresent(event -> {
          throw new IllegalArgumentException(format("An '%s' event with the same name already exists on analysis '%s'",
            candidateEvent.getCategory(),
            candidateEvent.getAnalysisUuid()));
        });
    };
  }

  private static Consumer<EventDto> checkVersionNameLength(UpdateEventRequest request) {
    return candidateEvent -> checkVersionName(candidateEvent.getCategory(), request.getName());
  }

  private SnapshotDto getAnalysis(DbSession dbSession, EventDto event) {
    return dbClient.snapshotDao().selectByUuid(dbSession, event.getAnalysisUuid())
      .orElseThrow(() -> new IllegalStateException(format("Analysis '%s' is not found", event.getAnalysisUuid())));
  }

  private static Function<EventDto, EventDto> updateNameAndDescription(UpdateEventRequest request) {
    return event -> {
      ofNullable(request.getName()).ifPresent(event::setName);
      return event;
    };
  }

  private static Function<EventDto, UpdateEventResponse> toWsResponse() {
    return dbEvent -> {
      Event.Builder wsEvent = Event.newBuilder()
        .setKey(dbEvent.getUuid())
        .setCategory(fromLabel(dbEvent.getCategory()).name())
        .setAnalysis(dbEvent.getAnalysisUuid());
      ofNullable(dbEvent.getName()).ifPresent(wsEvent::setName);
      ofNullable(dbEvent.getDescription()).ifPresent(wsEvent::setDescription);

      return UpdateEventResponse.newBuilder().setEvent(wsEvent).build();
    };
  }

  private static Function<Request, UpdateEventRequest> toUpdateEventRequest() {
    return request -> new UpdateEventRequest(
      request.mandatoryParam(PARAM_EVENT),
      request.param(PARAM_NAME));
  }

  private static class UpdateEventRequest {
    private final String event;
    private final String name;

    public UpdateEventRequest(String event, String name) {
      this.event = requireNonNull(event, "Event key is required");
      this.name = requireNonNull(name, "Name is required");
    }

    public String getEvent() {
      return event;
    }

    @CheckForNull
    public String getName() {
      return name;
    }
  }
}
