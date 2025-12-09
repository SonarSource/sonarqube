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
package org.sonar.server.almintegration.ws.github;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmIntegrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.server.tester.UserSessionRule.standalone;

public class GetGithubClientIdActionIT {

  @Rule
  public UserSessionRule userSession = standalone();

  private final System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final WsActionTester ws = new WsActionTester(new GetGithubClientIdAction(db.getDbClient(), userSession));

  @Test
  public void get_client_id() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting(alm -> alm.setClientId("client_123").setClientSecret("client_secret_123"));

    AlmIntegrations.GithubClientIdWsResponse response = ws.newRequest().setParam(GetGithubClientIdAction.PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .executeProtobuf(AlmIntegrations.GithubClientIdWsResponse.class);

    assertThat(response.getClientId()).isEqualTo(githubAlmSetting.getClientId());
  }

  @Test
  public void fail_when_missing_create_project_permission() {
    TestRequest request = ws.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_almSetting_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);

    TestRequest request = ws.newRequest().setParam(GetGithubClientIdAction.PARAM_ALM_SETTING, "unknown");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Github Setting 'unknown' not found");
  }

  @Test
  public void fail_when_client_id_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting(s -> s.setClientId(null));

    TestRequest request = ws.newRequest()
        .setParam(GetGithubClientIdAction.PARAM_ALM_SETTING, githubAlmSetting.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No client ID for setting with key '%s'", githubAlmSetting.getKey());
  }
}
