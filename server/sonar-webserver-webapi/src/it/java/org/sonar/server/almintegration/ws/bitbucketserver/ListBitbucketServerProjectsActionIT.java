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
package org.sonar.server.almintegration.ws.bitbucketserver;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Project;
import org.sonar.alm.client.bitbucketserver.ProjectList;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmIntegrations;
import org.sonarqube.ws.AlmIntegrations.ListBitbucketserverProjectsWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class ListBitbucketServerProjectsActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final BitbucketServerRestClient bitbucketServerRestClient = mock(BitbucketServerRestClient.class);
  private final WsActionTester ws = new WsActionTester(new ListBitbucketServerProjectsAction(db.getDbClient(), userSession, bitbucketServerRestClient));

  private static final String PARAM_ALM_SETTING = "almSetting";
  private static final String PARAM_START = "start";
  private static final int DEFAULT_START = 1; // First item has id = 1
  private static final String PARAM_PAGE_SIZE = "pageSize";
  private static final int DEFAULT_PAGE_SIZE = 25;
  private static final int MAX_PAGE_SIZE = 100;

  private int totalMockedProjects = 100;

  @Before
  public void before() {
    when(bitbucketServerRestClient.getProjects(anyString(), anyString(), any(), anyInt()))
      .thenAnswer(invocation -> {
        Integer start = invocation.getArgument(2); // Nullable
        int pageSize = invocation.getArgument(3);

        start = (start == null) ? DEFAULT_START : start;
        return createTestProjectList(totalMockedProjects, start, pageSize);
      });
  }

  @Test
  public void handle_should_list_projects_when_default_pagination_parameters() {
    UserDto user = createUserWithPermissions();
    AlmSettingDto almSetting = createAlmSettingWithPat(user);

    ListBitbucketserverProjectsWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, almSetting.getKey())
      .executeProtobuf(ListBitbucketserverProjectsWsResponse.class);

    validateResponse(response, DEFAULT_START, DEFAULT_PAGE_SIZE, false, DEFAULT_PAGE_SIZE + DEFAULT_START);
  }

  @Test
  public void handle_should_list_projects_when_custom_start() {
    totalMockedProjects = 55;
    UserDto user = createUserWithPermissions();
    AlmSettingDto almSetting = createAlmSettingWithPat(user);

    ListBitbucketserverProjectsWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, almSetting.getKey())
      .setParam(PARAM_START, "51")
      .executeProtobuf(ListBitbucketserverProjectsWsResponse.class);

    validateResponse(response, 51, 5, true, 51);
  }

  @Test
  public void handle_should_list_projects_when_custom_page_size() {
    UserDto user = createUserWithPermissions();
    AlmSettingDto almSetting = createAlmSettingWithPat(user);

    ListBitbucketserverProjectsWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, almSetting.getKey())
      .setParam(PARAM_PAGE_SIZE, "10")
      .executeProtobuf(ListBitbucketserverProjectsWsResponse.class);

    validateResponse(response, DEFAULT_START, 10, false, 10 + DEFAULT_START);
  }

  @Test
  public void handle_should_list_projects_when_zero_page_size() {
    UserDto user = createUserWithPermissions();
    AlmSettingDto almSetting = createAlmSettingWithPat(user);

    ListBitbucketserverProjectsWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, almSetting.getKey())
      .setParam(PARAM_PAGE_SIZE, "0")
      .executeProtobuf(ListBitbucketserverProjectsWsResponse.class);

    validateResponse(response, DEFAULT_START, DEFAULT_PAGE_SIZE, false, DEFAULT_PAGE_SIZE + DEFAULT_START);
  }

  @Test
  public void handle_should_list_projects_when_negative_page_size() {
    UserDto user = createUserWithPermissions();
    AlmSettingDto almSetting = createAlmSettingWithPat(user);

    ListBitbucketserverProjectsWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, almSetting.getKey())
      .setParam(PARAM_PAGE_SIZE, "-1")
      .executeProtobuf(ListBitbucketserverProjectsWsResponse.class);

    validateResponse(response, DEFAULT_START, DEFAULT_PAGE_SIZE, false, DEFAULT_PAGE_SIZE + DEFAULT_START);
  }

  @Test
  public void handle_should_return_empty_list_when_out_of_bounds_start() {
    totalMockedProjects = 30;
    UserDto user = createUserWithPermissions();
    AlmSettingDto almSetting = createAlmSettingWithPat(user);

    ListBitbucketserverProjectsWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, almSetting.getKey())
      .setParam(PARAM_START, "50") // Out-of-bounds start
      .setParam(PARAM_PAGE_SIZE, "10")
      .executeProtobuf(ListBitbucketserverProjectsWsResponse.class);

    validateResponse(response, 50, 0, true, 50);
  }

  @Test
  public void handle_should_throw_illegal_argument_exception_when_invalid_start() {
    UserDto user = createUserWithPermissions();
    AlmSettingDto almSetting = createAlmSettingWithPat(user);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, almSetting.getKey())
      .setParam(PARAM_START, "notAnInt");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The '%s' parameter cannot be parsed as an integer value: %s".formatted(PARAM_START, "notAnInt"));
  }

  @Test
  public void handle_should_throw_illegal_argument_exception_when_out_of_bounds_page_size() {
    UserDto user = createUserWithPermissions();
    AlmSettingDto almSetting = createAlmSettingWithPat(user);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, almSetting.getKey())
      .setParam(PARAM_PAGE_SIZE, String.valueOf(MAX_PAGE_SIZE + 1));

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("'%s' value (%s) must be less than %s".formatted(PARAM_PAGE_SIZE, MAX_PAGE_SIZE + 1, MAX_PAGE_SIZE));
  }

  @Test
  public void handle_should_throw_illegal_argument_exception_when_invalid_page_size() {
    UserDto user = createUserWithPermissions();
    AlmSettingDto almSetting = createAlmSettingWithPat(user);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, almSetting.getKey())
      .setParam(PARAM_PAGE_SIZE, "notAnInt");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("'%s' value '%s' cannot be parsed as an integer".formatted(PARAM_PAGE_SIZE, "notAnInt"));
  }

  @Test
  public void handle_should_throw_illegal_argument_exception_when_pat_is_missing() {
    createUserWithPermissions();
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();

    TestRequest request = ws.newRequest().setParam(PARAM_ALM_SETTING, almSetting.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No personal access token found");
  }

  @Test
  public void handle_should_throw_not_found_exception_when_alm_setting_is_missing() {
    UserDto user = createUserWithPermissions();
    db.getDbClient().almPatDao().insert(db.getSession(), newAlmPatDto(), user.getLogin(), null);

    TestRequest request = ws.newRequest().setParam(PARAM_ALM_SETTING, "testKey");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform Setting '%s' not found".formatted("testKey"));
  }

  @Test
  public void handle_should_throw_unauthorized_exception_when_user_is_not_logged_in() {
    TestRequest request = ws.newRequest().setParam(PARAM_ALM_SETTING, "any");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void handle_should_throw_forbidden_exception_when_user_has_no_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = ws.newRequest().setParam(PARAM_ALM_SETTING, "any");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void definition_should_be_documented() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.2");
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleFormat()).isEqualTo("json");
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple(PARAM_ALM_SETTING, true),
        tuple(PARAM_START, false),
        tuple(PARAM_PAGE_SIZE, false));
  }

  // region utility methods

  private UserDto createUserWithPermissions() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    return user;
  }

  private AlmSettingDto createAlmSettingWithPat(UserDto user) {
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    return almSetting;
  }

  private ProjectList createTestProjectList(int nbProjects, int start, int pageSize) {
    int end = Math.min(start + pageSize - 1, nbProjects);

    List<Project> projects = IntStream.rangeClosed(start, end)
      .mapToObj(this::createTestProject)
      .toList();

    boolean isLastPage = end == nbProjects;
    int nextPageStart = isLastPage ? start : end + 1;

    return new ProjectList(isLastPage, nextPageStart, projects.size(), projects);
  }

  private Project createTestProject(int id) {
    return new Project("project-key-" + id, "project-name-" + id, id);
  }

  private void validateResponse(ListBitbucketserverProjectsWsResponse response, int start, int pageSize, boolean lastPage, int nextPageStart) {
    int expectedProjectCount = Math.max(0, Math.min(pageSize, totalMockedProjects - start + 1));

    assertThat(response.getPaging().getPageSize()).isEqualTo(expectedProjectCount);
    assertThat(response.getIsLastPage()).isEqualTo(lastPage);
    assertThat(response.getNextPageStart()).isEqualTo(nextPageStart);
    assertThat(response.getProjectsCount()).isEqualTo(expectedProjectCount);

    assertThat(response.getProjectsList())
      .extracting(AlmIntegrations.AlmProject::getKey, AlmIntegrations.AlmProject::getName)
      .containsExactlyElementsOf(IntStream.rangeClosed(start, start + expectedProjectCount - 1)
        .mapToObj(id -> tuple("project-key-" + id, "project-name-" + id))
        .toList());
  }

  // endregion utility methods

}
