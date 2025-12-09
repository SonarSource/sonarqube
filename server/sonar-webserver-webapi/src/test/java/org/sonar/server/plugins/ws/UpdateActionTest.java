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

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.PluginUpdate.Status;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateActionTest {
  private static final String ACTION_KEY = "update";
  private static final String KEY_PARAM = "key";
  private static final String PLUGIN_KEY = "pluginKey";

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private UpdateCenterMatrixFactory updateCenterFactory = mock(UpdateCenterMatrixFactory.class);
  private UpdateCenter updateCenter = mock(UpdateCenter.class);
  private PluginDownloader pluginDownloader = mock(PluginDownloader.class);
  private UpdateAction underTest = new UpdateAction(updateCenterFactory, pluginDownloader, userSessionRule);
  private WsActionTester tester = new WsActionTester(underTest);


  @Before
  public void setUp() {
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(Optional.of(updateCenter));
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() {
    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_system_administrator() {
    userSessionRule.logIn().setNonSystemAdministrator();

    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void action_update_is_defined() {
    WebService.Action action = tester.getDef();
    assertThat(action.key()).isEqualTo(ACTION_KEY);
    assertThat(action.isPost()).isTrue();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNull();

    assertThat(action.params()).hasSize(1);
    WebService.Param key = action.param(KEY_PARAM);
    assertThat(key).isNotNull();
    assertThat(key.isRequired()).isTrue();
    assertThat(key.description()).isNotNull();
  }

  @Test
  public void IAE_is_raised_when_key_param_is_not_provided() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void IAE_is_raised_when_there_is_no_plugin_update_for_the_key() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> tester.newRequest().setParam(KEY_PARAM, PLUGIN_KEY).execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("No plugin with key 'pluginKey'");
  }

  @Test
  public void IAE_is_raised_when_update_center_is_unavailable() {
    logInAsSystemAdministrator();
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tester.newRequest().setParam(KEY_PARAM, PLUGIN_KEY).execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("No plugin with key 'pluginKey'");
  }

  @Test
  public void if_plugin_has_an_update_download_is_triggered_with_latest_version_from_updatecenter() {
    logInAsSystemAdministrator();
    Version version = Version.create("1.0");
    when(updateCenter.findPluginUpdates()).thenReturn(ImmutableList.of(
      PluginUpdate.createWithStatus(new Release(Plugin.factory(PLUGIN_KEY), version), Status.COMPATIBLE)));

    TestResponse response = tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY)
      .execute();

    verify(pluginDownloader).download(PLUGIN_KEY, version);
    assertThat(response.getInput()).isEmpty();
  }

  private void logInAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }

}
