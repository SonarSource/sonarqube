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

import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.component.db.SnapshotMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;

public class SnapshotDao implements DaoComponent {

  private final System2 system2;

  public SnapshotDao(System2 system) {
    this.system2 = system;
  }

  @CheckForNull
  public SnapshotDto getNullableByKey(DbSession session, Long id) {
    return mapper(session).selectByKey(id);
  }

  public SnapshotDto getByKey(DbSession session, Long key) {
    SnapshotDto value = getNullableByKey(session, key);
    if (value == null) {
      throw new NotFoundException(String.format("Key '%s' not found", key));
    }

    return value;
  }

  public SnapshotDto insert(DbSession session, SnapshotDto item) {
    if (item.getCreatedAt() == null) {
      item.setCreatedAt(system2.now());
    }
    mapper(session).insert(item);
    return item;
  }

  @CheckForNull
  public SnapshotDto getLastSnapshot(DbSession session, SnapshotDto snapshot) {
    return mapper(session).selectLastSnapshot(snapshot.getResourceId());
  }

  @CheckForNull
  public SnapshotDto getLastSnapshotOlderThan(DbSession session, SnapshotDto snapshot) {
    return mapper(session).selectLastSnapshotOlderThan(snapshot.getResourceId(), snapshot.getCreatedAt());
  }

  public List<SnapshotDto> findSnapshotAndChildrenOfProjectScope(DbSession session, SnapshotDto snapshot) {
    return mapper(session).selectSnapshotAndChildrenOfScope(snapshot.getId(), Scopes.PROJECT);
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

  public boolean isLast(SnapshotDto snapshotTested, @Nullable SnapshotDto previousLastSnapshot) {
    return previousLastSnapshot == null || previousLastSnapshot.getCreatedAt() < snapshotTested.getCreatedAt();
  }

  private SnapshotMapper mapper(DbSession session) {
    return session.getMapper(SnapshotMapper.class);
  }
}
