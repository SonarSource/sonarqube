/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.io.File;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PluginInfo;
import org.sonar.db.DbTester;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.ws.WsTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class InstalledActionTest {
  private static final String DUMMY_CONTROLLER_KEY = "dummy";
  private static final String JSON_EMPTY_PLUGIN_LIST = "{" +
    "  \"plugins\":" + "[]" +
    "}";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ServerPluginRepository pluginRepository = mock(ServerPluginRepository.class);
  private UpdateCenterMatrixFactory updateCenterMatrixFactory = mock(UpdateCenterMatrixFactory.class, RETURNS_DEEP_STUBS);
  private Request request = mock(Request.class);
  private WsTester.TestResponse response = new WsTester.TestResponse();
  private InstalledAction underTest = new InstalledAction(pluginRepository, new PluginWSCommons(), updateCenterMatrixFactory, db.getDbClient());

  @Test
  public void action_installed_is_defined() {
    WsTester wsTester = new WsTester();
    WebService.NewController newController = wsTester.context().createController(DUMMY_CONTROLLER_KEY);

    underTest.define(newController);
    newController.done();

    WebService.Controller controller = wsTester.controller(DUMMY_CONTROLLER_KEY);
    assertThat(controller.actions()).extracting("key").containsExactly("installed");

    WebService.Action action = controller.actions().iterator().next();
    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNotNull();
  }

  @Test
  public void empty_array_is_returned_when_there_is_not_plugin_installed() throws Exception {
    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(JSON_EMPTY_PLUGIN_LIST);
  }

  @Test
  public void empty_array_when_update_center_is_unavailable() throws Exception {
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.<UpdateCenter>absent());

    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(JSON_EMPTY_PLUGIN_LIST);
  }

  @Test
  public void empty_fields_are_not_serialized_to_json() throws Exception {
    when(pluginRepository.getPluginInfos()).thenReturn(
      of(new PluginInfo("").setName("").setJarFile(new File(""))));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee(""),
      p -> p.setHash("abcdA"),
      p -> p.setUpdatedAt(111111L));

    underTest.handle(request, response);

    assertThat(response.outputAsString()).doesNotContain("name").doesNotContain("key");
  }

  @Test
  public void verify_properties_displayed_in_json_per_plugin() throws Exception {
    String jarFilename = getClass().getSimpleName() + "/" + "some.jar";
    when(pluginRepository.getPluginInfos()).thenReturn(of(
      new PluginInfo("plugKey")
        .setName("plugName")
        .setDescription("desc_it")
        .setVersion(Version.create("1.0"))
        .setLicense("license_hey")
        .setOrganizationName("org_name")
        .setOrganizationUrl("org_url")
        .setHomepageUrl("homepage_url")
        .setIssueTrackerUrl("issueTracker_url")
        .setImplementationBuild("sou_rev_sha1")
        .setSonarLintSupported(true)
        .setJarFile(new File(getClass().getResource(jarFilename).toURI()))));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("plugKey"),
      p -> p.setHash("abcdplugKey"),
      p -> p.setUpdatedAt(111111L));

    underTest.handle(request, response);

    verifyZeroInteractions(updateCenterMatrixFactory);
    assertJson(response.outputAsString()).isSimilarTo(
      "{" +
        "  \"plugins\":" +
        "  [" +
        "    {" +
        "      \"key\": \"plugKey\"," +
        "      \"name\": \"plugName\"," +
        "      \"description\": \"desc_it\"," +
        "      \"version\": \"1.0\"," +
        "      \"license\": \"license_hey\"," +
        "      \"organizationName\": \"org_name\"," +
        "      \"organizationUrl\": \"org_url\"," +
        "      \"homepageUrl\": \"homepage_url\"," +
        "      \"issueTrackerUrl\": \"issueTracker_url\"," +
        "      \"implementationBuild\": \"sou_rev_sha1\"," +
        "      \"sonarLintSupported\": true," +
        "      \"filename\": \"some.jar\"," +
        "      \"hash\": \"abcdplugKey\"," +
        "      \"updatedAt\": 111111" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void category_is_returned_when_in_additional_fields() throws Exception {
    String jarFilename = getClass().getSimpleName() + "/" + "some.jar";
    when(pluginRepository.getPluginInfos()).thenReturn(of(
      new PluginInfo("plugKey")
        .setName("plugName")
        .setDescription("desc_it")
        .setVersion(Version.create("1.0"))
        .setLicense("license_hey")
        .setOrganizationName("org_name")
        .setOrganizationUrl("org_url")
        .setHomepageUrl("homepage_url")
        .setIssueTrackerUrl("issueTracker_url")
        .setImplementationBuild("sou_rev_sha1")
        .setJarFile(new File(getClass().getResource(jarFilename).toURI()))));
    UpdateCenter updateCenter = mock(UpdateCenter.class);
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.of(updateCenter));
    when(updateCenter.findAllCompatiblePlugins()).thenReturn(
      Arrays.asList(
        Plugin.factory("plugKey")
          .setCategory("cat_1")));

    db.pluginDbTester().insertPlugin(
      p -> p.setKee("plugKey"),
      p -> p.setHash("abcdplugKey"),
      p -> p.setUpdatedAt(111111L));

    when(request.paramAsStrings(Param.FIELDS)).thenReturn(singletonList("category"));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).isSimilarTo(
      "{" +
        "  \"plugins\":" +
        "  [" +
        "    {" +
        "      \"key\": \"plugKey\"," +
        "      \"name\": \"plugName\"," +
        "      \"description\": \"desc_it\"," +
        "      \"version\": \"1.0\"," +
        "      \"category\":\"cat_1\"," +
        "      \"license\": \"license_hey\"," +
        "      \"organizationName\": \"org_name\"," +
        "      \"organizationUrl\": \"org_url\"," +
        "      \"homepageUrl\": \"homepage_url\"," +
        "      \"issueTrackerUrl\": \"issueTracker_url\"," +
        "      \"implementationBuild\": \"sou_rev_sha1\"" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void plugins_are_sorted_by_name_then_key_and_only_one_plugin_can_have_a_specific_name() throws Exception {
    when(pluginRepository.getPluginInfos()).thenReturn(
      of(
        plugin("A", "name2"),
        plugin("B", "name1"),
        plugin("C", "name0"),
        plugin("D", "name0")));

    db.pluginDbTester().insertPlugin(
      p -> p.setKee("A"),
      p -> p.setHash("abcdA"),
      p -> p.setUpdatedAt(111111L));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("B"),
      p -> p.setHash("abcdB"),
      p -> p.setUpdatedAt(222222L));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("C"),
      p -> p.setHash("abcdC"),
      p -> p.setUpdatedAt(333333L));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("D"),
      p -> p.setHash("abcdD"),
      p -> p.setUpdatedAt(444444L));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"plugins\":" +
        "  [" +
        "    {\"key\": \"C\"}" + "," +
        "    {\"key\": \"D\"}" + "," +
        "    {\"key\": \"B\"}" + "," +
        "    {\"key\": \"A\"}" +
        "  ]" +
        "}");
  }

  @Test
  public void only_one_plugin_can_have_a_specific_name_and_key() throws Exception {
    when(pluginRepository.getPluginInfos()).thenReturn(
      of(
        plugin("A", "name2"),
        plugin("A", "name2")));

    db.pluginDbTester().insertPlugin(
      p -> p.setKee("A"),
      p -> p.setHash("abcdA"),
      p -> p.setUpdatedAt(111111L));

    underTest.handle(request, response);

    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"plugins\":" +
        "  [" +
        "    {\"key\": \"A\"}" +
        "  ]" +
        "}");
    assertThat(response.outputAsString()).containsOnlyOnce("name2");
  }

  private PluginInfo plugin(String key, String name) {
    return new PluginInfo(key).setName(name).setVersion(Version.create("1.0")).setJarFile(new File("sonar-" + key + "-plugin-1.0.jar"));
  }

}
