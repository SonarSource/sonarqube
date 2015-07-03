/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.component.ws;

import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.component.ComponentDto;

public class ComponentJsonWriter {

  public void write(JsonWriter json, ComponentDto component, ComponentDto project) {
    json.beginObject()
      .prop("uuid", component.uuid())
      .prop("key", component.key())
      .prop("id", component.getId())
      .prop("qualifier", component.qualifier())
      .prop("name", component.name())
      .prop("longName", component.longName())
      .prop("enabled", component.isEnabled())
      .prop("path", component.path())
      // On a root project, parentProjectId is null but projectId is equal to itself, which make no sense.
      .prop("projectId", (component.projectUuid() != null && component.parentProjectId() != null) ? project.getId() : null)
      // TODO replace with parentProjectId when sonar-ws-client is not used anymore by tests...
      .prop("subProjectId", component.parentProjectId())
      .endObject();
  }
}
