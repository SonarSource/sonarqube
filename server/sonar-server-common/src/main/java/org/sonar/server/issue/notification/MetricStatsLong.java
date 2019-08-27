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
package org.sonar.server.issue.notification;

public class MetricStatsLong {
  private long onCurrentAnalysis = 0;
  private long offCurrentAnalysis = 0;

  MetricStatsLong add(long toAdd, boolean onCurrentAnalysis) {
    if (onCurrentAnalysis) {
      this.onCurrentAnalysis += toAdd;
    } else {
      this.offCurrentAnalysis += toAdd;
    }
    return this;
  }

  public long getOnCurrentAnalysis() {
    return onCurrentAnalysis;
  }

  public long getOffCurrentAnalysis() {
    return offCurrentAnalysis;
  }

  public long getTotal() {
    return onCurrentAnalysis + offCurrentAnalysis;
  }

  @Override
  public String toString() {
    return "MetricStatsLong{" +
      "on=" + onCurrentAnalysis +
      ", off=" + offCurrentAnalysis +
      '}';
  }
}
