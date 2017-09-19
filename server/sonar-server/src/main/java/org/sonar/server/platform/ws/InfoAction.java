/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.ws;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.ce.http.CeHttpClient;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.telemetry.TelemetryDataLoader;
import org.sonar.server.user.UserSession;

import static java.util.Arrays.stream;
import static org.sonar.server.telemetry.TelemetryDataJsonWriter.writeTelemetryData;

/**
 * Implementation of the {@code info} action for the System WebService.
 */
public class InfoAction extends BaseInfoWsAction {

  private final CeHttpClient ceHttpClient;
  private final SystemInfoSection[] systemInfoSections;
  private final HealthChecker healthChecker;
  private final TelemetryDataLoader statistics;

  public InfoAction(UserSession userSession, CeHttpClient ceHttpClient, HealthChecker healthChecker, TelemetryDataLoader statistics,
    SystemInfoSection... systemInfoSections) {
    super(userSession);
    this.ceHttpClient = ceHttpClient;
    this.healthChecker = healthChecker;
    this.statistics = statistics;
    this.systemInfoSections = systemInfoSections;
  }

  @Override
  protected void doHandle(Request request, Response response) {
    try (JsonWriter json = response.newJsonWriter()) {
      writeJson(json);
    }
  }

  private void writeJson(JsonWriter json) {
    json.beginObject();

    writeHealthToJson(json);
    List<ProtobufSystemInfo.Section> sections = stream(systemInfoSections)
      .map(SystemInfoSection::toProtobuf)
      .collect(MoreCollectors.toArrayList());
    ceHttpClient.retrieveSystemInfo()
      .ifPresent(ce -> sections.addAll(ce.getSectionsList()));

    writeSectionsToJson(sections, json);
    writeStatisticsToJson(json);

    json.endObject();
  }

  private void writeHealthToJson(JsonWriter json) {
    Health health = healthChecker.checkNode();
    json.prop("Health", health.getStatus().name());
    json.name("Health Causes").beginArray().values(health.getCauses()).endArray();
  }

  private void writeStatisticsToJson(JsonWriter json) {
    json.name("Statistics");
    writeTelemetryData(json, statistics.load());
  }

}
