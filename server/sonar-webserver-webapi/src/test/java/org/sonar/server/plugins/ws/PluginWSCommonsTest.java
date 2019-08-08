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

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.platform.PluginInfo;
import org.sonar.db.plugin.PluginDto;
import org.sonar.server.plugins.InstalledPlugin;
import org.sonar.server.plugins.InstalledPlugin.FileAndMd5;
import org.sonar.server.ws.WsTester;
import org.sonar.updatecenter.common.Plugin;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.server.plugins.ws.PluginWSCommons.toJSon;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonar.updatecenter.common.PluginUpdate.Status.COMPATIBLE;
import static org.sonar.updatecenter.common.PluginUpdate.Status.DEPENDENCIES_REQUIRE_SONAR_UPGRADE;
import static org.sonar.updatecenter.common.PluginUpdate.Status.INCOMPATIBLE;
import static org.sonar.updatecenter.common.PluginUpdate.Status.REQUIRE_SONAR_UPGRADE;

public class PluginWSCommonsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private WsTester.TestResponse response = new WsTester.TestResponse();
  private JsonWriter jsonWriter = response.newJsonWriter();

  @Test
  public void verify_properties_written_by_writePluginMetadata() {
    PluginWSCommons.writePluginInfo(jsonWriter, gitPluginInfo(), null, null, null);

    jsonWriter.close();
    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo("{" +
      "  \"key\": \"scmgit\"," +
      "  \"name\": \"Git\"," +
      "  \"description\": \"Git SCM Provider.\"," +
      "  \"version\": \"1.0\"," +
      "  \"license\": \"GNU LGPL 3\"," +
      "  \"organizationName\": \"SonarSource\"," +
      "  \"organizationUrl\": \"http://www.sonarsource.com\"," +
      "  \"homepageUrl\": \"https://redirect.sonarsource.com/plugins/scmgit.html\"," +
      "  \"issueTrackerUrl\": \"http://jira.sonarsource.com/browse/SONARSCGIT\"" +
      "}");
  }

  @Test
  public void verify_properties_written_by_writePluginMetadata_with_dto() {
    PluginDto pluginDto = new PluginDto().setUpdatedAt(123456L);
    PluginWSCommons.writePluginInfo(jsonWriter, gitPluginInfo(), null, pluginDto, null);

    jsonWriter.close();
    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo("{" +
      "  \"key\": \"scmgit\"," +
      "  \"name\": \"Git\"," +
      "  \"description\": \"Git SCM Provider.\"," +
      "  \"version\": \"1.0\"," +
      "  \"license\": \"GNU LGPL 3\"," +
      "  \"organizationName\": \"SonarSource\"," +
      "  \"organizationUrl\": \"http://www.sonarsource.com\"," +
      "  \"homepageUrl\": \"https://redirect.sonarsource.com/plugins/scmgit.html\"," +
      "  \"issueTrackerUrl\": \"http://jira.sonarsource.com/browse/SONARSCGIT\"," +
      "  \"updatedAt\": 123456" +
      "}");
  }

  @Test
  public void verify_properties_written_by_writeMetadata_with_compressed_plugin() throws IOException {
    PluginDto dto = new PluginDto().setUpdatedAt(100L);
    FileAndMd5 loadedJar = new FileAndMd5(temp.newFile());
    FileAndMd5 compressedJar = new FileAndMd5(temp.newFile());
    InstalledPlugin installedFile = new InstalledPlugin(gitPluginInfo(), loadedJar, compressedJar);

    PluginWSCommons.writePluginInfo(jsonWriter, gitPluginInfo(), null, dto, installedFile);

    jsonWriter.close();
    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo("{" +
        "  \"key\": \"scmgit\"," +
        "  \"name\": \"Git\"," +
        "  \"description\": \"Git SCM Provider.\"," +
        "  \"version\": \"1.0\"," +
        "  \"license\": \"GNU LGPL 3\"," +
        "  \"organizationName\": \"SonarSource\"," +
        "  \"organizationUrl\": \"http://www.sonarsource.com\"," +
        "  \"homepageUrl\": \"https://redirect.sonarsource.com/plugins/scmgit.html\"," +
        "  \"issueTrackerUrl\": \"http://jira.sonarsource.com/browse/SONARSCGIT\"," +
        "  \"sonarLintSupported\": true," +
        "  \"updatedAt\": 100," +
        "  \"filename\": \"" + loadedJar.getFile().getName() + "\"," +
        "  \"hash\": \"" + loadedJar.getMd5() + "\"" +
      "}");
  }

  @Test
  public void verify_properties_written_by_writeMetadata() {
    PluginWSCommons.writePluginInfo(jsonWriter, gitPluginInfo(), "cat_1", null, null);

    jsonWriter.close();
    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo("{" +
      "  \"key\": \"scmgit\"," +
      "  \"name\": \"Git\"," +
      "  \"description\": \"Git SCM Provider.\"," +
      "  \"version\": \"1.0\"," +
      "  \"license\": \"GNU LGPL 3\"," +
      "  \"category\":\"cat_1\"" +
      "  \"organizationName\": \"SonarSource\"," +
      "  \"organizationUrl\": \"http://www.sonarsource.com\"," +
      "  \"homepageUrl\": \"https://redirect.sonarsource.com/plugins/scmgit.html\"," +
      "  \"issueTrackerUrl\": \"http://jira.sonarsource.com/browse/SONARSCGIT\"," +
      "}");
  }

  @Test
  public void verify_properties_written_by_writePluginUpdate() {
    PluginWSCommons.writePluginUpdate(jsonWriter, PluginUpdate.createForPluginRelease(newRelease(), version("1.0")));

    jsonWriter.close();
    assertJson(response.outputAsString()).isSimilarTo("{" +
      "  \"key\": \"pkey\"," +
      "  \"name\": \"p_name\"," +
      "  \"description\": \"p_description\"," +
      "  \"category\": \"p_category\"," +
      "  \"license\": \"p_license\"," +
      "  \"organizationName\": \"p_orga_name\"," +
      "  \"organizationUrl\": \"p_orga_url\"," +
      "  \"termsAndConditionsUrl\": \"p_t_and_c_url\"" +
      "  \"release\": {" +
      "     \"version\": \"1.0\"," +
      "     \"date\": \"2015-04-16\"" +
      "  }" +
      "}");
  }

  @Test
  public void verify_properties_written_by_writeMetadata_from_plugin() {
    jsonWriter.beginObject();
    PluginWSCommons.writePlugin(jsonWriter, newPlugin());
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).isSimilarTo("{" +
      "  \"key\": \"pkey\"," +
      "  \"name\": \"p_name\"," +
      "  \"description\": \"p_description\"," +
      "  \"category\": \"p_category\"," +
      "  \"license\": \"p_license\"," +
      "  \"organizationName\": \"p_orga_name\"," +
      "  \"organizationUrl\": \"p_orga_url\"," +
      "  \"termsAndConditionsUrl\": \"p_t_and_c_url\"" +
      "}");
  }

  @Test
  public void writeRelease() {
    jsonWriter.beginObject();
    PluginWSCommons.writeRelease(jsonWriter, newRelease().setDisplayVersion("1.0 (build 42)"));
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).withStrictArrayOrder().isSimilarTo("{" +
      "  \"release\": {" +
      "     \"version\": \"1.0 (build 42)\"," +
      "     \"date\": \"2015-04-16\"," +
      "     \"description\": \"release description\"," +
      "     \"changeLogUrl\": \"http://change.org/plugin\"" +
      "  }" +
      "}");
  }
  @Test
  public void status_COMPATIBLE_is_displayed_COMPATIBLE_in_JSON() {
    assertThat(toJSon(COMPATIBLE)).isEqualTo("COMPATIBLE");
  }

  @Test
  public void status_INCOMPATIBLE_is_displayed_INCOMPATIBLE_in_JSON() {
    assertThat(toJSon(INCOMPATIBLE)).isEqualTo("INCOMPATIBLE");
  }

  @Test
  public void status_REQUIRE_SONAR_UPGRADE_is_displayed_REQUIRES_UPGRADE_in_JSON() {
    assertThat(toJSon(REQUIRE_SONAR_UPGRADE)).isEqualTo("REQUIRES_SYSTEM_UPGRADE");
  }

  @Test
  public void status_DEPENDENCIES_REQUIRE_SONAR_UPGRADE_is_displayed_DEPS_REQUIRE_SYSTEM_UPGRADE_in_JSON() {
    assertThat(toJSon(DEPENDENCIES_REQUIRE_SONAR_UPGRADE)).isEqualTo("DEPS_REQUIRE_SYSTEM_UPGRADE");
  }

  @Test
  public void writeUpdate_renders_key_name_and_description_of_requirements() {
    PluginUpdate pluginUpdate = new PluginUpdate();
    pluginUpdate.setRelease(
      new Release(newPlugin(), version("1.0")).addOutgoingDependency(newRelease()));

    jsonWriter.beginObject();
    PluginWSCommons.writeUpdate(jsonWriter, pluginUpdate);
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).isSimilarTo("{" +
      "  \"update\": {" +
      "    \"requires\": [" +
      "      {" +
      "        \"key\": \"pkey\"," +
      "        \"name\": \"p_name\"," +
      "        \"description\": \"p_description\"" +
      "      }" +
      "   ]" +
      "  }" +
      "}");
  }

  private static Version version(String version) {
    return Version.create(version);
  }

  private static Release release(String key) {
    return new Release(Plugin.factory(key), version("1.0"));
  }

  private PluginInfo gitPluginInfo() {
    return new PluginInfo("scmgit")
      .setName("Git")
      .setDescription("Git SCM Provider.")
      .setVersion(Version.create("1.0"))
      .setLicense("GNU LGPL 3")
      .setOrganizationName("SonarSource")
      .setOrganizationUrl("http://www.sonarsource.com")
      .setHomepageUrl("https://redirect.sonarsource.com/plugins/scmgit.html")
      .setIssueTrackerUrl("http://jira.sonarsource.com/browse/SONARSCGIT")
      .setSonarLintSupported(true)
      .setJarFile(new File("sonar-scm-git-plugin-1.0.jar"));
  }

  private Plugin newPlugin() {
    return Plugin.factory("pkey")
      .setName("p_name")
      .setCategory("p_category")
      .setDescription("p_description")
      .setLicense("p_license")
      .setOrganization("p_orga_name")
      .setOrganizationUrl("p_orga_url")
      .setTermsConditionsUrl("p_t_and_c_url");
  }

  private Release newRelease() {
    return new Release(newPlugin(), version("1.0")).setDate(parseDate("2015-04-16"))
      .setDownloadUrl("http://toto.com/file.jar")
      .setDescription("release description")
      .setChangelogUrl("http://change.org/plugin");
  }

}
