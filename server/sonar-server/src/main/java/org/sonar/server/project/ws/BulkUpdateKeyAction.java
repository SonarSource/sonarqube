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

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.Map;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentKeyUpdaterDao;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ParamNames;
import org.sonar.server.component.ComponentService;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects.BulkUpdateKeyWsResponse;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.db.component.ComponentKeyUpdaterDao.checkIsProjectOrModule;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_BULK_UPDATE_KEY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_DRY_RUN;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_FROM;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_TO;

public class BulkUpdateKeyAction implements ProjectsWsAction {
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final ComponentKeyUpdaterDao componentKeyUpdater;
  private final ComponentService componentService;
  private final UserSession userSession;

  public BulkUpdateKeyAction(DbClient dbClient, ComponentFinder componentFinder, ComponentService componentService, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentKeyUpdater = dbClient.componentKeyUpdaterDao();
    this.componentFinder = componentFinder;
    this.componentService = componentService;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    doDefine(context);
  }

  public WebService.NewAction doDefine(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_BULK_UPDATE_KEY)
      .setDescription("Bulk update a project or module key and all its sub-components keys. " +
        "The bulk update allows to replace a part of the current key by another string on the current project and all its sub-modules.<br>" +
        "It's possible to simulate the bulk update by setting the parameter '%s' at true. No key is updated with a dry run.<br>" +
        "Ex: to rename a project with key 'my_project' to 'my_new_project' and all its sub-components keys, call the WS with parameters:" +
        "<ul>" +
        "  <li>%s: my_project</li>" +
        "  <li>%s: my_</li>" +
        "  <li>%s: my_new_</li>" +
        "</ul>" +
        "Either '%s' or '%s' must be provided.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>",
        PARAM_DRY_RUN,
        PARAM_PROJECT, PARAM_FROM, PARAM_TO,
        PARAM_PROJECT_ID, PARAM_PROJECT)
      .setSince("6.1")
      .setPost(true)
      .setResponseExample(getClass().getResource("bulk_update_key-example.json"))
      .setHandler(this);

    action.setChangelog(
      new Change("6.4", "Moved from api/components/bulk_update_key to api/projects/bulk_update_key"));

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project or module ID")
      .setDeprecatedKey("id", "6.4")
      .setDeprecatedSince("6.4")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project or module key")
      .setDeprecatedKey("key", "6.4")
      .setExampleValue("my_old_project");

    action.createParam(PARAM_FROM)
      .setDescription("String to match in components keys")
      .setRequired(true)
      .setExampleValue("_old");

    action.createParam(PARAM_TO)
      .setDescription("String replacement in components keys")
      .setRequired(true)
      .setExampleValue("_new");

    action.createParam(PARAM_DRY_RUN)
      .setDescription("Simulate bulk update. No component key is updated.")
      .setBooleanPossibleValues()
      .setDefaultValue(false);

    return action;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    writeProtobuf(doHandle(toWsRequest(request)), request, response);
  }

  private BulkUpdateKeyWsResponse doHandle(BulkUpdateKeyRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto projectOrModule = componentFinder.getByUuidOrKey(dbSession, request.getId(), request.getKey(), ParamNames.ID_AND_KEY);
      checkIsProjectOrModule(projectOrModule);
      userSession.checkComponentPermission(UserRole.ADMIN, projectOrModule);

      Map<String, String> newKeysByOldKeys = componentKeyUpdater.simulateBulkUpdateKey(dbSession, projectOrModule.uuid(), request.getFrom(), request.getTo());
      Map<String, Boolean> newKeysWithDuplicateMap = componentKeyUpdater.checkComponentKeys(dbSession, ImmutableList.copyOf(newKeysByOldKeys.values()));

      if (!request.isDryRun()) {
        checkNoDuplicate(newKeysWithDuplicateMap);
        bulkUpdateKey(dbSession, request, projectOrModule);
      }

      return buildResponse(newKeysByOldKeys, newKeysWithDuplicateMap);
    }
  }

  private static void checkNoDuplicate(Map<String, Boolean> newKeysWithDuplicateMap) {
    newKeysWithDuplicateMap.entrySet().forEach(entry -> checkRequest(!entry.getValue(), "Impossible to update key: a component with key \"%s\" already exists.", entry.getKey()));
  }

  private void bulkUpdateKey(DbSession dbSession, BulkUpdateKeyRequest request, ComponentDto projectOrModule) {
    componentService.bulkUpdateKey(dbSession, projectOrModule, request.getFrom(), request.getTo());
  }

  private static BulkUpdateKeyWsResponse buildResponse(Map<String, String> newKeysByOldKeys, Map<String, Boolean> newKeysWithDuplicateMap) {
    BulkUpdateKeyWsResponse.Builder response = BulkUpdateKeyWsResponse.newBuilder();

    newKeysByOldKeys.entrySet().stream()
      // sort by old key
      .sorted(Comparator.comparing(Map.Entry::getKey))
      .forEach(
        entry -> {
          String newKey = entry.getValue();
          response.addKeysBuilder()
            .setKey(entry.getKey())
            .setNewKey(newKey)
            .setDuplicate(newKeysWithDuplicateMap.getOrDefault(newKey, false));
        });

    return response.build();
  }

  private static BulkUpdateKeyRequest toWsRequest(Request request) {
    return BulkUpdateKeyRequest.builder()
      .setId(request.param(PARAM_PROJECT_ID))
      .setKey(request.param(PARAM_PROJECT))
      .setFrom(request.mandatoryParam(PARAM_FROM))
      .setTo(request.mandatoryParam(PARAM_TO))
      .setDryRun(request.mandatoryParamAsBoolean(PARAM_DRY_RUN))
      .build();
  }

  private static class BulkUpdateKeyRequest {
    private final String id;
    private final String key;
    private final String from;
    private final String to;
    private final boolean dryRun;

    public BulkUpdateKeyRequest(Builder builder) {
      this.id = builder.id;
      this.key = builder.key;
      this.from = builder.from;
      this.to = builder.to;
      this.dryRun = builder.dryRun;
    }

    @CheckForNull
    public String getId() {
      return id;
    }

    @CheckForNull
    public String getKey() {
      return key;
    }

    public String getFrom() {
      return from;
    }

    public String getTo() {
      return to;
    }

    public boolean isDryRun() {
      return dryRun;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  public static class Builder {
    private String id;
    private String key;
    private String from;
    private String to;
    private boolean dryRun;

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

    public Builder setFrom(String from) {
      this.from = from;
      return this;
    }

    public Builder setTo(String to) {
      this.to = to;
      return this;
    }

    public Builder setDryRun(boolean dryRun) {
      this.dryRun = dryRun;
      return this;
    }

    public BulkUpdateKeyRequest build() {
      checkArgument(from != null && !from.isEmpty(), "The string to match must not be empty");
      checkArgument(to != null && !to.isEmpty(), "The string replacement must not be empty");
      return new BulkUpdateKeyRequest(this);
    }
  }
}
