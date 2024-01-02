/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.telemetry.TelemetryExtension;

import static org.sonar.api.utils.DateUtils.DATETIME_FORMAT;

public class TelemetryDataJsonWriter {

  @VisibleForTesting
  static final String SCIM_PROPERTY = "scim";

  private static final String LANGUAGE_PROPERTY = "language";

  private final List<TelemetryExtension> extensions;

  private final System2 system2;

  public TelemetryDataJsonWriter(List<TelemetryExtension> extensions, System2 system2) {
    this.extensions = extensions;
    this.system2 = system2;
  }

  public void writeTelemetryData(JsonWriter json, TelemetryData statistics) {
    json.beginObject();
    json.prop("id", statistics.getServerId());
    json.prop("version", statistics.getVersion());
    json.prop("messageSequenceNumber", statistics.getMessageSequenceNumber());
    json.prop("localTimestamp", toUtc(system2.now()));
    statistics.getEdition().ifPresent(e -> json.prop("edition", e.name().toLowerCase(Locale.ENGLISH)));
    json.prop("defaultQualityGate", statistics.getDefaultQualityGate());
    json.name("database");
    json.beginObject();
    json.prop("name", statistics.getDatabase().name());
    json.prop("version", statistics.getDatabase().version());
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

    if (statistics.getInstallationDate() != null) {
      json.prop("installationDate", toUtc(statistics.getInstallationDate()));
    }
    if (statistics.getInstallationVersion() != null) {
      json.prop("installationVersion", statistics.getInstallationVersion());
    }
    json.prop("docker", statistics.isInDocker());

    json.prop(SCIM_PROPERTY, statistics.isScimEnabled());

    writeUserData(json, statistics);
    writeProjectData(json, statistics);
    writeProjectStatsData(json, statistics);
    writeQualityGates(json, statistics);

    extensions.forEach(e -> e.write(json));

    json.endObject();
  }

  private static void writeUserData(JsonWriter json, TelemetryData statistics) {
    if (statistics.getUserTelemetries() != null) {
      json.name("users");
      json.beginArray();
      statistics.getUserTelemetries().forEach(user -> {
        json.beginObject();
        json.prop("userUuid", DigestUtils.sha3_224Hex(user.getUuid()));
        json.prop("status", user.isActive() ? "active" : "inactive");
        json.prop("identityProvider", user.getExternalIdentityProvider());

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
        json.prop("projectUuid", project.projectUuid());
        if (project.lastAnalysis() != null) {
          json.prop("lastAnalysis", toUtc(project.lastAnalysis()));
        }
        json.prop(LANGUAGE_PROPERTY, project.language());
        json.prop("loc", project.loc());
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
        json.prop("qualityGate", project.getQualityGate());
        json.prop("scm", project.getScm());
        json.prop("ci", project.getCi());
        json.prop("devopsPlatform", project.getDevopsPlatform());
        project.getBugs().ifPresent(bugs -> json.prop("bugs", bugs));
        project.getVulnerabilities().ifPresent(vulnerabilities -> json.prop("vulnerabilities", vulnerabilities));
        project.getSecurityHotspots().ifPresent(securityHotspots -> json.prop("securityHotspots", securityHotspots));
        project.getTechnicalDebt().ifPresent(technicalDebt -> json.prop("technicalDebt", technicalDebt));
        project.getDevelopmentCost().ifPresent(developmentCost -> json.prop("developmentCost", developmentCost));
        json.endObject();
      });
      json.endArray();
    }
  }

  private static void writeQualityGates(JsonWriter json, TelemetryData statistics) {
    if (statistics.getQualityGates() != null) {
      json.name("quality-gates");
      json.beginArray();
      statistics.getQualityGates().forEach(qualityGate -> {
        json.beginObject();
        json.prop("uuid", qualityGate.uuid());
        json.prop("caycStatus", qualityGate.caycStatus());
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
