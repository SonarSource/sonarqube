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
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.platform.PluginInfo;
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
  private static final PluginInfo GIT_PLUGIN_METADATA = new PluginInfo("scmgit")
    .setName("Git")
    .setDescription("Git SCM Provider.")
    .setVersion(Version.create("1.0"))
    .setLicense("GNU LGPL 3")
    .setOrganizationName("SonarSource")
    .setOrganizationUrl("http://www.sonarsource.com")
    .setHomepageUrl("http://redirect.sonarsource.com/plugins/scmgit.html")
    .setIssueTrackerUrl("http://jira.codehaus.org/browse/SONARSCGIT")
    .setFile(new File("/home/user/sonar-scm-git-plugin-1.0.jar"));
  private static final Plugin PLUGIN = new Plugin("p_key")
    .setName("p_name")
    .setCategory("p_category")
    .setDescription("p_description")
    .setLicense("p_license")
    .setOrganization("p_orga_name")
    .setOrganizationUrl("p_orga_url")
    .setTermsConditionsUrl("p_t_and_c_url");
  private static final Release RELEASE = new Release(PLUGIN, version("1.0")).setDate(parseDate("2015-04-16"))
    .setDownloadUrl("http://toto.com/file.jar")
    .setDescription("release description")
    .setChangelogUrl("http://change.org/plugin");

  private WsTester.TestResponse response = new WsTester.TestResponse();
  private JsonWriter jsonWriter = response.newJsonWriter();
  private PluginWSCommons underTest = new PluginWSCommons();

  @Test
  public void verify_properties_written_by_writePluginMetadata() {
    underTest.writePluginMetadata(jsonWriter, GIT_PLUGIN_METADATA);

    jsonWriter.close();
    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo("{" +
      "  \"key\": \"scmgit\"," +
      "  \"name\": \"Git\"," +
      "  \"description\": \"Git SCM Provider.\"," +
      "  \"version\": \"1.0\"," +
      "  \"license\": \"GNU LGPL 3\"," +
      "  \"organizationName\": \"SonarSource\"," +
      "  \"organizationUrl\": \"http://www.sonarsource.com\"," +
      "  \"homepage\": \"http://redirect.sonarsource.com/plugins/scmgit.html\"," +
      "  \"issueTrackerUrl\": \"http://jira.codehaus.org/browse/SONARSCGIT\"" +
      "}");
  }

  @Test
  public void verify_properties_written_by_writeMetadata() {
    jsonWriter.beginObject();
    underTest.writeMetadata(jsonWriter, GIT_PLUGIN_METADATA);
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo("{" +
      "  \"key\": \"scmgit\"," +
      "  \"name\": \"Git\"," +
      "  \"description\": \"Git SCM Provider.\"," +
      "  \"version\": \"1.0\"," +
      "  \"license\": \"GNU LGPL 3\"," +
      "  \"organizationName\": \"SonarSource\"," +
      "  \"organizationUrl\": \"http://www.sonarsource.com\"," +
      "  \"homepage\": \"http://redirect.sonarsource.com/plugins/scmgit.html\"," +
      "  \"issueTrackerUrl\": \"http://jira.codehaus.org/browse/SONARSCGIT\"," +
      "}");
  }

  @Test
  public void verify_properties_written_by_writePluginUpdate() {
    underTest.writePluginUpdate(jsonWriter, PluginUpdate.createForPluginRelease(RELEASE, version("1.0")));

    jsonWriter.close();
    assertJson(response.outputAsString()).isSimilarTo("{" +
      "  \"key\": \"p_key\"," +
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
    underTest.writeMetadata(jsonWriter, PLUGIN);
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).isSimilarTo("{" +
      "  \"key\": \"p_key\"," +
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
    underTest.writeRelease(jsonWriter, RELEASE);
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo("{" +
      "  \"release\": {" +
      "     \"version\": \"1.0\"," +
      "     \"date\": \"2015-04-16\"," +
      "     \"description\": \"release description\"," +
      "     \"changeLogUrl\": \"http://change.org/plugin\"" +
      "  }" +
      "}");
  }

  @Test
  public void writeArtifact_from_release_writes_artifact_object_and_file_name() {
    jsonWriter.beginObject();
    underTest.writeArtifact(jsonWriter, release("p_key").setDownloadUrl("http://toto.com/file.jar"));
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo("{" +
      "  \"artifact\": {" +
      "     \"name\": \"file.jar\"," +
      "     \"url\": \"http://toto.com/file.jar\"" +
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
  public void writeUpdate_renders_key_name_and_description_of_outgoing_dependencies() {
    PluginUpdate pluginUpdate = new PluginUpdate();
    pluginUpdate.setRelease(
      new Release(PLUGIN, version("1.0")).addOutgoingDependency(RELEASE)
    );

    jsonWriter.beginObject();
    underTest.writeUpdate(jsonWriter, pluginUpdate);
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).isSimilarTo("{" +
      "  \"update\": {" +
      "    \"requires\": [" +
      "      {" +
      "        \"key\": \"p_key\"," +
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
    return new Release(new Plugin(key), version("1.0"));
  }
}
