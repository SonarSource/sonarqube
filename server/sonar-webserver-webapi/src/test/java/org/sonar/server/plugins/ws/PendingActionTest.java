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
package org.sonar.server.plugins.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.PluginUninstaller;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class PendingActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private PluginDownloader pluginDownloader = mock(PluginDownloader.class);
  private PluginUninstaller pluginUninstaller = mock(PluginUninstaller.class);
  private ServerPluginRepository serverPluginRepository = mock(ServerPluginRepository.class);
  private UpdateCenterMatrixFactory updateCenterMatrixFactory = mock(UpdateCenterMatrixFactory.class, RETURNS_DEEP_STUBS);
  private PendingAction underTest = new PendingAction(userSession, pluginDownloader, serverPluginRepository,
    pluginUninstaller, updateCenterMatrixFactory);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void action_pending_is_defined() {
    WebService.Action action = tester.getDef();
    assertThat(action.isPost()).isFalse();
    assertThat(action.key()).isEqualTo("pending");
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNotNull();
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() {
    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();

    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void empty_arrays_are_returned_when_there_nothing_pending() {
    logInAsSystemAdministrator();

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"installing\": []," +
        "  \"removing\": []," +
        "  \"updating\": []" +
        "}");
  }

  @Test
  public void empty_arrays_are_returned_when_update_center_is_unavailable() {
    logInAsSystemAdministrator();
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.empty());

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"installing\": []," +
        "  \"removing\": []," +
        "  \"updating\": []" +
        "}");
  }

  @Test
  public void verify_properties_displayed_in_json_per_installing_plugin() {
    logInAsSystemAdministrator();
    newUpdateCenter("scmgit");
    when(pluginDownloader.getDownloadedPlugins()).thenReturn(of(newScmGitPluginInfo()));

    TestResponse response = tester.newRequest().execute();

    response.assertJson(
      "{" +
        "  \"installing\": " +
        "  [" +
        "    {" +
        "      \"key\": \"scmgit\"," +
        "      \"name\": \"Git\"," +
        "      \"description\": \"Git SCM Provider.\"," +
        "      \"version\": \"1.0\"," +
        "      \"license\": \"GNU LGPL 3\"," +
        "      \"category\":\"cat_1\"," +
        "      \"organizationName\": \"SonarSource\"," +
        "      \"organizationUrl\": \"http://www.sonarsource.com\"," +
        "      \"homepageUrl\": \"https://redirect.sonarsource.com/plugins/scmgit.html\"," +
        "      \"issueTrackerUrl\": \"http://jira.sonarsource.com/browse/SONARSCGIT\"," +
        "      \"implementationBuild\": \"9ce9d330c313c296fab051317cc5ad4b26319e07\"" +
        "    }" +
        "  ]," +
        "  \"removing\": []," +
        "  \"updating\": []" +
        "}");
  }

  @Test
  public void verify_properties_displayed_in_json_per_removing_plugin() {
    logInAsSystemAdministrator();
    when(pluginUninstaller.getUninstalledPlugins()).thenReturn(of(newScmGitPluginInfo()));

    TestResponse response = tester.newRequest().execute();

    response.assertJson(
      "{" +
        "  \"installing\": []," +
        "  \"updating\": []," +
        "  \"removing\": " +
        "  [" +
        "    {" +
        "      \"key\": \"scmgit\"," +
        "      \"name\": \"Git\"," +
        "      \"description\": \"Git SCM Provider.\"," +
        "      \"version\": \"1.0\"," +
        "      \"license\": \"GNU LGPL 3\"," +
        "      \"organizationName\": \"SonarSource\"," +
        "      \"organizationUrl\": \"http://www.sonarsource.com\"," +
        "      \"homepageUrl\": \"https://redirect.sonarsource.com/plugins/scmgit.html\"," +
        "      \"issueTrackerUrl\": \"http://jira.sonarsource.com/browse/SONARSCGIT\"," +
        "      \"implementationBuild\": \"9ce9d330c313c296fab051317cc5ad4b26319e07\"" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void verify_properties_displayed_in_json_per_updating_plugin() {
    logInAsSystemAdministrator();
    newUpdateCenter("scmgit");
    when(serverPluginRepository.getPluginInfos()).thenReturn(of(newScmGitPluginInfo()));
    when(pluginDownloader.getDownloadedPlugins()).thenReturn(of(newScmGitPluginInfo()));

    TestResponse response = tester.newRequest().execute();

    response.assertJson(
      "{" +
        "  \"installing\": []," +
        "  \"removing\": []," +
        "  \"updating\": " +
        "  [" +
        "    {" +
        "      \"key\": \"scmgit\"" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void verify_properties_displayed_in_json_per_installing_removing_and_updating_plugins() {
    logInAsSystemAdministrator();
    PluginInfo installed = newPluginInfo("java");
    PluginInfo removedPlugin = newPluginInfo("js");
    PluginInfo newPlugin = newPluginInfo("php");

    newUpdateCenter("scmgit");
    when(serverPluginRepository.getPluginInfos()).thenReturn(of(installed));
    when(pluginUninstaller.getUninstalledPlugins()).thenReturn(of(removedPlugin));
    when(pluginDownloader.getDownloadedPlugins()).thenReturn(of(newPlugin, installed));

    TestResponse response = tester.newRequest().execute();

    response.assertJson(
      "{" +
        "  \"installing\":" +
        "  [" +
        "    {" +
        "      \"key\": \"php\"" +
        "    }" +
        "  ]," +
        "  \"removing\":" +
        "  [" +
        "    {" +
        "      \"key\": \"js\"" +
        "    }" +
        "  ]," +
        "  \"updating\": " +
        "  [" +
        "    {" +
        "      \"key\": \"java\"" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void installing_plugins_are_sorted_by_name_then_key_and_are_unique() {
    logInAsSystemAdministrator();
    when(pluginDownloader.getDownloadedPlugins()).thenReturn(of(
      newPluginInfo(0).setName("Foo"),
      newPluginInfo(3).setName("Bar"),
      newPluginInfo(2).setName("Bar")));

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"installing\": " +
        "  [" +
        "    {" +
        "      \"key\": \"key2\"," +
        "      \"name\": \"Bar\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key3\"," +
        "      \"name\": \"Bar\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key0\"," +
        "      \"name\": \"Foo\"," +
        "    }" +
        "  ]," +
        "  \"removing\": []," +
        "  \"updating\": []" +
        "}");
  }

  @Test
  public void removing_plugins_are_sorted_and_unique() {
    logInAsSystemAdministrator();
    when(pluginUninstaller.getUninstalledPlugins()).thenReturn(of(
      newPluginInfo(0).setName("Foo"),
      newPluginInfo(3).setName("Bar"),
      newPluginInfo(2).setName("Bar")));

    TestResponse response = tester.newRequest().execute();

    assertJson(response.getInput()).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"installing\": []," +
        "  \"updating\": []," +
        "  \"removing\": " +
        "  [" +
        "    {" +
        "      \"key\": \"key2\"," +
        "      \"name\": \"Bar\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key3\"," +
        "      \"name\": \"Bar\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key0\"," +
        "      \"name\": \"Foo\"," +
        "    }" +
        "  ]" +
        "}");
  }

  private PluginInfo newScmGitPluginInfo() {
    return new PluginInfo("scmgit")
      .setName("Git")
      .setDescription("Git SCM Provider.")
      .setVersion(Version.create("1.0"))
      .setLicense("GNU LGPL 3")
      .setOrganizationName("SonarSource")
      .setOrganizationUrl("http://www.sonarsource.com")
      .setHomepageUrl("https://redirect.sonarsource.com/plugins/scmgit.html")
      .setIssueTrackerUrl("http://jira.sonarsource.com/browse/SONARSCGIT")
      .setImplementationBuild("9ce9d330c313c296fab051317cc5ad4b26319e07");
  }

  private PluginInfo newPluginInfo(String key) {
    return new PluginInfo(key);
  }

  private UpdateCenter newUpdateCenter(String... pluginKeys) {
    UpdateCenter updateCenter = mock(UpdateCenter.class);
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.of(updateCenter));
    List<Plugin> plugins = new ArrayList<>();
    for (String pluginKey : pluginKeys) {
      plugins.add(Plugin.factory(pluginKey).setCategory("cat_1"));
    }
    when(updateCenter.findAllCompatiblePlugins()).thenReturn(plugins);
    return updateCenter;
  }

  private PluginInfo newPluginInfo(int id) {
    return new PluginInfo("key" + id).setName("name" + id);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
