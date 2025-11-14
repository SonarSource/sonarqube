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

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentService;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.IndexersImpl;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.project.ProjectLifeCycleListenersImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_FROM;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_TO;

public class UpdateKeyActionIT {
  private static final String ANOTHER_KEY = "another_key";

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public final UserSessionRule userSessionRule = UserSessionRule.standalone();
  private final DbClient dbClient = db.getDbClient();
  private final Indexers indexers = new IndexersImpl();
  private final ComponentService componentService = new ComponentService(dbClient, userSessionRule, indexers, new ProjectLifeCycleListenersImpl());
  private final ComponentFinder componentFinder = new ComponentFinder(dbClient, null);
  private final WsActionTester ws = new WsActionTester(new UpdateKeyAction(dbClient, componentService, componentFinder));

  @Test
  public void update_key_of_project_referenced_by_its_key() {
    ProjectData project = insertProject();
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project.getProjectDto());

    call(project.projectKey(), ANOTHER_KEY);

    assertThat(selectMainBranchByKey(project.projectKey())).isEmpty();
    assertThat(selectMainBranchByKey(ANOTHER_KEY).get().uuid()).isEqualTo(project.getMainBranchComponent().uuid());

    assertThat(selectProjectByKey(project.projectKey())).isEmpty();
    assertThat(selectProjectByKey(ANOTHER_KEY).get().getUuid()).isEqualTo(project.getProjectDto().getUuid());
  }

  @Test
  public void fail_if_not_authorized() {
    ProjectData project = insertProject();
    userSessionRule.addProjectPermission(ProjectPermission.USER, project.getProjectDto());

    String projectKey = project.projectKey();
    assertThatThrownBy(() -> call(projectKey, ANOTHER_KEY))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_new_key_is_not_provided() {
    ProjectData project = insertProject();
    userSessionRule.addProjectPermission(ProjectPermission.ADMIN, project.getProjectDto());

    String projectKey = project.projectKey();
    assertThatThrownBy(() -> call(projectKey, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'to' parameter is missing");
  }

  @Test
  public void fail_if_key_not_provided() {
    assertThatThrownBy(() -> call(null, ANOTHER_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'from' parameter is missing");
  }

  @Test
  public void fail_if_project_does_not_exist() {
    assertThatThrownBy(() -> call("UNKNOWN_UUID", ANOTHER_KEY))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void api_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.since()).isEqualTo("6.1");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.key()).isEqualTo("update_key");
    assertThat(definition.changelog()).hasSize(1);
    assertThat(definition.params())
      .hasSize(2)
      .extracting(Param::key)
      .containsOnlyOnce("from", "to");
  }

  private ProjectData insertProject() {
    return db.components().insertPrivateProject();
  }

  private String call(@Nullable String key, @Nullable String newKey) {
    TestRequest request = ws.newRequest();

    if (key != null) {
      request.setParam(PARAM_FROM, key);
    }
    if (newKey != null) {
      request.setParam(PARAM_TO, newKey);
    }

    return request.execute().getInput();
  }

  private Optional<ComponentDto> selectMainBranchByKey(String key) {
    return db.getDbClient().componentDao().selectByKey(db.getSession(), key);
  }

  private Optional<ProjectDto> selectProjectByKey(String key) {
    return db.getDbClient().projectDao().selectProjectByKey(db.getSession(), key);
  }
}
