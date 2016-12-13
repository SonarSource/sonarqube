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
package org.sonar.db.component;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class SnapshotDao implements Dao {

  public static boolean isLast(SnapshotDto snapshotTested, @Nullable SnapshotDto previousLastSnapshot) {
    return previousLastSnapshot == null || previousLastSnapshot.getCreatedAt() < snapshotTested.getCreatedAt();
  }

  /**
   * @deprecated use {@link #selectByUuid(DbSession, String)}
   */
  @Deprecated
  @CheckForNull
  public SnapshotDto selectById(DbSession session, long id) {
    return mapper(session).selectByKey(id);
  }

  /**
   * @deprecated use {@link #selectByUuid(DbSession, String)}
   */
  @Deprecated
  public SnapshotDto selectOrFailById(DbSession session, long id) {
    SnapshotDto value = selectById(session, id);
    if (value == null) {
      throw new RowNotFoundException(String.format("Snapshot id does not exist: %d", id));
    }
    return value;
  }

  public List<SnapshotDto> selectByIds(DbSession dbSession, Collection<Long> snapshotIds) {
    return executeLargeInputs(snapshotIds, mapper(dbSession)::selectByIds);
  }

  public Optional<SnapshotDto> selectByUuid(DbSession dbSession, String analysisUuid) {
    List<SnapshotDto> dtos = mapper(dbSession).selectByUuids(Collections.singletonList(analysisUuid));
    if (dtos.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(dtos.iterator().next());
  }

  public List<SnapshotDto> selectByUuids(DbSession dbSession, Collection<String> analysisUuids) {
    return executeLargeInputs(analysisUuids, mapper(dbSession)::selectByUuids);
  }

  public Optional<SnapshotDto> selectLastAnalysisByComponentUuid(DbSession session, String componentUuid) {
    return Optional.ofNullable(mapper(session).selectLastSnapshotByComponentUuid(componentUuid));
  }

  public Optional<SnapshotDto> selectLastAnalysisByRootComponentUuid(DbSession session, String componentUuid) {
    return Optional.ofNullable(mapper(session).selectLastSnapshotByRootComponentUuid(componentUuid));
  }

  public List<SnapshotDto> selectLastAnalysesByRootComponentUuids(DbSession dbSession, Collection<String> componentUuids) {
    return executeLargeInputs(componentUuids, mapper(dbSession)::selectLastSnapshotsByRootComponentUuids);
  }

  public List<SnapshotDto> selectAnalysesByQuery(DbSession session, SnapshotQuery query) {
    return mapper(session).selectSnapshotsByQuery(query);
  }

  @CheckForNull
  public SnapshotDto selectAnalysisByQuery(DbSession session, SnapshotQuery query) {
    List<SnapshotDto> dtos = mapper(session).selectSnapshotsByQuery(query);
    if (dtos.isEmpty()) {
      return null;
    }
    checkState(dtos.size() == 1, "Expected one analysis to be returned, got %s", dtos.size());
    return dtos.get(0);
  }

  /**
   * Since this relies on tables EVENTS, this can return results only for root components (PROJECT, VIEW or DEVELOPER).
   */
  public List<SnapshotDto> selectPreviousVersionSnapshots(DbSession session, String componentUuid, String lastVersion) {
    return mapper(session).selectPreviousVersionSnapshots(componentUuid, lastVersion);
  }

  @CheckForNull
  public SnapshotDto selectOldestSnapshot(DbSession session, String componentUuid) {
    List<SnapshotDto> snapshotDtos = mapper(session).selectOldestSnapshots(componentUuid, new RowBounds(0, 1));
    return snapshotDtos.isEmpty() ? null : snapshotDtos.get(0);
  }

  public void switchIsLastFlagAndSetProcessedStatus(DbSession dbSession, String componentUuid, String analysisUuid) {
    SnapshotMapper mapper = mapper(dbSession);
    mapper.unsetIsLastFlagForComponentUuid(componentUuid);
    mapper(dbSession).setIsLastFlagForAnalysisUuid(analysisUuid);
  }

  public SnapshotDto insert(DbSession session, SnapshotDto item) {
    mapper(session).insert(item);
    return item;
  }

  public void insert(DbSession session, Collection<SnapshotDto> items) {
    for (SnapshotDto item : items) {
      insert(session, item);
    }
  }

  public void insert(DbSession session, SnapshotDto item, SnapshotDto... others) {
    insert(session, Lists.asList(item, others));
  }

  public void update(DbSession dbSession, SnapshotDto analysis) {
    mapper(dbSession).update(analysis);
  }

  /**
   * Used by Governance
   */
  @CheckForNull
  public ViewsSnapshotDto selectSnapshotBefore(String componentUuid, long date, DbSession dbSession) {
    return from(mapper(dbSession).selectSnapshotBefore(componentUuid, date))
      .first()
      .orNull();
  }


  private static SnapshotMapper mapper(DbSession session) {
    return session.getMapper(SnapshotMapper.class);
  }
}
