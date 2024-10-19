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

import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.PortfolioData;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.project.DeletedProject;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ANALYZED_BEFORE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ON_PROVISIONED_ONLY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_QUALIFIERS;

public class BulkDeleteActionIT {

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone().logIn();

  private final ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
  private final DbClient dbClient = db.getDbClient();
  private final ProjectLifeCycleListeners projectLifeCycleListeners = mock(ProjectLifeCycleListeners.class);
  private final Random random = new SecureRandom();

  private final BulkDeleteAction underTest = new BulkDeleteAction(componentCleanerService, dbClient, userSession, projectLifeCycleListeners);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void delete_projects() {
    userSession.addPermission(ADMINISTER);
    ProjectData projectData1ToDelete = db.components().insertPrivateProject();
    ProjectDto project1ToDelete = projectData1ToDelete.getProjectDto();
    ProjectData projectData2ToDelete = db.components().insertPrivateProject();
    ProjectDto project2ToDelete = projectData2ToDelete.getProjectDto();
    ComponentDto toKeep = db.components().insertPrivateProject().getMainBranchComponent();

    TestResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project1ToDelete.getKey() + "," + project2ToDelete.getKey())
      .execute();

    assertThat(result.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    assertThat(result.getInput()).isEmpty();
    verifyEntityDeleted(project1ToDelete, project2ToDelete);
    verifyListenersOnProjectsDeleted(projectData1ToDelete, projectData2ToDelete);
  }

  @Test
  public void delete_projects_by_keys() {
    userSession.addPermission(ADMINISTER);
    ProjectData toDeleteInOrg1 = db.components().insertPrivateProject();
    ProjectData toDeleteInOrg2 = db.components().insertPrivateProject();
    ComponentDto toKeep = db.components().insertPrivateProject().getMainBranchComponent();

    ws.newRequest()
      .setParam(PARAM_PROJECTS, toDeleteInOrg1.getProjectDto().getKey() + "," + toDeleteInOrg2.getProjectDto().getKey())
      .execute();

    verifyEntityDeleted(toDeleteInOrg1.getProjectDto(), toDeleteInOrg2.getProjectDto());
    verifyListenersOnProjectsDeleted(toDeleteInOrg1, toDeleteInOrg2);
  }

  @Test
  public void throw_IllegalArgumentException_if_request_without_any_parameters() {
    userSession.addPermission(ADMINISTER);
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    try {
      TestRequest request = ws.newRequest();
      assertThatThrownBy(request::execute)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("At least one parameter among analyzedBefore, projects and q must be provided");
    } finally {
      verifyNoDeletions();
      verifyNoMoreInteractions(projectLifeCycleListeners);
    }
  }

  @Test
  public void projects_that_dont_exist_are_ignored_and_dont_break_bulk_deletion() {
    userSession.addPermission(ADMINISTER);
    ProjectData toDelete1 = db.components().insertPrivateProject();
    ProjectData toDelete2 = db.components().insertPrivateProject();

    ws.newRequest()
      .setParam("projects", toDelete1.getProjectDto().getKey() + ",missing," + toDelete2.getProjectDto().getKey() + ",doesNotExist")
      .execute();

    verifyEntityDeleted(toDelete1.getProjectDto(), toDelete2.getProjectDto());
    verifyListenersOnProjectsDeleted(toDelete1, toDelete2);
  }

  @Test
  public void old_projects() {
    userSession.logIn().addPermission(ADMINISTER);
    long aLongTimeAgo = 1_000_000_000L;
    long recentTime = 3_000_000_000L;
    ProjectData oldProjectData = db.components().insertPublicProject();
    ProjectDto oldProject = oldProjectData.getProjectDto();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(oldProjectData.getMainBranchComponent()).setCreatedAt(aLongTimeAgo));
    ProjectData recentProjectData = db.components().insertPublicProject();
    ProjectDto recentProject = recentProjectData.getProjectDto();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(recentProjectData.getMainBranchComponent()).setCreatedAt(aLongTimeAgo));
    ComponentDto branch = db.components().insertProjectBranch(recentProjectData.getMainBranchComponent());
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(branch).setCreatedAt(recentTime));
    db.commit();

    ws.newRequest()
      .setParam(PARAM_ANALYZED_BEFORE, formatDate(new Date(recentTime)))
      .execute();

    verifyEntityDeleted(oldProject);
    verifyListenersOnProjectsDeleted(oldProjectData);
  }

  @Test
  public void provisioned_projects() {
    userSession.logIn().addPermission(ADMINISTER);
    ProjectData provisionedProject = db.components().insertPrivateProject();
    ProjectData analyzedProjectData = db.components().insertPrivateProject();
    ProjectDto analyzedProject = analyzedProjectData.getProjectDto();
    db.components().insertSnapshot(newAnalysis(analyzedProjectData.getMainBranchComponent()));

    ws.newRequest().setParam(PARAM_PROJECTS, provisionedProject.getProjectDto().getKey() + "," + analyzedProject.getKey()).setParam(PARAM_ON_PROVISIONED_ONLY, "true").execute();

    verifyEntityDeleted(provisionedProject.getProjectDto());
    verifyListenersOnProjectsDeleted(provisionedProject);
  }

  @Test
  public void delete_more_than_50_projects() {
    userSession.logIn().addPermission(ADMINISTER);
    ProjectData[] projects = IntStream.range(0, 55).mapToObj(i -> db.components().insertPrivateProject()).toArray(ProjectData[]::new);

    List<String> projectKeys = Stream.of(projects).map(ProjectData::getProjectDto).map(ProjectDto::getKey).toList();
    ws.newRequest().setParam(PARAM_PROJECTS, String.join(",", projectKeys)).execute();

    verifyEntityDeleted(Stream.of(projects).map(ProjectData::getProjectDto).toArray(ProjectDto[]::new));
    verifyListenersOnProjectsDeleted(projects);
  }

  @Test
  public void projects_and_views() {
    userSession.logIn().addPermission(ADMINISTER);
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto view = db.components().insertPrivatePortfolio();
    PortfolioDto portfolioDto = db.components().getPortfolioDto(view);

    ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getProjectDto().getKey() + "," + view.getKey())
      .setParam(PARAM_QUALIFIERS, String.join(",", Qualifiers.PROJECT, Qualifiers.VIEW))
      .execute();

    verifyEntityDeleted(project.getProjectDto(), portfolioDto);
    verifyListenersOnProjectsDeleted(project, portfolioDto);
  }

  @Test
  public void delete_by_key_query_with_partial_match_case_insensitive() {
    userSession.logIn().addPermission(ADMINISTER);
    ProjectData matchKeyProject = db.components().insertPrivateProject(p -> p.setKey("project-_%-key"));
    ProjectData matchUppercaseKeyProject = db.components().insertPrivateProject(p -> p.setKey("PROJECT-_%-KEY"));
    ProjectDto noMatchProject = db.components().insertPrivateProject(p -> p.setKey("project-key-without-escaped-characters")).getProjectDto();

    ws.newRequest().setParam(Param.TEXT_QUERY, "JeCt-_%-k").execute();

    verifyEntityDeleted(matchKeyProject.getProjectDto(), matchUppercaseKeyProject.getProjectDto());
    verifyListenersOnProjectsDeleted(matchKeyProject, matchUppercaseKeyProject);
  }

  /**
   * SONAR-10356
   */
  @Test
  public void delete_only_the_1000_first_projects() {
    userSession.logIn().addPermission(ADMINISTER);
    List<String> keys = IntStream.range(0, 1_010).mapToObj(i -> "key" + i).toList();
    keys.forEach(key -> db.components().insertPrivateProject(p -> p.setKey(key)).getMainBranchComponent());

    ws.newRequest()
      .setParam("projects", StringUtils.join(keys, ","))
      .execute();

    verify(componentCleanerService, times(1_000)).deleteEntity(any(DbSession.class), any(EntityDto.class));
    ArgumentCaptor<Set<DeletedProject>> projectsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(projectLifeCycleListeners).onProjectsDeleted(projectsCaptor.capture());
    assertThat(projectsCaptor.getValue()).hasSize(1_000);
  }

  @Test
  public void projectLifeCycleListeners_onProjectsDeleted_called_even_if_delete_fails() {
    userSession.logIn().addPermission(ADMINISTER);
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    ProjectData project3 = db.components().insertPrivateProject();
    ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
    RuntimeException expectedException = new RuntimeException("Faking delete failing on 2nd project");
    doNothing()
      .doThrow(expectedException)
      .when(componentCleanerService)
      .deleteEntity(any(), any(ProjectDto.class));

    try {
      ws.newRequest()
        .setParam("projects", project1.getProjectDto().getKey() + "," + project2.getProjectDto().getKey() + "," + project3.getProjectDto().getKey())
        .execute();
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(expectedException);
      verifyListenersOnProjectsDeleted(project1, project2, project3);
    }
  }

  @Test
  public void global_administrator_deletes_projects_by_keys() {
    userSession.logIn().addPermission(ADMINISTER);
    ProjectData toDelete1 = db.components().insertPrivateProject();
    ProjectData toDelete2 = db.components().insertPrivateProject();

    ws.newRequest()
      .setParam("projects", toDelete1.getProjectDto().getKey() + "," + toDelete2.getProjectDto().getKey())
      .execute();

    verifyEntityDeleted(toDelete1.getProjectDto(), toDelete2.getProjectDto());
    verifyListenersOnProjectsDeleted(toDelete1, toDelete2);
  }

  @Test
  public void should_throw_IAE_when_providing_future_date_as_analyzed_before_date() {
    userSession.logIn().addPermission(ADMINISTER);

    Date now = new Date();
    Date futureDate = new DateTime(now).plusDays(random.nextInt(1, Integer.MAX_VALUE)).toDate();
    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(project1).setCreatedAt(now.getTime()));
    ComponentDto project2 = db.components().insertPublicProject().getMainBranchComponent();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(project2).setCreatedAt(now.getTime()));
    db.commit();

    TestRequest request = ws.newRequest().setParam(PARAM_ANALYZED_BEFORE, formatDate(futureDate));

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Provided value for parameter analyzedBefore must not be a future date");
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();
    TestRequest request = ws.newRequest().setParam("ids", "whatever-the-uuid");
    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");

    verifyNoDeletions();
    verifyNoMoreInteractions(projectLifeCycleListeners);
  }

  private void verifyEntityDeleted(EntityDto... entities) {
    ArgumentCaptor<EntityDto> argument = ArgumentCaptor.forClass(EntityDto.class);
    verify(componentCleanerService, times(entities.length)).deleteEntity(any(DbSession.class), argument.capture());

    for (EntityDto entity : entities) {
      assertThat(argument.getAllValues()).extracting(EntityDto::getUuid).contains(entity.getUuid());
    }
  }

  private void verifyNoDeletions() {
    verifyNoMoreInteractions(componentCleanerService);
  }

  private void verifyListenersOnProjectsDeleted(ProjectData projectData, PortfolioDto portfolioDto) {
    Map<EntityDto, String> entityWithBranh = new HashMap<>();
    entityWithBranh.put(projectData.getProjectDto(), projectData.getMainBranchDto().getUuid());
    entityWithBranh.put(portfolioDto, null);

    verifyListenersOnProjectsDeleted(entityWithBranh);
  }

  private void verifyListenersOnProjectsDeleted(ProjectData... projectData) {
    verifyListenersOnProjectsDeleted(Arrays.stream(projectData).collect(Collectors.toMap(ProjectData::getProjectDto, data -> data.getMainBranchDto().getUuid())));
  }

  private void verifyListenersOnProjectsDeleted(PortfolioData... portfolioData) {
    verifyListenersOnProjectsDeleted(Arrays.stream(portfolioData).collect(Collectors.toMap(PortfolioData::getPortfolioDto, null)));
  }

  private void verifyListenersOnProjectsDeleted(Map<EntityDto, String> entityWithBranchUuid) {
    verify(projectLifeCycleListeners)
      .onProjectsDeleted(entityWithBranchUuid.entrySet()
        .stream().map(entry -> new DeletedProject(Project.from(entry.getKey()), entry.getValue())).collect(Collectors.toSet()));
  }
}
