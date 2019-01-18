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
package org.sonar.db.purge;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public final class PurgeSnapshotQuery {
  private final String componentUuid;
  private String[] status;
  private Boolean islast;
  /**
   * If {@code true}, selects only analysis which have not been purged from historical and duplication data before.
   */
  private Boolean notPurged;

  public PurgeSnapshotQuery(String componentUuid) {
    this.componentUuid = requireNonNull(componentUuid, "componentUuid can't be null");
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public String[] getStatus() {
    return status;
  }

  public PurgeSnapshotQuery setStatus(String[] status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public Boolean getIslast() {
    return islast;
  }

  public PurgeSnapshotQuery setIslast(@Nullable Boolean islast) {
    this.islast = islast;
    return this;
  }

  @CheckForNull
  public Boolean getNotPurged() {
    return notPurged;
  }

  public PurgeSnapshotQuery setNotPurged(@Nullable Boolean notPurged) {
    this.notPurged = notPurged;
    return this;
  }

}
