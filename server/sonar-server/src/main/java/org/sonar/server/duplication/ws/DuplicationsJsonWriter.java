/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.duplication.ws;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;

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

  private static void writeDuplications(List<DuplicationsParser.Block> blocks, Map<String, String> refByComponentKey, JsonWriter json) {
    for (DuplicationsParser.Block block : blocks) {
      json.beginObject().name("blocks").beginArray();
      for (DuplicationsParser.Duplication duplication : block.getDuplications()) {
        writeDuplication(refByComponentKey, duplication, json);
      }
      json.endArray().endObject();
    }
  }

  private static void writeDuplication(Map<String, String> refByComponentKey, DuplicationsParser.Duplication duplication, JsonWriter json) {
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
    Map<String, ComponentDto> parentProjectsByUuid = newHashMap();
    for (Map.Entry<String, String> entry : refByComponentKey.entrySet()) {
      String componentKey = entry.getKey();
      String ref = entry.getValue();
      Optional<ComponentDto> fileOptional = componentDao.selectByKey(session, componentKey);
      if (fileOptional.isPresent()) {
        ComponentDto file = fileOptional.get();
        json.name(ref).beginObject();

        addFile(json, file);
        ComponentDto project = getProject(file.projectUuid(), projectsByUuid, session);
        ComponentDto parentProject = getParentProject(file.getRootUuid(), parentProjectsByUuid, session);
        addProject(json, project, parentProject);

        json.endObject();
      }
    }
  }

  private static void addFile(JsonWriter json, ComponentDto file) {
    json.prop("key", file.key());
    json.prop("uuid", file.uuid());
    json.prop("name", file.longName());
  }

  private static void addProject(JsonWriter json, @Nullable ComponentDto project, @Nullable ComponentDto subProject) {
    if (project != null) {
      json.prop("project", project.key());
      json.prop("projectUuid", project.uuid());
      json.prop("projectName", project.longName());

      // Do not return sub project if sub project and project are the same
      boolean displaySubProject = subProject != null && !subProject.uuid().equals(project.uuid());
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
      Optional<ComponentDto> projectOptional = componentDao.selectByUuid(session, projectUuid);
      if (projectOptional.isPresent()) {
        project = projectOptional.get();
        projectsByUuid.put(project.uuid(), project);
      }
    }
    return project;
  }

  private ComponentDto getParentProject(String rootUuid, Map<String, ComponentDto> subProjectsByUuid, DbSession session) {
    ComponentDto project = subProjectsByUuid.get(rootUuid);
    if (project == null) {
      Optional<ComponentDto> projectOptional = componentDao.selectByUuid(session, rootUuid);
      if (projectOptional.isPresent()) {
        project = projectOptional.get();
        subProjectsByUuid.put(project.uuid(), project);
      }
    }
    return project;
  }

}
