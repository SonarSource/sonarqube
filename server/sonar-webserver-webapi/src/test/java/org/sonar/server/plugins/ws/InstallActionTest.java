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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.extension.PluginRiskConsent;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.PLUGINS_RISK_CONSENT;

@RunWith(DataProviderRunner.class)
public class InstallActionTest {
  private static final String ACTION_KEY = "install";
  private static final String KEY_PARAM = "key";
  private static final String PLUGIN_KEY = "pluginKey";

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private UpdateCenterMatrixFactory updateCenterFactory = mock(UpdateCenterMatrixFactory.class);
  private UpdateCenter updateCenter = mock(UpdateCenter.class);
  private PluginDownloader pluginDownloader = mock(PluginDownloader.class);
  private Configuration configuration = mock(Configuration.class);
  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private InstallAction underTest = new InstallAction(updateCenterFactory, pluginDownloader, userSessionRule, configuration,
    editionProvider);
  private WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void wireMocks() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(Optional.of(updateCenter));
    when(configuration.get(PLUGINS_RISK_CONSENT)).thenReturn(Optional.of(PluginRiskConsent.ACCEPTED.name()));
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() {
    TestRequest wsRequest = tester.newRequest();
    assertThatThrownBy(wsRequest::execute)
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

    TestRequest request = tester.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void IAE_is_raised_when_there_is_no_available_plugin_for_the_key() {
    logInAsSystemAdministrator();

    TestRequest request = tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("No plugin with key 'pluginKey'");
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

    TestRequest request = tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY);
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("SonarSource commercial plugin with key '" + PLUGIN_KEY + "' can only be installed as part of a SonarSource edition");
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
  public void IAE_is_raised_when_WS_used_on_commercial_edition() {
    logInAsSystemAdministrator();
    Version version = Version.create("1.0");
    when(updateCenter.findAvailablePlugins()).thenReturn(ImmutableList.of(
      PluginUpdate.createWithStatus(new Release(Plugin.factory(PLUGIN_KEY), version), PluginUpdate.Status.COMPATIBLE)));

    when(editionProvider.get()).thenReturn(Optional.of(Edition.DEVELOPER));

    TestRequest request = tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY);
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("This WS is unsupported in commercial edition. Please install plugin manually.");
  }

  @Test
  public void IAE_is_raised_when_update_center_is_unavailable() {
    logInAsSystemAdministrator();
    when(updateCenterFactory.getUpdateCenter(anyBoolean())).thenReturn(Optional.empty());

    TestRequest request = tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY);
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("No plugin with key 'pluginKey'");
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

  @Test
  public void handle_givenRiskConsentNotAccepted_expectServerError() {
    logInAsSystemAdministrator();

    when(configuration.get(PLUGINS_RISK_CONSENT)).thenReturn(Optional.of(PluginRiskConsent.NOT_ACCEPTED.name()));

    TestRequest request = tester.newRequest()
      .setParam(KEY_PARAM, PLUGIN_KEY);
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Can't install plugin without accepting firstly plugins risk consent");

  }

  private void logInAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }
}
