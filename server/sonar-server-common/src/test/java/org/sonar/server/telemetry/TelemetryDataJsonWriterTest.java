/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.telemetry.TelemetryExtension;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.user.UserTelemetryDto;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.telemetry.TelemetryDataJsonWriter.SCIM_PROPERTY;
import static org.sonar.test.JsonAssert.assertJson;

@RunWith(DataProviderRunner.class)
public class TelemetryDataJsonWriterTest {

  private final Random random = new Random();

  private final TelemetryExtension extension = mock(TelemetryExtension.class);

  private final System2 system2 = mock(System2.class);

  private final TelemetryDataJsonWriter underTest = new TelemetryDataJsonWriter(List.of(extension), system2);

  @Test
  public void write_server_id_version_and_sequence() {
    TelemetryData data = telemetryBuilder().build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"id\": \"" + data.getServerId() + "\"," +
      "  \"version\": \"" + data.getVersion() + "\"," +
      "  \"messageSequenceNumber\": " + data.getMessageSequenceNumber() +
      "}");
  }

  @Test
  public void does_not_write_edition_if_null() {
    TelemetryData data = telemetryBuilder().build();

    String json = writeTelemetryData(data);

    assertThat(json).doesNotContain("edition");
  }

  @Test
  @UseDataProvider("allEditions")
  public void writes_edition_if_non_null(EditionProvider.Edition edition) {
    TelemetryData data = telemetryBuilder()
      .setEdition(edition)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"edition\": \"" + edition.name().toLowerCase(Locale.ENGLISH) + "\"" +
      "}");
  }

  @Test
  public void writes_database() {
    String name = randomAlphabetic(12);
    String version = randomAlphabetic(10);
    TelemetryData data = telemetryBuilder()
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
    TelemetryData data = telemetryBuilder()
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
    TelemetryData data = telemetryBuilder()
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
    TelemetryData data = telemetryBuilder()
      .setInstallationDate(null)
      .build();

    String json = writeTelemetryData(data);

    assertThat(json).doesNotContain("installationDate");
  }

  @Test
  public void write_installation_date_in_utc_format() {
    TelemetryData data = telemetryBuilder()
      .setInstallationDate(1_000L)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"installationDate\":\"1970-01-01T00:00:01+0000\"," +
      "}");
  }

  @Test
  public void does_not_write_installation_version_if_null() {
    TelemetryData data = telemetryBuilder()
      .setInstallationVersion(null)
      .build();

    String json = writeTelemetryData(data);

    assertThat(json).doesNotContain("installationVersion");
  }

  @Test
  public void write_installation_version() {
    String installationVersion = randomAlphabetic(5);
    TelemetryData data = telemetryBuilder()
      .setInstallationVersion(installationVersion)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"installationVersion\":\"" + installationVersion + "\"" +
      "}");
  }

  @Test
  @UseDataProvider("getFeatureFlagEnabledStates")
  public void write_docker_flag(boolean isInDocker) {
    TelemetryData data = telemetryBuilder()
      .setInDocker(isInDocker)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"docker\":" + isInDocker +
      "}");
  }

  @Test
  @UseDataProvider("getFeatureFlagEnabledStates")
  public void write_scim_feature_flag(boolean isScimEnabled) {
    TelemetryData data = telemetryBuilder()
      .setIsScimEnabled(isScimEnabled)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" + format("  \"%s\":", SCIM_PROPERTY) + isScimEnabled + "}");
  }

  @Test
  public void writes_has_unanalyzed_languages() {
    TelemetryData data = telemetryBuilder()
      .setHasUnanalyzedC(true)
      .setHasUnanalyzedCpp(false)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("""
      {
        "hasUnanalyzedC": true,
        "hasUnanalyzedCpp": false,
      }
      """);
  }

  @Test
  public void writes_security_custom_config() {
    TelemetryData data = telemetryBuilder()
      .setCustomSecurityConfigs(Set.of("php", "java"))
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"customSecurityConfig\": [\"php\", \"java\"]" +
      "}");
  }

  @Test
  public void writes_local_timestamp() {
    when(system2.now()).thenReturn(1000L);

    TelemetryData data = telemetryBuilder().build();
    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"localTimestamp\": \"1970-01-01T00:00:01+0000\"" +
      "}");
  }

  @Test
  public void writes_all_users_with_anonymous_md5_uuids() {
    TelemetryData data = telemetryBuilder()
      .setUsers(attachUsers())
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"users\": [" +
      "    {" +
      "      \"userUuid\":\"" + DigestUtils.sha3_224Hex("uuid-0") + "\"," +
      "      \"lastActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"identityProvider\":\"gitlab\"," +
      "      \"lastSonarlintActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"status\":\"active\"" +
      "    }," +
      "    {" +
      "      \"userUuid\":\"" + DigestUtils.sha3_224Hex("uuid-1") + "\"," +
      "      \"lastActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"identityProvider\":\"gitlab\"," +
      "      \"lastSonarlintActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"status\":\"inactive\"" +
      "    }," +
      "    {" +
      "      \"userUuid\":\"" + DigestUtils.sha3_224Hex("uuid-2") + "\"," +
      "      \"lastActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"identityProvider\":\"gitlab\"," +
      "      \"lastSonarlintActivity\":\"1970-01-01T00:00:00+0000\"," +
      "      \"status\":\"active\"" +
      "    }" +
      "  ]" +
      "}");
  }

  @Test
  public void writes_all_projects() {
    TelemetryData data = telemetryBuilder()
      .setProjects(attachProjects())
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"projects\": [" +
      "    {" +
      "      \"projectUuid\": \"uuid-0\"," +
      "      \"lastAnalysis\":\"1970-01-01T00:00:00+0000\"," +
      "      \"language\": \"lang-0\"," +
      "      \"loc\": 2" +
      "    }," +
      "    {" +
      "      \"projectUuid\": \"uuid-1\"," +
      "      \"lastAnalysis\":\"1970-01-01T00:00:00+0000\"," +
      "      \"language\": \"lang-1\"," +
      "      \"loc\": 4" +
      "    }," +
      "    {" +
      "      \"projectUuid\": \"uuid-2\"," +
      "      \"lastAnalysis\":\"1970-01-01T00:00:00+0000\"," +
      "      \"language\": \"lang-2\"," +
      "      \"loc\": 6" +
      "    }" +
      "  ]" +
      "}");
  }

  @Test
  public void writes_all_projects_stats_with_analyzed_languages() {
    TelemetryData data = telemetryBuilder()
      .setProjectStatistics(attachProjectStats())
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("""
      {
        "projects-general-stats": [
          {
            "projectUuid": "uuid-0",
            "branchCount": 2,
            "pullRequestCount": 2,
            "scm": "scm-0",
            "ci": "ci-0",
            "devopsPlatform": "devops-0"
          },
          {
            "projectUuid": "uuid-1",
            "branchCount": 4,
            "pullRequestCount": 4,
            "scm": "scm-1",
            "ci": "ci-1",
            "devopsPlatform": "devops-1"
          },
          {
            "projectUuid": "uuid-2",
            "branchCount": 6,
            "pullRequestCount": 6,
            "scm": "scm-2",
            "ci": "ci-2",
            "devopsPlatform": "devops-2"
          }
        ]
      }
      """
    );
  }

  @Test
  public void writes_all_projects_stats_with_unanalyzed_languages() {
    TelemetryData data = telemetryBuilder()
      .setProjectStatistics(attachProjectStats())
      .build();

    String json = writeTelemetryData(data);
    assertThat(json).doesNotContain("hasUnanalyzedC", "hasUnanalyzedCpp");
  }

  private static TelemetryData.Builder telemetryBuilder() {
    return TelemetryData.builder()
      .setServerId("foo")
      .setVersion("bar")
      .setMessageSequenceNumber(1L)
      .setPlugins(Collections.emptyMap())
      .setDatabase(new TelemetryData.Database("H2", "11"));
  }

  @NotNull
  private static List<UserTelemetryDto> attachUsers() {
    return IntStream.range(0, 3)
      .mapToObj(
        i -> new UserTelemetryDto().setUuid("uuid-" + i).setActive(i % 2 == 0).setLastConnectionDate(1L).setLastSonarlintConnectionDate(2L).setExternalIdentityProvider("gitlab"))
      .collect(Collectors.toList());
  }

  private static List<TelemetryData.Project> attachProjects() {
    return IntStream.range(0, 3).mapToObj(i -> new TelemetryData.Project("uuid-" + i, 1L, "lang-" + i, (i + 1L) * 2L)).collect(Collectors.toList());
  }

  private List<TelemetryData.ProjectStatistics> attachProjectStats() {
    return IntStream.range(0, 3).mapToObj(i -> new TelemetryData.ProjectStatistics("uuid-" + i, (i + 1L) * 2L, (i + 1L) * 2L, "scm-" + i, "ci-" + i, "devops-" + i))
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

  @DataProvider
  public static Set<Boolean> getFeatureFlagEnabledStates() {
    return Set.of(true, false);
  }
}
