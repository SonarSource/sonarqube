/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects.SearchMyProjectsWsResponse;
import org.sonarqube.ws.Projects.SearchMyProjectsWsResponse.Link;
import org.sonarqube.ws.Projects.SearchMyProjectsWsResponse.Project;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.project.ws.SearchMyProjectsData.builder;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchMyProjectsAction implements ProjectsWsAction {
  private static final int MAX_SIZE = 500;

  private final DbClient dbClient;
  private final UserSession userSession;

  public SearchMyProjectsAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("search_my_projects")
      .setDescription("Return list of projects for which the current user has 'Administer' permission. Maximum 1'000 projects are returned.")
      .setResponseExample(getClass().getResource("search_my_projects-example.json"))
      .addPagingParams(100, MAX_SIZE)
      .setSince("6.0")
      .setInternal(true)
      .setHandler(this);

  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchMyProjectsWsResponse searchMyProjectsWsResponse = doHandle(toRequest(request));
    writeProtobuf(searchMyProjectsWsResponse, request, response);
  }

  private SearchMyProjectsWsResponse doHandle(SearchMyProjectsRequest request) {
    checkAuthenticated();

    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchMyProjectsData data = load(dbSession, request);
      return buildResponse(request, data);
    }
  }

  private static SearchMyProjectsWsResponse buildResponse(SearchMyProjectsRequest request, SearchMyProjectsData data) {
    SearchMyProjectsWsResponse.Builder response = SearchMyProjectsWsResponse.newBuilder();

    ProjectDtoToWs projectDtoToWs = new ProjectDtoToWs(data);

    data.projects().stream()
      .map(projectDtoToWs)
      .forEach(response::addProjects);

    response.getPagingBuilder()
      .setPageIndex(request.getPage())
      .setPageSize(request.getPageSize())
      .setTotal(data.totalNbOfProjects())
      .build();

    return response.build();
  }

  private void checkAuthenticated() {
    userSession.checkLoggedIn();
  }

  private static SearchMyProjectsRequest toRequest(Request request) {
    return SearchMyProjectsRequest.builder()
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .build();
  }

  private static class ProjectDtoToWs implements Function<ProjectDto, Project> {
    private final SearchMyProjectsData data;

    private ProjectDtoToWs(SearchMyProjectsData data) {
      this.data = data;
    }

    @Override
    public Project apply(ProjectDto dto) {
      Project.Builder project = Project.newBuilder();

      project
        .setKey(dto.getKey())
        .setName(dto.getName());
      ofNullable(emptyToNull(dto.getDescription())).ifPresent(project::setDescription);
      data.projectLinksFor(dto.getUuid()).stream()
        .map(ProjectLinkDtoToWs.INSTANCE)
        .forEach(project::addLinks);

      String mainBranchUuid = data.mainBranchUuidForProjectUuid(dto.getUuid());
      data.lastSnapshot(mainBranchUuid).ifPresent(s -> {
        project.setLastAnalysisDate(formatDateTime(s.getCreatedAt()));
        ofNullable(s.getRevision()).ifPresent(project::setRevision);
      });
      data.qualityGateStatusFor(mainBranchUuid).ifPresent(project::setQualityGate);

      return project.build();
    }
  }

  private enum ProjectLinkDtoToWs implements Function<ProjectLinkDto, Link> {
    INSTANCE;

    @Override
    public Link apply(ProjectLinkDto dto) {
      Link.Builder link = Link.newBuilder();
      link.setHref(dto.getHref());

      if (!isNullOrEmpty(dto.getName())) {
        link.setName(dto.getName());
      }
      if (!isNullOrEmpty(dto.getType())) {
        link.setType(dto.getType());
      }

      return link.build();
    }
  }

  private SearchMyProjectsData load(DbSession dbSession, SearchMyProjectsRequest request) {
    SearchMyProjectsData.Builder data = builder();
    ProjectsResult searchResult = searchProjects(dbSession, request);
    List<ProjectDto> projects = searchResult.projects;

    List<String> projectUuids = projects.stream().map(ProjectDto::getUuid).toList();
    List<ProjectLinkDto> projectLinks = dbClient.projectLinkDao().selectByProjectUuids(dbSession, projectUuids);

    List<BranchDto> branches = searchResult.branches;

    Set<String> mainBranchUuids = branches.stream().map(BranchDto::getUuid).collect(Collectors.toSet());
    List<SnapshotDto> snapshots = dbClient.snapshotDao()
      .selectLastAnalysesByRootComponentUuids(dbSession, mainBranchUuids);
    List<LiveMeasureDto> qualityGates = dbClient.liveMeasureDao()
      .selectByComponentUuidsAndMetricKeys(dbSession, mainBranchUuids, singletonList(CoreMetrics.ALERT_STATUS_KEY));

    data
      .setProjects(projects)
      .setBranches(searchResult.branches)
      .setProjectLinks(projectLinks)
      .setSnapshots(snapshots)
      .setQualityGates(qualityGates)
      .setTotalNbOfProjects(searchResult.total);

    return data.build();
  }

  private ProjectsResult searchProjects(DbSession dbSession, SearchMyProjectsRequest request) {
    String userUuid = requireNonNull(userSession.getUuid(), "Current user must be authenticated");

    List<String> entitiesUuid = dbClient.roleDao().selectEntityUuidsByPermissionAndUserUuidAndQualifier(dbSession, UserRole.ADMIN, userUuid, Set.of(Qualifiers.PROJECT));

    ImmutableSet<String> subSetEntityUuids = ImmutableSet.copyOf(entitiesUuid.subList(0, Math.min(entitiesUuid.size(), DatabaseUtils.PARTITION_SIZE_FOR_ORACLE)));
    Pagination pagination = Pagination.forPage(request.page).andSize(request.pageSize);
    List<ProjectDto> projectDtos = dbClient.projectDao().selectByUuids(dbSession, subSetEntityUuids, pagination);

    List<BranchDto> branchDtos = dbClient.branchDao().selectMainBranchesByProjectUuids(dbSession, projectDtos.stream().map(ProjectDto::getUuid).collect(Collectors.toSet()));

    return new ProjectsResult(projectDtos, branchDtos, subSetEntityUuids.size());
  }

  private static class ProjectsResult {

    private final List<ProjectDto> projects;
    private final List<BranchDto> branches;
    private final int total;

    private ProjectsResult(List<ProjectDto> projects, List<BranchDto> branches, int total) {
      this.projects = projects;
      this.branches = branches;
      assertThatAllCollectionsHaveSameSize(projects, branches);
      this.total = total;
    }

    private static void assertThatAllCollectionsHaveSameSize(List<ProjectDto> projects, List<BranchDto> branches) {
      if (projects.size() != branches.size()) {
        throw new IllegalStateException("There must be the same number of projects as the branches.");
      }
    }
  }

  private static class SearchMyProjectsRequest {
    private final Integer page;
    private final Integer pageSize;

    private SearchMyProjectsRequest(Builder builder) {
      this.page = builder.page;
      this.pageSize = builder.pageSize;
    }

    @CheckForNull
    public Integer getPage() {
      return page;
    }

    @CheckForNull
    public Integer getPageSize() {
      return pageSize;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  private static class Builder {
    private Integer page;
    private Integer pageSize;

    private Builder() {
      // enforce method constructor
    }

    public Builder setPage(@Nullable Integer page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(@Nullable Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public SearchMyProjectsRequest build() {
      return new SearchMyProjectsRequest(this);
    }
  }
}
