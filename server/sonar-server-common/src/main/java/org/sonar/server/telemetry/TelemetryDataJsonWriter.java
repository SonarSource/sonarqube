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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.utils.text.JsonWriter;

import static org.sonar.api.utils.DateUtils.DATETIME_FORMAT;

public class TelemetryDataJsonWriter {

  public static final String LANGUAGE_PROP = "language";

  public void writeTelemetryData(JsonWriter json, TelemetryData statistics) {
    json.beginObject();
    json.prop("id", statistics.getServerId());
    json.prop("version", statistics.getVersion());
    statistics.getEdition().ifPresent(e -> json.prop("edition", e.name().toLowerCase(Locale.ENGLISH)));
    statistics.getLicenseType().ifPresent(e -> json.prop("licenseType", e));
    json.name("database");
    json.beginObject();
    json.prop("name", statistics.getDatabase().getName());
    json.prop("version", statistics.getDatabase().getVersion());
    json.endObject();
    json.name("plugins");
    json.beginArray();
    statistics.getPlugins().forEach((plugin, version) -> {
      json.beginObject();
      json.prop("name", plugin);
      json.prop("version", version);
      json.endObject();
    });
    json.endArray();

    if (!statistics.getCustomSecurityConfigs().isEmpty()) {
      json.name("customSecurityConfig");
      json.beginArray();
      json.values(statistics.getCustomSecurityConfigs());
      json.endArray();
    }

    statistics.hasUnanalyzedC().ifPresent(hasUnanalyzedC -> json.prop("hasUnanalyzedC", hasUnanalyzedC));
    statistics.hasUnanalyzedCpp().ifPresent(hasUnanalyzedCpp -> json.prop("hasUnanalyzedCpp", hasUnanalyzedCpp));

    json.name("externalAuthProviders");
    json.beginArray();
    statistics.getExternalAuthenticationProviders().forEach(json::value);
    json.endArray();

    if (statistics.getInstallationDate() != null) {
      json.prop("installationDate", statistics.getInstallationDate());
    }
    if (statistics.getInstallationVersion() != null) {
      json.prop("installationVersion", statistics.getInstallationVersion());
    }
    json.prop("docker", statistics.isInDocker());

    writeUserData(json, statistics);
    writeProjectData(json, statistics);
    writeProjectStatsData(json, statistics);

    json.endObject();
  }

  private static void writeUserData(JsonWriter json, TelemetryData statistics) {
    if (statistics.getUserTelemetries() != null) {
      json.name("users");
      json.beginArray();
      statistics.getUserTelemetries().forEach(user -> {
        json.beginObject();
        json.prop("userUuid", user.getUuid());
        json.prop("status", user.isActive() ? "active" : "inactive");

        if (user.getLastConnectionDate() != null) {
          json.prop("lastActivity", toUtc(user.getLastConnectionDate()));
        }
        if (user.getLastSonarlintConnectionDate() != null) {
          json.prop("lastSonarlintActivity", toUtc(user.getLastSonarlintConnectionDate()));
        }

        json.endObject();
      });
      json.endArray();
    }
  }

  private static void writeProjectData(JsonWriter json, TelemetryData statistics) {
    if (statistics.getProjects() != null) {
      json.name("projects");
      json.beginArray();
      statistics.getProjects().forEach(project -> {
        json.beginObject();
        json.prop("projectUuid", project.getProjectUuid());
        if (project.getLastAnalysis() != null) {
          json.prop("lastAnalysis", toUtc(project.getLastAnalysis()));
        }
        json.prop(LANGUAGE_PROP, project.getLanguage());
        json.prop("loc", project.getLoc());
        json.endObject();
      });
      json.endArray();
    }
  }

  private static void writeProjectStatsData(JsonWriter json, TelemetryData statistics) {
    if (statistics.getProjectStatistics() != null) {
      json.name("projects-general-stats");
      json.beginArray();
      statistics.getProjectStatistics().forEach(project -> {
        json.beginObject();
        json.prop("projectUuid", project.getProjectUuid());
        json.prop("branchCount", project.getBranchCount());
        json.prop("pullRequestCount", project.getPullRequestCount());
        json.prop("scm", project.getScm());
        json.prop("ci", project.getCi());
        json.prop("alm", project.getAlm());
        json.endObject();
      });
      json.endArray();
    }
  }

  @NotNull
  private static String toUtc(long date) {
    return DateTimeFormatter.ofPattern(DATETIME_FORMAT)
      .withZone(ZoneOffset.UTC)
      .format(Instant.ofEpochMilli(date));
  }

}
