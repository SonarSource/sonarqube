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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.exceptions.UnauthorizedException;
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

public class BulkDeleteActionTest {

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone().logIn();

  private final ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
  private final DbClient dbClient = db.getDbClient();
  private final ProjectLifeCycleListeners projectLifeCycleListeners = mock(ProjectLifeCycleListeners.class);

  private final BulkDeleteAction underTest = new BulkDeleteAction(componentCleanerService, dbClient, userSession, projectLifeCycleListeners);
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void delete_projects() {
    userSession.addPermission(ADMINISTER);
    ComponentDto project1ToDelete = db.components().insertPrivateProject();
    ComponentDto project2ToDelete = db.components().insertPrivateProject();
    ComponentDto toKeep = db.components().insertPrivateProject();

    TestResponse result = ws.newRequest()
      .setParam(PARAM_PROJECTS, project1ToDelete.getKey() + "," + project2ToDelete.getKey())
      .execute();

    assertThat(result.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
    assertThat(result.getInput()).isEmpty();
    verifyComponentDeleted(project1ToDelete, project2ToDelete);
    verifyListenersOnProjectsDeleted(project1ToDelete, project2ToDelete);
  }

  @Test
  public void delete_projects_by_keys() {
    userSession.addPermission(ADMINISTER);
    ComponentDto toDeleteInOrg1 = db.components().insertPrivateProject();
    ComponentDto toDeleteInOrg2 = db.components().insertPrivateProject();
    ComponentDto toKeep = db.components().insertPrivateProject();

    ws.newRequest()
      .setParam(PARAM_PROJECTS, toDeleteInOrg1.getKey() + "," + toDeleteInOrg2.getKey())
      .execute();

    verifyComponentDeleted(toDeleteInOrg1, toDeleteInOrg2);
    verifyListenersOnProjectsDeleted(toDeleteInOrg1, toDeleteInOrg2);
  }

  @Test
  public void throw_IllegalArgumentException_if_request_without_any_parameters() {
    userSession.addPermission(ADMINISTER);
    ComponentDto project = db.components().insertPrivateProject();

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
    ComponentDto toDelete1 = db.components().insertPrivateProject();
    ComponentDto toDelete2 = db.components().insertPrivateProject();

    ws.newRequest()
      .setParam("projects", toDelete1.getKey() + ",missing," + toDelete2.getKey() + ",doesNotExist")
      .execute();

    verifyComponentDeleted(toDelete1, toDelete2);
    verifyListenersOnProjectsDeleted(toDelete1, toDelete2);
  }

  @Test
  public void old_projects() {
    userSession.logIn().addPermission(ADMINISTER);
    long aLongTimeAgo = 1_000_000_000L;
    long recentTime = 3_000_000_000L;
    ComponentDto oldProject = db.components().insertPublicProject();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(oldProject).setCreatedAt(aLongTimeAgo));
    ComponentDto recentProject = db.components().insertPublicProject();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(recentProject).setCreatedAt(aLongTimeAgo));
    ComponentDto branch = db.components().insertProjectBranch(recentProject);
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(branch).setCreatedAt(recentTime));
    db.commit();

    ws.newRequest()
      .setParam(PARAM_ANALYZED_BEFORE, formatDate(new Date(recentTime)))
      .execute();

    verifyComponentDeleted(oldProject);
    verifyListenersOnProjectsDeleted(oldProject);
  }

  @Test
  public void provisioned_projects() {
    userSession.logIn().addPermission(ADMINISTER);
    ComponentDto provisionedProject = db.components().insertPrivateProject();
    ComponentDto analyzedProject = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(analyzedProject));

    ws.newRequest().setParam(PARAM_PROJECTS, provisionedProject.getKey() + "," + analyzedProject.getKey()).setParam(PARAM_ON_PROVISIONED_ONLY, "true").execute();

    verifyComponentDeleted(provisionedProject);
    verifyListenersOnProjectsDeleted(provisionedProject);
  }

  @Test
  public void delete_more_than_50_projects() {
    userSession.logIn().addPermission(ADMINISTER);
    ComponentDto[] projects = IntStream.range(0, 55).mapToObj(i -> db.components().insertPrivateProject()).toArray(ComponentDto[]::new);

    List<String> projectKeys = Stream.of(projects).map(ComponentDto::getKey).collect(Collectors.toList());
    ws.newRequest().setParam(PARAM_PROJECTS, String.join(",", projectKeys)).execute();

    verifyComponentDeleted(projects);
    verifyListenersOnProjectsDeleted(projects);
  }

  @Test
  public void projects_and_views() {
    userSession.logIn().addPermission(ADMINISTER);
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto view = db.components().insertPrivatePortfolio();

    ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey() + "," + view.getKey())
      .setParam(PARAM_QUALIFIERS, String.join(",", Qualifiers.PROJECT, Qualifiers.VIEW))
      .execute();

    verifyComponentDeleted(project, view);
    verifyListenersOnProjectsDeleted(project, view);
  }

  @Test
  public void delete_by_key_query_with_partial_match_case_insensitive() {
    userSession.logIn().addPermission(ADMINISTER);
    ComponentDto matchKeyProject = db.components().insertPrivateProject(p -> p.setKey("project-_%-key"));
    ComponentDto matchUppercaseKeyProject = db.components().insertPrivateProject(p -> p.setKey("PROJECT-_%-KEY"));
    ComponentDto noMatchProject = db.components().insertPrivateProject(p -> p.setKey("project-key-without-escaped-characters"));

    ws.newRequest().setParam(Param.TEXT_QUERY, "JeCt-_%-k").execute();

    verifyComponentDeleted(matchKeyProject, matchUppercaseKeyProject);
    verifyListenersOnProjectsDeleted(matchKeyProject, matchUppercaseKeyProject);
  }

  /**
   * SONAR-10356
   */
  @Test
  public void delete_only_the_1000_first_projects() {
    userSession.logIn().addPermission(ADMINISTER);
    List<String> keys = IntStream.range(0, 1_010).mapToObj(i -> "key" + i).collect(MoreCollectors.toArrayList());
    keys.forEach(key -> db.components().insertPrivateProject(p -> p.setKey(key)));

    ws.newRequest()
      .setParam("projects", StringUtils.join(keys, ","))
      .execute();

    verify(componentCleanerService, times(1_000)).delete(any(DbSession.class), any(ComponentDto.class));
    ArgumentCaptor<Set<Project>> projectsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(projectLifeCycleListeners).onProjectsDeleted(projectsCaptor.capture());
    assertThat(projectsCaptor.getValue()).hasSize(1_000);
  }

  @Test
  public void projectLifeCycleListeners_onProjectsDeleted_called_even_if_delete_fails() {
    userSession.logIn().addPermission(ADMINISTER);
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
    RuntimeException expectedException = new RuntimeException("Faking delete failing on 2nd project");
    doNothing()
      .doThrow(expectedException)
      .when(componentCleanerService)
      .delete(any(), any(ProjectDto.class));

    try {
      ws.newRequest()
        .setParam("projects", project1.getKey() + "," + project2.getKey() + "," + project3.getKey())
        .execute();
    } catch (RuntimeException e) {
      assertThat(e).isSameAs(expectedException);
      verifyListenersOnProjectsDeleted(project1, project2, project3);
    }
  }

  @Test
  public void global_administrator_deletes_projects_by_keys() {
    userSession.logIn().addPermission(ADMINISTER);
    ComponentDto toDelete1 = db.components().insertPrivateProject();
    ComponentDto toDelete2 = db.components().insertPrivateProject();

    ws.newRequest()
      .setParam("projects", toDelete1.getKey() + "," + toDelete2.getKey())
      .execute();

    verifyComponentDeleted(toDelete1, toDelete2);
    verifyListenersOnProjectsDeleted(toDelete1, toDelete2);
  }

  @Test
  public void should_throw_IAE_when_providing_future_date_as_analyzed_before_date() {
    userSession.logIn().addPermission(ADMINISTER);

    Date now = new Date();
    Date futureDate = new DateTime(now).plusDays(RandomUtils.nextInt() + 1).toDate();
    ComponentDto project1 = db.components().insertPublicProject();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(project1).setCreatedAt(now.getTime()));
    ComponentDto project2 = db.components().insertPublicProject();
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

  private void verifyComponentDeleted(ComponentDto... projects) {
    ArgumentCaptor<ComponentDto> argument = ArgumentCaptor.forClass(ComponentDto.class);
    verify(componentCleanerService, times(projects.length)).delete(any(DbSession.class), argument.capture());

    for (ComponentDto project : projects) {
      assertThat(argument.getAllValues()).extracting(ComponentDto::uuid).contains(project.uuid());
    }
  }

  private void verifyNoDeletions() {
    verifyNoMoreInteractions(componentCleanerService);
  }

  private void verifyListenersOnProjectsDeleted(ComponentDto... components) {
    verify(projectLifeCycleListeners)
      .onProjectsDeleted(Arrays.stream(components).map(Project::from).collect(Collectors.toSet()));
  }
}
