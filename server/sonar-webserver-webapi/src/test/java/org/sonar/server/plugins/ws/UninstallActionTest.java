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
package org.sonar.server.plugins.ws;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.plugins.PluginUninstaller;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(DataProviderRunner.class)
public class UninstallActionTest {
  private static final String ACTION_KEY = "uninstall";
  private static final String KEY_PARAM = "key";
  private static final String PLUGIN_KEY = "findbugs";

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private PluginUninstaller pluginUninstaller = mock(PluginUninstaller.class);
  private UninstallAction underTest = new UninstallAction(pluginUninstaller, userSessionRule);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() {
    TestRequest request = tester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_system_administrator() {
    userSessionRule.logIn().setNonSystemAdministrator();

    TestRequest request = tester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void action_uninstall_is_defined() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo(ACTION_KEY);
    assertThat(action.isPost()).isTrue();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNull();

    assertThat(action.params()).hasSize(1);
    WebService.Param keyParam = action.param(KEY_PARAM);
    assertThat(keyParam).isNotNull();
    assertThat(keyParam.isRequired()).isTrue();
    assertThat(keyParam.description()).isNotNull();
  }

  @Test
  public void IAE_is_raised_when_key_param_is_not_provided() {
    logInAsSystemAdministrator();

    TestRequest request = tester.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void uninstaller_is_called() {
    logInAsSystemAdministrator();

    TestResponse response = tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY)
      .execute();

    verify(pluginUninstaller).uninstall(PLUGIN_KEY);
    assertThat(response.getInput()).isEmpty();
  }

  private void logInAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }

}
