/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.ce.http.CeHttpClient;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.telemetry.TelemetryDataLoader;

import static java.util.Arrays.stream;

public class StandaloneSystemInfoWriter extends SystemInfoWriter {
  private final CeHttpClient ceHttpClient;
  private final HealthChecker healthChecker;
  private final SystemInfoSection[] systemInfoSections;

  public StandaloneSystemInfoWriter(TelemetryDataLoader telemetry, CeHttpClient ceHttpClient, HealthChecker healthChecker, SystemInfoSection... systemInfoSections) {
    super(telemetry);
    this.ceHttpClient = ceHttpClient;
    this.healthChecker = healthChecker;
    this.systemInfoSections = systemInfoSections;
  }

  @Override
  public void write(JsonWriter json) {
    writeHealth(json);

    List<ProtobufSystemInfo.Section> sections = stream(systemInfoSections)
      .map(SystemInfoSection::toProtobuf)
      .collect(MoreCollectors.toArrayList());
    ceHttpClient.retrieveSystemInfo()
      .ifPresent(ce -> sections.addAll(ce.getSectionsList()));

    writeSections(sections, json);
    writeTelemetry(json);
  }

  private void writeHealth(JsonWriter json) {
    Health health = healthChecker.checkNode();
    writeHealth(health, json);
  }
}
