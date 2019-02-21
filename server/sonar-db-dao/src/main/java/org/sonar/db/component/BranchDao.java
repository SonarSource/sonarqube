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
package org.sonar.db.component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class BranchDao implements Dao {

  private final System2 system2;

  public BranchDao(System2 system2) {
    this.system2 = system2;
  }

  public void insert(DbSession dbSession, BranchDto dto) {
    setKeyType(dto);
    mapper(dbSession).insert(dto, system2.now());
  }

  public void upsert(DbSession dbSession, BranchDto dto) {
    BranchMapper mapper = mapper(dbSession);
    long now = system2.now();
    setKeyType(dto);
    if (mapper.update(dto, now) == 0) {
      mapper.insert(dto, now);
    }
  }

  private static void setKeyType(BranchDto dto) {
    if (dto.getBranchType() == BranchType.PULL_REQUEST) {
      dto.setKeyType(KeyType.PULL_REQUEST);
    } else {
      dto.setKeyType(KeyType.BRANCH);
    }
  }

  public int updateMainBranchName(DbSession dbSession, String projectUuid, String newBranchKey) {
    long now = system2.now();
    return mapper(dbSession).updateMainBranchName(projectUuid, newBranchKey, now);
  }

  /**
   * Set or unset the uuid of the manual baseline analysis by updating the manual_baseline_analysis_uuid column, if:
   *
   * - the specified uuid exists
   * - and the specified uuid corresponds to a long-living branch (including the main branch)
   *
   * @return the number of rows that were updated
   */
  public int updateManualBaseline(DbSession dbSession, String uuid, @Nullable String analysisUuid) {
    long now = system2.now();
    return mapper(dbSession).updateManualBaseline(uuid, analysisUuid == null || analysisUuid.isEmpty() ? null : analysisUuid, now);
  }

  public Optional<BranchDto> selectByBranchKey(DbSession dbSession, String projectUuid, String key) {
    return selectByKey(dbSession, projectUuid, key, KeyType.BRANCH);
  }

  public Optional<BranchDto> selectByPullRequestKey(DbSession dbSession, String projectUuid, String key) {
    return selectByKey(dbSession, projectUuid, key, KeyType.PULL_REQUEST);
  }

  private static Optional<BranchDto> selectByKey(DbSession dbSession, String projectUuid, String key, KeyType keyType) {
    return Optional.ofNullable(mapper(dbSession).selectByKey(projectUuid, key, keyType));
  }

  public Collection<BranchDto> selectByComponent(DbSession dbSession, ComponentDto component) {
    String projectUuid = component.getMainBranchProjectUuid();
    if (projectUuid == null) {
      projectUuid = component.projectUuid();
    }
    return mapper(dbSession).selectByProjectUuid(projectUuid);
  }

  public List<BranchDto> selectByUuids(DbSession session, Collection<String> uuids) {
    return executeLargeInputs(uuids, mapper(session)::selectByUuids);
  }

  public Optional<BranchDto> selectByUuid(DbSession session, String uuid) {
    return Optional.ofNullable(mapper(session).selectByUuid(uuid));
  }

  public boolean hasNonMainBranches(DbSession dbSession) {
    return mapper(dbSession).countNonMainBranches() > 0L;
  }

  public long countByTypeAndCreationDate(DbSession dbSession, BranchType branchType, long sinceDate) {
    return mapper(dbSession).countByTypeAndCreationDate(branchType.name(), sinceDate);
  }

  private static BranchMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(BranchMapper.class);
  }
}
