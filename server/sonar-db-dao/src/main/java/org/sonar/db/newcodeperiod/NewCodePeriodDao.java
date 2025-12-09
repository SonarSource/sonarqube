/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.newcodeperiod;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.Preconditions.checkArgument;

public class NewCodePeriodDao implements Dao {
  private static final String MSG_PROJECT_UUID_NOT_SPECIFIED = "Project uuid must be specified.";
  private final System2 system2;
  private final UuidFactory uuidFactory;

  public NewCodePeriodDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public Optional<NewCodePeriodDto> selectByUuid(DbSession dbSession, String uuid) {
    return mapper(dbSession).selectByUuid(uuid);
  }

  public Optional<NewCodePeriodDto> selectGlobal(DbSession dbSession) {
    return ofNullable(mapper(dbSession).selectGlobal());
  }

  public void insert(DbSession dbSession, NewCodePeriodDto dto) {
    requireNonNull(dto.getType(), "Type of NewCodePeriod must be specified.");
    long currentTime = system2.now();
    mapper(dbSession).insert(dto.setCreatedAt(currentTime)
      .setUpdatedAt(currentTime)
      .setUuid(ofNullable(dto.getUuid()).orElse(uuidFactory.create())));
  }

  public void upsert(DbSession dbSession, NewCodePeriodDto dto) {
    NewCodePeriodMapper mapper = mapper(dbSession);
    long currentTime = system2.now();
    dto.setUpdatedAt(currentTime);
    if (mapper.update(dto) == 0) {
      dto.setCreatedAt(currentTime);
      dto.setUuid(uuidFactory.create());
      mapper.insert(dto);
    }
  }

  public void update(DbSession dbSession, NewCodePeriodDto dto) {
    requireNonNull(dto.getUuid(), "Uuid of NewCodePeriod must be specified.");
    mapper(dbSession).update(dto.setUpdatedAt(system2.now()));
  }

  public void updateBranchReferenceValues(DbSession dbSession, BranchDto branchDto, String newBranchName) {
    requireNonNull(branchDto, "Original referenced branch must be specified.");
    requireNonNull(branchDto.getProjectUuid(), MSG_PROJECT_UUID_NOT_SPECIFIED);
    requireNonNull(newBranchName, "New branch name must be specified.");
    selectAllByProject(dbSession, branchDto.getProjectUuid()).stream()
      .filter(newCP -> NewCodePeriodType.REFERENCE_BRANCH.equals(newCP.getType()) && branchDto.getBranchKey().equals(newCP.getValue()))
      .forEach(newCodePeriodDto -> update(dbSession, newCodePeriodDto.setValue(newBranchName)));
  }

  public Optional<NewCodePeriodDto> selectByProject(DbSession dbSession, String projectUuid) {
    requireNonNull(projectUuid, MSG_PROJECT_UUID_NOT_SPECIFIED);
    return ofNullable(mapper(dbSession).selectByProject(projectUuid));
  }

  public List<NewCodePeriodDto> selectAllByProject(DbSession dbSession, String projectUuid) {
    requireNonNull(projectUuid, MSG_PROJECT_UUID_NOT_SPECIFIED);
    return mapper(dbSession).selectAllByProject(projectUuid);
  }

  public Optional<NewCodePeriodDto> selectByBranch(DbSession dbSession, String projectUuid, String branchUuid) {
    requireNonNull(projectUuid, MSG_PROJECT_UUID_NOT_SPECIFIED);
    requireNonNull(branchUuid, "Branch uuid must be specified.");
    return ofNullable(mapper(dbSession).selectByBranch(projectUuid, branchUuid));
  }

  public Set<String> selectBranchesReferencing(DbSession dbSession, String projectUuid, String referenceBranchName) {
    return mapper(dbSession).selectBranchesReferencing(projectUuid, referenceBranchName);
  }

  public boolean existsByProjectAnalysisUuid(DbSession dbSession, String projectAnalysisUuid) {
    requireNonNull(projectAnalysisUuid, MSG_PROJECT_UUID_NOT_SPECIFIED);
    return mapper(dbSession).countByProjectAnalysis(projectAnalysisUuid) > 0;
  }

  /**
   * Deletes an entry. It can be the global setting or a specific project or branch setting.
   * Note that deleting project's setting doesn't delete the settings of the branches belonging to that project.
   */
  public void delete(DbSession dbSession, @Nullable String projectUuid, @Nullable String branchUuid) {
    checkArgument(branchUuid == null || projectUuid != null, "branchUuid must be null if projectUuid is null");
    mapper(dbSession).delete(projectUuid, branchUuid);
  }

  private static NewCodePeriodMapper mapper(DbSession session) {
    return session.getMapper(NewCodePeriodMapper.class);
  }

  public List<NewCodePeriodDto> selectAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }
}
