/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.plugins.ws;

import java.io.File;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.ws.WsTester;
import org.sonar.updatecenter.common.Version;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class PendingPluginsWsActionTest {

  public static final PluginInfo GIT_PLUGIN_INFO = new PluginInfo("scmgit")
    .setName("Git")
    .setDescription("Git SCM Provider.")
    .setVersion(Version.create("1.0"))
    .setLicense("GNU LGPL 3")
    .setOrganizationName("SonarSource")
    .setOrganizationUrl("http://www.sonarsource.com")
    .setHomepageUrl("http://redirect.sonarsource.com/plugins/scmgit.html")
    .setIssueTrackerUrl("http://jira.codehaus.org/browse/SONARSCGIT")
    .setFile(new File("/home/user/sonar-scm-git-plugin-1.0.jar"))
    .setImplementationBuild("9ce9d330c313c296fab051317cc5ad4b26319e07");
  private static final String DUMMY_CONTROLLER_KEY = "dummy";
  public static final PluginInfo PLUGIN_2_2 = new PluginInfo("key2").setName("name2");
  public static final PluginInfo PLUGIN_2_1 = new PluginInfo("key1").setName("name2");
  public static final PluginInfo PLUGIN_0_0 = new PluginInfo("key0").setName("name0");

  private PluginDownloader pluginDownloader = mock(PluginDownloader.class);
  private ServerPluginRepository serverPluginRepository = mock(ServerPluginRepository.class);
  private PendingPluginsWsAction underTest = new PendingPluginsWsAction(pluginDownloader, serverPluginRepository, new PluginWSCommons());

  private Request request = mock(Request.class);
  private WsTester.TestResponse response = new WsTester.TestResponse();

  @Test
  public void action_pending_is_defined() {
    WsTester wsTester = new WsTester();
    WebService.NewController newController = wsTester.context().createController(DUMMY_CONTROLLER_KEY);

    underTest.define(newController);
    newController.done();

    WebService.Controller controller = wsTester.controller(DUMMY_CONTROLLER_KEY);
    assertThat(controller.actions()).extracting("key").containsExactly("pending");

    WebService.Action action = controller.actions().iterator().next();
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNotNull();
  }

  @Test
  public void empty_arrays_are_returned_when_there_nothing_pending() throws Exception {
    underTest.handle(request, response);

    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo(
      "{" +
        "  \"installing\": []," +
        "  \"removing\": []" +
        "}"
      );
  }

  @Test
  public void verify_properties_displayed_in_json_per_installing_plugin() throws Exception {
    when(pluginDownloader.getDownloadedPlugins()).thenReturn(of(GIT_PLUGIN_INFO));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(
      "{" +
        "  \"installing\": " +
        "  [" +
        "    {" +
        "      \"key\": \"scmgit\"," +
        "      \"name\": \"Git\"," +
        "      \"description\": \"Git SCM Provider.\"," +
        "      \"version\": \"1.0\"," +
        "      \"license\": \"GNU LGPL 3\"," +
        "      \"organizationName\": \"SonarSource\"," +
        "      \"organizationUrl\": \"http://www.sonarsource.com\"," +
        "      \"homepage\": \"http://redirect.sonarsource.com/plugins/scmgit.html\"," +
        "      \"issueTrackerUrl\": \"http://jira.codehaus.org/browse/SONARSCGIT\"," +
        "      \"implementationBuild\": \"9ce9d330c313c296fab051317cc5ad4b26319e07\"" +
        "    }" +
        "  ]," +
        "  \"removing\": []" +
        "}"
      );
  }

  @Test
  public void verify_properties_displayed_in_json_per_removing_plugin() throws Exception {
    when(serverPluginRepository.getUninstalledPlugins()).thenReturn(of(GIT_PLUGIN_INFO));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(
      "{" +
        "  \"installing\": []," +
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
        "      \"homepage\": \"http://redirect.sonarsource.com/plugins/scmgit.html\"," +
        "      \"issueTrackerUrl\": \"http://jira.codehaus.org/browse/SONARSCGIT\"," +
        "      \"implementationBuild\": \"9ce9d330c313c296fab051317cc5ad4b26319e07\"" +
        "    }" +
        "  ]" +
        "}"
      );
  }

  @Test
  public void installing_plugin_are_sorted_by_name_then_key_and_are_unique() throws Exception {
    when(pluginDownloader.getDownloadedPlugins()).thenReturn(of(
      PLUGIN_2_2,
      PLUGIN_2_1,
      PLUGIN_2_2,
      PLUGIN_0_0
      ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo(
      "{" +
        "  \"installing\": " +
        "  [" +
        "    {" +
        "      \"key\": \"key0\"," +
        "      \"name\": \"name0\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key1\"," +
        "      \"name\": \"name2\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key2\"," +
        "      \"name\": \"name2\"," +
        "    }" +
        "  ]," +
        "  \"removing\": []" +
        "}"
      );
  }

  @Test
  public void removing_plugin_are_sorted_and_unique() throws Exception {
    when(serverPluginRepository.getUninstalledPlugins()).thenReturn(of(
      PLUGIN_2_2,
      PLUGIN_2_1,
      PLUGIN_2_2,
      PLUGIN_0_0
      ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo(
      "{" +
        "  \"installing\": []," +
        "  \"removing\": " +
        "  [" +
        "    {" +
        "      \"key\": \"key0\"," +
        "      \"name\": \"name0\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key1\"," +
        "      \"name\": \"name2\"," +
        "    }," +
        "    {" +
        "      \"key\": \"key2\"," +
        "      \"name\": \"name2\"," +
        "    }" +
        "  ]" +
        "}"
      );
  }
}
