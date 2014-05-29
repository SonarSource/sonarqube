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

package org.sonar.server.duplication.ws;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.persistence.ComponentDao;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class DuplicationsWriter implements ServerComponent {

  private final ComponentDao componentDao;

  public DuplicationsWriter(ComponentDao componentDao) {
    this.componentDao = componentDao;
  }

  @VisibleForTesting
  void write(List<DuplicationsParser.Block> blocks, JsonWriter json, DbSession session) {
    Map<String, String> refByComponentKey = newHashMap();
    json.name("duplications").beginArray();
    writeDuplications(blocks, refByComponentKey, json, session);
    json.endArray();

    json.name("files").beginObject();
    writeFiles(refByComponentKey, json, session);
    json.endObject();
  }

  private void writeDuplications(List<DuplicationsParser.Block> blocks, Map<String, String> refByComponentKey, JsonWriter json, DbSession session) {
    for (DuplicationsParser.Block block : blocks) {
      json.beginObject().name("blocks").beginArray();
      for (DuplicationsParser.Duplication duplication : block.duplications()) {
        writeDuplication(refByComponentKey, duplication, json);
      }
      json.endArray().endObject();
    }
  }

  private void writeDuplication(Map<String, String> refByComponentKey, DuplicationsParser.Duplication duplication, JsonWriter json) {
    String componentKey = duplication.file().key();
    String ref = refByComponentKey.get(componentKey);
    if (ref == null) {
      ref = Integer.toString(refByComponentKey.size() + 1);
      refByComponentKey.put(componentKey, ref);
    }

    json.beginObject();
    json.prop("from", duplication.from());
    json.prop("size", duplication.size());
    json.prop("_ref", ref);
    json.endObject();
  }

  private void writeFiles(Map<String, String> refByComponentKey, JsonWriter json, DbSession session) {
    Map<Long, ComponentDto> projectById = newHashMap();
    for (Map.Entry<String, String> entry : refByComponentKey.entrySet()) {
      String componentKey = entry.getKey();
      String ref = entry.getValue();
      ComponentDto file = componentDao.getNullableByKey(session, componentKey);
      if (file != null) {
        json.name(ref).beginObject();
        json.prop("key", file.key());
        json.prop("name", file.longName());
        ComponentDto project = projectById.get(file.projectId());
        if (project == null) {
          project = componentDao.getById(file.projectId(), session);
          projectById.put(file.projectId(), project);
        }
        json.prop("projectName", project != null ? project.longName() : null);
        json.endObject();
      }
    }
  }

}
