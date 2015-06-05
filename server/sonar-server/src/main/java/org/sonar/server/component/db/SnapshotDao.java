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

package org.sonar.server.component.db;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Scopes;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.component.SnapshotQuery;
import org.sonar.core.component.db.SnapshotMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.exceptions.NotFoundException;

public class SnapshotDao implements DaoComponent {

  @CheckForNull
  public SnapshotDto selectNullableById(DbSession session, Long id) {
    return mapper(session).selectByKey(id);
  }

  public SnapshotDto selectById(DbSession session, Long key) {
    SnapshotDto value = selectNullableById(session, key);
    if (value == null) {
      throw new NotFoundException(String.format("Key '%s' not found", key));
    }
    return value;
  }

  @CheckForNull
  public SnapshotDto selectLastSnapshotByComponentId(DbSession session, long componentId) {
    return mapper(session).selectLastSnapshot(componentId);
  }

  public List<SnapshotDto> selectSnapshotsByComponentId(DbSession session, long componentId) {
    return mapper(session).selectSnapshotsByQuery(new SnapshotQuery().setComponentId(componentId));
  }

  public List<SnapshotDto> selectSnapshotsByQuery(DbSession session, SnapshotQuery query) {
    return mapper(session).selectSnapshotsByQuery(query);
  }

  public List<SnapshotDto> selectPreviousVersionSnapshots(DbSession session, long componentId, String lastVersion) {
    return mapper(session).selectPreviousVersionSnapshots(componentId, lastVersion);
  }

  public List<SnapshotDto> selectSnapshotAndChildrenOfProjectScope(DbSession session, long snapshotId) {
    return mapper(session).selectSnapshotAndChildrenOfScope(snapshotId, Scopes.PROJECT);
  }

  public int updateSnapshotAndChildrenLastFlagAndStatus(DbSession session, SnapshotDto snapshot, boolean isLast, String status) {
    Long rootId = snapshot.getId();
    String path = snapshot.getPath() + snapshot.getId() + ".%";
    Long pathRootId = snapshot.getRootIdOrSelf();

    return mapper(session).updateSnapshotAndChildrenLastFlagAndStatus(rootId, pathRootId, path, isLast, status);
  }

  public int updateSnapshotAndChildrenLastFlag(DbSession session, SnapshotDto snapshot, boolean isLast) {
    Long rootId = snapshot.getId();
    String path = snapshot.getPath() + snapshot.getId() + ".%";
    Long pathRootId = snapshot.getRootIdOrSelf();

    return mapper(session).updateSnapshotAndChildrenLastFlag(rootId, pathRootId, path, isLast);
  }

  public static boolean isLast(SnapshotDto snapshotTested, @Nullable SnapshotDto previousLastSnapshot) {
    return previousLastSnapshot == null || previousLastSnapshot.getCreatedAt() < snapshotTested.getCreatedAt();
  }

  public SnapshotDto insert(DbSession session, SnapshotDto item) {
    mapper(session).insert(item);
    return item;
  }

  private SnapshotMapper mapper(DbSession session) {
    return session.getMapper(SnapshotMapper.class);
  }
}
