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

package org.sonar.server.almsettings;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;

public class SetGithubBindingActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new SetGithubBindingAction(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null)));

  @Test
  public void set_github_project_binding() {
    UserDto user = db.users().insertUser();
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam("almSetting", githubAlmSetting.getKey())
      .setParam("project", project.getKey())
      .setParam("repository", "myproject")
      .execute();

    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), project).get())
      .extracting(ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid, ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug)
      .containsOnly(githubAlmSetting.getUuid(), project.uuid(), "myproject", null);
  }

  @Test
  public void override_existing_github_project_binding() {
    UserDto user = db.users().insertUser();
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ComponentDto project = db.components().insertPrivateProject();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);
    AlmSettingDto anotherGithubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    ws.newRequest()
      .setParam("almSetting", anotherGithubAlmSetting.getKey())
      .setParam("project", project.getKey())
      .setParam("repository", "myproject")
      .execute();

    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), project).get())
      .extracting(ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid, ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug)
      .containsOnly(anotherGithubAlmSetting.getUuid(), project.uuid(), "myproject", null);
  }

  @Test
  public void fail_when_alm_setting_does_not_exist() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No ALM setting with almSetting 'unknown' has been found");

    ws.newRequest()
      .setParam("almSetting", "unknown")
      .setParam("project", project.getKey())
      .setParam("repository", "myproject")
      .execute();
  }

  @Test
  public void fail_when_project_does_not_exist() {
    UserDto user = db.users().insertUser();
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(ADMIN, project);

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("almSetting", githubAlmSetting.getKey())
      .setParam("project", "unknown")
      .setParam("repository", "myproject")
      .execute();
  }

  @Test
  public void fail_when_missing_administer_permission_on_project() {
    UserDto user = db.users().insertUser();
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(USER, project);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("almSetting", githubAlmSetting.getKey())
      .setParam("project", project.getKey())
      .setParam("repository", "myproject")
      .execute();
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("almSetting", true), tuple("project", true), tuple("repository", true));
  }
}
