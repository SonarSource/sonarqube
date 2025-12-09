/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.base.Joiner;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Projects.SearchWsResponse;
import org.sonarqube.ws.Projects.SearchWsResponse.Component;
import org.sonarqube.ws.client.component.ComponentsWsParameters;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ANALYZED_BEFORE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ON_PROVISIONED_ONLY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class SearchActionIT {

  private static final String PROJECT_KEY_1 = "project1";
  private static final String PROJECT_KEY_2 = "project2";
  private static final String PROJECT_KEY_3 = "project3";

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public final DbTester db = DbTester.create();

  private final ManagedProjectService managedProjectService = mock(ManagedProjectService.class);

  private final WsActionTester ws = new WsActionTester(new SearchAction(db.getDbClient(), userSession, managedProjectService));

  @Test
  public void handle_whenSearchingByKeyQueryWithPartialMatchCaseInsensitive() {
    userSession.addPermission(ADMINISTER);
    db.components().insertPrivateProject(p -> p.setKey("project-_%-key")).getMainBranchComponent();
    db.components().insertPrivateProject(p -> p.setKey("PROJECT-_%-KEY")).getMainBranchComponent();
    db.components().insertPrivateProject(p -> p.setKey("project-key-without-escaped-characters")).getMainBranchComponent();

    SearchWsResponse response = call(SearchRequest.builder().setQuery("JeCt-_%-k").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("project-_%-key", "PROJECT-_%-KEY");
  }

  @Test
  public void handle_whenSearchingPrivateProjects() {
    userSession.addPermission(ADMINISTER);
    db.components().insertPrivateProject(p -> p.setKey("private-key")).getMainBranchComponent();
    db.components().insertPublicProject(p -> p.setKey("public-key")).getMainBranchComponent();

    SearchWsResponse response = call(SearchRequest.builder().setVisibility("private").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("private-key");
  }

  @Test
  public void handle_whenSearchingPublicProjects() {
    userSession.addPermission(ADMINISTER);
    db.components().insertPrivateProject(p -> p.setKey("private-key")).getMainBranchComponent();
    db.components().insertPublicProject(p -> p.setKey("public-key")).getMainBranchComponent();

    SearchWsResponse response = call(SearchRequest.builder().setVisibility("public").build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("public-key");
  }

  @Test
  public void handle_whenSearchingProjectsWithNoQualifierSet() {
    userSession.addPermission(ADMINISTER);
    db.components().insertPrivateProject(p -> p.setKey(PROJECT_KEY_1)).getMainBranchComponent();
    db.components().insertPublicPortfolio();

    SearchWsResponse response = call(SearchRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1);
  }

  @Test
  public void handle_whenSearchingProjects() {
    userSession.addPermission(ADMINISTER);
    ProjectData project = db.components().insertPrivateProject(p -> p.setKey(PROJECT_KEY_1));
    db.components().insertPrivateProject(p -> p.setKey(PROJECT_KEY_2));
    db.components().insertPublicPortfolio();

    ComponentDto directory = newDirectory(project.getMainBranchComponent(), "dir");
    ComponentDto file = newFileDto(directory);
    db.components().insertComponents(directory, file);

    SearchWsResponse response = call(SearchRequest.builder().setQualifiers(singletonList("TRK")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, PROJECT_KEY_2);
  }

  @Test
  public void handle_whenSearchingViews() {
    userSession.addPermission(ADMINISTER);
    db.components().insertPrivateProject(p -> p.setKey(PROJECT_KEY_1));
    db.components().insertPublicPortfolio(p -> p.setKey("view1"));

    SearchWsResponse response = call(SearchRequest.builder().setQualifiers(singletonList("VW")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("view1");
  }

  @Test
  public void handle_whenSearchingProjectsAndViews() {
    userSession.addPermission(ADMINISTER);
    db.components().insertPrivateProject(p -> p.setKey(PROJECT_KEY_1));
    db.components().insertPublicPortfolio(p -> p.setKey("view1"));

    SearchWsResponse response = call(SearchRequest.builder().setQualifiers(asList("TRK", "VW")).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, "view1");
  }

  @Test
  public void handle_whenSearchingAll() {
    userSession.addPermission(ADMINISTER);
    db.components().insertPrivateProject(p -> p.setKey(PROJECT_KEY_1));
    db.components().insertPrivateProject(p -> p.setKey(PROJECT_KEY_2));
    db.components().insertPrivateProject(p -> p.setKey(PROJECT_KEY_3));
    SearchWsResponse response = call(SearchRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(PROJECT_KEY_1, PROJECT_KEY_2, PROJECT_KEY_3);
  }

  @Test
  public void handle_whenSearchingOldProjects() {
    userSession.addPermission(ADMINISTER);
    long aLongTimeAgo = 1_000_000_000L;
    long inBetween = 2_000_000_000L;
    long recentTime = 3_000_000_000L;

    ProjectData oldProject = db.components().insertPublicProject();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(oldProject.getMainBranchDto()).setCreatedAt(aLongTimeAgo));
    BranchDto branch = db.components().insertProjectBranch(oldProject.getProjectDto());
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(branch).setCreatedAt(inBetween));

    ProjectData recentProject = db.components().insertPublicProject();
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(recentProject.getMainBranchDto()).setCreatedAt(recentTime));
    db.commit();

    SearchWsResponse result = call(SearchRequest.builder().setAnalyzedBefore(toStringAtUTC(new Date(recentTime + 1_000))).build());
    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::getLastAnalysisDate)
      .containsExactlyInAnyOrder(tuple(oldProject.getProjectDto().getKey(), formatDateTime(inBetween)), tuple(recentProject.projectKey(), formatDateTime(recentTime)));

    result = call(SearchRequest.builder().setAnalyzedBefore(toStringAtUTC(new Date(recentTime))).build());
    assertThat(result.getComponentsList()).extracting(Component::getKey, Component::getLastAnalysisDate)
      .containsExactlyInAnyOrder(tuple(oldProject.getProjectDto().getKey(), formatDateTime(inBetween)));

    result = call(SearchRequest.builder().setAnalyzedBefore(toStringAtUTC(new Date(aLongTimeAgo + 1_000L))).build());
    assertThat(result.getComponentsList()).isEmpty();
  }

  @Test
  public void handle_whenMainBranchAnalyzed_shouldReturnLatestAnalysisDateForAllBranchesAndRevision() {
    userSession.addPermission(ADMINISTER);
    ProjectData project = db.components().insertPublicProject();
    SnapshotDto snapshotProject = db.components().insertSnapshot(project, s -> s.setLast(true).setCreatedAt(1_000_000L));
    BranchDto branch = db.components().insertProjectBranch(project.getProjectDto());

    // Make sure branch analysis is later
    SnapshotDto snapshotBranch = db.components().insertSnapshot(branch, s -> s.setLast(true).setCreatedAt(2_000_000L));

    SearchWsResponse response = call(SearchRequest.builder().build());
    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::getLastAnalysisDate, Component::getRevision)
      .containsExactlyInAnyOrder(
        tuple(project.projectKey(), formatDateTime(snapshotBranch.getCreatedAt()), snapshotProject.getRevision()));
  }

  @Test
  public void handle_whenMainBranchUnanalyzed_shouldStillReturnLatestBranchAnalysisDateAndEmptyRevision() {
    userSession.addPermission(ADMINISTER);
    ProjectData project = db.components().insertPublicProject();
    BranchDto branch = db.components().insertProjectBranch(project.getProjectDto());
    SnapshotDto snapshot = db.components().insertSnapshot(branch, s -> s.setLast(true));

    SearchWsResponse response = call(SearchRequest.builder().build());
    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::getLastAnalysisDate, Component::getRevision)
      .containsOnly(tuple(project.projectKey(), formatDateTime(snapshot.getAnalysisDate()), ""));
  }

  @Test
  public void handle_whenAllBranchesUnanalyzed_shouldNotReturnAnalysisDate() {
    userSession.addPermission(ADMINISTER);
    ProjectData oldProject = db.components().insertPublicProject();
    db.components().insertProjectBranch(oldProject.getProjectDto());

    SearchWsResponse response = call(SearchRequest.builder().build());
    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::getLastAnalysisDate, Component::getRevision)
      .containsOnly(tuple(oldProject.projectKey(), "", ""));
  }

  @Test
  public void handle_whenSearching_shouldReturnManagedStatus() {
    userSession.addPermission(ADMINISTER);
    ProjectData managedProject = db.components().insertPrivateProject();
    ProjectData notManagedProject = db.components().insertPrivateProject();

    when(managedProjectService.getProjectUuidToManaged(any(), eq(Set.of(managedProject.projectUuid(), notManagedProject.projectUuid()))))
      .thenReturn(Map.of(
        managedProject.projectUuid(), true,
        notManagedProject.projectUuid(), false));

    SearchWsResponse result = call(SearchRequest.builder().build());
    assertThat(result.getComponentsList())
      .extracting(Component::getKey, Component::getManaged)
      .containsExactlyInAnyOrder(
        tuple(managedProject.projectKey(), true),
        tuple(notManagedProject.projectKey(), false));
  }

  private static String toStringAtUTC(Date d) {
    OffsetDateTime offsetTime = d.toInstant().atOffset(ZoneOffset.UTC);
    return DateUtils.formatDateTime(offsetTime);
  }

  @Test
  public void handle_whenSearchingByKey_shouldNotReturnBranches() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.components().insertProjectBranch(project);
    userSession.addPermission(ADMINISTER);

    SearchWsResponse response = call(SearchRequest.builder().build());

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(project.getKey());
  }

  @Test
  public void handle_whenSearching_shouldBePaginated() {
    userSession.addPermission(ADMINISTER);
    for (int i = 1; i <= 9; i++) {
      int j = i;
      db.components().insertPrivateProject("project-uuid-" + i, p -> p.setKey("project-key-" + j).setName("Project Name " + j));
    }

    SearchWsResponse response = call(SearchRequest.builder().setPage(2).setPageSize(3).build());
    assertThat(response.getComponentsList()).extracting(Component::getKey).containsExactly("project-key-4", "project-key-5", "project-key-6");
  }

  @Test
  public void handle_whenSearchingProvisionedProjects() {
    userSession.addPermission(ADMINISTER);
    ProjectDto provisionedProject = db.components().insertPrivateProject().getProjectDto();
    ProjectData analyzedProject = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(analyzedProject.getMainBranchDto()));

    SearchWsResponse response = call(SearchRequest.builder().setOnProvisionedOnly(true).build());

    assertThat(response.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(provisionedProject.getKey())
      .doesNotContain(analyzedProject.projectKey());
  }

  @Test
  public void handle_whenSearchingByComponentKeys() {
    userSession.addPermission(ADMINISTER);
    ProjectDto jdk = db.components().insertPrivateProject().getProjectDto();
    ProjectDto sonarqube = db.components().insertPrivateProject().getProjectDto();
    ProjectDto sonarlint = db.components().insertPrivateProject().getProjectDto();

    SearchWsResponse result = call(SearchRequest.builder()
      .setProjects(Arrays.asList(jdk.getKey(), sonarqube.getKey()))
      .build());

    assertThat(result.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(jdk.getKey(), sonarqube.getKey())
      .doesNotContain(sonarlint.getKey());
  }

  @Test
  public void handle_whenSearchingMoreThanThousandProjects_shouldFail() {
    SearchRequest request = SearchRequest.builder()
      .setProjects(Collections.nCopies(1_001, "foo"))
      .build();
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'projects' can contain only 1000 values, got 1001");
  }

  @Test
  public void handle_whenSearchingWithoutAdminPermission_shouldFail() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES);

    SearchRequest request = SearchRequest.builder().build();
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void handle_whenSearchingWithInvalidQualifier() {
    userSession.addPermission(ADMINISTER_QUALITY_PROFILES);

    SearchRequest request = SearchRequest.builder().setQualifiers(singletonList("BRC")).build();
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'qualifiers' (BRC) must be one of: [TRK, VW, APP]");
  }

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("search");
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isEqualTo("""
      Search for projects or views to administrate them.
      <ul>
        <li>The response field 'lastAnalysisDate' takes into account the analysis of all branches and pull requests, not only the main branch.</li>
        <li>The response field 'revision' takes into account the analysis of the main branch only.</li>
      </ul>
      Requires 'Administer System' permission""");
    assertThat(action.isInternal()).isFalse();
    assertThat(action.since()).isEqualTo("6.3");
    assertThat(action.handler()).isEqualTo(ws.getDef().handler());
    assertThat(action.responseExample()).isEqualTo(getClass().getResource("search-example.json"));

    var definition = ws.getDef();
    assertThat(definition.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired, WebService.Param::description, WebService.Param::possibleValues, WebService.Param::defaultValue,
        WebService.Param::since)
      .containsExactlyInAnyOrder(
        tuple("q", false, "Limit search to: <ul><li>component names that contain the supplied string</li><li>component keys that contain the supplied string</li></ul>", null, null,
          null),
        tuple("qualifiers", false, "Comma-separated list of component qualifiers. Filter the results with the specified qualifiers", Set.of("TRK", "VW", "APP"), "TRK", null),
        tuple("p", false, "1-based page number", null, "1", null),
        tuple("projects", false, "Comma-separated list of project keys", null, null, "6.6"),
        tuple("ps", false, "Page size. Must be greater than 0 and less or equal than 500", null, "100", null),
        tuple("visibility", false,
          "Filter the projects that should be visible to everyone (public), or only specific user/groups (private).<br/>If no visibility is specified, the default project visibility will be used.",
          Set.of("private", "public"), null, "6.4"),
        tuple("analyzedBefore", false,
          "Filter the projects for which the last analysis of all branches are older than the given date (exclusive).<br> Either a date (server timezone) or datetime can be provided.",
          null, null, "6.6"),
        tuple("onProvisionedOnly", false, "Filter the projects that are provisioned", Set.of("true", "false", "yes", "no"), "false", "6.6"));
  }

  @Test
  public void json_example() {
    userSession.addPermission(ADMINISTER);
    ProjectData publicProject = db.components().insertPublicProject("project-uuid-1", p -> p.setName("Project Name 1").setKey("project-key-1").setPrivate(false));
    ProjectData privateProject = db.components().insertPrivateProject("project-uuid-2", p -> p.setName("Project Name 2").setKey("project-key-2"));
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(publicProject.getMainBranchDto())
      .setCreatedAt(parseDateTime("2017-03-01T11:39:03+0300").getTime())
      .setRevision("cfb82f55c6ef32e61828c4cb3db2da12795fd767"));
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(privateProject.getMainBranchDto())
      .setCreatedAt(parseDateTime("2017-03-02T15:21:47+0300").getTime())
      .setRevision("7be96a94ac0c95a61ee6ee0ef9c6f808d386a355"));
    db.commit();

    String response = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .execute().getInput();

    assertJson(response).isSimilarTo(ws.getDef().responseExampleAsString());
    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(response);
  }

  private SearchWsResponse call(SearchRequest wsRequest) {
    TestRequest request = ws.newRequest();
    List<String> qualifiers = wsRequest.getQualifiers();
    if (!qualifiers.isEmpty()) {
      request.setParam(ComponentsWsParameters.PARAM_QUALIFIERS, Joiner.on(",").join(qualifiers));
    }
    ofNullable(wsRequest.getQuery()).ifPresent(query -> request.setParam(TEXT_QUERY, query));
    ofNullable(wsRequest.getPage()).ifPresent(page -> request.setParam(PAGE, String.valueOf(page)));
    ofNullable(wsRequest.getPageSize()).ifPresent(pageSize -> request.setParam(PAGE_SIZE, String.valueOf(pageSize)));
    ofNullable(wsRequest.getVisibility()).ifPresent(v -> request.setParam(PARAM_VISIBILITY, v));
    ofNullable(wsRequest.getAnalyzedBefore()).ifPresent(d -> request.setParam(PARAM_ANALYZED_BEFORE, d));
    ofNullable(wsRequest.getProjects()).ifPresent(l1 -> request.setParam(PARAM_PROJECTS, String.join(",", l1)));
    request.setParam(PARAM_ON_PROVISIONED_ONLY, String.valueOf(wsRequest.isOnProvisionedOnly()));
    return request.executeProtobuf(SearchWsResponse.class);
  }

}
