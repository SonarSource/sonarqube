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
package org.sonar.server.telemetry;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.measure.index.ProjectMeasuresStatistics;

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
    .setProjectMeasuresStatistics(ProjectMeasuresStatistics.builder()
      .setProjectCount(12)
      .setProjectCountByLanguage(Collections.emptyMap())
      .setNclocByLanguage(Collections.emptyMap())
      .build())
    .setNcloc(42L)
    .setDatabase(new TelemetryData.Database("H2", "11"))
    .setUsingBranches(true);

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
  public void write_user_count() {
    int userCount = random.nextInt(590);
    TelemetryData data = SOME_TELEMETRY_DATA
      .setUserCount(userCount)
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"userCount\": " + userCount +
      "}");
  }

  @Test
  public void write_project_count_and_ncloc_and_no_stat_by_language() {
    int projectCount = random.nextInt(8909);
    TelemetryData data = SOME_TELEMETRY_DATA
      .setProjectMeasuresStatistics(ProjectMeasuresStatistics.builder()
        .setProjectCount(projectCount)
        .setProjectCountByLanguage(Collections.emptyMap())
        .setNclocByLanguage(Collections.emptyMap())
        .build())
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"projectCount\": " + projectCount + "," +
      "  \"projectCountByLanguage\": []," +
      "  \"nclocByLanguage\": []" +
      "}");
  }

  @Test
  public void write_project_stats_by_language() {
    int projectCount = random.nextInt(8909);
    Map<String, Long> countByLanguage = IntStream.range(0, 1 + random.nextInt(10))
      .boxed()
      .collect(MoreCollectors.uniqueIndex(i -> "P" + i, i -> 100L + i));
    Map<String, Long> nclocByLanguage = IntStream.range(0, 1 + random.nextInt(10))
      .boxed()
      .collect(MoreCollectors.uniqueIndex(i -> "P" + i, i -> 1_000L + i));
    TelemetryData data = SOME_TELEMETRY_DATA
      .setProjectMeasuresStatistics(ProjectMeasuresStatistics.builder()
        .setProjectCount(projectCount)
        .setProjectCountByLanguage(countByLanguage)
        .setNclocByLanguage(nclocByLanguage)
        .build())
      .build();

    String json = writeTelemetryData(data);

    assertJson(json).isSimilarTo("{" +
      "  \"projectCount\": " + projectCount + "," +
      "  \"projectCountByLanguage\": " +
      "[" +
      countByLanguage.entrySet().stream().map(e -> "{\"language\":\"" + e.getKey() + "\",\"count\":" + e.getValue() + "}").collect(joining()) +
      "]," +
      "  \"nclocByLanguage\": " +
      "[" +
      nclocByLanguage.entrySet().stream().map(e -> "{\"language\":\"" + e.getKey() + "\",\"ncloc\":" + e.getValue() + "}").collect(joining()) +
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
