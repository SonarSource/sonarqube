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
package org.sonar.server.issue.notification;

public class MetricStatsInt {
  private int onLeak = 0;
  private int offLeak = 0;

  MetricStatsInt increment(boolean onLeak) {
    if (onLeak) {
      this.onLeak += 1;
    } else {
      this.offLeak += 1;
    }
    return this;
  }

  public int getOnLeak() {
    return onLeak;
  }

  public int getOffLeak() {
    return offLeak;
  }

  public int getTotal() {
    return onLeak + offLeak;
  }

  @Override
  public String toString() {
    return "MetricStatsInt{" +
      "onLeak=" + onLeak +
      ", offLeak=" + offLeak +
      '}';
  }
}
