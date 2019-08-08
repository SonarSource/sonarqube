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
package org.sonar.server.project.ws;

import java.util.stream.IntStream;
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
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Projects.SearchMyProjectsWsResponse;
import org.sonarqube.ws.Projects.SearchMyProjectsWsResponse.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchMyProjectsActionTest {

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
    user = db.users().insertUser();
    userSession.logIn(user);
    alertStatusMetric = dbClient.metricDao().insert(dbSession, newMetricDto().setKey(ALERT_STATUS_KEY).setValueType(ValueType.LEVEL.name()));
    db.commit();

    ws = new WsActionTester(new SearchMyProjectsAction(dbClient, userSession));
  }

  @Test
  public void search_json_example() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto jdk7 = insertJdk7(organizationDto);
    ComponentDto cLang = insertClang(organizationDto);
    db.componentLinks().insertProvidedLink(jdk7, l -> l.setHref("http://www.oracle.com").setType(ProjectLinkDto.TYPE_HOME_PAGE).setName("Home"));
    db.componentLinks().insertProvidedLink(jdk7, l -> l.setHref("http://download.java.net/openjdk/jdk8/").setType(ProjectLinkDto.TYPE_SOURCES).setName("Sources"));
    long oneTime = DateUtils.parseDateTime("2016-06-10T13:17:53+0000").getTime();
    long anotherTime = DateUtils.parseDateTime("2016-06-11T14:25:53+0000").getTime();
    SnapshotDto jdk7Snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(jdk7).setCreatedAt(oneTime));
    SnapshotDto cLangSnapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(cLang).setCreatedAt(anotherTime));
    dbClient.liveMeasureDao().insert(dbSession, newLiveMeasure(jdk7, alertStatusMetric).setData(Level.ERROR.name()));
    dbClient.liveMeasureDao().insert(dbSession, newLiveMeasure(cLang, alertStatusMetric).setData(Level.OK.name()));
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, cLang);
    db.commit();
    System.setProperty("user.timezone", "UTC");

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search_my_projects-example.json"));
  }

  @Test
  public void return_only_current_user_projects() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto jdk7 = insertJdk7(organizationDto);
    ComponentDto cLang = insertClang(organizationDto);
    UserDto anotherUser = db.users().insertUser(newUserDto());
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(anotherUser, UserRole.ADMIN, cLang);

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isEqualTo(1);
  }

  @Test
  public void return_only_first_1000_projects() {
    OrganizationDto organization = db.organizations().insert();
    IntStream.range(0, 1_010).forEach(i -> {
      ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organization));
      db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, project);
    });

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getPaging().getTotal()).isEqualTo(1_000);
  }

  @Test
  public void sort_projects_by_name() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto b_project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setName("B_project_name"));
    ComponentDto c_project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setName("c_project_name"));
    ComponentDto a_project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setName("A_project_name"));

    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, b_project);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, a_project);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, c_project);

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isEqualTo(3);
  }

  @Test
  public void paginate_projects() {
    OrganizationDto organizationDto = db.organizations().insert();
    for (int i = 0; i < 10; i++) {
      ComponentDto project = db.components().insertComponent(ComponentTesting.newPrivateProjectDto(organizationDto).setName("project-" + i));
      db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, project);
    }

    SearchMyProjectsWsResponse result = ws.newRequest()
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .executeProtobuf(SearchMyProjectsWsResponse.class);

    assertThat(result.getProjectsCount()).isEqualTo(3);
    assertThat(result.getProjectsList()).extracting(Project::getName).containsExactly("project-3", "project-4", "project-5");
  }

  @Test
  public void return_only_projects_when_user_is_admin() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto jdk7 = insertJdk7(organizationDto);
    ComponentDto clang = insertClang(organizationDto);

    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user, UserRole.ISSUE_ADMIN, clang);

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isEqualTo(1);
  }

  @Test
  public void does_not_return_views() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto jdk7 = insertJdk7(organizationDto);
    ComponentDto view = insertView(organizationDto);

    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, view);

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isEqualTo(1);
  }

  @Test
  public void does_not_return_branches() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, project);

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsList())
      .extracting(Project::getKey)
      .containsExactlyInAnyOrder(project.getDbKey());
  }

  @Test
  public void admin_via_groups() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto jdk7 = insertJdk7(org);
    ComponentDto cLang = insertClang(org);

    GroupDto group = db.users().insertGroup(org);
    db.users().insertMember(group, user);

    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group, UserRole.USER, cLang);

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isEqualTo(1);
  }

  @Test
  public void admin_via_groups_and_users() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto jdk7 = insertJdk7(org);
    ComponentDto cLang = insertClang(org);
    ComponentDto sonarqube = db.components().insertPrivateProject(org);

    GroupDto group = db.users().insertGroup(org);
    db.users().insertMember(group, user);

    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, jdk7);
    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, cLang);
    // admin via group and user
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, sonarqube);
    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, sonarqube);

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isEqualTo(3);
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

    callWs();
  }

  private ComponentDto insertClang(OrganizationDto organizationDto) {
    return db.components().insertComponent(newPrivateProjectDto(organizationDto, Uuids.UUID_EXAMPLE_01)
      .setName("Clang")
      .setDbKey("clang"));
  }

  private ComponentDto insertJdk7(OrganizationDto organizationDto) {
    return db.components().insertComponent(newPrivateProjectDto(organizationDto, Uuids.UUID_EXAMPLE_02)
      .setName("JDK 7")
      .setDbKey("net.java.openjdk:jdk7")
      .setDescription("JDK"));
  }

  private ComponentDto insertView(OrganizationDto organizationDto) {
    return db.components().insertComponent(newView(organizationDto, "752d8bfd-420c-4a83-a4e5-8ab19b13c8fc")
      .setName("Java")
      .setDbKey("Java"));
  }

  private SearchMyProjectsWsResponse callWs() {
    return ws.newRequest()
      .executeProtobuf(SearchMyProjectsWsResponse.class);
  }

}
