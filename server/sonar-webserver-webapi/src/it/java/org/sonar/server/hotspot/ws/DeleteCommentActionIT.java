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
package org.sonar.server.hotspot.ws;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class DeleteCommentActionIT {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private System2 system2 = mock(System2.class);
  private HotspotWsSupport hotspotWsSupport = new HotspotWsSupport(dbClient, userSessionRule, system2);

  private DeleteCommentAction underTest = new DeleteCommentAction(dbClient, hotspotWsSupport, userSessionRule);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void verify_ws_def() {
    assertThat(actionTester.getDef().isInternal()).isTrue();
    assertThat(actionTester.getDef().isPost()).isTrue();

    Param commentKeyParam = actionTester.getDef().param("comment");
    assertThat(commentKeyParam).isNotNull();
    assertThat(commentKeyParam.isRequired()).isTrue();
  }

  @Test
  public void delete_comment_from_hotspot_private_project() {
    UserDto userDeletingOwnComment = dbTester.users().insertUser();

    ProjectData projectData = dbTester.components().insertPrivateProject();

    IssueDto hotspot = dbTester.issues().insertHotspot(h -> h.setProject(projectData.getMainBranchComponent()));
    IssueChangeDto comment = dbTester.issues().insertComment(hotspot, userDeletingOwnComment, "Some comment");

    userSessionRule.logIn(userDeletingOwnComment);
    userSessionRule.addProjectPermission(ProjectPermission.USER, projectData.getProjectDto());

    TestRequest request = newRequest(comment.getKey());

    TestResponse response = request.execute();
    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(getHotspotCommentByKey(comment.getKey()))
      .isEmpty();
  }

  @Test
  public void delete_comment_from_hotspot_public_project() {
    UserDto userAddingComment = dbTester.users().insertUser();

    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    IssueDto hotspot = dbTester.issues().insertHotspot(h -> h.setProject(project));
    IssueChangeDto comment = dbTester.issues().insertComment(hotspot, userAddingComment, "Some comment");

    assertThat(getHotspotCommentByKey(comment.getKey()))
      .isNotEmpty();

    userSessionRule.logIn(userAddingComment);
    userSessionRule.registerProjects(projectData.getProjectDto());

    TestRequest request = newRequest(comment.getKey());

    TestResponse response = request.execute();
    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(getHotspotCommentByKey(comment.getKey()))
      .isEmpty();
  }

  @Test
  public void fails_with_UnauthorizedException_if_user_is_anonymous() {
    userSessionRule.anonymous();

    TestRequest request = actionTester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void fails_if_comment_with_provided_key_does_not_exist() {
    userSessionRule.logIn();

    TestRequest request = newRequest("not-existing-comment-key");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Comment with key 'not-existing-comment-key' does not exist");
  }

  @Test
  public void fails_if_trying_to_delete_comment_of_another_user_in_private_project() {
    UserDto userTryingToDelete = dbTester.users().insertUser();
    UserDto userWithHotspotComment = dbTester.users().insertUser();

    ProjectData projectData = dbTester.components().insertPrivateProject();

    IssueDto hotspot = dbTester.issues().insertHotspot(h -> h.setProject(projectData.getMainBranchComponent()));
    IssueChangeDto comment = dbTester.issues().insertComment(hotspot, userWithHotspotComment, "Some comment");

    userSessionRule.logIn(userTryingToDelete);
    userSessionRule.addProjectPermission(ProjectPermission.USER, projectData.getProjectDto());

    TestRequest request = newRequest(comment.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("You can only delete your own comments");
  }

  @Test
  public void fails_if_trying_to_delete_comment_of_another_user_in_public_project() {
    UserDto userTryingToDelete = dbTester.users().insertUser();
    UserDto userWithHotspotComment = dbTester.users().insertUser();

    ProjectData projectData = dbTester.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    IssueDto hotspot = dbTester.issues().insertHotspot(h -> h.setProject(project));
    IssueChangeDto comment = dbTester.issues().insertComment(hotspot, userWithHotspotComment, "Some comment");
    userSessionRule.logIn(userTryingToDelete).registerProjects(projectData.getProjectDto());

    TestRequest request = newRequest(comment.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("You can only delete your own comments");
  }

  private TestRequest newRequest(String commentKey) {
    return actionTester.newRequest()
      .setParam("comment", commentKey);
  }

  private Optional<IssueChangeDto> getHotspotCommentByKey(String commentKey) {
    return dbClient.issueChangeDao().selectCommentByKey(dbTester.getSession(), commentKey);
  }
}
