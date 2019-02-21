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

import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Settings.ListDefinitionsWsResponse;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.setting.ws.SettingsWs.SETTING_ON_BRANCHES;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_BRANCH;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_COMPONENT;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListDefinitionsAction implements SettingsWsAction {

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final PropertyDefinitions propertyDefinitions;
  private final SettingsWsSupport settingsWsSupport;

  public ListDefinitionsAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, PropertyDefinitions propertyDefinitions,
    SettingsWsSupport settingsWsSupport) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.propertyDefinitions = propertyDefinitions;
    this.settingsWsSupport = settingsWsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("list_definitions")
      .setDescription("List settings definitions.<br>" +
        "Requires 'Browse' permission when a component is specified<br/>" +
        "To access licensed settings, authentication is required<br/>" +
        "To access secured settings, one of the following permissions is required: " +
        "<ul>" +
        "<li>'Execute Analysis'</li>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>")
      .setResponseExample(getClass().getResource("list_definitions-example.json"))
      .setSince("6.3")
      .setChangelog(new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)))
      .setHandler(this);
    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    settingsWsSupport.addBranchParam(action);
    settingsWsSupport.addPullRequestParam(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    writeProtobuf(doHandle(request), request, response);
  }

  private ListDefinitionsWsResponse doHandle(Request request) {
    ListDefinitionsRequest wsRequest = toWsRequest(request);
    Optional<ComponentDto> component = loadComponent(wsRequest);
    Optional<String> qualifier = getQualifier(component);
    ListDefinitionsWsResponse.Builder wsResponse = ListDefinitionsWsResponse.newBuilder();
    propertyDefinitions.getAll().stream()
      .filter(definition -> qualifier.map(s -> definition.qualifiers().contains(s)).orElseGet(definition::global))
      .filter(definition -> wsRequest.getBranch() == null || SETTING_ON_BRANCHES.contains(definition.key()))
      .filter(definition -> settingsWsSupport.isVisible(definition.key(), component))
      .sorted(comparing(PropertyDefinition::category, String::compareToIgnoreCase)
        .thenComparingInt(PropertyDefinition::index)
        .thenComparing(PropertyDefinition::name, String::compareToIgnoreCase))
      .forEach(definition -> addDefinition(definition, wsResponse));
    return wsResponse.build();
  }

  private static ListDefinitionsRequest toWsRequest(Request request) {
    return new ListDefinitionsRequest()
      .setComponent(request.param(PARAM_COMPONENT))
      .setBranch(request.param(PARAM_BRANCH))
      .setPullRequest(request.param(PARAM_PULL_REQUEST));
  }

  private static Optional<String> getQualifier(Optional<ComponentDto> component) {
    return component.isPresent() ? Optional.of(component.get().qualifier()) : Optional.empty();
  }

  private Optional<ComponentDto> loadComponent(ListDefinitionsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String componentKey = request.getComponent();
      if (componentKey == null) {
        return Optional.empty();
      }
      ComponentDto component = componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, request.getBranch(), request.getPullRequest());
      userSession.checkComponentPermission(USER, component);
      return Optional.of(component);
    }
  }

  private void addDefinition(PropertyDefinition definition, ListDefinitionsWsResponse.Builder wsResponse) {
    String key = definition.key();
    Settings.Definition.Builder builder = wsResponse.addDefinitionsBuilder()
      .setKey(key)
      .setType(Settings.Type.valueOf(definition.type().name()))
      .setMultiValues(definition.multiValues());
    ofNullable(emptyToNull(definition.deprecatedKey())).ifPresent(builder::setDeprecatedKey);
    ofNullable(emptyToNull(definition.name())).ifPresent(builder::setName);
    ofNullable(emptyToNull(definition.description())).ifPresent(builder::setDescription);
    String category = propertyDefinitions.getCategory(key);
    ofNullable(emptyToNull(category)).ifPresent(builder::setCategory);
    String subCategory = propertyDefinitions.getSubCategory(key);
    ofNullable(emptyToNull(subCategory)).ifPresent(builder::setSubCategory);
    ofNullable(emptyToNull(definition.defaultValue())).ifPresent(builder::setDefaultValue);
    List<String> options = definition.options();
    if (!options.isEmpty()) {
      builder.addAllOptions(options);
    }
    List<PropertyFieldDefinition> fields = definition.fields();
    if (!fields.isEmpty()) {
      fields.forEach(fieldDefinition -> addField(fieldDefinition, builder));
    }
  }

  private static void addField(PropertyFieldDefinition fieldDefinition, Settings.Definition.Builder builder) {
    builder.addFieldsBuilder()
      .setKey(fieldDefinition.key())
      .setName(fieldDefinition.name())
      .setDescription(fieldDefinition.description())
      .setType(Settings.Type.valueOf(fieldDefinition.type().name()))
      .addAllOptions(fieldDefinition.options())
      .build();
  }

  private static class ListDefinitionsRequest {

    private String branch;
    private String component;
    private String pullRequest;

    public ListDefinitionsRequest setComponent(@Nullable String component) {
      this.component = component;
      return this;
    }

    @CheckForNull
    public String getComponent() {
      return component;
    }

    public ListDefinitionsRequest setBranch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    @CheckForNull
    public String getBranch() {
      return branch;
    }

    public ListDefinitionsRequest setPullRequest(@Nullable String pullRequest) {
      this.pullRequest = pullRequest;
      return this;
    }

    @CheckForNull
    public String getPullRequest() {
      return pullRequest;
    }
  }
}
