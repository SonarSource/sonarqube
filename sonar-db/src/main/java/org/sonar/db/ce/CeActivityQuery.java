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
package org.sonar.db.ce;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class CeActivityQuery {

  private boolean onlyCurrents = false;
  private String componentUuid;
  private CeActivityDto.Status status;
  private String type;
  private Long minSubmittedAt;
  private Long maxFinishedAt;

  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  public CeActivityQuery setComponentUuid(@Nullable String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  public boolean isOnlyCurrents() {
    return onlyCurrents;
  }

  public CeActivityQuery setOnlyCurrents(boolean onlyCurrents) {
    this.onlyCurrents = onlyCurrents;
    return this;
  }

  @CheckForNull
  public CeActivityDto.Status getStatus() {
    return status;
  }

  public CeActivityQuery setStatus(@Nullable CeActivityDto.Status status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public String getType() {
    return type;
  }

  public CeActivityQuery setType(@Nullable String type) {
    this.type = type;
    return this;
  }

  @CheckForNull
  public Long getMaxFinishedAt() {
    return maxFinishedAt;
  }

  public CeActivityQuery setMaxFinishedAt(@Nullable Long l) {
    this.maxFinishedAt = l;
    return this;
  }

  @CheckForNull
  public Long getMinSubmittedAt() {
    return minSubmittedAt;
  }

  public CeActivityQuery setMinSubmittedAt(@Nullable Long l) {
    this.minSubmittedAt = l;
    return this;
  }
}
