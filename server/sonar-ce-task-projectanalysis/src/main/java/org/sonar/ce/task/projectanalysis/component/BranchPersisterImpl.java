/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Arrays;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.project.Project;

import static org.sonar.core.config.PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE;

/**
 * Creates or updates the data in table {@code PROJECT_BRANCHES} for the current root.
 */
public class BranchPersisterImpl implements BranchPersister {
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final ConfigurationRepository configurationRepository;

  public BranchPersisterImpl(DbClient dbClient, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder, ConfigurationRepository configurationRepository) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.configurationRepository = configurationRepository;
  }

  public void persist(DbSession dbSession) {
    Branch branch = analysisMetadataHolder.getBranch();
    Project project = analysisMetadataHolder.getProject();
    String branchUuid = treeRootHolder.getRoot().getUuid();

    ComponentDto branchComponentDto = dbClient.componentDao().selectByUuid(dbSession, branchUuid)
      .orElseThrow(() -> new IllegalStateException("Component has been deleted by end-user during analysis"));

    // insert or update in table project_branches
    dbClient.branchDao().upsert(dbSession, toBranchDto(dbSession, branchComponentDto, branch, project, checkIfExcludedFromPurge()));
  }

  private boolean checkIfExcludedFromPurge() {
    if (analysisMetadataHolder.getBranch().isMain()) {
      return true;
    }

    if (BranchType.PULL_REQUEST.equals(analysisMetadataHolder.getBranch().getType())) {
      return false;
    }

    String[] branchesToKeep = configurationRepository.getConfiguration().getStringArray(BRANCHES_TO_KEEP_WHEN_INACTIVE);
    return Arrays.stream(branchesToKeep)
      .map(Pattern::compile)
      .anyMatch(excludePattern -> excludePattern.matcher(analysisMetadataHolder.getBranch().getName()).matches());
  }

  protected BranchDto toBranchDto(DbSession dbSession, ComponentDto componentDto, Branch branch, Project project, boolean excludeFromPurge) {
    BranchDto dto = new BranchDto();
    dto.setUuid(componentDto.uuid());

    dto.setIsMain(branch.isMain());
    dto.setProjectUuid(project.getUuid());
    dto.setBranchType(branch.getType());
    dto.setExcludeFromPurge(excludeFromPurge);

    // merge branch is only present if it's not a main branch and not an application
    if (!branch.isMain() && !ComponentQualifiers.APP.equals(componentDto.qualifier())) {
      dto.setMergeBranchUuid(branch.getReferenceBranchUuid());
    }

    if (branch.getType() == BranchType.PULL_REQUEST) {
      String pullRequestKey = analysisMetadataHolder.getPullRequestKey();
      dto.setKey(pullRequestKey);

      DbProjectBranches.PullRequestData pullRequestData = getBuilder(dbSession, project.getUuid(), pullRequestKey)
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

  private DbProjectBranches.PullRequestData.Builder getBuilder(DbSession dbSession, String projectUuid, String pullRequestKey) {
    return dbClient.branchDao().selectByPullRequestKey(dbSession, projectUuid, pullRequestKey)
      .map(BranchDto::getPullRequestData)
      .map(DbProjectBranches.PullRequestData::toBuilder)
      .orElse(DbProjectBranches.PullRequestData.newBuilder());
  }

  private static <T> T firstNonNull(@Nullable T first, T second) {
    return (first != null) ? first : second;
  }

}
