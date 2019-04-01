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
package org.sonar.ce.task.projectanalysis.component;

import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbProjectBranches;

/**
 * Creates or updates the data in table {@code PROJECT_BRANCHES} for the current root.
 */
public class BranchPersisterImpl implements BranchPersister {
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public BranchPersisterImpl(DbClient dbClient, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  public void persist(DbSession dbSession) {
    Branch branch = analysisMetadataHolder.getBranch();
    String branchUuid = treeRootHolder.getRoot().getUuid();

    ComponentDto branchComponentDto = dbClient.componentDao().selectByUuid(dbSession, branchUuid)
      .orElseThrow(() -> new IllegalStateException("Component has been deleted by end-user during analysis"));

    // insert or update in table project_branches
    dbClient.branchDao().upsert(dbSession, toBranchDto(branchComponentDto, branch));
  }

  protected BranchDto toBranchDto(ComponentDto componentDto, Branch branch) {
    BranchDto dto = new BranchDto();
    dto.setUuid(componentDto.uuid());

    // MainBranchProjectUuid will be null if it's a main branch
    dto.setProjectUuid(firstNonNull(componentDto.getMainBranchProjectUuid(), componentDto.projectUuid()));
    dto.setBranchType(branch.getType());

    // merge branch is only present if it's not a main branch and not an application
    if (!branch.isMain() && !Qualifiers.APP.equals(componentDto.qualifier())) {
      dto.setMergeBranchUuid(branch.getMergeBranchUuid());
    }

    if (branch.getType() == BranchType.PULL_REQUEST) {
      dto.setKey(analysisMetadataHolder.getPullRequestKey());

      DbProjectBranches.PullRequestData pullRequestData = DbProjectBranches.PullRequestData.newBuilder()
        .setBranch(branch.getName())
        .setTitle(branch.getName())
        .setTarget(branch.getTargetBranchName())
        .build();
      dto.setPullRequestData(pullRequestData);
    } else {
      dto.setKey(branch.getName());
    }

    return dto;
  }

  private static <T> T firstNonNull(@Nullable T first, T second) {
    return (first != null) ? first : second;
  }

}
