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
package org.sonar.server.project.ws;

import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.Metric.Level;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Projects.SearchMyProjectsWsResponse;
import org.sonarqube.ws.Projects.SearchMyProjectsWsResponse.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newMeasure;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchMyProjectsActionIT {

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
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
    ProjectData jdk7 = insertJdk7();
    ProjectData cLang = insertClang();
    db.projectLinks().insertProvidedLink(jdk7.getProjectDto(), l -> l.setHref("http://www.oracle.com").setType(ProjectLinkDto.TYPE_HOME_PAGE).setName("Home"));
    db.projectLinks().insertProvidedLink(jdk7.getProjectDto(), l -> l.setHref("http://download.java.net/openjdk/jdk8/").setType(ProjectLinkDto.TYPE_SOURCES).setName("Sources"));
    long oneTime = DateUtils.parseDateTime("2016-06-10T13:17:53+0000").getTime();
    long anotherTime = DateUtils.parseDateTime("2016-06-11T14:25:53+0000").getTime();
    SnapshotDto jdk7Snapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(jdk7.getMainBranchDto()).setCreatedAt(oneTime));
    SnapshotDto cLangSnapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(cLang.getMainBranchDto()).setCreatedAt(anotherTime));
    dbClient.measureDao().insert(dbSession, newMeasure(jdk7.getMainBranchDto(), alertStatusMetric, Level.ERROR.name()));
    dbClient.measureDao().insert(dbSession, newMeasure(cLang.getMainBranchDto(), alertStatusMetric, Level.OK.name()));
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, jdk7.getProjectDto());
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, cLang.getProjectDto());
    db.commit();
    System.setProperty("user.timezone", "UTC");

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("search_my_projects-example.json"));
  }

  @Test
  public void return_only_current_user_projects() {
    ProjectData jdk7 = insertJdk7();
    ProjectData cLang = insertClang();
    UserDto anotherUser = db.users().insertUser(newUserDto());
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, jdk7.getProjectDto());
    db.users().insertProjectPermissionOnUser(anotherUser, ProjectPermission.ADMIN, cLang.getProjectDto());

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isOne();
  }

  @Test
  public void return_only_first_1000_projects() {
    IntStream.range(0, 1_010).forEach(i -> {
      ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
      db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, project);
    });

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getPaging().getTotal()).isEqualTo(1_000);
  }

  @Test
  public void sort_projects_by_name() {
    ComponentDto b_project = db.components().insertPrivateProject(p -> p.setName("B_project_name")).getMainBranchComponent();
    ComponentDto c_project = db.components().insertPrivateProject(p -> p.setName("c_project_name")).getMainBranchComponent();
    ComponentDto a_project = db.components().insertPrivateProject(p -> p.setName("A_project_name")).getMainBranchComponent();

    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, b_project);
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, a_project);
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, c_project);

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isEqualTo(3);
  }

  @Test
  public void paginate_projects() {
    for (int i = 0; i < 10; i++) {
      int j = i;
      ProjectData project = db.components().insertPrivateProject(p -> p.setName("project-" + j));
      db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, project.getProjectDto());
    }

    SearchMyProjectsWsResponse result = ws.newRequest()
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .executeProtobuf(SearchMyProjectsWsResponse.class);

    assertThat(result.getProjectsList()).extracting(Project::getName).containsExactly("project-3", "project-4", "project-5");
    assertThat(result.getProjectsCount()).isEqualTo(3);
  }

  @Test
  public void return_only_projects_when_user_is_admin() {
    ProjectData jdk7 = insertJdk7();
    ProjectData clang = insertClang();

    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, jdk7.getProjectDto());
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ISSUE_ADMIN, clang.getProjectDto());

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isOne();
  }

  @Test
  public void does_not_return_views() {
    ProjectData jdk7 = insertJdk7();
    PortfolioDto portfolio = insertPortfolio();

    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, jdk7.getProjectDto());
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, portfolio);

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isOne();
  }

  @Test
  public void does_not_return_branches() {
    ProjectData project = db.components().insertPublicProject();
    BranchDto branch = db.components().insertProjectBranch(project.getProjectDto());
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, project.getProjectDto());

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsList())
      .extracting(Project::getKey)
      .containsExactlyInAnyOrder(project.getProjectDto().getKey());
  }

  @Test
  public void admin_via_groups() {
    ProjectData jdk7 = insertJdk7();
    ProjectData cLang = insertClang();

    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);

    db.users().insertEntityPermissionOnGroup(group, ProjectPermission.ADMIN, jdk7.getProjectDto());
    db.users().insertEntityPermissionOnGroup(group, ProjectPermission.USER, cLang.getProjectDto());

    SearchMyProjectsWsResponse result = callWs();

    assertThat(result.getProjectsCount()).isOne();
  }

  @Test
  public void admin_via_groups_and_users() {
    ProjectData jdk7 = insertJdk7();
    ProjectData cLang = insertClang();
    ProjectData sonarqube = db.components().insertPrivateProject();

    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);

    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, jdk7.getProjectDto());
    db.users().insertEntityPermissionOnGroup(group, ProjectPermission.ADMIN, cLang.getProjectDto());
    // admin via group and user
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, sonarqube.getProjectDto());
    db.users().insertEntityPermissionOnGroup(group, ProjectPermission.ADMIN, sonarqube.getProjectDto());

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

    assertThatThrownBy(this::callWs)
      .isInstanceOf(UnauthorizedException.class);
  }

  private ProjectData insertClang() {
    return db.components().insertPrivateProject(Uuids.UUID_EXAMPLE_01, p -> p
      .setName("Clang")
      .setKey("clang"));
  }

  private ProjectData insertJdk7() {
    return db.components().insertPrivateProject(Uuids.UUID_EXAMPLE_02, p -> p
      .setName("JDK 7")
      .setKey("net.java.openjdk:jdk7")
      .setDescription("JDK"));
  }

  private PortfolioDto insertPortfolio() {
    String uuid = "752d8bfd-420c-4a83-a4e5-8ab19b13c8fc";
    return db.components().insertPublicPortfolioDto(p -> p.setUuid("752d8bfd-420c-4a83-a4e5-8ab19b13c8fc")
        .setName("Java")
        .setKey("Java"),
      p -> p.setRootUuid(uuid));
  }

  private SearchMyProjectsWsResponse callWs() {
    return ws.newRequest()
      .executeProtobuf(SearchMyProjectsWsResponse.class);
  }

}
