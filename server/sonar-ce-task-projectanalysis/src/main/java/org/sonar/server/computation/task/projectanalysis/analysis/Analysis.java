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
package org.sonar.server.computation.task.projectanalysis.analysis;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class Analysis {

  private long id;
  private String uuid;
  private long createdAt;

  private Analysis(Builder builder) {
    this.id = builder.id;
    this.uuid = builder.uuid;
    this.createdAt = builder.createdAt;
  }

  public long getId() {
    return id;
  }

  public String getUuid() {
    return uuid;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public static final class Builder {
    @CheckForNull
    private Long id;
    @CheckForNull
    private String uuid;
    @CheckForNull
    private Long createdAt;

    public Builder setId(long id) {
      this.id = id;
      return this;
    }

    public Builder setUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Analysis build() {
      checkNotNull(id, "id cannot be null");
      checkNotNull(uuid, "uuid cannot be null");
      checkNotNull(createdAt, "createdAt cannot be null");
      return new Analysis(this);
    }
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Analysis analysis = (Analysis) o;
    return id == analysis.id;
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }

  @Override
  public String toString() {
    return "Analysis{" +
      "id=" + id +
      ", uuid='" + uuid + '\'' +
      ", createdAt=" + createdAt +
      '}';
  }
}
