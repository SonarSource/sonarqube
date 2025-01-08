/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonArray;
import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.internal.json.JsonValue;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.db.DbTester;
import org.sonar.db.plugin.PluginDto.Type;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;
import org.sonar.server.plugins.ServerPlugin;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

@RunWith(DataProviderRunner.class)
public class InstalledActionIT {
  private static final String JSON_EMPTY_PLUGIN_LIST = "{" +
    "  \"plugins\":" + "[]" +
    "}";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn();

  private UpdateCenterMatrixFactory updateCenterMatrixFactory = mock(UpdateCenterMatrixFactory.class, RETURNS_DEEP_STUBS);
  private ServerPluginRepository serverPluginRepository = mock(ServerPluginRepository.class);
  private InstalledAction underTest = new InstalledAction(serverPluginRepository, userSession, updateCenterMatrixFactory, db.getDbClient());
  private WsActionTester tester = new WsActionTester(underTest);

  @DataProvider
  public static Object[][] editionBundledLicenseValues() {
    return new Object[][] {
      {"sonarsource", "SONARSOURCE"},
      {"SonarSource", "SONARSOURCE"},
      {"SonaRSOUrce", "SonarSource"},
      {"SONARSOURCE", "SonarSource"},
      {"commercial", "SONARSOURCE"},
      {"Commercial", "SONARSOURCE"},
      {"COMMERCIAL", "SonarSource"},
      {"COmmERCiaL", "SonarSource"},
    };
  }

  @Test
  public void action_installed_is_defined() {
    Action action = tester.getDef();

    assertThat(action.isPost()).isFalse();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.responseExample()).isNotNull();
  }

  @Test
  public void empty_array_is_returned_when_there_is_not_plugin_installed() {
    String response = tester.newRequest().execute().getInput();

    assertJson(response).withStrictArrayOrder().isSimilarTo(JSON_EMPTY_PLUGIN_LIST);
  }

  @Test
  public void empty_array_when_update_center_is_unavailable() {
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.empty());

    String response = tester.newRequest().execute().getInput();

    assertJson(response).withStrictArrayOrder().isSimilarTo(JSON_EMPTY_PLUGIN_LIST);
  }

  @Test
  public void filter_by_plugin_type() throws IOException {
    when(serverPluginRepository.getPlugins()).thenReturn(
      Arrays.asList(
        newInstalledPlugin(new PluginInfo("foo-external-1")
          .setName("foo-external-1"),
          PluginType.EXTERNAL),
        newInstalledPlugin(new PluginInfo("foo-bundled-1")
          .setName("foo-bundled-1"),
          PluginType.BUNDLED),
        newInstalledPlugin(new PluginInfo("foo-external-2")
          .setName("foo-external-2"),
          PluginType.EXTERNAL)));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("foo-external-1"),
      p -> p.setType(Type.EXTERNAL),
      p -> p.setUpdatedAt(100L));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("foo-bundled-1"),
      p -> p.setType(Type.BUNDLED),
      p -> p.setUpdatedAt(101L));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("foo-external-2"),
      p -> p.setType(Type.EXTERNAL),
      p -> p.setUpdatedAt(102L));

    // no type param
    String response = tester.newRequest().execute().getInput();

    JsonArray jsonArray = Json.parse(response).asObject().get("plugins").asArray();
    assertThat(jsonArray).hasSize(3);
    assertThat(jsonArray).extracting(JsonValue::asObject)
      .extracting(members -> members.get("key").asString())
      .containsExactlyInAnyOrder("foo-external-1", "foo-bundled-1", "foo-external-2");

    // type param == BUNDLED
    response = tester.newRequest().setParam("type", "BUNDLED").execute().getInput();

    jsonArray = Json.parse(response).asObject().get("plugins").asArray();
    assertThat(jsonArray).hasSize(1);
    assertThat(jsonArray).extracting(JsonValue::asObject)
      .extracting(members -> members.get("key").asString())
      .containsExactlyInAnyOrder("foo-bundled-1");

    // type param == EXTERNAL
    response = tester.newRequest().setParam("type", "EXTERNAL").execute().getInput();

    jsonArray = Json.parse(response).asObject().get("plugins").asArray();
    assertThat(jsonArray).hasSize(2);
    assertThat(jsonArray).extracting(JsonValue::asObject)
      .extracting(members -> members.get("key").asString())
      .containsExactlyInAnyOrder("foo-external-1", "foo-external-2");
  }

  @Test
  public void empty_fields_are_not_serialized_to_json() throws IOException {
    when(serverPluginRepository.getPlugins()).thenReturn(
      singletonList(newInstalledPlugin(new PluginInfo("foo")
        .setName(null)
        .setDescription(null)
        .setLicense(null)
        .setOrganizationName(null)
        .setOrganizationUrl(null)
        .setImplementationBuild(null)
        .setHomepageUrl(null)
        .setIssueTrackerUrl(null))));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("foo"),
      p -> p.setType(Type.EXTERNAL),
      p -> p.setUpdatedAt(100L));

    String response = tester.newRequest().execute().getInput();
    JsonObject json = Json.parse(response).asObject().get("plugins").asArray().get(0).asObject();
    assertThat(json.get("key")).isNotNull();
    assertThat(json.get("name")).isNotNull();
    assertThat(json.get("description")).isNull();
    assertThat(json.get("license")).isNull();
    assertThat(json.get("organizationName")).isNull();
    assertThat(json.get("organizationUrl")).isNull();
    assertThat(json.get("homepageUrl")).isNull();
    assertThat(json.get("issueTrackerUrl")).isNull();
    assertThat(json.get("requiredForLanguage")).isNull();
  }

  private ServerPlugin newInstalledPlugin(PluginInfo plugin) throws IOException {
    return newInstalledPlugin(plugin, PluginType.BUNDLED);
  }

  private ServerPlugin newInstalledPlugin(PluginInfo plugin, PluginType type) throws IOException {
    FileAndMd5 jar = new FileAndMd5(temp.newFile());
    return new ServerPlugin(plugin, type, null, jar, null);
  }

  @Test
  public void return_default_fields() throws Exception {
    ServerPlugin plugin = newInstalledPlugin(new PluginInfo("foo")
      .setName("plugName")
      .setDescription("desc_it")
      .setVersion(Version.create("1.0"))
      .setLicense("license_hey")
      .setOrganizationName("org_name")
      .setOrganizationUrl("org_url")
      .setHomepageUrl("homepage_url")
      .setIssueTrackerUrl("issueTracker_url")
      .setImplementationBuild("sou_rev_sha1")
      .addRequiredForLanguage("bar")
      .setSonarLintSupported(true));
    when(serverPluginRepository.getPlugins()).thenReturn(singletonList(plugin));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee(plugin.getPluginInfo().getKey()),
      p -> p.setType(Type.valueOf(plugin.getType().name())),
      p -> p.setUpdatedAt(100L));

    String response = tester.newRequest().execute().getInput();

    verifyNoMoreInteractions(updateCenterMatrixFactory);
    String expected = String.format("""
      {
        "plugins":
        [
          {
            "key": "foo",
            "name": "plugName",
            "description": "desc_it",
            "version": "1.0",
            "license": "license_hey",
            "organizationName": "org_name",
            "organizationUrl": "org_url",
            "editionBundled": false,
            "homepageUrl": "homepage_url",
            "issueTrackerUrl": "issueTracker_url",
            "implementationBuild": "sou_rev_sha1",
            "sonarLintSupported": true,
            "filename": "%s",
            "hash": "%s",
            "updatedAt": 100,
            "requiredForLanguages": ["bar"]
          }
        ]
      }""", plugin.getJar().getFile().getName(), plugin.getJar().getMd5());
    assertJson(response).isSimilarTo(expected);
  }

  @Test
  public void category_is_returned_when_in_additional_fields() throws Exception {
    String jarFilename = getClass().getSimpleName() + "/" + "some.jar";
    File jar = new File(getClass().getResource(jarFilename).toURI());
    when(serverPluginRepository.getPlugins()).thenReturn(asList(
      newInstalledPlugin(new PluginInfo("plugKey")
        .setName("plugName")
        .setDescription("desc_it")
        .setVersion(Version.create("1.0"))
        .setLicense("license_hey")
        .setOrganizationName("org_name")
        .setOrganizationUrl("org_url")
        .setHomepageUrl("homepage_url")
        .setIssueTrackerUrl("issueTracker_url")
        .setImplementationBuild("sou_rev_sha1"))));
    // .setJarFile(jar), new FileAndMd5(jar), null
    UpdateCenter updateCenter = mock(UpdateCenter.class);
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.of(updateCenter));
    when(updateCenter.findAllCompatiblePlugins()).thenReturn(
      asList(
        Plugin.factory("plugKey")
          .setCategory("cat_1")));

    db.pluginDbTester().insertPlugin(
      p -> p.setKee("plugKey"),
      p -> p.setType(Type.EXTERNAL),
      p -> p.setFileHash("abcdplugKey"),
      p -> p.setUpdatedAt(111111L));

    String response = tester.newRequest()
      .setParam(WebService.Param.FIELDS, "category")
      .execute().getInput();

    assertJson(response).isSimilarTo(
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
        "      \"organizationUrl\": \"org_url\",\n" +
        "      \"editionBundled\": false," +
        "      \"homepageUrl\": \"homepage_url\"," +
        "      \"issueTrackerUrl\": \"issueTracker_url\"," +
        "      \"implementationBuild\": \"sou_rev_sha1\"" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void plugins_are_sorted_by_name_then_key_and_only_one_plugin_can_have_a_specific_name() throws IOException {
    when(serverPluginRepository.getPlugins()).thenReturn(
      asList(
        plugin("A", "name2"),
        plugin("B", "name1"),
        plugin("C", "name0"),
        plugin("D", "name0")));

    db.pluginDbTester().insertPlugin(
      p -> p.setKee("A"),
      p -> p.setType(Type.EXTERNAL),
      p -> p.setFileHash("abcdA"),
      p -> p.setUpdatedAt(111111L));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("B"),
      p -> p.setType(Type.EXTERNAL),
      p -> p.setFileHash("abcdB"),
      p -> p.setUpdatedAt(222222L));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("C"),
      p -> p.setType(Type.EXTERNAL),
      p -> p.setFileHash("abcdC"),
      p -> p.setUpdatedAt(333333L));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee("D"),
      p -> p.setType(Type.EXTERNAL),
      p -> p.setFileHash("abcdD"),
      p -> p.setUpdatedAt(444444L));

    String resp = tester.newRequest().execute().getInput();

    assertJson(resp).withStrictArrayOrder().isSimilarTo(
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
  @UseDataProvider("editionBundledLicenseValues")
  public void commercial_plugins_from_SonarSource_has_flag_editionBundled_true_based_on_jar_info(String license, String organization) throws Exception {
    String jarFilename = getClass().getSimpleName() + "/" + "some.jar";
    String pluginKey = "plugKey";
    File jar = new File(getClass().getResource(jarFilename).toURI());
    when(serverPluginRepository.getPlugins()).thenReturn(asList(
      new ServerPlugin(new PluginInfo(pluginKey)
        .setName("plugName")
        .setVersion(Version.create("1.0"))
        .setLicense(license)
        .setOrganizationName(organization)
        .setImplementationBuild("sou_rev_sha1"),
        PluginType.BUNDLED,
        null,
        new FileAndMd5(jar), null)));
    db.pluginDbTester().insertPlugin(
      p -> p.setKee(pluginKey),
      p -> p.setType(Type.BUNDLED),
      p -> p.setFileHash("abcdplugKey"),
      p -> p.setUpdatedAt(111111L));
    // ensure flag editionBundled is computed from jar info by enabling datacenter with other organization and license values
    UpdateCenter updateCenter = mock(UpdateCenter.class);
    when(updateCenterMatrixFactory.getUpdateCenter(false)).thenReturn(Optional.of(updateCenter));
    when(updateCenter.findAllCompatiblePlugins()).thenReturn(
      singletonList(
        Plugin.factory(pluginKey)
          .setOrganization("foo")
          .setLicense("bar")
          .setCategory("cat_1")));

    String response = tester.newRequest().execute().getInput();

    verifyNoInteractions(updateCenterMatrixFactory);
    assertJson(response)
      .isSimilarTo("{" +
        "  \"plugins\":" +
        "  [" +
        "    {" +
        "      \"key\": \"plugKey\"," +
        "      \"name\": \"plugName\"," +
        "      \"license\": \"" + license + "\"," +
        "      \"organizationName\": \"" + organization + "\"," +
        "      \"editionBundled\": true" +
        "    }" +
        "  ]" +
        "}");
  }

  @Test
  public void only_one_plugin_can_have_a_specific_name_and_key() throws IOException {
    when(serverPluginRepository.getPlugins()).thenReturn(
      asList(
        plugin("A", "name2"),
        plugin("A", "name2")));

    db.pluginDbTester().insertPlugin(
      p -> p.setKee("A"),
      p -> p.setType(Type.EXTERNAL),
      p -> p.setFileHash("abcdA"),
      p -> p.setUpdatedAt(111111L));

    String response = tester.newRequest().execute().getInput();

    assertJson(response).withStrictArrayOrder().isSimilarTo(
      "{" +
        "  \"plugins\":" +
        "  [" +
        "    {\"key\": \"A\"}" +
        "  ]" +
        "}");
    assertThat(response).containsOnlyOnce("name2");
  }

  @Test
  public void fail_if_not_logged_in() {
    userSession.anonymous();
    TestRequest testRequest = tester.newRequest();
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  private ServerPlugin plugin(String key, String name) throws IOException {
    File file = temp.newFile();
    PluginInfo info = new PluginInfo(key)
      .setName(name)
      .setVersion(Version.create("1.0"));
    info.setJarFile(file);
    return new ServerPlugin(info, PluginType.BUNDLED, null, new FileAndMd5(file), null);
  }

}
