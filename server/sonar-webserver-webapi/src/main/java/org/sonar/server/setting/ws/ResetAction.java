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
package org.sonar.server.setting.ws;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.setting.ws.SettingValidations.SettingData;
import org.sonar.server.user.UserSession;

import static java.util.Collections.emptyList;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_BRANCH;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_COMPONENT;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_KEYS;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;

public class ResetAction implements SettingsWsAction {

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final SettingsUpdater settingsUpdater;
  private final UserSession userSession;
  private final PropertyDefinitions definitions;
  private final SettingValidations validations;

  public ResetAction(DbClient dbClient, ComponentFinder componentFinder, SettingsUpdater settingsUpdater, UserSession userSession, PropertyDefinitions definitions,
    SettingValidations validations) {
    this.dbClient = dbClient;
    this.settingsUpdater = settingsUpdater;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
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
        new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)),
        new Change("7.1", "The settings defined in conf/sonar.properties are read-only and can't be changed"))
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_KEYS)
      .setDescription("Comma-separated list of keys")
      .setExampleValue("sonar.links.scm,sonar.debt.hoursInDay")
      .setRequired(true);
    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setDeprecatedKey("componentKey", "6.3")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setInternal(true)
      .setSince("6.6");
    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001)
      .setInternal(true)
      .setSince("7.1");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ResetRequest resetRequest = toWsRequest(request);
      Optional<ComponentDto> component = getComponent(dbSession, resetRequest);
      checkPermissions(component);
      resetRequest.getKeys().forEach(key -> {
        SettingsWsSupport.validateKey(key);
        SettingData data = new SettingData(key, emptyList(), component.orElse(null));
        ImmutableList.of(validations.scope(), validations.qualifier())
          .forEach(validation -> validation.accept(data));
      });

      List<String> keys = getKeys(resetRequest);
      if (component.isPresent()) {
        settingsUpdater.deleteComponentSettings(dbSession, component.get(), keys);
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
      .collect(MoreCollectors.toSet()));
  }

  private static ResetRequest toWsRequest(Request request) {
    return new ResetRequest()
      .setKeys(request.mandatoryParamAsStrings(PARAM_KEYS))
      .setComponent(request.param(PARAM_COMPONENT))
      .setBranch(request.param(PARAM_BRANCH))
      .setPullRequest(request.param(PARAM_PULL_REQUEST));
  }

  private Optional<ComponentDto> getComponent(DbSession dbSession, ResetRequest request) {
    String componentKey = request.getComponent();
    if (componentKey == null) {
      return Optional.empty();
    }
    return Optional.of(componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, request.getBranch(), request.getPullRequest()));
  }

  private void checkPermissions(Optional<ComponentDto> component) {
    if (component.isPresent()) {
      userSession.checkComponentPermission(UserRole.ADMIN, component.get());
    } else {
      userSession.checkIsSystemAdministrator();
    }
  }

  private static class ResetRequest {

    private String branch;
    private String pullRequest;
    private String component;
    private List<String> keys;

    public ResetRequest setBranch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    @CheckForNull
    public String getBranch() {
      return branch;
    }

    public ResetRequest setPullRequest(@Nullable String pullRequest) {
      this.pullRequest = pullRequest;
      return this;
    }

    @CheckForNull
    public String getPullRequest() {
      return pullRequest;
    }

    public ResetRequest setComponent(@Nullable String component) {
      this.component = component;
      return this;
    }

    @CheckForNull
    public String getComponent() {
      return component;
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
