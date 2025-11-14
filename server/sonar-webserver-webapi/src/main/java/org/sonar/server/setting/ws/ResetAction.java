/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.setting.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.setting.ws.SettingValidations.SettingData;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_BRANCH;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_COMPONENT;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_KEYS;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class ResetAction implements SettingsWsAction {
  private final DbClient dbClient;
  private final SettingsUpdater settingsUpdater;
  private final UserSession userSession;
  private final PropertyDefinitions definitions;
  private final SettingValidations validations;

  public ResetAction(DbClient dbClient, SettingsUpdater settingsUpdater, UserSession userSession, PropertyDefinitions definitions, SettingValidations validations) {
    this.dbClient = dbClient;
    this.settingsUpdater = settingsUpdater;
    this.userSession = userSession;
    this.definitions = definitions;
    this.validations = validations;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("reset")
      .setDescription("Remove a setting value.<br>" +
        "The settings defined in conf/sonar.properties are read-only and can't be changed.<br/>" +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>")
      .setSince("6.1")
      .setChangelog(
        new Change("10.1", "Param 'component' now only accept keys for projects, applications, portfolios or subportfolios"),
        new Change("10.1", format("Internal parameters '%s' and '%s' were removed", PARAM_BRANCH, PARAM_PULL_REQUEST)),
        new Change("8.8", "Deprecated parameter 'componentKey' has been removed"),
        new Change("7.6", format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)),
        new Change("7.1", "The settings defined in conf/sonar.properties are read-only and can't be changed"))
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_KEYS)
      .setDescription("Comma-separated list of keys")
      .setExampleValue("sonar.links.scm,sonar.debt.hoursInDay")
      .setRequired(true);
    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key. Only keys for projects, applications, portfolios or subportfolios are accepted.")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ResetRequest resetRequest = toWsRequest(request);
      Optional<EntityDto> entity = getEntity(dbSession, resetRequest);
      checkPermissions(entity);
      resetRequest.getKeys().forEach(key -> {
        SettingsWsSupport.validateKey(key);
        SettingData data = new SettingData(key, emptyList(), entity.orElse(null));
        validations.validateScope(data);
        validations.validateQualifier(data);
      });

      List<String> keys = getKeys(resetRequest);
      if (entity.isPresent()) {
        settingsUpdater.deleteComponentSettings(dbSession, entity.get(), keys);
      } else {
        settingsUpdater.deleteGlobalSettings(dbSession, keys);
      }
      dbSession.commit();
      response.noContent();
    }
  }

  private List<String> getKeys(ResetRequest request) {
    return new ArrayList<>(request.getKeys().stream()
      .map(key -> {
        PropertyDefinition definition = definitions.get(key);
        return definition != null ? definition.key() : key;
      })
      .collect(Collectors.toSet()));
  }

  private static ResetRequest toWsRequest(Request request) {
    return new ResetRequest()
      .setKeys(request.mandatoryParamAsStrings(PARAM_KEYS))
      .setEntity(request.param(PARAM_COMPONENT));
  }

  private Optional<EntityDto> getEntity(DbSession dbSession, ResetRequest request) {
    String componentKey = request.getEntity();
    if (componentKey == null) {
      return Optional.empty();
    }

    return Optional.of(dbClient.entityDao().selectByKey(dbSession, componentKey)
      .orElseThrow(() -> new NotFoundException(format("Component key '%s' not found", componentKey))));
  }

  private void checkPermissions(Optional<EntityDto> component) {
    if (component.isPresent()) {
      userSession.checkEntityPermission(ProjectPermission.ADMIN, component.get());
    } else {
      userSession.checkIsSystemAdministrator();
    }
  }

  private static class ResetRequest {
    private String entity = null;
    private List<String> keys = null;

    public ResetRequest setEntity(@Nullable String entity) {
      this.entity = entity;
      return this;
    }

    @CheckForNull
    public String getEntity() {
      return entity;
    }

    public ResetRequest setKeys(List<String> keys) {
      this.keys = keys;
      return this;
    }

    public List<String> getKeys() {
      return keys;
    }
  }
}
