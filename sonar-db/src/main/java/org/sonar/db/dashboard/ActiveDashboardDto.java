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
package org.sonar.db.dashboard;

import com.google.common.base.Objects;

public final class ActiveDashboardDto {
  private Long id;
  private Long dashboardId;
  private Long userId;
  private Integer orderIndex;

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public ActiveDashboardDto setId(Long id) {
    this.id = id;
    return this;
  }

  /**
   * @return the dashboardId
   */
  public Long getDashboardId() {
    return dashboardId;
  }

  /**
   * @param dashboardId the dashboardId to set
   */
  public ActiveDashboardDto setDashboardId(Long dashboardId) {
    this.dashboardId = dashboardId;
    return this;
  }

  /**
   * @return the userId
   */
  public Long getUserId() {
    return userId;
  }

  /**
   * @param userId the userId to set
   */
  public ActiveDashboardDto setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  /**
   * @return the orderIndex
   */
  public Integer getOrderIndex() {
    return orderIndex;
  }

  /**
   * @param orderIndex the orderIndex to set
   */
  public ActiveDashboardDto setOrderIndex(Integer orderIndex) {
    this.orderIndex = orderIndex;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActiveDashboardDto that = (ActiveDashboardDto) o;
    return !(id != null ? !id.equals(that.id) : that.id != null);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
