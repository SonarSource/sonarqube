/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.v2.api.projectbindings.controller;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.projectbindings.service.ProjectBindingInformation;
import org.sonar.server.common.projectbindings.service.ProjectBindingsSearchRequest;
import org.sonar.server.common.projectbindings.service.ProjectBindingsService;
import org.sonar.server.exceptions.ResourceForbiddenException;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.model.RestPage;
import org.sonar.server.v2.api.projectbindings.model.ProjectBinding;
import org.sonar.server.v2.api.projectbindings.request.ProjectBindingsSearchRestRequest;
import org.sonar.server.v2.api.projectbindings.response.ProjectBindingsSearchRestResponse;
import org.sonar.server.v2.api.response.PageRestResponse;

import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.server.exceptions.BadRequestException.throwBadRequestException;

public class DefaultProjectBindingsController implements ProjectBindingsController {

  private final UserSession userSession;
  private final ProjectBindingsService projectBindingsService;

  public DefaultProjectBindingsController(UserSession userSession, ProjectBindingsService projectBindingsService) {
    this.userSession = userSession;
    this.projectBindingsService = projectBindingsService;
  }

  @Override
  public ProjectBinding getProjectBinding(String id) {
    Optional<ProjectAlmSettingDto> projectAlmSettingDto = projectBindingsService.findProjectBindingByUuid(id);
    if (projectAlmSettingDto.isPresent()) {
      ProjectDto projectDto = projectBindingsService.findProjectFromBinding(projectAlmSettingDto.get())
        .orElseThrow(() -> new IllegalStateException(String.format("Project (uuid '%s') not found for binding '%s'", projectAlmSettingDto.get().getProjectUuid(), id)));
      userSession.checkEntityPermissionOrElseThrowResourceForbiddenException(USER, projectDto);
      return toProjectBinding(projectDto, projectAlmSettingDto.get());
    } else {
      throw new ResourceForbiddenException();
    }
  }

  @Override
  public ProjectBindingsSearchRestResponse searchProjectBindings(ProjectBindingsSearchRestRequest restRequest, RestPage restPage) {
    userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);
    validateSearchParameters(restRequest);
    ProjectBindingsSearchRequest serviceRequest = new ProjectBindingsSearchRequest(
      restRequest.repository(), restRequest.dopSettingId(), restRequest.repositoryUrl(), restPage.pageIndex(), restPage.pageSize());
    SearchResults<ProjectBindingInformation> searchResults = projectBindingsService.findProjectBindingsByRequest(serviceRequest);
    List<ProjectBinding> projectBindings = toProjectBindings(searchResults);
    return new ProjectBindingsSearchRestResponse(projectBindings, new PageRestResponse(restPage.pageIndex(), restPage.pageSize(), searchResults.total()));
  }

  private static void validateSearchParameters(ProjectBindingsSearchRestRequest request) {
    boolean hasRepositoryUrl = StringUtils.isNotBlank(request.repositoryUrl());
    boolean hasOtherParams = StringUtils.isNotBlank(request.repository()) || StringUtils.isNotBlank(request.dopSettingId());

    if (hasRepositoryUrl && hasOtherParams) {
      throwBadRequestException("'repositoryUrl' parameter cannot be used together with 'repository' or 'dopSettingId' parameters");
    }
  }

  private static List<ProjectBinding> toProjectBindings(SearchResults<ProjectBindingInformation> searchResults) {
    return searchResults.searchResults().stream()
      .map(DefaultProjectBindingsController::toProjectBinding)
      .toList();
  }

  private static ProjectBinding toProjectBinding(ProjectBindingInformation projectBindingInformation) {
    return new ProjectBinding(
      projectBindingInformation.id(),
      projectBindingInformation.devOpsPlatformSettingId(),
      projectBindingInformation.projectId(),
      projectBindingInformation.projectKey(),
      projectBindingInformation.repository(),
      projectBindingInformation.slug());
  }

  private static ProjectBinding toProjectBinding(ProjectDto projectDto, ProjectAlmSettingDto projectAlmSettingDto) {
    return new ProjectBinding(
      projectAlmSettingDto.getUuid(),
      projectAlmSettingDto.getAlmSettingUuid(),
      projectAlmSettingDto.getProjectUuid(),
      projectDto.getKey(),
      projectAlmSettingDto.getAlmRepo(),
      projectAlmSettingDto.getAlmSlug());
  }

}
