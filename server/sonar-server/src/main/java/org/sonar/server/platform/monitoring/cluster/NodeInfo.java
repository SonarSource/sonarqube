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
package org.sonar.server.platform.monitoring.cluster;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

public class NodeInfo {

  private final String name;
  private final Map<String, String> attributes = new LinkedHashMap<>();
  private final List<ProtobufSystemInfo.Section> sections = new ArrayList<>();

  public NodeInfo(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public NodeInfo setAttribute(String key, String value) {
    this.attributes.put(key, value);
    return this;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public NodeInfo addSection(ProtobufSystemInfo.Section section) {
    this.sections.add(section);
    return this;
  }

  public List<ProtobufSystemInfo.Section> getSections() {
    return sections;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NodeInfo nodeInfo = (NodeInfo) o;
    return name.equals(nodeInfo.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
