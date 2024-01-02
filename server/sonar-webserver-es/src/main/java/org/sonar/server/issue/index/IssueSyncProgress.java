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
package org.sonar.server.issue.index;

public class IssueSyncProgress {

  private final int completedCount;
  private final int total;
  private final boolean hasFailures;
  private final boolean isCompleted;

  public IssueSyncProgress(boolean isCompleted, int completedCount, int total, boolean hasFailures) {
    this.completedCount = completedCount;
    this.hasFailures = hasFailures;
    this.isCompleted = isCompleted;
    this.total = total;
  }

  public int getCompletedCount() {
    return completedCount;
  }

  public boolean hasFailures() {
    return hasFailures;
  }

  public int getTotal() {
    return total;
  }

  public boolean isCompleted() {
    return completedCount == total || isCompleted;
  }
}
