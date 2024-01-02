/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

public class HotspotGroupDto {
  private String status;
  private long count;
  private boolean inLeak;

  public String getStatus() {
    return status;
  }

  public HotspotGroupDto setStatus(String status) {
    this.status = status;
    return this;
  }

  public long getCount() {
    return count;
  }

  public HotspotGroupDto setCount(long count) {
    this.count = count;
    return this;
  }

  public boolean isInLeak() {
    return inLeak;
  }

  public HotspotGroupDto setInLeak(boolean inLeak) {
    this.inLeak = inLeak;
    return this;
  }
}
