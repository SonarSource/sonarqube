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
package org.sonar.server.search;

import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;

import java.util.Calendar;
import java.util.Date;

public class NodeHealth {

  private boolean master;
  private String address;
  private long jvmHeapMax;
  private long jvmHeapUsed;
  private long fsAvailable;
  private long fsTotal;
  private long jvmThreads;
  private int cpuPercent;
  private long openFiles;
  private long jvmUptimeMillis;

  public boolean isMaster() {
    return master;
  }

  void setMaster(boolean master) {
    this.master = master;
  }

  public String getAddress() {
    return address;
  }

  void setAddress(String address) {
    this.address = address;
  }

  void setJvmHeapMax(long bytes) {
    this.jvmHeapMax = bytes;
  }

  void setJvmHeapUsed(long bytes) {
    this.jvmHeapUsed = bytes;
  }

  public long getJvmHeapMax() {
    return jvmHeapMax;
  }

  public long getJvmHeapUsed() {
    return jvmHeapUsed;
  }

  public String getJvmHeapUsedPercent() {
    return formatPercent(getJvmHeapUsed(), getJvmHeapMax());
  }

  void setFsAvailable(long bytes) {
    this.fsAvailable = bytes;
  }

  void setFsTotal(long bytes) {
    this.fsTotal = bytes;
  }

  public String getFsUsedPercent() {
    return formatPercent(fsTotal - fsAvailable, fsTotal);
  }

  private String formatPercent(long amount, long total) {
    return String.format("%.1f%%", 100 * amount * 1.0D / total);
  }

  void setJvmThreads(long threads) {
    this.jvmThreads = threads;
  }

  public long getJvmThreads() {
    return jvmThreads;
  }

  void setProcessCpuPercent(int cpuPercent) {
    this.cpuPercent = cpuPercent;
  }

  public String getProcessCpuPercent() {
    return formatPercent(Long.valueOf(cpuPercent), 100L);
  }

  void setOpenFiles(long avgOpenFileDescriptors) {
    this.openFiles = avgOpenFileDescriptors;
  }

  long getOpenFiles() {
    return openFiles;
  }

  void setJvmUptimeMillis(long millis) {
    this.jvmUptimeMillis = millis;
  }

  public long getJvmUptimeMillis() {
    return jvmUptimeMillis;
  }

  public Date getJvmUpSince() {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(calendar.getTimeInMillis() - getJvmUptimeMillis());
    return calendar.getTime();
  }

  NodeHealth(NodeStats nodesStats) {
    // Master/slave
    setMaster(nodesStats.getNode().isMasterNode());

    // Host IP and port
    setAddress(nodesStats.getNode().getAddress().toString());

    // JVM Heap Usage
    setJvmHeapMax(nodesStats.getJvm().getMem().getHeapMax().bytes());
    setJvmHeapUsed(nodesStats.getJvm().getMem().getHeapUsed().bytes());

    // Disk Usage
    setFsTotal(nodesStats.getFs().getTotal().getTotal().bytes());
    setFsAvailable(nodesStats.getFs().getTotal().getAvailable().bytes());

    // Ping ?

    // Threads
    setJvmThreads(nodesStats.getJvm().getThreads().count());

    // CPU
    if(nodesStats.getProcess().getCpu() != null) {
      setProcessCpuPercent(nodesStats.getProcess().cpu().getPercent());
    }

    // Open Files
    setOpenFiles(nodesStats.getProcess().getOpenFileDescriptors());

    // Uptime
    setJvmUptimeMillis(nodesStats.getJvm().getUptime().getMillis());
  }
}
