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
package org.sonar.server.project.ws;

import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsProjects.SearchMyProjectsWsResponse;
import org.sonarqube.ws.WsProjects.SearchMyProjectsWsResponse.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchMyProjectsActionTest {
  private static final String USER_LOGIN = "TESTER";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private WsActionTester ws;
  private UserDto user;
  private MetricDto alertStatusMetric;

  @Before
  public void setUp() {
    user = db.users().insertUser(newUserDto().setLogin(USER_LOGIN));
    userSession.login(this.user.getLogin()).setUserId(user.getId().intValue());
    alertStatusMetric = dbClient.metricDao().insert(dbSession, newMetricDto().setKey(ALERT_STATUS_KEY).setValueType(ValueType.LEVEL.name()));
    db.commit();

    ws = new WsActionTester(new SearchMyProjectsAction(dbClient, new SearchMyProjectsDataLoader(userSession, dbClient), userSession));
  }

  @Test
  public void search_json_example() {
    ComponentDto jdk7 = insertJdk7();
    ComponentDto cLang = insertClang();
    dbClient.componentLinkDao().insert(dbSession,
      new ComponentLinkDto().setComponentUuid(jdk7.uuid()).setHref("http://www.oracle.com").setType(ComponentLinkDto.TYPE_HOME_PAGE).setName("Home"));
    dbClient.componentLinkDao().insert(dbSession,
      new ComponentLinkDto().setComponentUuid(jdk7.uuid()).setHref("http://download.java.net/openjdk/jdk8/").setType(ComponentLinkDto.TYPE_SOURCES).setName("Sources"));
    long oneTime = DateUtils.parseDateTime("2016-06-10T13:17:53+0000").getTime();
    long anotherTime = DateUtils.parseDateTime("2016-06-11T14:25:53+0000").getTime();
    SnapshotDto jdk7Snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(jdk7).setCreatedAt(oneTime));
    SnapshotDto cLangSnapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(cLang).setCreatedAt(anotherTime));
    dbClient.measureDao().insert(dbSession, newMeasureDto(alertStatusMetric, jdk7, jdk7Snapshot).setData(Level.ERROR.name()));
    dbClient.measureDao().insert(dbSession, newMeasureDto(alertStatusMetric, cLang, cLangSnapshot).setData(Level.OK.name()));
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, cLang);
    db.commit();
    System.setProperty("user.timezone", "UTC");

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search_my_projects-example.json"));
  }

  @Test
  public void return_only_current_user_projects() {
    ComponentDto jdk7 = insertJdk7();
    ComponentDto cLang = insertClang();
    UserDto anotherUser = db.users().insertUser(newUserDto());
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(anotherUser, UserRole.ADMIN, cLang);

    SearchMyProjectsWsResponse result = call_ws();

    assertThat(result.getProjectsCount()).isEqualTo(1);
    assertThat(result.getProjects(0).getId()).isEqualTo(jdk7.uuid());
  }

  @Test
  public void sort_projects_by_name() {
    ComponentDto b_project = db.components().insertComponent(newProjectDto().setName("B_project_name"));
    ComponentDto c_project = db.components().insertComponent(newProjectDto().setName("c_project_name"));
    ComponentDto a_project = db.components().insertComponent(newProjectDto().setName("A_project_name"));

    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, b_project);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, a_project);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, c_project);

    SearchMyProjectsWsResponse result = call_ws();

    assertThat(result.getProjectsCount()).isEqualTo(3);
    assertThat(result.getProjectsList()).extracting(Project::getId)
      .containsExactly(a_project.uuid(), b_project.uuid(), c_project.uuid());
  }

  @Test
  public void paginate_projects() {
    for (int i = 0; i < 10; i++) {
      ComponentDto project = db.components().insertComponent(newProjectDto().setName("project-" + i));
      db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, project);
    }

    SearchMyProjectsWsResponse result = call_ws(ws.newRequest()
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3"));

    assertThat(result.getProjectsCount()).isEqualTo(3);
    assertThat(result.getProjectsList()).extracting(Project::getName).containsExactly("project-3", "project-4", "project-5");
  }

  @Test
  public void return_only_projects_when_user_is_admin() {
    ComponentDto jdk7 = insertJdk7();
    ComponentDto clang = insertClang();

    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user, UserRole.ISSUE_ADMIN, clang);

    SearchMyProjectsWsResponse result = call_ws();

    assertThat(result.getProjectsCount()).isEqualTo(1);
    assertThat(result.getProjects(0).getId()).isEqualTo(jdk7.uuid());
  }

  @Test
  public void do_not_return_views_or_developers() {
    ComponentDto jdk7 = insertJdk7();
    ComponentDto dev = insertDeveloper();
    ComponentDto view = insertView();

    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, dev);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, view);

    SearchMyProjectsWsResponse result = call_ws();

    assertThat(result.getProjectsCount()).isEqualTo(1);
    assertThat(result.getProjects(0).getId()).isEqualTo(jdk7.uuid());
  }

  @Test
  public void admin_via_groups() {
    ComponentDto jdk7 = insertJdk7();
    ComponentDto cLang = insertClang();

    GroupDto group = db.users().insertGroup(newGroupDto());
    db.users().insertMember(group, user);

    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group, UserRole.USER, cLang);

    SearchMyProjectsWsResponse result = call_ws();

    assertThat(result.getProjectsCount()).isEqualTo(1);
    assertThat(result.getProjects(0).getId()).isEqualTo(jdk7.uuid());
  }

  @Test
  public void admin_via_groups_and_users() {
    ComponentDto jdk7 = insertJdk7();
    ComponentDto cLang = insertClang();
    ComponentDto sonarqube = db.components().insertComponent(newProjectDto());

    GroupDto group = db.users().insertGroup(newGroupDto());
    db.users().insertMember(group, user);

    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, cLang);
    // admin via group and user
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, sonarqube);
    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, sonarqube);

    SearchMyProjectsWsResponse result = call_ws();

    assertThat(result.getProjectsCount()).isEqualTo(3);
    assertThat(result.getProjectsList()).extracting(Project::getId).containsOnly(jdk7.uuid(), cLang.uuid(), sonarqube.uuid());
  }

  @Test
  public void search_my_projects_by_name() {
    ComponentDto sonarqube = db.components().insertComponent(newProjectDto().setName("ONE_PROJECT_NAME"));
    ComponentDto jdk8 = db.components().insertComponent(newProjectDto().setName("TWO_PROJECT_NAME"));
    ComponentDto ruby = db.components().insertComponent(newProjectDto().setName("ANOTHER_42"));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(sonarqube), newAnalysis(jdk8), newAnalysis(ruby));
    db.components().indexAllComponents();
    db.commit();

    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, sonarqube);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk8);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, ruby);

    SearchMyProjectsWsResponse result = call_ws(ws.newRequest().setParam(TEXT_QUERY, "_project_"));

    assertThat(result.getProjectsCount()).isEqualTo(2);
    assertThat(result.getProjectsList()).extracting(Project::getId)
      .containsOnlyOnce(sonarqube.uuid(), jdk8.uuid())
      .doesNotContain(ruby.uuid());
  }

  @Test
  public void search_my_projects_by_exact_match_on_key() {
    ComponentDto sonarqube = db.components().insertComponent(newProjectDto().setKey("MY_PROJECT_KEY"));
    ComponentDto ruby = db.components().insertComponent(newProjectDto().setKey("MY_PROJECT_KEY_OR_ELSE"));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(sonarqube), newAnalysis(ruby));
    db.components().indexAllComponents();
    db.commit();

    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, sonarqube);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, ruby);

    SearchMyProjectsWsResponse result = call_ws(ws.newRequest().setParam(TEXT_QUERY, "MY_PROJECT_KEY"));

    assertThat(result.getProjectsCount()).isEqualTo(1);
    assertThat(result.getProjectsList()).extracting(Project::getId)
      .containsOnlyOnce(sonarqube.uuid())
      .doesNotContain(ruby.uuid());
  }

  @Test
  public void empty_response() {
    String result = ws.newRequest().execute().getInput();
    assertJson(result).isSimilarTo("{\"projects\":[]}");
  }

  @Test
  public void fail_if_not_authenticated() {
    userSession.anonymous();
    expectedException.expect(UnauthorizedException.class);

    call_ws();
  }

  @Test
  public void fail_if_query_length_is_less_than_3_characters() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'q' parameter must have at least 3 characters");

    call_ws(ws.newRequest().setParam(TEXT_QUERY, "ab"));
  }

  private ComponentDto insertClang() {
    return db.components().insertComponent(newProjectDto(Uuids.UUID_EXAMPLE_01)
      .setName("Clang")
      .setKey("clang"));
  }

  private ComponentDto insertJdk7() {
    return db.components().insertComponent(newProjectDto(Uuids.UUID_EXAMPLE_02)
      .setName("JDK 7")
      .setKey("net.java.openjdk:jdk7")
      .setDescription("JDK"));
  }

  private ComponentDto insertView() {
    return db.components().insertComponent(newView("752d8bfd-420c-4a83-a4e5-8ab19b13c8fc")
      .setName("Java")
      .setKey("Java"));
  }

  private ComponentDto insertDeveloper() {
    return db.components().insertComponent(newDeveloper("Joda", "4e607bf9-7ed0-484a-946d-d58ba7dab2fb")
      .setKey("joda"));
  }

  private SearchMyProjectsWsResponse call_ws() {
    return call_ws(ws.newRequest());
  }

  private SearchMyProjectsWsResponse call_ws(TestRequest request) {
    InputStream responseStream = request
      .setMediaType(MediaTypes.PROTOBUF)
      .execute().getInputStream();

    try {
      return SearchMyProjectsWsResponse.parseFrom(responseStream);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
