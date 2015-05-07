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
import org.sonar.api.ServerSide;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.db.ComponentDao;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

@ServerSide
public class DuplicationsJsonWriter {

  private final ComponentDao componentDao;

  public DuplicationsJsonWriter(ComponentDao componentDao) {
    this.componentDao = componentDao;
  }

  @VisibleForTesting
  void write(List<DuplicationsParser.Block> blocks, JsonWriter json, DbSession session) {
    Map<String, String> refByComponentKey = newHashMap();
    json.name("duplications").beginArray();
    writeDuplications(blocks, refByComponentKey, json);
    json.endArray();

    json.name("files").beginObject();
    writeFiles(refByComponentKey, json, session);
    json.endObject();
  }

  private void writeDuplications(List<DuplicationsParser.Block> blocks, Map<String, String> refByComponentKey, JsonWriter json) {
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
    Map<String, ComponentDto> projectsByUuid = newHashMap();
    Map<Long, ComponentDto> parentProjectsById = newHashMap();
    for (Map.Entry<String, String> entry : refByComponentKey.entrySet()) {
      String componentKey = entry.getKey();
      String ref = entry.getValue();
      ComponentDto file = componentDao.getNullableByKey(session, componentKey);
      if (file != null) {
        json.name(ref).beginObject();

        addFile(json, file);
        ComponentDto project = getProject(file.projectUuid(), projectsByUuid, session);
        ComponentDto parentProject = getParentProject(file.parentProjectId(), parentProjectsById, session);
        addProject(json, project, parentProject);

        json.endObject();
      }
    }
  }

  private void addFile(JsonWriter json, ComponentDto file) {
    json.prop("key", file.key());
    json.prop("uuid", file.uuid());
    json.prop("name", file.longName());
  }

  private void addProject(JsonWriter json, @Nullable ComponentDto project, @Nullable ComponentDto subProject) {
    if (project != null) {
      json.prop("project", project.key());
      json.prop("projectUuid", project.uuid());
      json.prop("projectName", project.longName());

      // Do not return sub project if sub project and project are the same
      boolean displaySubProject = subProject != null && !subProject.getId().equals(project.getId());
      if (displaySubProject) {
        json.prop("subProject", subProject.key());
        json.prop("subProjectUuid", subProject.uuid());
        json.prop("subProjectName", subProject.longName());
      }
    }
  }

  private ComponentDto getProject(String projectUuid, Map<String, ComponentDto> projectsByUuid, DbSession session) {
    ComponentDto project = projectsByUuid.get(projectUuid);
    if (project == null) {
      project = componentDao.getNullableByUuid(session, projectUuid);
      if (project != null) {
        projectsByUuid.put(project.uuid(), project);
      }
    }
    return project;
  }

  private ComponentDto getParentProject(@Nullable Long projectId, Map<Long, ComponentDto> subProjectsById, DbSession session) {
    ComponentDto project = subProjectsById.get(projectId);
    if (project == null && projectId != null) {
      project = componentDao.getNullableById(projectId, session);
      if (project != null) {
        subProjectsById.put(project.getId(), project);
      }
    }
    return project;
  }

}
