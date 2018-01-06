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
package org.sonar.db.purge;

public final class PurgeSnapshotQuery {
  private String componentUuid;
  private String[] status;
  private Boolean islast;
  private Boolean notPurged;

  public String[] getStatus() {
    return status;
  }

  public PurgeSnapshotQuery setStatus(String[] status) {
    this.status = status;
    return this;
  }

  public Boolean getIslast() {
    return islast;
  }

  public PurgeSnapshotQuery setIslast(Boolean islast) {
    this.islast = islast;
    return this;
  }

  public Boolean getNotPurged() {
    return notPurged;
  }

  public PurgeSnapshotQuery setNotPurged(Boolean notPurged) {
    this.notPurged = notPurged;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public PurgeSnapshotQuery setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

}
