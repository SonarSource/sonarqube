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
package org.sonar.ce.task.projectanalysis.analysis;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public class Analysis {

  private String uuid;
  private long createdAt;

  private Analysis(Builder builder) {
    this.uuid = builder.uuid;
    this.createdAt = builder.createdAt;
  }

  public String getUuid() {
    return uuid;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public static final class Builder {
    @CheckForNull
    private String uuid;
    @CheckForNull
    private Long createdAt;

    public Builder setUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Analysis build() {
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
    return Objects.equals(uuid, analysis.uuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }

  @Override
  public String toString() {
    return "Analysis{" +
      "uuid='" + uuid + '\'' +
      ", createdAt=" + createdAt +
      '}';
  }
}
