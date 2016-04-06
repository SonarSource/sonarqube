/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static org.sonar.server.component.ResourceTypeFunctions.RESOURCE_TYPE_TO_QUALIFIER;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class ComponentFinder {
  private static final String MSG_COMPONENT_ID_OR_KEY_TEMPLATE = "Either '%s' or '%s' must be provided, not both";

  private final DbClient dbClient;

  public ComponentFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public ComponentDto getByUuidOrKey(DbSession dbSession, @Nullable String componentUuid, @Nullable String componentKey, ParamNames parameterNames) {
    checkArgument(componentUuid != null ^ componentKey != null, MSG_COMPONENT_ID_OR_KEY_TEMPLATE, parameterNames.getUuidParam(), parameterNames.getKeyParam());

    if (componentUuid != null) {
      checkArgument(!componentUuid.isEmpty(), "The '%s' parameter must not be empty", parameterNames.getUuidParam());
      return getByUuid(dbSession, componentUuid);
    }

    checkArgument(!componentKey.isEmpty(), "The '%s' parameter must not be empty", parameterNames.getKeyParam());
    return getByKey(dbSession, componentKey);
  }

  public ComponentDto getByKey(DbSession dbSession, String key) {
    return getIfPresentOrFail(dbClient.componentDao().selectByKey(dbSession, key), format("Component key '%s' not found", key));
  }

  public ComponentDto getByUuid(DbSession dbSession, String uuid) {
    return getIfPresentOrFail(dbClient.componentDao().selectByUuid(dbSession, uuid), format("Component id '%s' not found", uuid));
  }

  /**
   * A project can be:
   * <ul>
   * <li>a project – ex: SonarQube</li>
   * <li>a view – ex: Language Team</li>
   * <li>a developer – ex: Simon Brandhof</li>
   * </ul>
   */
  public ComponentDto getRootComponentOrModuleByUuidOrKey(DbSession dbSession, @Nullable String projectUuid, @Nullable String projectKey, ResourceTypes resourceTypes) {
    ComponentDto project;
    if (projectUuid != null) {
      project = getIfPresentOrFail(dbClient.componentDao().selectByUuid(dbSession, projectUuid), format("Project id '%s' not found", projectUuid));
    } else {
      project = getIfPresentOrFail(dbClient.componentDao().selectByKey(dbSession, projectKey), format("Project key '%s' not found", projectKey));
    }
    checkIsProjectOrModule(project, resourceTypes);

    return project;
  }

  private static ComponentDto getIfPresentOrFail(Optional<ComponentDto> component, String errorMsg) {
    if (!component.isPresent()) {
      throw new NotFoundException(errorMsg);
    }
    return component.get();
  }

  private static void checkIsProjectOrModule(ComponentDto component, ResourceTypes resourceTypes) {
    Set<String> rootQualifiers = from(resourceTypes.getRoots())
      .transform(RESOURCE_TYPE_TO_QUALIFIER)
      .toSet();
    String qualifier = component.qualifier();

    checkRequest(rootQualifiers.contains(qualifier) || Qualifiers.MODULE.equals(qualifier),
      format("Component '%s' (id: %s) must be a project or a module.", component.key(), component.uuid()));
  }

  public enum ParamNames {
    PROJECT_ID_AND_KEY("projectId", "projectKey"),
    UUID_AND_KEY("uuid", "key"),
    ID_AND_KEY("id", "key"),
    COMPONENT_ID_AND_KEY("componentId", "componentKey"),
    BASE_COMPONENT_ID_AND_KEY("baseComponentId", "baseComponentKey"),
    DEVELOPER_ID_AND_KEY("developerId", "developerKey");

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
