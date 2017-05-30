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
package org.sonar.server.component;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.resources.Scopes;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class ComponentFinder {
  private static final String MSG_COMPONENT_ID_OR_KEY_TEMPLATE = "Either '%s' or '%s' must be provided, not both";
  private static final String MSG_PARAMETER_MUST_NOT_BE_EMPTY = "The '%s' parameter must not be empty";

  private final DbClient dbClient;
  private final ResourceTypes resourceTypes;

  public ComponentFinder(DbClient dbClient, ResourceTypes resourceTypes) {
    this.dbClient = dbClient;
    this.resourceTypes = resourceTypes;
  }

  public ComponentDto getByUuidOrKey(DbSession dbSession, @Nullable String componentUuid, @Nullable String componentKey, ParamNames parameterNames) {
    checkArgument(componentUuid != null ^ componentKey != null, MSG_COMPONENT_ID_OR_KEY_TEMPLATE, parameterNames.getUuidParam(), parameterNames.getKeyParam());

    if (componentUuid != null) {
      checkArgument(!componentUuid.isEmpty(), MSG_PARAMETER_MUST_NOT_BE_EMPTY, parameterNames.getUuidParam());
      return getByUuid(dbSession, componentUuid);
    }

    checkArgument(!componentKey.isEmpty(), MSG_PARAMETER_MUST_NOT_BE_EMPTY, parameterNames.getKeyParam());
    return getByKey(dbSession, componentKey);
  }

  public ComponentDto getByKey(DbSession dbSession, String key) {
    return checkFoundWithOptional(dbClient.componentDao().selectByKey(dbSession, key), "Component key '%s' not found", key);
  }

  public ComponentDto getByUuid(DbSession dbSession, String uuid) {
    return checkFoundWithOptional(dbClient.componentDao().selectByUuid(dbSession, uuid), "Component id '%s' not found", uuid);
  }

  public ComponentDto getRootComponentByUuidOrKey(DbSession dbSession, @Nullable String projectUuid, @Nullable String projectKey) {
    ComponentDto project;
    if (projectUuid != null) {
      project = checkFoundWithOptional(dbClient.componentDao().selectByUuid(dbSession, projectUuid), "Project id '%s' not found", projectUuid);
    } else {
      project = checkFoundWithOptional(dbClient.componentDao().selectByKey(dbSession, projectKey), "Project key '%s' not found", projectKey);
    }
    checkIsProject(project, resourceTypes);

    return project;
  }

  private static void checkIsProject(ComponentDto component, ResourceTypes resourceTypes) {
    Set<String> rootQualifiers = getRootQualifiers(resourceTypes);

    checkRequest(component.scope().equals(Scopes.PROJECT) && rootQualifiers.contains(component.qualifier()),
      format(
        "Component '%s' (id: %s) must be a project%s.",
        component.key(), component.uuid(),
        rootQualifiers.contains(Qualifiers.VIEW) ? " or a view" : ""));
  }

  private static Set<String> getRootQualifiers(ResourceTypes resourceTypes) {
    Collection<ResourceType> rootTypes = resourceTypes.getRoots();
    return rootTypes
      .stream()
      .map(ResourceType::getQualifier)
      .collect(MoreCollectors.toSet(rootTypes.size()));
  }

  public OrganizationDto getOrganization(DbSession dbSession, ComponentDto component) {
    String organizationUuid = component.getOrganizationUuid();
    return dbClient.organizationDao().selectByUuid(dbSession, organizationUuid)
      .orElseThrow(() -> new NotFoundException(String.format("Organization with uuid '%s' not found", organizationUuid)));
  }

  public enum ParamNames {
    PROJECT_ID_AND_KEY("projectId", "projectKey"),
    PROJECT_UUID_AND_KEY("projectUuid", "projectKey"),
    UUID_AND_KEY("uuid", "key"),
    ID_AND_KEY("id", "key"),
    COMPONENT_ID_AND_KEY("componentId", "componentKey"),
    BASE_COMPONENT_ID_AND_KEY("baseComponentId", "baseComponentKey"),
    DEVELOPER_ID_AND_KEY("developerId", "developerKey"),
    COMPONENT_ID_AND_COMPONENT("componentId", "component"),
    PROJECT_ID_AND_PROJECT("projectId", "project"),
    PROJECT_ID_AND_FROM("projectId", "from");

    private final String uuidParamName;
    private final String keyParamName;

    ParamNames(String uuidParamName, String keyParamName) {
      this.uuidParamName = uuidParamName;
      this.keyParamName = keyParamName;
    }

    public String getUuidParam() {
      return uuidParamName;
    }

    public String getKeyParam() {
      return keyParamName;
    }
  }
}
