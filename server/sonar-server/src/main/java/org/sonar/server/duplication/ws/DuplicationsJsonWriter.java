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

public class DuplicationsJsonWriter implements ServerComponent {

  private final ComponentDao componentDao;

  public DuplicationsJsonWriter(ComponentDao componentDao) {
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
    String ref = null;
    ComponentDto componentDto = duplication.file();
    if (componentDto != null) {
      String componentKey = componentDto.key();
      ref = refByComponentKey.get(componentKey);
      if (ref == null) {
        ref = Integer.toString(refByComponentKey.size() + 1);
        refByComponentKey.put(componentKey, ref);
      }
    }

    json.beginObject();
    json.prop("from", duplication.from());
    json.prop("size", duplication.size());
    json.prop("_ref", ref);
    json.endObject();
  }

  private void writeFiles(Map<String, String> refByComponentKey, JsonWriter json, DbSession session) {
    Map<Long, ComponentDto> projectsById = newHashMap();
    Map<Long, ComponentDto> subProjectsById = newHashMap();
    for (Map.Entry<String, String> entry : refByComponentKey.entrySet()) {
      String componentKey = entry.getKey();
      String ref = entry.getValue();
      ComponentDto file = componentDao.getNullableByKey(session, componentKey);
      if (file != null) {
        json.name(ref).beginObject();
        json.prop("key", file.key());
        json.prop("name", file.longName());

        Long projectId = file.projectId();
        ComponentDto project = projectsById.get(file.projectId());
        if (project == null && projectId != null) {
          project = componentDao.getById(projectId, session);
          if (project != null) {
            projectsById.put(projectId, project);
          }
        }

        Long subProjectId = file.subProjectId();
        ComponentDto subProject = subProjectsById.get(subProjectId);
        if (subProject == null && subProjectId != null) {
          subProject = componentDao.getById(subProjectId, session);
          if (subProject != null) {
            subProjectsById.put(subProject.getId(), subProject);
          }
        }

        if (project != null) {
          json.prop("project", project.key());
          json.prop("projectName", project.longName());

          // Do not return sub project if sub project and project are the same
          boolean displaySubProject = subProject != null && !subProject.getId().equals(project.getId());
          if (displaySubProject) {
            json.prop("subProject", subProject.key());
            json.prop("subProjectName", subProject.longName());
          }
        }

        json.endObject();
      }
    }
  }

}
