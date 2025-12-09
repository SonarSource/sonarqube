/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.telemetry.legacy;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.telemetry.TelemetryExtension;
import org.sonar.server.util.DigestUtil;

import static org.sonar.api.utils.DateUtils.DATETIME_FORMAT;

public class TelemetryDataJsonWriter {

  @VisibleForTesting
  static final String MANAGED_INSTANCE_PROPERTY = "managedInstanceInformation";
  @VisibleForTesting
  static final String CLOUD_USAGE_PROPERTY = "cloudUsage";

  private static final String LANGUAGE_PROPERTY = "language";
  private static final String VERSION = "version";
  private static final String NCD_ID = "ncdId";
  private static final String PROJECT_ID = "projectUuid";

  private final List<TelemetryExtension> extensions;

  private final System2 system2;

  public TelemetryDataJsonWriter(List<TelemetryExtension> extensions, System2 system2) {
    this.extensions = extensions;
    this.system2 = system2;
  }

  public void writeTelemetryData(JsonWriter json, TelemetryData telemetryData) {
    json.beginObject();
    json.prop("id", telemetryData.getServerId());
    json.prop(VERSION, telemetryData.getVersion());
    json.prop("messageSequenceNumber", telemetryData.getMessageSequenceNumber());
    json.prop("localTimestamp", toUtc(system2.now()));
    json.prop(NCD_ID, telemetryData.getNcdId());
    telemetryData.getEdition().ifPresent(e -> json.prop("edition", e.name().toLowerCase(Locale.ENGLISH)));
    json.prop("defaultQualityGate", telemetryData.getDefaultQualityGate());
    json.prop("sonarway_quality_gate_uuid", telemetryData.getSonarWayQualityGate());
    json.name("database");
    json.beginObject();
    json.prop("name", telemetryData.getDatabase().name());
    json.prop(VERSION, telemetryData.getDatabase().version());
    json.endObject();
    json.name("plugins");
    json.beginArray();
    telemetryData.getPlugins().forEach((plugin, version) -> {
      json.beginObject();
      json.prop("name", plugin);
      json.prop(VERSION, version);
      json.endObject();
    });
    json.endArray();

    if (!telemetryData.getCustomSecurityConfigs().isEmpty()) {
      json.name("customSecurityConfig");
      json.beginArray();
      json.values(telemetryData.getCustomSecurityConfigs());
      json.endArray();
    }

    telemetryData.hasUnanalyzedC().ifPresent(hasUnanalyzedC -> json.prop("hasUnanalyzedC", hasUnanalyzedC));
    telemetryData.hasUnanalyzedCpp().ifPresent(hasUnanalyzedCpp -> json.prop("hasUnanalyzedCpp", hasUnanalyzedCpp));

    if (telemetryData.getInstallationDate() != null) {
      json.prop("installationDate", toUtc(telemetryData.getInstallationDate()));
    }
    if (telemetryData.getInstallationVersion() != null) {
      json.prop("installationVersion", telemetryData.getInstallationVersion());
    }
    json.prop("container", telemetryData.isInContainer());

    writeUserData(json, telemetryData);
    writeProjectData(json, telemetryData);
    writeProjectStatsData(json, telemetryData);
    writeBranches(json, telemetryData);
    writeNewCodeDefinitions(json, telemetryData);
    writeQualityGates(json, telemetryData);
    writeQualityProfiles(json, telemetryData);
    writeManagedInstanceInformation(json, telemetryData.getManagedInstanceInformation());
    writeCloudUsage(json, telemetryData.getCloudUsage());
    extensions.forEach(e -> e.write(json));

    json.endObject();
  }

  private static void writeUserData(JsonWriter json, TelemetryData telemetryData) {
    if (telemetryData.getUserTelemetries() != null) {
      json.name("users");
      json.beginArray();
      telemetryData.getUserTelemetries().forEach(user -> {
        json.beginObject();
        json.prop("userUuid", DigestUtil.sha3_224Hex(user.getUuid()));
        json.prop("status", user.isActive() ? "active" : "inactive");
        json.prop("identityProvider", user.getExternalIdentityProvider());

        if (user.getLastConnectionDate() != null) {
          json.prop("lastActivity", toUtc(user.getLastConnectionDate()));
        }
        if (user.getLastSonarlintConnectionDate() != null) {
          json.prop("lastSonarlintActivity", toUtc(user.getLastSonarlintConnectionDate()));
        }
        json.prop("managed", user.getScimUuid() != null);

        json.endObject();
      });
      json.endArray();
    }
  }

  private static void writeProjectData(JsonWriter json, TelemetryData telemetryData) {
    if (telemetryData.getProjects() != null) {
      json.name("projects");
      json.beginArray();
      telemetryData.getProjects().forEach(project -> {
        json.beginObject();
        json.prop(PROJECT_ID, project.projectUuid());
        if (project.lastAnalysis() != null) {
          json.prop("lastAnalysis", toUtc(project.lastAnalysis()));
        }
        json.prop(LANGUAGE_PROPERTY, project.language());
        json.prop("loc", project.loc());
        json.prop("qualityProfile", project.qualityProfile());
        json.endObject();
      });
      json.endArray();
    }
  }

  private static void writeBranches(JsonWriter json, TelemetryData telemetryData) {
    if (telemetryData.getBranches() != null) {
      json.name("branches");
      json.beginArray();
      telemetryData.getBranches().forEach(branch -> {
        json.beginObject();
        json.prop(PROJECT_ID, branch.projectUuid());
        json.prop("branchUuid", branch.branchUuid());
        json.prop(NCD_ID, branch.ncdId());
        json.prop("greenQualityGateCount", branch.greenQualityGateCount());
        json.prop("analysisCount", branch.analysisCount());
        json.prop("excludeFromPurge", branch.excludeFromPurge());
        json.endObject();
      });
      json.endArray();
    }
  }

  private static void writeNewCodeDefinitions(JsonWriter json, TelemetryData telemetryData) {
    if (telemetryData.getNewCodeDefinitions() != null) {
      json.name("new-code-definitions");
      json.beginArray();
      telemetryData.getNewCodeDefinitions().forEach(ncd -> {
        json.beginObject();
        json.prop(NCD_ID, ncd.hashCode());
        json.prop("type", ncd.type());
        json.prop("value", ncd.value());
        json.prop("scope", ncd.scope());
        json.endObject();
      });
      json.endArray();
    }
  }

  private static void writeProjectStatsData(JsonWriter json, TelemetryData telemetryData) {
    if (telemetryData.getProjectStatistics() != null) {
      json.name("projects-general-stats");
      json.beginArray();
      telemetryData.getProjectStatistics().forEach(project -> {
        json.beginObject();
        json.prop(PROJECT_ID, project.getProjectUuid());
        json.prop("branchCount", project.getBranchCount());
        json.prop("pullRequestCount", project.getPullRequestCount());
        json.prop("qualityGate", project.getQualityGate());
        json.prop("scm", project.getScm());
        json.prop("ci", project.getCi());
        json.prop("devopsPlatform", project.getDevopsPlatform());
        json.prop(NCD_ID, project.getNcdId());
        json.prop("project_creation_method", project.getCreationMethod().name());
        json.prop("monorepo", project.isMonorepo());
        json.prop("is_ai_code_assured", project.isAiCodeAssured());
        project.getBugs().ifPresent(bugs -> json.prop("bugs", bugs));
        project.getVulnerabilities().ifPresent(vulnerabilities -> json.prop("vulnerabilities", vulnerabilities));
        project.getSecurityHotspots().ifPresent(securityHotspots -> json.prop("securityHotspots", securityHotspots));
        project.getTechnicalDebt().ifPresent(technicalDebt -> json.prop("technicalDebt", technicalDebt));
        project.getDevelopmentCost().ifPresent(developmentCost -> json.prop("developmentCost", developmentCost));
        project.getExternalSecurityReportExportedAt().ifPresent(exportedAt -> json.prop("externalSecurityReportExportedAt", exportedAt));
        json.endObject();
      });
      json.endArray();
    }
  }

  private static void writeQualityGates(JsonWriter json, TelemetryData telemetryData) {
    if (telemetryData.getQualityGates() != null) {
      json.name("quality-gates");
      json.beginArray();
      telemetryData.getQualityGates().forEach(qualityGate -> {
        json.beginObject();
        json.prop("uuid", qualityGate.uuid());
        json.prop("caycStatus", qualityGate.caycStatus());
        json.prop("aicaQualified", qualityGate.aicaQualified());
        json.name("conditions");
        json.beginArray();
        qualityGate.conditions().forEach(condition -> {
          json.beginObject();
          json.prop("metric", condition.getMetricKey());
          json.prop("comparison_operator", condition.getOperator().getDbValue());
          json.prop("error_value", condition.getErrorThreshold());
          json.endObject();
        });
        json.endArray();
        json.endObject();
      });
      json.endArray();
    }
  }

  private static void writeQualityProfiles(JsonWriter json, TelemetryData telemetryData) {
    if (telemetryData.getQualityProfiles() != null) {
      json.name("quality-profiles");
      json.beginArray();
      telemetryData.getQualityProfiles().forEach(qualityProfile -> {
        json.beginObject();
        json.prop("uuid", qualityProfile.uuid());
        json.prop("parentUuid", qualityProfile.parentUuid());
        json.prop(LANGUAGE_PROPERTY, qualityProfile.language());
        json.prop("default", qualityProfile.isDefault());
        json.prop("builtIn", qualityProfile.isBuiltIn());
        if (qualityProfile.builtInParent() != null) {
          json.prop("builtInParent", qualityProfile.builtInParent());
        }
        json.prop("rulesOverriddenCount", qualityProfile.rulesOverriddenCount());
        json.prop("rulesActivatedCount", qualityProfile.rulesActivatedCount());
        json.prop("rulesDeactivatedCount", qualityProfile.rulesDeactivatedCount());
        json.endObject();
      });
      json.endArray();
    }
  }
  private static void writeManagedInstanceInformation(JsonWriter json, TelemetryData.ManagedInstanceInformation provider) {
    json.name(MANAGED_INSTANCE_PROPERTY);
    json.beginObject();
    json.prop("isManaged", provider.isManaged());
    json.prop("provider", provider.isManaged() ? provider.provider() : null);
    json.endObject();
  }

  private static void writeCloudUsage(JsonWriter json, TelemetryData.CloudUsage cloudUsage) {
    json.name(CLOUD_USAGE_PROPERTY);
    json.beginObject();
    json.prop("kubernetes", cloudUsage.kubernetes());
    json.prop("kubernetesVersion", cloudUsage.kubernetesVersion());
    json.prop("kubernetesPlatform", cloudUsage.kubernetesPlatform());
    json.prop("kubernetesProvider", cloudUsage.kubernetesProvider());
    json.prop("isHelmAutoscalingEnabled", cloudUsage.isHelmAutoscalingEnabled());
    json.prop("isOnOpenshift", cloudUsage.isOnOpenshift());
    json.prop("officialHelmChart", cloudUsage.officialHelmChart());
    json.prop("containerRuntime", cloudUsage.containerRuntime());
    json.prop("officialImage", cloudUsage.officialImage());
    json.endObject();
  }

  @NotNull
  private static String toUtc(long date) {
    return DateTimeFormatter.ofPattern(DATETIME_FORMAT)
      .withZone(ZoneOffset.UTC)
      .format(Instant.ofEpochMilli(date));
  }

}
