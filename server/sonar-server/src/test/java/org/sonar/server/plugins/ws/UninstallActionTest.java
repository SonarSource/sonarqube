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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.plugins.PluginUninstaller;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class UninstallActionTest {
  private static final String DUMMY_CONTROLLER_KEY = "dummy";
  private static final String CONTROLLER_KEY = "api/plugins";
  private static final String ACTION_KEY = "uninstall";
  private static final String KEY_PARAM = "key";
  private static final String PLUGIN_KEY = "findbugs";

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ServerPluginRepository serverPluginRepository = mock(ServerPluginRepository.class);
  private PluginUninstaller pluginUninstaller = mock(PluginUninstaller.class);
  private UninstallAction underTest = new UninstallAction(serverPluginRepository, pluginUninstaller, userSessionRule);

  private WsTester wsTester = new WsTester(new PluginsWs(underTest));
  private Request invalidRequest = wsTester.newGetRequest(CONTROLLER_KEY, ACTION_KEY);
  private Request validRequest = wsTester.newGetRequest(CONTROLLER_KEY, ACTION_KEY).setParam(KEY_PARAM, PLUGIN_KEY);
  private WsTester.TestResponse response = new WsTester.TestResponse();

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() throws Exception {
    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    underTest.handle(validRequest, response);
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_system_administrator() throws Exception {
    userSessionRule.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    underTest.handle(validRequest, response);
  }

  @Test
  public void action_uninstall_is_defined() {
    logInAsSystemAdministrator();

    WsTester wsTester = new WsTester();
    WebService.NewController newController = wsTester.context().createController(DUMMY_CONTROLLER_KEY);

    underTest.define(newController);
    newController.done();

    WebService.Controller controller = wsTester.controller(DUMMY_CONTROLLER_KEY);
    assertThat(controller.actions()).extracting("key").containsExactly(ACTION_KEY);

    WebService.Action action = controller.actions().iterator().next();
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
  public void IAE_is_raised_when_key_param_is_not_provided() throws Exception {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);

    underTest.handle(invalidRequest, response);
  }

  @Test
  public void do_not_attempt_uninstall_if_no_plugin_in_repository_for_specified_key() throws Exception {
    logInAsSystemAdministrator();
    when(serverPluginRepository.getPluginInfo(PLUGIN_KEY)).thenReturn(null);

    underTest.handle(validRequest, response);

    verifyZeroInteractions(pluginUninstaller);
  }

  @Test
  @UseDataProvider("editionBundledOrganizationAndLicense")
  public void IAE_is_raised_when_plugin_is_installed_and_is_edition_bundled(String organization, String license) throws Exception {
    logInAsSystemAdministrator();
    when(serverPluginRepository.getPluginInfo(PLUGIN_KEY))
      .thenReturn(new PluginInfo(PLUGIN_KEY)
        .setOrganizationName(organization)
        .setLicense(license));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("SonarSource commercial plugin with key '" + PLUGIN_KEY + "' can only be uninstalled as part of a SonarSource edition");

    underTest.handle(validRequest, response);
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
  public void if_plugin_is_installed_uninstallation_is_triggered() throws Exception {
    logInAsSystemAdministrator();
    when(serverPluginRepository.getPluginInfo(PLUGIN_KEY)).thenReturn(new PluginInfo(PLUGIN_KEY));

    underTest.handle(validRequest, response);

    verify(pluginUninstaller).uninstall(PLUGIN_KEY);
    assertThat(response.outputAsString()).isEmpty();
  }

  private void logInAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }

}
