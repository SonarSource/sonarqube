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
package org.sonar.server.component;

import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.pushevent.PushEventDto;
import org.sonar.server.es.ProjectIndexer;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class ComponentServiceUpdateKeyTest {

  private final System2 system2 = System2.INSTANCE;

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final DbTester db = DbTester.create(system2);

  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private TestProjectIndexers projectIndexers = new TestProjectIndexers();
  private ProjectLifeCycleListeners projectLifeCycleListeners = mock(ProjectLifeCycleListeners.class);
  private ComponentService underTest = new ComponentService(dbClient, userSession, projectIndexers, projectLifeCycleListeners);

  @Test
  public void update_project_key() {
    ComponentDto project = insertSampleProject();
    ComponentDto file = componentDb.insertComponent(ComponentTesting.newFileDto(project, null).setKey("sample:root:src/File.xoo"));
    ComponentDto inactiveFile = componentDb.insertComponent(ComponentTesting.newFileDto(project, null).setKey("sample:root:src/InactiveFile.xoo").setEnabled(false));

    dbSession.commit();

    logInAsProjectAdministrator(project);
    underTest.updateKey(dbSession, componentDb.getProjectDto(project), "sample2:root");
    dbSession.commit();

    // Check project key has been updated
    assertThat(db.getDbClient().componentDao().selectByKey(dbSession, project.getKey())).isEmpty();
    assertThat(db.getDbClient().componentDao().selectByKey(dbSession, "sample2:root")).isNotNull();

    // Check file key has been updated
    assertThat(db.getDbClient().componentDao().selectByKey(dbSession, file.getKey())).isEmpty();
    assertThat(db.getDbClient().componentDao().selectByKey(dbSession, "sample2:root:src/File.xoo")).isNotNull();
    assertThat(db.getDbClient().componentDao().selectByKey(dbSession, "sample2:root:src/InactiveFile.xoo")).isNotNull();

    assertThat(dbClient.componentDao().selectByKey(dbSession, inactiveFile.getKey())).isEmpty();

    assertThat(projectIndexers.hasBeenCalled(project.uuid(), ProjectIndexer.Cause.PROJECT_KEY_UPDATE)).isTrue();

    Deque<PushEventDto> pushEvents = db.getDbClient().pushEventDao().selectChunkByProjectUuids(dbSession, Set.of(project.uuid()), 0L, "id", 20);

    assertThat(pushEvents).isNotEmpty();

    Optional<PushEventDto> event = pushEvents.stream().filter(e -> e.getProjectUuid().equals(project.uuid()) && e.getName().equals("ProjectKeyChanged")).findFirst();
    assertThat(event).isNotEmpty();

    String payload = new String(event.get().getPayload(), StandardCharsets.UTF_8);

    assertThat(payload).isEqualTo("{\"oldProjectKey\":\"sample:root\",\"newProjectKey\":\"sample2:root\"}");
  }

  @Test
  public void update_provisioned_project_key() {
    ComponentDto provisionedProject = insertProject("provisionedProject");

    dbSession.commit();

    logInAsProjectAdministrator(provisionedProject);
    underTest.updateKey(dbSession, componentDb.getProjectDto(provisionedProject), "provisionedProject2");
    dbSession.commit();

    assertComponentKeyHasBeenUpdated(provisionedProject.getKey(), "provisionedProject2");
    assertThat(projectIndexers.hasBeenCalled(provisionedProject.uuid(), ProjectIndexer.Cause.PROJECT_KEY_UPDATE)).isTrue();
  }

  @Test
  public void fail_to_update_project_key_without_admin_permission() {
    ComponentDto project = insertSampleProject();
    userSession.logIn("john").addProjectPermission(UserRole.USER, project);

    ProjectDto projectDto = componentDb.getProjectDto(project);
    assertThatThrownBy(() -> underTest.updateKey(dbSession, projectDto, "sample2:root"))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_old_key_and_new_key_are_the_same() {
    ComponentDto project = insertSampleProject();
    ComponentDto anotherProject = componentDb.insertPrivateProject();
    logInAsProjectAdministrator(project);

    ProjectDto projectDto = componentDb.getProjectDto(project);
    String anotherProjectDbKey = anotherProject.getKey();
    assertThatThrownBy(() -> underTest.updateKey(dbSession, projectDto,
      anotherProjectDbKey))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Impossible to update key: a component with key \"" + anotherProjectDbKey + "\" already exists.");
  }

  @Test
  public void fail_if_new_key_is_empty() {
    ComponentDto project = insertSampleProject();
    logInAsProjectAdministrator(project);

    ProjectDto projectDto = componentDb.getProjectDto(project);
    assertThatThrownBy(() -> underTest.updateKey(dbSession, projectDto, ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Malformed key for ''. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");
  }

  @Test
  public void fail_if_new_key_is_not_formatted_correctly() {
    ComponentDto project = insertSampleProject();
    logInAsProjectAdministrator(project);

    ProjectDto projectDto = componentDb.getProjectDto(project);
    assertThatThrownBy(() -> underTest.updateKey(dbSession, projectDto, "sample?root"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Malformed key for 'sample?root'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");
  }

  private ComponentDto insertSampleProject() {
    return insertProject("sample:root");
  }

  private ComponentDto insertProject(String key) {
    return componentDb.insertPrivateProject(c -> c.setKey(key));
  }

  private void assertComponentKeyHasBeenUpdated(String oldKey, String newKey) {
    assertThat(dbClient.componentDao().selectByKey(dbSession, oldKey)).isEmpty();
    assertThat(dbClient.componentDao().selectByKey(dbSession, newKey)).isPresent();
  }

  private void logInAsProjectAdministrator(ComponentDto provisionedProject) {
    userSession.logIn("john").addProjectPermission(UserRole.ADMIN, provisionedProject);
  }
}
