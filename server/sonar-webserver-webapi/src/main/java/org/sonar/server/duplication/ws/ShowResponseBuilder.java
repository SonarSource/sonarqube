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
package org.sonar.server.duplication.ws;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonarqube.ws.Duplications;
import org.sonarqube.ws.Duplications.Block;
import org.sonarqube.ws.Duplications.ShowResponse;

import static java.util.Optional.ofNullable;

public class ShowResponseBuilder {

  private final ComponentDao componentDao;

  public ShowResponseBuilder(DbClient dbClient) {
    this.componentDao = dbClient.componentDao();
  }

  @VisibleForTesting
  ShowResponseBuilder(ComponentDao componentDao) {
    this.componentDao = componentDao;
  }

  ShowResponse build(DbSession session, List<DuplicationsParser.Block> blocks, @Nullable String branch, @Nullable String pullRequest) {
    Map<String, Reference> refByComponentKey = new LinkedHashMap<>();
    ShowResponse.Builder response = ShowResponse.newBuilder();
    blocks.stream()
      .map(block -> toWsDuplication(block, refByComponentKey))
      .forEach(response::addDuplications);

    writeFileRefs(session, refByComponentKey, response, branch, pullRequest);
    return response.build();
  }

  private static Duplications.Duplication.Builder toWsDuplication(DuplicationsParser.Block block, Map<String, Reference> refByComponentKey) {
    Duplications.Duplication.Builder wsDuplication = Duplications.Duplication.newBuilder();
    block.getDuplications().stream()
      .map(duplication -> toWsBlock(duplication, refByComponentKey))
      .forEach(wsDuplication::addBlocks);

    return wsDuplication;
  }

  private static Block.Builder toWsBlock(Duplication duplication, Map<String, Reference> refByComponentKey) {
    Block.Builder block = Block.newBuilder();

    if (!duplication.removed()) {
      Reference ref = refByComponentKey.computeIfAbsent(duplication.componentDbKey(), k -> new Reference(
        Integer.toString(refByComponentKey.size() + 1),
        duplication.componentDto(),
        duplication.componentDbKey()));
      block.setRef(ref.getId());
    }

    block.setFrom(duplication.from());
    block.setSize(duplication.size());

    return block;
  }

  private void writeFileRefs(DbSession session, Map<String, Reference> refByComponentKey, ShowResponse.Builder response, @Nullable String branch, @Nullable String pullRequest) {
    Map<String, ComponentDto> projectsByUuid = new HashMap<>();
    Map<String, ComponentDto> parentModulesByUuid = new HashMap<>();

    for (Map.Entry<String, Reference> entry : refByComponentKey.entrySet()) {
      Reference ref = entry.getValue();
      ComponentDto file = ref.getDto();

      if (file != null) {
        ComponentDto project = getProject(file.projectUuid(), projectsByUuid, session);
        ComponentDto parentModule = getParentProject(file.moduleUuid(), parentModulesByUuid, session);
        response.putFiles(ref.getId(), toWsFile(file, project, parentModule, branch, pullRequest));
      } else {
        response.putFiles(ref.getId(), toWsFile(ref.getComponentKey(), branch, pullRequest));
      }
    }
  }

  private static Duplications.File toWsFile(String componentKey, @Nullable String branch, @Nullable String pullRequest) {
    Duplications.File.Builder wsFile = Duplications.File.newBuilder();
    String keyWithoutBranch = ComponentDto.removeBranchAndPullRequestFromKey(componentKey);
    wsFile.setKey(keyWithoutBranch);
    wsFile.setName(StringUtils.substringAfterLast(keyWithoutBranch, ":"));
    ofNullable(branch).ifPresent(wsFile::setBranch);
    ofNullable(pullRequest).ifPresent(wsFile::setPullRequest);
    return wsFile.build();
  }

  private static Duplications.File toWsFile(ComponentDto file, @Nullable ComponentDto project, @Nullable ComponentDto subProject,
    @Nullable String branch, @Nullable String pullRequest) {
    Duplications.File.Builder wsFile = Duplications.File.newBuilder();
    wsFile.setKey(file.getKey());
    wsFile.setUuid(file.uuid());
    wsFile.setName(file.longName());
    // Do not return sub project if sub project and project are the same
    ofNullable(project).ifPresent(p -> {
      wsFile.setProject(p.getKey());
      wsFile.setProjectUuid(p.uuid());
      wsFile.setProjectName(p.longName());
      // Do not return sub project if sub project and project are the same
      boolean displaySubProject = subProject != null && !subProject.uuid().equals(project.uuid());
      if (displaySubProject) {
        wsFile.setSubProject(subProject.getKey());
        wsFile.setSubProjectUuid(subProject.uuid());
        wsFile.setSubProjectName(subProject.longName());
      }
      ofNullable(branch).ifPresent(wsFile::setBranch);
      ofNullable(pullRequest).ifPresent(wsFile::setPullRequest);
    });
    return wsFile.build();
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

  private static class Reference {
    private final String id;
    private final ComponentDto dto;
    private final String componentKey;

    public Reference(String id, @Nullable ComponentDto dto, String componentKey) {
      this.id = id;
      this.dto = dto;
      this.componentKey = componentKey;
    }

    public String getId() {
      return id;
    }

    @CheckForNull
    public ComponentDto getDto() {
      return dto;
    }

    public String getComponentKey() {
      return componentKey;
    }

  }

}
