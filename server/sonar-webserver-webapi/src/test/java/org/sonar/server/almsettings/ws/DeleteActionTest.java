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
package org.sonar.server.almsettings.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;

public class DeleteActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new DeleteAction(db.getDbClient(), userSession,
    new AlmSettingsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null),
      mock(MultipleAlmFeature.class))));

  @Test
  public void delete() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertGitHubAlmSetting();

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession())).isEmpty();
  }

  @Test
  public void delete_alm_setting_also_delete_pat() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();
    AlmPatDto almPatDto = db.almPats().insert(p -> p.setAlmSettingUuid(almSettingDto.getUuid()), p -> p.setUserUuid(user.getUuid()));

    ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession())).isEmpty();
    assertThat(db.getDbClient().almPatDao().selectByUuid(db.getSession(), almPatDto.getUuid())).isNotPresent();
  }

  @Test
  public void delete_project_binding_during_deletion() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(almSetting, project);
    // Second setting having a project bound on it, should not be impacted by the deletion of the first one
    AlmSettingDto anotherAlmSetting2 = db.almSettings().insertGitHubAlmSetting();
    ProjectDto anotherProject = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(anotherAlmSetting2, anotherProject);

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    assertThat(db.getDbClient().almSettingDao().selectAll(db.getSession())).extracting(AlmSettingDto::getUuid).containsExactlyInAnyOrder(anotherAlmSetting2.getUuid());
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), project)).isEmpty();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), anotherProject)).isNotEmpty();
  }

  @Test
  public void fail_when_key_does_not_match_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", "unknown")
      .execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("DevOps Platform setting with key 'unknown' cannot be found");
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    AlmSettingDto almSettingDto = db.almSettings().insertGitHubAlmSetting();

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", almSettingDto.getKey())
      .execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.1");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("key", true));
  }
}
