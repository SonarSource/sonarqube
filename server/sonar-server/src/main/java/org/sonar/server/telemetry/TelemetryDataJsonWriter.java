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

import java.util.Locale;
import org.sonar.api.utils.text.JsonWriter;

import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;

public class TelemetryDataJsonWriter {
  private TelemetryDataJsonWriter() {
    // static methods
  }

  public static void writeTelemetryData(JsonWriter json, TelemetryData statistics) {
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
    json.prop("userCount", statistics.getUserCount());
    json.prop("projectCount", statistics.getProjectCount());
    json.prop("usingBranches", statistics.isUsingBranches());
    json.prop(NCLOC_KEY, statistics.getNcloc());
    json.name("projectCountByLanguage");
    json.beginArray();
    statistics.getProjectCountByLanguage().forEach((language, count) -> {
      json.beginObject();
      json.prop("language", language);
      json.prop("count", count);
      json.endObject();
    });
    json.endArray();
    json.name("nclocByLanguage");
    json.beginArray();
    statistics.getNclocByLanguage().forEach((language, ncloc) -> {
      json.beginObject();
      json.prop("language", language);
      json.prop("ncloc", ncloc);
      json.endObject();
    });
    json.endArray();
    json.endObject();
  }
}
