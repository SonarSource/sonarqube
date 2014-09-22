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
package org.sonar.process.monitor;

/**
 * Most of the timeouts involved in process monitoring, in milliseconds
 */
class Timeouts {

  private long terminationTimeout = 60000L;

  /**
   * [both monitor and monitored process] timeout of graceful termination before hard killing
   */
  long getTerminationTimeout() {
    return terminationTimeout;
  }

  /**
   * @see #getTerminationTimeout()
   */
  void setTerminationTimeout(long l) {
    this.terminationTimeout = l;
  }

}
