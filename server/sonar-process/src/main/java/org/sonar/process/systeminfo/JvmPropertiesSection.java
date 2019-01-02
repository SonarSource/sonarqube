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
package org.sonar.process.systeminfo;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

/**
 * Dumps {@link System#getProperties()}
 */
public class JvmPropertiesSection implements SystemInfoSection {

  private final String name;

  public JvmPropertiesSection(String name) {
    this.name = name;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName(name);

    Map<Object, Object> sortedProperties = new TreeMap<>(System.getProperties());
    for (Map.Entry<Object, Object> systemProp : sortedProperties.entrySet()) {
      if (systemProp.getValue() != null) {
        setAttribute(protobuf, Objects.toString(systemProp.getKey()), Objects.toString(systemProp.getValue()));
      }
    }
    return protobuf.build();
  }
}
