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
package org.sonar.server.projecttag.ws;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.projecttag.TagsWsSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;

public class SetActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn();
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ProjectDto project;

  private TestProjectIndexers projectIndexers = new TestProjectIndexers();

  private TagsWsSupport tagsWsSupport = new TagsWsSupport(dbClient, TestComponentFinder.from(db), userSession, projectIndexers, System2.INSTANCE);

  private WsActionTester ws = new WsActionTester(new SetAction(dbClient, tagsWsSupport));

  @Before
  public void setUp() {
    project = db.components().insertPrivateProjectDto();
    userSession.addProjectPermission(ADMIN, project);
  }

  @Test
  public void set_tags_exclude_empty_and_blank_values() {
    TestResponse response = call(project.getKey(), "finance , offshore, platform,   ,");

    assertTags(project.getKey(), "finance", "offshore", "platform");
    // FIXME verify(indexer).indexProject(project.uuid(), PROJECT_TAGS_UPDATE);

    assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void reset_tags() {
    project = db.components().insertPrivateProjectDto(c -> {
    }, p -> p.setTagsString("platform,scanner"));
    userSession.addProjectPermission(ADMIN, project);

    call(project.getKey(), "");

    assertNoTags(project.getKey());
  }

  @Test
  public void override_existing_tags() {
    project = db.components().insertPrivateProjectDto(c -> {
    }, p -> p.setTagsString("marketing,languages"));
    userSession.addProjectPermission(ADMIN, project);

    call(project.getKey(), "finance,offshore,platform");

    assertTags(project.getKey(), "finance", "offshore", "platform");
  }

  @Test
  public void set_tags_as_project_admin() {
    userSession.logIn().addProjectPermission(ADMIN, project);

    call(project.getKey(), "platform, lambda");

    assertTags(project.getKey(), "platform", "lambda");
  }

  @Test
  public void do_not_duplicate_tags() {
    call(project.getKey(), "atlas, atlas, atlas");

    assertTags(project.getKey(), "atlas");
  }

  @Test
  public void fail_if_tag_does_not_respect_format() {
    String projectKey = project.getKey();
    assertThatThrownBy(() -> call(projectKey, "_finance_"))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Tag '_finance_' is invalid. Tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'");
  }

  @Test
  public void fail_if_not_project_admin() {
    userSession.logIn().addProjectPermission(USER, project);

    String projectKey = project.getKey();
    assertThatThrownBy(() -> call(projectKey, "platform"))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_no_project() {
    assertThatThrownBy(() -> call(null, "platform"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_no_tags() {
    String projectKey = project.getKey();
    assertThatThrownBy(() -> call(projectKey, null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_component_is_a_view() {
    ComponentDto view = db.components().insertPrivatePortfolio(v -> v.setKey("VIEW_KEY"));

    String viewKey = view.getKey();
    assertThatThrownBy(() -> call(viewKey, "point-of-view"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'VIEW_KEY' not found");
  }

  @Test
  public void fail_if_component_is_a_module() {
    ComponentDto projectComponent = dbClient.componentDao().selectByUuid(dbSession, project.getUuid()).get();
    ComponentDto module = db.components().insertComponent(newModuleDto(projectComponent).setKey("MODULE_KEY"));

    String moduleKey = module.getKey();
    assertThatThrownBy(() -> call(moduleKey, "modz"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'MODULE_KEY' not found");
  }

  @Test
  public void fail_if_component_is_a_file() {
    ComponentDto projectComponent = dbClient.componentDao().selectByUuid(dbSession, project.getUuid()).get();
    ComponentDto file = db.components().insertComponent(newFileDto(projectComponent).setKey("FILE_KEY"));

    String fileKey = file.getKey();
    assertThatThrownBy(() -> call(fileKey, "secret"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'FILE_KEY' not found");
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
    ofNullable(projectKey).ifPresent(p -> request.setParam("project", p));
    ofNullable(tags).ifPresent(t -> request.setParam("tags", tags));

    return request.execute();
  }

  private void assertTags(String projectKey, String... tags) {
    assertThat(dbClient.projectDao().selectProjectByKey(dbSession, projectKey).get().getTags()).containsExactlyInAnyOrder(tags);
  }

  private void assertNoTags(String projectKey) {
    assertThat(dbClient.projectDao().selectProjectByKey(dbSession, projectKey).get().getTags()).isEmpty();
  }
}
