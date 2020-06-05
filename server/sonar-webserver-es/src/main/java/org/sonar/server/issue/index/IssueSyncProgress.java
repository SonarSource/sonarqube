/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.issue.index;

public class IssueSyncProgress {
  private static final int PERCENT_100 = 100;

  private final int completed;
  private final int total;

  public IssueSyncProgress(int completed, int total) {
    this.completed = completed;
    this.total = total;
  }

  public int getCompleted() {
    return completed;
  }

  public int getTotal() {
    return total;
  }

  public int toPercentCompleted() {
    if (total != 0) {
      return (int) Math.floor(PERCENT_100 * (double) completed / total);
    }
    return PERCENT_100;
  }

  public boolean isCompleted() {
    return completed == total;
  }
}
