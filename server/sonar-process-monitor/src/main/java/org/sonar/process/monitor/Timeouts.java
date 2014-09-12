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

  private long terminationTimeout = 120000L;
  private long jmxConnectionTimeout = 30000L;
  private long monitorPingInterval = 3000L;
  private long autokillPingTimeout = 60000L;
  private long autokillPingInterval = 3000L;

  /**
   * [monitor] Timeout to get connected to RMI MXBean while process is alive
   */
  long getJmxConnectionTimeout() {
    return jmxConnectionTimeout;
  }

  /**
   * @see #getJmxConnectionTimeout()
   */
  void setJmxConnectionTimeout(long l) {
    this.jmxConnectionTimeout = l;
  }

  /**
   * [monitor] Delay between each ping request
   */
  long getMonitorPingInterval() {
    return monitorPingInterval;
  }

  /**
   * @see #getMonitorPingInterval()
   */
  void setMonitorPingInterval(long l) {
    this.monitorPingInterval = l;
  }

  /**
   * [monitored process] maximum age of last received ping before process autokills
   */
  long getAutokillPingTimeout() {
    return autokillPingTimeout;
  }

  /**
   * @see #getAutokillPingTimeout() 
   */
  void setAutokillPingTimeout(long l) {
    this.autokillPingTimeout = l;
  }

  /**
   * [monitored process] delay between checks of freshness of received pings
   */
  long getAutokillPingInterval() {
    return autokillPingInterval;
  }

  /**
   * @see #getAutokillPingInterval()
   */
  void setAutokillPingInterval(long l) {
    this.autokillPingInterval = l;
  }

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
