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
package org.sonar.server.platform.monitoring;

import java.util.Map;
import java.util.TreeMap;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;
import static org.sonar.process.ProcessProperties.Property.AUTH_JWT_SECRET;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

@ServerSide
public class SettingsSection implements SystemInfoSection, Global {

  private static final int MAX_VALUE_LENGTH = 500;
  private static final String PASSWORD_VALUE = "xxxxxxxx";
  private final Settings settings;

  public SettingsSection(Settings settings) {
    this.settings = settings;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("Settings");

    PropertyDefinitions definitions = settings.getDefinitions();
    TreeMap<String, String> orderedProps = new TreeMap<>(settings.getProperties());
    for (Map.Entry<String, String> prop : orderedProps.entrySet()) {
      String key = prop.getKey();
      String value = obfuscateValue(definitions, key, prop.getValue());
      setAttribute(protobuf, key, value);
    }
    return protobuf.build();
  }

  private static String obfuscateValue(PropertyDefinitions definitions, String key, String value) {
    PropertyDefinition def = definitions.get(key);
    if (def != null && def.type() == PropertyType.PASSWORD) {
      return PASSWORD_VALUE;
    }
    if (endsWithIgnoreCase(key, ".secured") ||
      containsIgnoreCase(key, "password") ||
      containsIgnoreCase(key, "passcode") ||
      AUTH_JWT_SECRET.getKey().equals(key)) {
      return PASSWORD_VALUE;
    }
    return abbreviate(value, MAX_VALUE_LENGTH);
  }
}
