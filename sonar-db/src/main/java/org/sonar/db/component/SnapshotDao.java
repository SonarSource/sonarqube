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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.resources.Scopes;
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

  @CheckForNull
  public SnapshotDto selectById(DbSession session, long id) {
    return mapper(session).selectByKey(id);
  }

  public SnapshotDto selectOrFailById(DbSession session, long id) {
    SnapshotDto value = selectById(session, id);
    if (value == null) {
      throw new RowNotFoundException(String.format("Snapshot id does not exist: %d", id));
    }
    return value;
  }

  public List<SnapshotDto> selectByIds(DbSession dbSession, List<Long> snapshotIds) {
    return executeLargeInputs(snapshotIds, mapper(dbSession)::selectByIds);
  }

  @CheckForNull
  public SnapshotDto selectLastSnapshotByComponentUuid(DbSession session, String componentUuid) {
    return mapper(session).selectLastSnapshot(componentUuid);
  }

  public boolean hasLastSnapshotByComponentUuid(DbSession session, String componentUUid) {
    return mapper(session).countLastSnapshotByComponentUuid(componentUUid) > 0;
  }

  public List<SnapshotDto> selectSnapshotsByQuery(DbSession session, SnapshotQuery query) {
    return mapper(session).selectSnapshotsByQuery(query);
  }

  @CheckForNull
  public SnapshotDto selectSnapshotByQuery(DbSession session, SnapshotQuery query) {
    List<SnapshotDto> snapshotDtos = mapper(session).selectSnapshotsByQuery(query);
    if (snapshotDtos.isEmpty()) {
      return null;
    }
    checkState(snapshotDtos.size() == 1, "Expected one snapshot to be returned, got %s", snapshotDtos.size());
    return snapshotDtos.get(0);
  }

  public List<SnapshotDto> selectPreviousVersionSnapshots(DbSession session, String componentUuid, String lastVersion) {
    return mapper(session).selectPreviousVersionSnapshots(componentUuid, lastVersion);
  }

  @CheckForNull
  public SnapshotDto selectOldestSnapshot(DbSession session, String componentUuid) {
    List<SnapshotDto> snapshotDtos = mapper(session).selectOldestSnapshots(componentUuid, new RowBounds(0, 1));
    return snapshotDtos.isEmpty() ? null : snapshotDtos.get(0);
  }

  public List<SnapshotDto> selectSnapshotAndChildrenOfProjectScope(DbSession session, long snapshotId) {
    return mapper(session).selectSnapshotAndChildrenOfScope(snapshotId, Scopes.PROJECT);
  }

  public int updateSnapshotAndChildrenLastFlagAndStatus(DbSession session, SnapshotDto snapshot, boolean isLast, String status) {
    Long rootId = snapshot.getId();
    String path = Strings.nullToEmpty(snapshot.getPath()) + snapshot.getId() + ".%";
    Long pathRootId = snapshot.getRootIdOrSelf();

    return mapper(session).updateSnapshotAndChildrenLastFlagAndStatus(rootId, pathRootId, path, isLast, status);
  }

  public int updateSnapshotAndChildrenLastFlag(DbSession session, SnapshotDto snapshot, boolean isLast) {
    Long rootId = snapshot.getId();
    String path = Strings.nullToEmpty(snapshot.getPath()) + snapshot.getId() + ".%";
    Long pathRootId = snapshot.getRootIdOrSelf();

    return mapper(session).updateSnapshotAndChildrenLastFlag(rootId, pathRootId, path, isLast);
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
