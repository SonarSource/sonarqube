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

import com.google.common.base.Optional;
import java.util.Arrays;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.ws.WsTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class PendingActionTest {

  private static final String DUMMY_CONTROLLER_KEY = "dummy";

  PluginDownloader pluginDownloader = mock(PluginDownloader.class);
  ServerPluginRepository serverPluginRepository = mock(ServerPluginRepository.class);
  UpdateCenterMatrixFactory updateCenterMatrixFactory = mock(UpdateCenterMatrixFactory.class, RETURNS_DEEP_STUBS);
  PendingAction underTest = new PendingAction(pluginDownloader, serverPluginRepository, new PluginWSCommons(), updateCenterMatrixFactory);
  Request request = mock(Request.class);
  WsTester.TestResponse response = new WsTester.TestResponse();

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

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"installing\": []," +
        "  \"removing\": []" +
        "}"
      );
  }

  @Test
  public void empty_arrays_are_returned_when_update_center_is_unavailable() throws Exception {
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.<UpdateCenter>absent());

    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"installing\": []," +
        "  \"removing\": []" +
        "}"
      );
  }

  @Test
  public void verify_properties_displayed_in_json_per_installing_plugin() throws Exception {
    when(pluginDownloader.getDownloadedPlugins()).thenReturn(of(gitPluginInfo()));
    UpdateCenter updateCenter = mock(UpdateCenter.class);
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.of(updateCenter));
    when(updateCenter.findAllCompatiblePlugins()).thenReturn(
      Arrays.asList(
        new Plugin("scmgit")
          .setCategory("cat_1")
        )
      );

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
        "      \"category\":\"cat_1\"," +
        "      \"organizationName\": \"SonarSource\"," +
        "      \"organizationUrl\": \"http://www.sonarsource.com\"," +
        "      \"homepageUrl\": \"http://redirect.sonarsource.com/plugins/scmgit.html\"," +
        "      \"issueTrackerUrl\": \"http://jira.sonarsource.com/browse/SONARSCGIT\"," +
        "      \"implementationBuild\": \"9ce9d330c313c296fab051317cc5ad4b26319e07\"" +
        "    }" +
        "  ]," +
        "  \"removing\": []" +
        "}"
      );
  }

  @Test
  public void verify_properties_displayed_in_json_per_removing_plugin() throws Exception {
    when(serverPluginRepository.getUninstalledPlugins()).thenReturn(of(gitPluginInfo()));

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
        "      \"homepageUrl\": \"http://redirect.sonarsource.com/plugins/scmgit.html\"," +
        "      \"issueTrackerUrl\": \"http://jira.sonarsource.com/browse/SONARSCGIT\"," +
        "      \"implementationBuild\": \"9ce9d330c313c296fab051317cc5ad4b26319e07\"" +
        "    }" +
        "  ]" +
        "}"
      );
  }

  @Test
  public void installing_plugins_are_sorted_by_name_then_key_and_are_unique() throws Exception {
    when(pluginDownloader.getDownloadedPlugins()).thenReturn(of(
      newPluginInfo(0).setName("Foo"),
      newPluginInfo(3).setName("Bar"),
      newPluginInfo(2).setName("Bar")
      ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(
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
        "  \"removing\": []" +
        "}"
      );
  }

  @Test
  public void removing_plugins_are_sorted_and_unique() throws Exception {
    when(serverPluginRepository.getUninstalledPlugins()).thenReturn(of(
      newPluginInfo(0).setName("Foo"),
      newPluginInfo(3).setName("Bar"),
      newPluginInfo(2).setName("Bar")
      ));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"installing\": []," +
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
        "}"
      );
  }

  public PluginInfo gitPluginInfo() {
    return new PluginInfo("scmgit")
      .setName("Git")
      .setDescription("Git SCM Provider.")
      .setVersion(Version.create("1.0"))
      .setLicense("GNU LGPL 3")
      .setOrganizationName("SonarSource")
      .setOrganizationUrl("http://www.sonarsource.com")
      .setHomepageUrl("http://redirect.sonarsource.com/plugins/scmgit.html")
      .setIssueTrackerUrl("http://jira.sonarsource.com/browse/SONARSCGIT")
      .setImplementationBuild("9ce9d330c313c296fab051317cc5ad4b26319e07");
  }

  public PluginInfo newPluginInfo(int id) {
    return new PluginInfo("key" + id).setName("name" + id);
  }
}
