/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.resources.Scopes;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

public class ComponentFinder {
  private static final String MSG_COMPONENT_ID_OR_KEY_TEMPLATE = "Either '%s' or '%s' must be provided";
  private static final String MSG_PARAMETER_MUST_NOT_BE_EMPTY = "The '%s' parameter must not be empty";
  private static final String LABEL_PROJECT = "Project";
  private static final String LABEL_COMPONENT = "Component";
  private static final String LABEL_PROJECT_NOT_FOUND = "Project '%s' not found";

  private final DbClient dbClient;
  private final ResourceTypes resourceTypes;

  public ComponentFinder(DbClient dbClient, ResourceTypes resourceTypes) {
    this.dbClient = dbClient;
    this.resourceTypes = resourceTypes;
  }

  public ComponentDto getByUuidOrKey(DbSession dbSession, @Nullable String componentUuid, @Nullable String componentKey, ParamNames parameterNames) {
    checkByUuidOrKey(componentUuid, componentKey, parameterNames);

    if (componentUuid != null) {
      return getByUuidFromMainBranch(dbSession, checkParamNotEmpty(componentUuid, parameterNames.getUuidParam()));
    }

    return getByKey(dbSession, checkParamNotEmpty(componentKey, parameterNames.getKeyParam()));
  }

  public ProjectDto getProjectByKey(DbSession dbSession, String projectKey) {
    return dbClient.projectDao().selectProjectByKey(dbSession, projectKey)
      .orElseThrow(() -> new NotFoundException(String.format(LABEL_PROJECT_NOT_FOUND, projectKey)));
  }

  public ProjectDto getApplicationByKey(DbSession dbSession, String applicationKey) {
    return dbClient.projectDao().selectApplicationByKey(dbSession, applicationKey)
      .orElseThrow(() -> new NotFoundException(String.format("Application '%s' not found", applicationKey)));
  }

  public ProjectDto getProjectOrApplicationByKey(DbSession dbSession, String projectKey) {
    return dbClient.projectDao().selectProjectOrAppByKey(dbSession, projectKey)
      .orElseThrow(() -> new NotFoundException(String.format(LABEL_PROJECT_NOT_FOUND, projectKey)));
  }

  public ProjectDto getProjectByUuid(DbSession dbSession, String projectUuid) {
    return dbClient.projectDao().selectByUuid(dbSession, projectUuid)
      .filter(p -> Qualifiers.PROJECT.equals(p.getQualifier()))
      .orElseThrow(() -> new NotFoundException(String.format(LABEL_PROJECT_NOT_FOUND, projectUuid)));
  }

  public ProjectDto getProjectByUuidOrKey(DbSession dbSession, @Nullable String projectUuid, @Nullable String projectKey, ParamNames parameterNames) {
    checkByUuidOrKey(projectUuid, projectKey, parameterNames);

    if (projectUuid != null) {
      return getProjectByUuid(dbSession, checkParamNotEmpty(projectUuid, parameterNames.getUuidParam()));
    }

    return getProjectByKey(dbSession, checkParamNotEmpty(projectKey, parameterNames.getKeyParam()));
  }

  private static String checkParamNotEmpty(String value, String param) {
    checkArgument(!value.isEmpty(), MSG_PARAMETER_MUST_NOT_BE_EMPTY, param);
    return value;
  }

  public BranchDto getBranchByUuid(DbSession dbSession, String branchUuid) {
    return dbClient.branchDao().selectByUuid(dbSession, branchUuid)
      .orElseThrow(() -> new NotFoundException(String.format("Branch uuid '%s' not found", branchUuid)));
  }

  public BranchDto getBranchOrPullRequest(DbSession dbSession, ComponentDto project, @Nullable String branchKey, @Nullable String pullRequestKey) {
    return getBranchOrPullRequest(dbSession, project.uuid(), project.getKey(), branchKey, pullRequestKey);
  }

  public BranchDto getBranchOrPullRequest(DbSession dbSession, ProjectDto project, @Nullable String branchKey, @Nullable String pullRequestKey) {
    return getBranchOrPullRequest(dbSession, project.getUuid(), project.getKey(), branchKey, pullRequestKey);
  }

  public BranchDto getBranchOrPullRequest(DbSession dbSession, String projectUuid, String projectKey, @Nullable String branchKey, @Nullable String pullRequestKey) {
    if (branchKey != null) {
      return dbClient.branchDao().selectByBranchKey(dbSession, projectUuid, branchKey)
        .orElseThrow(() -> new NotFoundException(String.format("Branch '%s' in project '%s' not found", branchKey, projectKey)));
    } else if (pullRequestKey != null) {
      return dbClient.branchDao().selectByPullRequestKey(dbSession, projectUuid, pullRequestKey)
        .orElseThrow(() -> new NotFoundException(String.format("Pull request '%s' in project '%s' not found", pullRequestKey, projectKey)));
    }
    return dbClient.branchDao().selectByUuid(dbSession, projectUuid)
      .orElseThrow(() -> new NotFoundException(String.format("Main branch in project '%s' not found", projectKey)));
  }

  public BranchDto getMainBranch(DbSession dbSession, ProjectDto projectDto) {
    return dbClient.branchDao().selectByUuid(dbSession, projectDto.getUuid())
      .orElseThrow(() -> new IllegalStateException(String.format("Can't find main branch for project '%s'", projectDto.getKey())));
  }

  private static void checkByUuidOrKey(@Nullable String componentUuid, @Nullable String componentKey, ParamNames parameterNames) {
    checkArgument(componentUuid != null ^ componentKey != null, MSG_COMPONENT_ID_OR_KEY_TEMPLATE, parameterNames.getUuidParam(), parameterNames.getKeyParam());
  }

  public ComponentDto getByKey(DbSession dbSession, String key) {
    return getByKey(dbSession, key, LABEL_COMPONENT);
  }

  private ComponentDto getByKey(DbSession dbSession, String key, String label) {
    return checkComponent(dbClient.componentDao().selectByKey(dbSession, key), "%s key '%s' not found", label, key);
  }

  /**
   * This method only returns components in main branches.
   */
  public ComponentDto getByUuidFromMainBranch(DbSession dbSession, String uuid) {
    return getByUuidFromMainBranch(dbSession, uuid, LABEL_COMPONENT);
  }

  private ComponentDto getByUuidFromMainBranch(DbSession dbSession, String uuid, String label) {
    return checkComponent(dbClient.componentDao().selectByUuid(dbSession, uuid), "%s id '%s' not found", label, uuid);
  }

  private static ComponentDto checkComponent(Optional<ComponentDto> componentDto, String message, Object... messageArguments) {
    if (componentDto.isPresent() && componentDto.get().isEnabled() && componentDto.get().getMainBranchProjectUuid() == null) {
      return componentDto.get();
    }
    throw new NotFoundException(format(message, messageArguments));
  }

  public ComponentDto getRootComponentByUuidOrKey(DbSession dbSession, @Nullable String projectUuid, @Nullable String projectKey) {
    ComponentDto project;
    if (projectUuid != null) {
      project = getByUuidFromMainBranch(dbSession, projectUuid, LABEL_PROJECT);
    } else {
      project = getByKey(dbSession, projectKey, LABEL_PROJECT);
    }
    checkIsProject(project);

    return project;
  }

  private ComponentDto checkIsProject(ComponentDto component) {
    Set<String> rootQualifiers = getRootQualifiers(resourceTypes);

    checkRequest(component.scope().equals(Scopes.PROJECT) && rootQualifiers.contains(component.qualifier()),
      format(
        "Component '%s' (id: %s) must be a project%s.",
        component.getKey(), component.uuid(),
        rootQualifiers.contains(Qualifiers.VIEW) ? " or a view" : ""));

    return component;
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
    Optional<OrganizationDto> organizationDto = dbClient.organizationDao().selectByUuid(dbSession, organizationUuid);
    return checkFoundWithOptional(organizationDto, "Organization with uuid '%s' not found", organizationUuid);
  }

  public ComponentDto getByKeyAndBranch(DbSession dbSession, String key, String branch) {
    Optional<ComponentDto> componentDto = dbClient.componentDao().selectByKeyAndBranch(dbSession, key, branch);
    if (componentDto.isPresent() && componentDto.get().isEnabled()) {
      return componentDto.get();
    }
    throw new NotFoundException(format("Component '%s' on branch '%s' not found", key, branch));
  }

  public ComponentDto getByKeyAndPullRequest(DbSession dbSession, String key, String pullRequest) {
    Optional<ComponentDto> componentDto = dbClient.componentDao().selectByKeyAndPullRequest(dbSession, key, pullRequest);
    if (componentDto.isPresent() && componentDto.get().isEnabled()) {
      return componentDto.get();
    }
    throw new NotFoundException(format("Component '%s' of pull request '%s' not found", key, pullRequest));
  }

  public ComponentDto getByKeyAndOptionalBranchOrPullRequest(DbSession dbSession, String key, @Nullable String branch, @Nullable String pullRequest) {
    checkArgument(branch == null || pullRequest == null, "Either branch or pull request can be provided, not both");
    if (branch != null) {
      return getByKeyAndBranch(dbSession, key, branch);
    } else if (pullRequest != null) {
      return getByKeyAndPullRequest(dbSession, key, pullRequest);
    }
    return getByKey(dbSession, key);
  }

  public Optional<ComponentDto> getOptionalByKeyAndOptionalBranchOrPullRequest(DbSession dbSession, String key, @Nullable String branch, @Nullable String pullRequest) {
    checkArgument(branch == null || pullRequest == null, "Either branch or pull request can be provided, not both");
    if (branch != null) {
      return dbClient.componentDao().selectByKeyAndBranch(dbSession, key, branch);
    } else if (pullRequest != null) {
      return dbClient.componentDao().selectByKeyAndPullRequest(dbSession, key, pullRequest);
    }
    return dbClient.componentDao().selectByKey(dbSession, key);
  }

  public enum ParamNames {
    PROJECT_ID_AND_KEY("projectId", "projectKey"),
    PROJECT_UUID_AND_KEY("projectUuid", "projectKey"),
    PROJECT_UUID_AND_PROJECT("projectUuid", "project"),
    UUID_AND_KEY("uuid", "key"),
    ID_AND_KEY("id", "key"),
    COMPONENT_ID_AND_KEY("componentId", "componentKey"),
    BASE_COMPONENT_ID_AND_KEY("baseComponentId", "component"),
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
