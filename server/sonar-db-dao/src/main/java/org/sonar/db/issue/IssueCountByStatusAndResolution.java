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
package org.sonar.db.issue;

import java.io.Serializable;
import javax.annotation.CheckForNull;

public final class IssueCountByStatusAndResolution implements Serializable {

  private String status;
  private String resolution;
  private Integer count = 0;

  public IssueCountByStatusAndResolution() {
    //Nothing to do
  }

  public String getStatus() {
    return status;
  }

  public IssueCountByStatusAndResolution setStatus(String status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public String getResolution() {
    return resolution;
  }

  public IssueCountByStatusAndResolution setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public Integer getCount() {
    return count;
  }

  public IssueCountByStatusAndResolution setCount(Integer count) {
    this.count = count;
    return this;
  }
}
