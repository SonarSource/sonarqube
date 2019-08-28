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
package org.sonar.server.plugins.ws;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class InstallActionTest {
  private static final String DUMMY_CONTROLLER_KEY = "dummy";
  private static final String ACTION_KEY = "install";
  private static final String KEY_PARAM = "key";
  private static final String PLUGIN_KEY = "pluginKey";

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UpdateCenterMatrixFactory updateCenterFactory = mock(UpdateCenterMatrixFactory.class);
  private UpdateCenter updateCenter = mock(UpdateCenter.class);
  private PluginDownloader pluginDownloader = mock(PluginDownloader.class);
  private InstallAction underTest = new InstallAction(updateCenterFactory, pluginDownloader, userSessionRule);
  private WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void wireMocks() {
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(Optional.of(updateCenter));
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() {
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest().execute();
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_system_administrator() {
    userSessionRule.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest().execute();
  }

  @Test
  public void action_install_is_defined() {
    WebService.Action action = tester.getDef();
    assertThat(action.isPost()).isTrue();
    assertThat(action.key()).isEqualTo(ACTION_KEY);
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
    expectedException.expect(IllegalArgumentException.class);

    tester.newRequest().execute();
  }

  @Test
  public void IAE_is_raised_when_there_is_no_available_plugin_for_the_key() {
    logInAsSystemAdministrator();
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("No plugin with key 'pluginKey'");

    tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY)
      .execute();
  }

  @Test
  @UseDataProvider("editionBundledOrganizationAndLicense")
  public void IAE_is_raised_when_plugin_is_edition_bundled(String organization, String license) {
    logInAsSystemAdministrator();
    Version version = Version.create("1.0");
    when(updateCenter.findAvailablePlugins()).thenReturn(ImmutableList.of(
      PluginUpdate.createWithStatus(new Release(Plugin.factory(PLUGIN_KEY)
        .setLicense(license)
        .setOrganization(organization), version), PluginUpdate.Status.COMPATIBLE)));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("SonarSource commercial plugin with key '" + PLUGIN_KEY + "' can only be installed as part of a SonarSource edition");

    tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY)
      .execute();
  }

  @DataProvider
  public static Object[][] editionBundledOrganizationAndLicense() {
    return new Object[][] {
      {"SonarSource", "SonarSource"},
      {"SonarSource", "Commercial"},
      {"sonarsource", "SOnArSOURCE"}
    };
  }

  @Test
  public void IAE_is_raised_when_update_center_is_unavailable() {
    logInAsSystemAdministrator();
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(Optional.absent());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("No plugin with key 'pluginKey'");

    tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY)
      .execute();
  }

  @Test
  public void if_plugin_is_found_available_download_is_triggered_with_latest_version_from_updatecenter() {
    logInAsSystemAdministrator();
    Version version = Version.create("1.0");
    when(updateCenter.findAvailablePlugins()).thenReturn(ImmutableList.of(
      PluginUpdate.createWithStatus(new Release(Plugin.factory(PLUGIN_KEY), version), PluginUpdate.Status.COMPATIBLE)));

    TestResponse response = tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY)
      .execute();

    verify(pluginDownloader).download(PLUGIN_KEY, version);
    response.assertNoContent();
  }

  private void logInAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }
}
