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

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.ProjectAnalyses.CreateEventResponse;
import org.sonarqube.ws.ProjectAnalyses.Event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.sonar.db.event.EventValidator.MAX_NAME_LENGTH;
import static org.sonar.server.projectanalysis.ws.EventCategory.OTHER;
import static org.sonar.server.projectanalysis.ws.EventCategory.VERSION;
import static org.sonar.server.projectanalysis.ws.EventCategory.fromLabel;
import static org.sonar.server.projectanalysis.ws.EventValidator.checkVersionName;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_ANALYSIS;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_CATEGORY;
import static org.sonar.server.projectanalysis.ws.ProjectAnalysesWsParameters.PARAM_NAME;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateEventAction implements ProjectAnalysesWsAction {
  private static final Set<String> ALLOWED_QUALIFIERS = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.APP);

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final System2 system;
  private final UserSession userSession;

  public CreateEventAction(DbClient dbClient, UuidFactory uuidFactory, System2 system, UserSession userSession) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.system = system;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("create_event")
      .setDescription("Create a project analysis event.<br>" +
        "Only event of category '%s' and '%s' can be created.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer System'</li>" +
        "  <li>'Administer' rights on the specified project</li>" +
        "</ul>",
        VERSION.name(), OTHER.name())
      .setSince("6.3")
      .setPost(true)
      .setResponseExample(getClass().getResource("create_event-example.json"))
      .setHandler(this);

    action.createParam(PARAM_ANALYSIS)
      .setDescription("Analysis key")
      .setExampleValue(Uuids.UUID_EXAMPLE_01)
      .setRequired(true);

    action.createParam(PARAM_CATEGORY)
      .setDescription("Category")
      .setDefaultValue(OTHER)
      .setPossibleValues(VERSION, OTHER);

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(MAX_NAME_LENGTH)
      .setDescription("Name")
      .setExampleValue("5.6");
  }

  @Override
  public void handle(Request httpRequest, Response httpResponse) throws Exception {
    CreateEventRequest request = toAddEventRequest(httpRequest);
    CreateEventResponse response = doHandle(request);

    writeProtobuf(response, httpRequest, httpResponse);
  }

  private CreateEventResponse doHandle(CreateEventRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SnapshotDto analysis = getAnalysis(dbSession, request);
      ComponentDto project = getProjectOrApplication(dbSession, analysis);
      checkRequest(request, project);
      checkExistingDbEvents(dbSession, request, analysis);
      EventDto dbEvent = insertDbEvent(dbSession, request, analysis);
      return toCreateEventResponse(dbEvent);
    }
  }

  private EventDto insertDbEvent(DbSession dbSession, CreateEventRequest request, SnapshotDto analysis) {
    EventDto dbEvent = dbClient.eventDao().insert(dbSession, toDbEvent(request, analysis));
    if (VERSION.equals(request.getCategory())) {
      analysis.setProjectVersion(request.getName());
      dbClient.snapshotDao().update(dbSession, analysis);
    }
    dbSession.commit();
    return dbEvent;
  }

  private SnapshotDto getAnalysis(DbSession dbSession, CreateEventRequest request) {
    return dbClient.snapshotDao().selectByUuid(dbSession, request.getAnalysis())
      .orElseThrow(() -> new NotFoundException(format("Analysis '%s' is not found", request.getAnalysis())));
  }

  private ComponentDto getProjectOrApplication(DbSession dbSession, SnapshotDto analysis) {
    ComponentDto project = dbClient.componentDao().selectByUuid(dbSession, analysis.getComponentUuid()).orElse(null);
    checkState(project != null, "Project of analysis '%s' is not found", analysis.getUuid());
    checkArgument(ALLOWED_QUALIFIERS.contains(project.qualifier()) && Scopes.PROJECT.equals(project.scope()),
      "An event must be created on a project or an application");
    return project;
  }

  private void checkRequest(CreateEventRequest request, ComponentDto component) {
    userSession.checkComponentPermission(UserRole.ADMIN, component);
    checkArgument(EventCategory.VERSION != request.getCategory() || Qualifiers.PROJECT.equals(component.qualifier()), "A version event must be created on a project");
    checkVersionName(request.getCategory(), request.getName());
  }

  private static CreateEventRequest toAddEventRequest(Request request) {
    return CreateEventRequest.builder()
      .setAnalysis(request.mandatoryParam(PARAM_ANALYSIS))
      .setName(request.mandatoryParam(PARAM_NAME))
      .setCategory(request.mandatoryParamAsEnum(PARAM_CATEGORY, EventCategory.class))
      .build();
  }

  private EventDto toDbEvent(CreateEventRequest request, SnapshotDto analysis) {
    return new EventDto()
      .setUuid(uuidFactory.create())
      .setAnalysisUuid(analysis.getUuid())
      .setComponentUuid(analysis.getComponentUuid())
      .setCategory(request.getCategory().getLabel())
      .setName(request.getName())
      .setCreatedAt(system.now())
      .setDate(analysis.getCreatedAt());
  }

  private static CreateEventResponse toCreateEventResponse(EventDto dbEvent) {
    Event.Builder wsEvent = Event.newBuilder()
      .setKey(dbEvent.getUuid())
      .setCategory(fromLabel(dbEvent.getCategory()).name())
      .setAnalysis(dbEvent.getAnalysisUuid())
      .setName(dbEvent.getName());
    ofNullable(dbEvent.getDescription()).ifPresent(wsEvent::setDescription);

    return CreateEventResponse.newBuilder().setEvent(wsEvent).build();
  }

  private void checkExistingDbEvents(DbSession dbSession, CreateEventRequest request, SnapshotDto analysis) {
    List<EventDto> dbEvents = dbClient.eventDao().selectByAnalysisUuid(dbSession, analysis.getUuid());
    Predicate<EventDto> similarEventExisting = filterSimilarEvents(request);
    dbEvents.stream()
      .filter(similarEventExisting)
      .findAny()
      .ifPresent(throwException(request));
  }

  private static Predicate<EventDto> filterSimilarEvents(CreateEventRequest request) {
    switch (request.getCategory()) {
      case VERSION:
        return dbEvent -> VERSION.getLabel().equals(dbEvent.getCategory());
      case OTHER:
        return dbEvent -> OTHER.getLabel().equals(dbEvent.getCategory()) && request.getName().equals(dbEvent.getName());
      default:
        throw new IllegalStateException("Event category not handled: " + request.getCategory());
    }
  }

  private static Consumer<EventDto> throwException(CreateEventRequest request) {
    switch (request.getCategory()) {
      case VERSION:
        return dbEvent -> {
          throw new IllegalArgumentException(format("A version event already exists on analysis '%s'", request.getAnalysis()));
        };
      case OTHER:
        return dbEvent -> {
          throw new IllegalArgumentException(format("An '%s' event with the same name already exists on analysis '%s'", OTHER.getLabel(), request.getAnalysis()));
        };
      default:
        throw new IllegalStateException("Event category not handled: " + request.getCategory());
    }
  }

  private static class CreateEventRequest {
    private final String analysis;
    private final EventCategory category;
    private final String name;

    private CreateEventRequest(Builder builder) {
      analysis = builder.analysis;
      category = builder.category;
      name = builder.name;
    }

    public String getAnalysis() {
      return analysis;
    }

    public EventCategory getCategory() {
      return category;
    }

    public String getName() {
      return name;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  private static class Builder {
    private String analysis;
    private EventCategory category = EventCategory.OTHER;
    private String name;

    private Builder() {
      // enforce static factory method
    }

    public Builder setAnalysis(String analysis) {
      this.analysis = analysis;
      return this;
    }

    public Builder setCategory(EventCategory category) {
      this.category = category;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public CreateEventRequest build() {
      checkArgument(analysis != null, "Analysis key is required");
      checkArgument(category != null, "Category is required");
      checkArgument(name != null, "Name is required");

      return new CreateEventRequest(this);
    }
  }
}
