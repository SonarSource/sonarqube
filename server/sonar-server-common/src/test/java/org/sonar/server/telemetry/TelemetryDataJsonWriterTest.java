/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.telemetry;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.user.UserTelemetryDto;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.test.JsonAssert.assertJson;

@RunWith(DataProviderRunner.class)
public class TelemetryDataJsonWriterTest {

  private static final TelemetryData.Builder SOME_TELEMETRY_DATA = TelemetryData.builder()
    .setServerId("foo")
    .setVersion("bar")
    .setPlugins(Collections.emptyMap())
    .setExternalAuthenticationProviders(asList("github", "gitlab"))
    .setDatabase(new TelemetryData.Database("H2", "11"));

  private final Random random = new Random();

  private final TelemetryDataJsonWriter underTest = new TelemetryDataJsonWriter();

  @Test
  public void write_server_id_and_version() {
    TelemetryData data = SOME_TELEMETRY_DATA.build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"id\": \"" + data.getServerId() + "\"," +
      "  \"version\": \"" + data.getVersion() + "\"" +
      "}");
  }

  @Test
  public void does_not_write_edition_if_null() {
    TelemetryData data = SOME_TELEMETRY_DATA.build();

    String json = writeTelemetryData(data);

    assertThat(json).doesNotContain("edition");
  }

  @Test
  public void write_external_auth_providers() {
    TelemetryData data = SOME_TELEMETRY_DATA.build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{ \"externalAuthProviders\": [ \"github\", \"gitlab\" ] }");
  }

  @Test
  @UseDataProvider("allEditions")
  public void writes_edition_if_non_null(EditionProvider.Edition edition) {
    TelemetryData data = SOME_TELEMETRY_DATA
      .setEdition(edition)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"edition\": \"" + edition.name().toLowerCase(Locale.ENGLISH) + "\"" +
      "}");
  }

  @Test
  public void does_not_write_license_type_if_null() {
    TelemetryData data = SOME_TELEMETRY_DATA.build();

    String json = writeTelemetryData(data);

    assertThat(json).doesNotContain("licenseType");
  }

  @Test
  public void writes_licenseType_if_non_null() {
    String expected = randomAlphabetic(12);
    TelemetryData data = SOME_TELEMETRY_DATA
      .setLicenseType(expected)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"licenseType\": \"" + expected + "\"" +
      "}");
  }

  @Test
  public void writes_database() {
    String name = randomAlphabetic(12);
    String version = randomAlphabetic(10);
    TelemetryData data = SOME_TELEMETRY_DATA
      .setDatabase(new TelemetryData.Database(name, version))
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"database\": {" +
      "    \"name\": \"" + name + "\"," +
      "    \"version\": \"" + version + "\"" +
      "  }" +
      "}");
  }

  @Test
  public void writes_no_plugins() {
    TelemetryData data = SOME_TELEMETRY_DATA
      .setPlugins(Collections.emptyMap())
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"plugins\": []" +
      "}");
  }

  @Test
  public void writes_all_plugins() {
    Map<String, String> plugins = IntStream.range(0, 1 + random.nextInt(10))
      .boxed()
      .collect(MoreCollectors.uniqueIndex(i -> "P" + i, i -> "V" + i));
    TelemetryData data = SOME_TELEMETRY_DATA
      .setPlugins(plugins)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"plugins\": " +
      "[" +
      plugins.entrySet().stream().map(e -> "{\"name\":\"" + e.getKey() + "\",\"version\":\"" + e.getValue() + "\"}").collect(joining(",")) +
      "]" +
      "}");
  }

  @Test
  public void does_not_write_installation_date_if_null() {
    TelemetryData data = SOME_TELEMETRY_DATA
      .setInstallationDate(null)
      .build();

    String json = writeTelemetryData(data);

    assertThat(json).doesNotContain("installationDate");
  }

  @Test
  public void write_installation_date() {
    long installationDate = random.nextInt(590);
    TelemetryData data = SOME_TELEMETRY_DATA
      .setInstallationDate(installationDate)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"installationDate\": " + installationDate +
      "}");
  }

  @Test
  public void does_not_write_installation_version_if_null() {
    TelemetryData data = SOME_TELEMETRY_DATA
      .setInstallationVersion(null)
      .build();

    String json = writeTelemetryData(data);

    assertThat(json).doesNotContain("installationVersion");
  }

  @Test
  public void write_installation_version() {
    String installationVersion = randomAlphabetic(5);
    TelemetryData data = SOME_TELEMETRY_DATA
      .setInstallationVersion(installationVersion)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"installationVersion\":\"" + installationVersion + "\"" +
      "}");
  }

  @Test
  public void write_docker_flag() {
    boolean inDocker = random.nextBoolean();
    TelemetryData data = SOME_TELEMETRY_DATA
      .setInDocker(inDocker)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"docker\":" + inDocker +
      "}");
  }

  @Test
  public void writes_has_unanalyzed_languages() {
    TelemetryData data = SOME_TELEMETRY_DATA
      .setHasUnanalyzedC(true)
      .setHasUnanalyzedCpp(false)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"hasUnanalyzedC\":true," +
      "  \"hasUnanalyzedCpp\":false," +
      "}");
  }

  @Test
  public void writes_security_custom_config() {
    TelemetryData data = SOME_TELEMETRY_DATA
      .setCustomSecurityConfigs(Arrays.asList("php", "java"))
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"customSecurityConfig\": [\"php\", \"java\"]" +
      "}");
  }

  @Test
  public void writes_all_users() {
    TelemetryData data = SOME_TELEMETRY_DATA
      .setUsers(getUsers())
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"users\": [" +
      "    {" +
      "      \"userUuid\":\"uuid-0\"," +
      "      \"lastActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"lastSonarlintActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"status\":\"active\"" +
      "    }," +
      "    {" +
      "      \"userUuid\":\"uuid-1\"," +
      "      \"lastActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"lastSonarlintActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"status\":\"inactive\"" +
      "    }," +
      "    {" +
      "      \"userUuid\":\"uuid-2\"," +
      "      \"lastActivity\":\"1970-01-01T01:00:00+0100\"," +
      "      \"lastSonarlintActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"status\":\"active\"" +
      "    }" +
      "  ]" +
      "}");
  }

  @Test
  public void writes_all_projects() {
    TelemetryData data = SOME_TELEMETRY_DATA
      .setProjects(getProjects())
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"projects\": [" +
      "    {" +
      "      \"projectUuid\": \"uuid-0\"," +
      "      \"language\": \"lang-0\"," +
      "      \"lastAnalysis\":\"1970-01-01T00:00:00+0000\"," +
      "      \"loc\": 2" +
      "    }," +
      "    {" +
      "      \"projectUuid\": \"uuid-1\"," +
      "      \"language\": \"lang-1\"," +
      "      \"lastAnalysis\":\"1970-01-01T00:00:00+0000\"," +
      "      \"loc\": 4" +
      "    }," +
      "    {" +
      "      \"projectUuid\": \"uuid-2\"," +
      "      \"language\": \"lang-2\"," +
      "      \"lastAnalysis\":\"1970-01-01T00:00:00+0000\"," +
      "      \"loc\": 6" +
      "    }" +
      "  ]" +
      "}");
  }

  @Test
  public void writes_all_projects_stats() {
    TelemetryData data = SOME_TELEMETRY_DATA
      .setProjectStatistics(getProjectStats())
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"projects-general-stats\": [" +
      "    {" +
      "      \"projectUuid\": \"uuid-0\"," +
      "      \"branchCount\": 2," +
      "      \"pullRequestCount\": 2," +
      "      \"scm\": \"scm-0\"," +
      "      \"ci\": \"ci-0\"," +
      "      \"alm\": \"alm-0\"" +
      "    }," +
      "    {" +
      "      \"projectUuid\": \"uuid-1\"," +
      "      \"branchCount\": 4," +
      "      \"pullRequestCount\": 4," +
      "      \"scm\": \"scm-1\"," +
      "      \"ci\": \"ci-1\"," +
      "      \"alm\": \"alm-1\"" +
      "    }," +
      "    {" +
      "      \"projectUuid\": \"uuid-2\"," +
      "      \"branchCount\": 6," +
      "      \"pullRequestCount\": 6," +
      "      \"scm\": \"scm-2\"," +
      "      \"ci\": \"ci-2\"," +
      "      \"alm\": \"alm-2\"" +
      "    }" +
      "  ]" +
      "}");
  }

  @NotNull
  private static List<UserTelemetryDto> getUsers() {
    return IntStream.range(0, 3).mapToObj(i -> new UserTelemetryDto().setUuid("uuid-" + i).setActive(i % 2 == 0).setLastConnectionDate(1L).setLastSonarlintConnectionDate(2L))
      .collect(Collectors.toList());
  }

  private static List<TelemetryData.Project> getProjects() {
    return IntStream.range(0, 3).mapToObj(i -> new TelemetryData.Project("uuid-" + i, 1L, "lang-" + i, (i + 1L) * 2L)).collect(Collectors.toList());
  }

  private List<TelemetryData.ProjectStatistics> getProjectStats() {
    return IntStream.range(0, 3).mapToObj(i -> new TelemetryData.ProjectStatistics("uuid-" + i, (i + 1L) * 2L, (i + 1L) * 2L, "scm-" + i, "ci-" + i, "alm-" + i))
      .collect(Collectors.toList());
  }

  @DataProvider
  public static Object[][] allEditions() {
    return Arrays.stream(EditionProvider.Edition.values())
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  private String writeTelemetryData(TelemetryData data) {
    StringWriter jsonString = new StringWriter();
    try (JsonWriter json = JsonWriter.of(jsonString)) {
      underTest.writeTelemetryData(json, data);
    }
    return jsonString.toString();
  }
}
