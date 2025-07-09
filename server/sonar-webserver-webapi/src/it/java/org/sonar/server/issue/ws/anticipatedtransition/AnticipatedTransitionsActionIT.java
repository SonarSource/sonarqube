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
package org.sonar.server.issue.ws.anticipatedtransition;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.issue.AnticipatedTransitionDao;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentTypeTree;
import org.sonar.server.component.ComponentTypes;
import org.sonar.server.component.DefaultComponentTypes;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.component.ProjectTesting.newPrivateProjectDto;
import static org.sonar.db.permission.ProjectPermission.CODEVIEWER;
import static org.sonar.db.permission.ProjectPermission.ISSUE_ADMIN;

public class AnticipatedTransitionsActionIT {

  private static final String PROJECT_UUID = "123";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final ComponentFinder componentFinder = new ComponentFinder(db.getDbClient(), new ComponentTypes(new ComponentTypeTree[] {DefaultComponentTypes.get()}));
  private final AnticipatedTransitionsActionValidator validator = new AnticipatedTransitionsActionValidator(db.getDbClient(), componentFinder, userSession);
  private final UuidFactory uuidFactory = new SequenceUuidFactory();
  private final AnticipatedTransitionDao anticipatedTransitionDao = db.getDbClient().anticipatedTransitionDao();
  private final AnticipatedTransitionParser anticipatedTransitionParser = new AnticipatedTransitionParser();
  private final AnticipatedTransitionHandler handler = new AnticipatedTransitionHandler(anticipatedTransitionParser, anticipatedTransitionDao, uuidFactory, db.getDbClient());
  private final WsActionTester ws = new WsActionTester(new AnticipatedTransitionsAction(validator, handler));

  @Test
  public void definition() {
    var definition = ws.getDef();
    assertThat(definition.key()).isEqualTo("anticipated_transitions");
    assertThat(definition.description()).isEqualTo("""
      Receive a list of anticipated transitions that can be applied to not yet discovered issues on a specific project.<br>
      Requires the following permission: 'Administer Issues' on the specified project.<br>
      Only <code>falsepositive</code>, <code>wontfix</code> and <code>accept</code> transitions are supported.<br>
      Upon successful execution, the HTTP status code returned is 202 (Accepted).<br><br>
      Request example:
      <pre><code>[
        {
          "ruleKey": "squid:S0001",
          "issueMessage": "issueMessage1",
          "filePath": "filePath1",
          "line": 1,
          "lineHash": "lineHash1",
          "transition": "falsepositive",
          "comment": "comment1"
        },
        {
          "ruleKey": "squid:S0002",
          "issueMessage": "issueMessage2",
          "filePath": "filePath2",
          "line": 2,
          "lineHash": "lineHash2",
          "transition": "wontfix",
          "comment": "comment2"
        }
      ]</code></pre>""");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key, WebService.Param::isRequired, WebService.Param::description, WebService.Param::since)
      .containsExactlyInAnyOrder(
        tuple("projectKey", true, "The key of the project", "10.2"));
  }

  @Test
  public void givenRequestWithTransitions_whenHandle_thenAllTransitionsAreSaved() throws IOException, URISyntaxException {
    // given
    ProjectDto projectDto = mockProjectDto();
    mockUser(projectDto, ISSUE_ADMIN);
    String requestBody = readTestResourceFile("request-with-transitions.json");

    // when
    TestResponse response = getTestRequest(projectDto, requestBody).execute();

    // then
    assertThat(anticipatedTransitionDao.selectByProjectUuid(db.getSession(), projectDto.getUuid())).hasSize(2);
    assertThat(response.getStatus()).isEqualTo(202);
  }

  @Test
  public void givenRequestWithNoTransitions_whenHandle_thenNoTransitionsAreSaved() {
    // given
    ProjectDto projectDto = mockProjectDto();
    mockUser(projectDto, ISSUE_ADMIN);
    String requestBody = "[]";

    // when
    TestResponse response = getTestRequest(projectDto, requestBody).execute();

    // then
    assertThat(anticipatedTransitionDao.selectByProjectUuid(db.getSession(), projectDto.getUuid())).isEmpty();
    assertThat(response.getStatus()).isEqualTo(202);
  }

  @Test
  public void givenRequestWithInvalidBody_whenHandle_thenExceptionIsThrown() {
    // given
    ProjectDto projectDto = mockProjectDto();
    mockUser(projectDto, ISSUE_ADMIN);
    String requestBody = "invalidJson";

    // when then
    TestRequest request = getTestRequest(projectDto, requestBody);

    assertThatThrownBy(request::execute)
      .hasMessage("Unable to parse anticipated transitions from request body.")
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void givenTransitionsForUserAndProjectAlreadyExistInDb_whenHandle_thenTheNewTransitionsShouldReplaceTheOldOnes() throws IOException, URISyntaxException {
    // given
    ProjectDto projectDto = mockProjectDto();
    mockUser(projectDto, ISSUE_ADMIN);
    String requestBody = readTestResourceFile("request-with-transitions.json");
    TestResponse response1 = getTestRequest(projectDto, requestBody).execute();
    assertThat(anticipatedTransitionDao.selectByProjectUuid(db.getSession(), projectDto.getUuid())).hasSize(2);
    assertThat(response1.getStatus()).isEqualTo(202);

    // when
    String requestBody2 = """
      [
        {
          "ruleKey": "squid:S0003",
          "issueMessage": "issueMessage3",
          "filePath": "filePath3",
          "line": 3,
          "lineHash": "lineHash3",
          "transition": "wontfix",
          "comment": "comment3"
        }
      ]""";
    TestResponse response2 = getTestRequest(projectDto, requestBody2).execute();

    // then
    assertThat(anticipatedTransitionDao.selectByProjectUuid(db.getSession(), projectDto.getUuid())).hasSize(1);
    assertThat(response2.getStatus()).isEqualTo(202);
  }

  @Test
  public void givenRequestWithNoTransitions_whenHandle_thenExistingTransitionsForUserAndProjectShouldBePurged() throws IOException, URISyntaxException {
    // given
    ProjectDto projectDto = mockProjectDto();
    mockUser(projectDto, ISSUE_ADMIN);
    String requestBody = readTestResourceFile("request-with-transitions.json");
    TestResponse response1 = getTestRequest(projectDto, requestBody).execute();
    assertThat(anticipatedTransitionDao.selectByProjectUuid(db.getSession(), projectDto.getUuid())).hasSize(2);
    assertThat(response1.getStatus()).isEqualTo(202);

    // when
    String requestBody2 = "[]";
    TestResponse response2 = getTestRequest(projectDto, requestBody2).execute();

    // then
    assertThat(anticipatedTransitionDao.selectByProjectUuid(db.getSession(), projectDto.getUuid())).isEmpty();
    assertThat(response2.getStatus()).isEqualTo(202);
  }

  @Test
  public void givenUserWithoutAdminIssuesPermission_whenHandle_thenThrowException() throws IOException, URISyntaxException {
    // given
    ProjectDto projectDto = mockProjectDto();
    mockUser(projectDto, CODEVIEWER);
    String requestBody = readTestResourceFile("request-with-transitions.json");

    // when
    TestRequest request = getTestRequest(projectDto, requestBody);

    // then
    assertThatThrownBy(request::execute)
      .hasMessage("Insufficient privileges")
      .isInstanceOf(ForbiddenException.class);
  }

  private TestRequest getTestRequest(ProjectDto projectDto, String requestBody) {
    return ws.newRequest()
      .setParam("projectKey", projectDto.getKey())
      .setMethod("POST")
      .setMediaType("application/json")
      .setPayload(requestBody);
  }

  private void mockUser(ProjectDto projectDto, ProjectPermission permission) {
    UserDto user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, permission, projectDto);
    userSession.logIn(user);
  }

  private ProjectDto mockProjectDto() {
    ProjectDto projectDto = newPrivateProjectDto(PROJECT_UUID);
    db.getDbClient().projectDao().insert(db.getSession(), projectDto);
    db.commit();
    return projectDto;
  }

  private String readTestResourceFile(String fileName) throws IOException, URISyntaxException {
    return Files.readString(Path.of(getClass().getResource(fileName).toURI()));
  }

}
