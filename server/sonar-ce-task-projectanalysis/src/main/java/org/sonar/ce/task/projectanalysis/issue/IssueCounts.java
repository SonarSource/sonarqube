/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.issue;

import org.sonar.api.issue.IssueStatus;

/**
 * Mutable per-rule issue counts, broken down by {@link IssueStatus}. Only the 5 statuses relevant
 * to telemetry are tracked; {@code FIXED} and any future status are ignored.
 */
public class IssueCounts {

  private int open = 0;
  private int confirmed = 0;
  private int falsePositive = 0;
  private int accepted = 0;
  private int inSandbox = 0;

  void increment(IssueStatus status) {
    switch (status) {
      case OPEN -> open++;
      case CONFIRMED -> confirmed++;
      case FALSE_POSITIVE -> falsePositive++;
      case ACCEPTED -> accepted++;
      case IN_SANDBOX -> inSandbox++;
      default -> {
        // FIXED and any future values: not one of our 5 tracked buckets
      }
    }
  }

  public int open() {
    return open;
  }

  public int confirmed() {
    return confirmed;
  }

  public int falsePositive() {
    return falsePositive;
  }

  public int accepted() {
    return accepted;
  }

  public int inSandbox() {
    return inSandbox;
  }
}
