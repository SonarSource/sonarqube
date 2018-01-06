/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.projecttag.ws;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;

public class SetActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setRoot();
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ComponentDto project;

  private TestProjectIndexers projectIndexers = new TestProjectIndexers();

  private WsActionTester ws = new WsActionTester(new SetAction(dbClient, TestComponentFinder.from(db), userSession, projectIndexers));

  @Before
  public void setUp() {
    project = db.components().insertPrivateProject();
  }

  @Test
  public void set_tags_exclude_empty_and_blank_values() {
    TestResponse response = call(project.getDbKey(), "finance , offshore, platform,   ,");

    assertTags(project.getDbKey(), "finance", "offshore", "platform");
    // FIXME verify(indexer).indexProject(project.uuid(), PROJECT_TAGS_UPDATE);

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void reset_tags() {
    project = db.components().insertPrivateProject(p -> p.setTagsString("platform,scanner"));

    call(project.getDbKey(), "");

    assertNoTags(project.getDbKey());
  }

  @Test
  public void override_existing_tags() {
    project = db.components().insertPrivateProject(p -> p.setTagsString("marketing,languages"));

    call(project.getDbKey(), "finance,offshore,platform");

    assertTags(project.getDbKey(), "finance", "offshore", "platform");
  }

  @Test
  public void set_tags_as_project_admin() {
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    call(project.getDbKey(), "platform, lambda");

    assertTags(project.getDbKey(), "platform", "lambda");
  }

  @Test
  public void do_not_duplicate_tags() {
    call(project.getDbKey(), "atlas, atlas, atlas");

    assertTags(project.getDbKey(), "atlas");
  }

  @Test
  public void fail_if_tag_does_not_respect_format() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("_finance_' is invalid. Project tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'");

    call(project.getDbKey(), "_finance_");
  }

  @Test
  public void fail_if_not_project_admin() {
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    expectedException.expect(ForbiddenException.class);

    call(project.getDbKey(), "platform");
  }

  @Test
  public void fail_if_no_project() {
    expectedException.expect(IllegalArgumentException.class);

    call(null, "platform");
  }

  @Test
  public void fail_if_no_tags() {
    expectedException.expect(IllegalArgumentException.class);

    call(project.getDbKey(), null);
  }

  @Test
  public void fail_if_component_is_a_view() {
    ComponentDto view = db.components().insertView(v -> v.setDbKey("VIEW_KEY"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component 'VIEW_KEY' is not a project");

    call(view.getDbKey(), "point-of-view");
  }

  @Test
  public void fail_if_component_is_a_module() {
    ComponentDto module = db.components().insertComponent(newModuleDto(project).setDbKey("MODULE_KEY"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component 'MODULE_KEY' is not a project");

    call(module.getDbKey(), "modz");
  }

  @Test
  public void fail_if_component_is_a_file() {
    ComponentDto file = db.components().insertComponent(newFileDto(project).setDbKey("FILE_KEY"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component 'FILE_KEY' is not a project");

    call(file.getDbKey(), "secret");
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertMainBranch(organization);
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    call(branch.getDbKey(), "secret");
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.params()).extracting(WebService.Param::key)
      .containsOnly("project", "tags");
    assertThat(definition.description()).isNotEmpty();
    assertThat(definition.since()).isEqualTo("6.4");
  }

  private TestResponse call(@Nullable String projectKey, @Nullable String tags) {
    TestRequest request = ws.newRequest();
    setNullable(projectKey, p -> request.setParam("project", p));
    setNullable(tags, t -> request.setParam("tags", tags));

    return request.execute();
  }

  private void assertTags(String projectKey, String... tags) {
    assertThat(dbClient.componentDao().selectOrFailByKey(dbSession, projectKey).getTags()).containsExactlyInAnyOrder(tags);
  }

  private void assertNoTags(String projectKey) {
    assertThat(dbClient.componentDao().selectOrFailByKey(dbSession, projectKey).getTags()).isEmpty();
  }
}
