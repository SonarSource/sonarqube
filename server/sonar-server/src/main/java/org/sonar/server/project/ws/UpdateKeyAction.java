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
package org.sonar.server.project.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ParamNames;
import org.sonar.server.component.ComponentService;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_UPDATE_KEY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_FROM;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_TO;

public class UpdateKeyAction implements ProjectsWsAction {
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final ComponentService componentService;

  public UpdateKeyAction(DbClient dbClient, ComponentFinder componentFinder, ComponentService componentService) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.componentService = componentService;
  }

  @Override
  public void define(WebService.NewController context) {
    doDefine(context);
  }

  public WebService.NewAction doDefine(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_UPDATE_KEY)
      .setDescription("Update a project or module key and all its sub-components keys.<br>" +
        "Either '%s' or '%s' must be provided.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>",
        PARAM_FROM, PARAM_PROJECT_ID)
      .setSince("6.1")
      .setPost(true)
      .setHandler(this);

    action.setChangelog(
      new Change("6.4", "Move from api/components/update_key to api/projects/update_key"));

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project or module id")
      .setDeprecatedKey("id", "6.4")
      .setDeprecatedSince("6.4")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_FROM)
      .setDescription("Project or module key")
      .setDeprecatedKey("key", "6.4")
      .setExampleValue("my_old_project");

    action.createParam(PARAM_TO)
      .setDescription("New component key")
      .setRequired(true)
      .setDeprecatedKey("newKey", "6.4")
      .setExampleValue("my_new_project");

    return action;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toWsRequest(request));
    response.noContent();
  }

  private void doHandle(UpdateKeyRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto projectOrModule = componentFinder.getByUuidOrKey(dbSession, request.getId(), request.getKey(), ParamNames.PROJECT_ID_AND_FROM);
      componentService.updateKey(dbSession, projectOrModule, request.getNewKey());
    }
  }

  private static UpdateKeyRequest toWsRequest(Request request) {
    return UpdateKeyRequest.builder()
      .setId(request.param(PARAM_PROJECT_ID))
      .setKey(request.param(PARAM_FROM))
      .setNewKey(request.mandatoryParam(PARAM_TO))
      .build();
  }

  private static class UpdateKeyRequest {
    private final String id;
    private final String key;
    private final String newKey;

    public UpdateKeyRequest(Builder builder) {
      this.id = builder.id;
      this.key = builder.key;
      this.newKey = builder.newKey;
    }

    @CheckForNull
    public String getId() {
      return id;
    }

    @CheckForNull
    public String getKey() {
      return key;
    }

    public String getNewKey() {
      return newKey;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  private static class Builder {
    private String id;
    private String key;
    private String newKey;

    private Builder() {
      // enforce method constructor
    }

    public Builder setId(@Nullable String id) {
      this.id = id;
      return this;
    }

    public Builder setKey(@Nullable String key) {
      this.key = key;
      return this;
    }

    public Builder setNewKey(String newKey) {
      this.newKey = newKey;
      return this;
    }

    public UpdateKeyRequest build() {
      checkArgument(newKey != null && !newKey.isEmpty(), "The new key must not be empty");
      return new UpdateKeyRequest(this);
    }
  }
}
