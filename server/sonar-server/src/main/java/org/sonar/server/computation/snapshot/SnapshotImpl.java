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

package org.sonar.server.computation.snapshot;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class SnapshotImpl implements Snapshot {

  private long id;
  private long createdAt;

  private SnapshotImpl(Builder builder) {
    this.id = builder.id;
    this.createdAt = builder.createdAt;
  }

  @Override
  public long id() {
    return id;
  }

  @Override
  public long createdAt() {
    return createdAt;
  }

  public static final class Builder {
    @CheckForNull
    private Long id;
    @CheckForNull
    private Long createdAt;

    public Builder setId(long id) {
      this.id = id;
      return this;
    }

    public Builder setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public SnapshotImpl build() {
      checkNotNull(id, "id cannot be null");
      checkNotNull(createdAt, "createdAt cannot be null");
      return new SnapshotImpl(this);
    }
  }

  @Override
  public String toString() {
    return "SnapshotImpl{" +
      "id=" + id +
      ", createdAt=" + createdAt +
      '}';
  }
}
