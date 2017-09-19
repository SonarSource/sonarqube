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
package org.sonar.server.platform.monitoring;

import java.util.Map;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

@ServerSide
public class SettingsSection implements SystemInfoSection, Global {

  static final int MAX_VALUE_LENGTH = 500;
  private final Settings settings;

  public SettingsSection(Settings settings) {
    this.settings = settings;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("Settings");

    PropertyDefinitions definitions = settings.getDefinitions();
    for (Map.Entry<String, String> prop : settings.getProperties().entrySet()) {
      String key = prop.getKey();
      PropertyDefinition def = definitions.get(key);
      if (def == null || def.type() != PropertyType.PASSWORD) {
        setAttribute(protobuf, key, abbreviate(prop.getValue(), MAX_VALUE_LENGTH));
      }
    }
    return protobuf.build();
  }
}
