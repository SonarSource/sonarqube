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

import org.junit.Test;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.server.ws.WsTester;

import java.io.File;

import static org.sonar.core.plugins.DefaultPluginMetadata.create;
import static org.sonar.test.JsonAssert.assertJson;

public class PluginWSCommonsTest {
  public static final DefaultPluginMetadata GIT_PLUGIN_METADATA = create("scmgit")
      .setName("Git")
      .setDescription("Git SCM Provider.")
      .setVersion("1.0")
      .setLicense("GNU LGPL 3")
      .setOrganization("SonarSource")
      .setOrganizationUrl("http://www.sonarsource.com")
      .setHomepage("http://redirect.sonarsource.com/plugins/scmgit.html")
      .setIssueTrackerUrl("http://jira.codehaus.org/browse/SONARSCGIT")
      .setFile(new File("/home/user/sonar-scm-git-plugin-1.0.jar"));

  private WsTester.TestResponse response = new WsTester.TestResponse();
  private JsonWriter jsonWriter = response.newJsonWriter();
  private PluginWSCommons underTest = new PluginWSCommons();

  @Test
  public void verify_properties_written_by_writePluginMetadata() throws Exception {
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
        "  \"urls\": {" +
        "    \"homepage\": \"http://redirect.sonarsource.com/plugins/scmgit.html\"," +
        "    \"issueTracker\": \"http://jira.codehaus.org/browse/SONARSCGIT\"" +
        "  }," +
        "  \"artifact\": {" +
        "    \"name\": \"sonar-scm-git-plugin-1.0.jar\"" +
        "  }" +
        "}");
  }

  @Test
  public void verify_properties_written_by_writeMetadata() throws Exception {
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
        "}");
  }

  @Test
  public void verify_properties_written_by_writeUrls() throws Exception {
    jsonWriter.beginObject();
    underTest.writeUrls(jsonWriter, GIT_PLUGIN_METADATA);
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo("{" +
        "  \"urls\": {" +
        "    \"homepage\": \"http://redirect.sonarsource.com/plugins/scmgit.html\"," +
        "    \"issueTracker\": \"http://jira.codehaus.org/browse/SONARSCGIT\"" +
        "  }," +
        "}");
  }

  @Test
  public void writeArtifact_supports_null_file() throws Exception {
    jsonWriter.beginObject();
    underTest.writeArtifact(jsonWriter, DefaultPluginMetadata.create("key"));
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo("{}");
  }

  @Test
  public void writeArtifact_writes_artifact_object_and_file_name() throws Exception {
    jsonWriter.beginObject();
    underTest.writeArtifact(jsonWriter, GIT_PLUGIN_METADATA);
    jsonWriter.endObject();

    jsonWriter.close();
    assertJson(response.outputAsString()).setStrictArrayOrder(true).isSimilarTo("{" +
        "  \"artifact\": {" +
        "     \"name\": \"sonar-scm-git-plugin-1.0.jar\"" +
        "  }" +
        "}");
  }
}