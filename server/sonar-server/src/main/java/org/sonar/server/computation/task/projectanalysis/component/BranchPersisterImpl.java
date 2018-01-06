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
package org.sonar.server.computation.task.projectanalysis.component;

import java.util.Date;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;

import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;

public class BranchPersisterImpl implements BranchPersister {
  private final DbClient dbClient;
  private final System2 system2;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public BranchPersisterImpl(DbClient dbClient, System2 system2, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  public void persist(DbSession dbSession) {
    Branch branch = analysisMetadataHolder.getBranch();
    String branchUuid = treeRootHolder.getRoot().getUuid();

    com.google.common.base.Optional<ComponentDto> branchComponentDtoOpt = dbClient.componentDao().selectByUuid(dbSession, branchUuid);

    ComponentDto branchComponentDto;
    if (branch.isMain()) {
      checkState(branchComponentDtoOpt.isPresent(), "Project has been deleted by end-user during analysis");
      branchComponentDto = branchComponentDtoOpt.get();

    } else {
      // inserts new row in table projects if it's the first time branch is analyzed
      branchComponentDto = branchComponentDtoOpt.or(() -> insertIntoProjectsTable(dbSession, branchUuid));

    }
    // insert or update in table project_branches
    dbClient.branchDao().upsert(dbSession, toBranchDto(branchComponentDto, branch));
  }

  private static void checkState(boolean condition, String msg) {
    if (!condition) {
      throw new IllegalStateException(msg);
    }
  }

  private static <T> T firstNonNull(@Nullable T first, T second) {
    return (first != null) ? first : second;
  }

  private static BranchDto toBranchDto(ComponentDto componentDto, Branch branch) {
    BranchDto dto = new BranchDto();
    dto.setUuid(componentDto.uuid());
    // MainBranchProjectUuid will be null if it's a main branch
    dto.setProjectUuid(firstNonNull(componentDto.getMainBranchProjectUuid(), componentDto.projectUuid()));
    dto.setKey(branch.getName());
    dto.setBranchType(branch.getType());
    // merge branch is only present if it's a short living branch
    dto.setMergeBranchUuid(branch.getMergeBranchUuid().orElse(null));
    return dto;
  }

  private ComponentDto insertIntoProjectsTable(DbSession dbSession, String branchUuid) {
    String mainBranchProjectUuid = analysisMetadataHolder.getProject().getUuid();
    ComponentDto project = dbClient.componentDao().selectOrFailByUuid(dbSession, mainBranchProjectUuid);
    ComponentDto branchDto = project.copy();
    branchDto.setUuid(branchUuid);
    branchDto.setProjectUuid(branchUuid);
    branchDto.setRootUuid(branchUuid);
    branchDto.setUuidPath(UUID_PATH_OF_ROOT);
    branchDto.setModuleUuidPath(UUID_PATH_SEPARATOR + branchUuid + UUID_PATH_SEPARATOR);
    branchDto.setMainBranchProjectUuid(mainBranchProjectUuid);
    branchDto.setDbKey(treeRootHolder.getRoot().getKey());
    branchDto.setCreatedAt(new Date(system2.now()));
    dbClient.componentDao().insert(dbSession, branchDto);
    return branchDto;
  }
}
