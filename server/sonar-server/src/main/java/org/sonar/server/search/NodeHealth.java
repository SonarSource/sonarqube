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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
  private long fieldCacheMemory;
  private long filterCacheMemory;
  private List<Performance> performanceStats;

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

  public List<Performance> getPerformanceStats() {
    return performanceStats;
  }

  public long getFieldCacheMemory() {
    return fieldCacheMemory;
  }

  public long getFilterCacheMemory() {
    return filterCacheMemory;
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
    if (nodesStats.getProcess().getCpu() != null) {
      setProcessCpuPercent(nodesStats.getProcess().cpu().getPercent());
    }

    // Open Files
    setOpenFiles(nodesStats.getProcess().getOpenFileDescriptors());

    // Uptime
    setJvmUptimeMillis(nodesStats.getJvm().getUptime().getMillis());

    // Performance Stat
    performanceStats = new ArrayList<Performance>();

    // IndexStat
    long indexCount = nodesStats.getIndices().getIndexing().getTotal().getIndexCount();
    long indexTotalTime = nodesStats.getIndices().getIndexing().getTotal().getIndexTimeInMillis();
    performanceStats.add(
      new Performance("Average Indexing Time")
        .setWarnThreshold(10)
        .setErrorThreshold(50)
        .setMessage("Too complex documents or low IO/CPU")
        .setValue(indexCount > 0L ? indexTotalTime / indexCount : 0.0));

    // Query stats
    long queryCount = nodesStats.getIndices().getSearch().getTotal().getQueryCount();
    long queryTotalTime = nodesStats.getIndices().getSearch().getTotal().getQueryTimeInMillis();
    performanceStats.add(
      new Performance("Average Querying Time")
        .setWarnThreshold(50)
        .setErrorThreshold(500)
        .setMessage("Inefficient query and/or filters")
        .setValue(queryCount > 0L ? queryTotalTime / queryCount : 0.0));

    // Fetch stats
    long fetchCount = nodesStats.getIndices().getSearch().getTotal().getFetchCount();
    long fetchTotalTime = nodesStats.getIndices().getSearch().getTotal().getFetchTimeInMillis();
    performanceStats.add(
      new Performance("Average Fetching Time")
        .setWarnThreshold(8)
        .setErrorThreshold(15)
        .setMessage("Slow IO, fetch-size too large or documents too big")
        .setValue(fetchCount > 0L ? fetchTotalTime / fetchCount : 0.0));

    // Get stats
    long getCount = nodesStats.getIndices().getGet().getCount();
    long getTotalTime = nodesStats.getIndices().getGet().getTimeInMillis();
    performanceStats.add(
      new Performance("Average Get Time")
        .setWarnThreshold(5)
        .setErrorThreshold(10)
        .setMessage("Slow IO")
        .setValue(getCount > 0L ? getTotalTime / getCount : 0.0));

    // Refresh Stat
    long refreshCount = nodesStats.getIndices().getRefresh().getTotal();
    long refreshTotalTime = nodesStats.getIndices().getRefresh().getTotalTimeInMillis();
    performanceStats.add(
      new Performance("Average Refreshing Time")
        .setWarnThreshold(10)
        .setErrorThreshold(20)
        .setMessage("Slow IO")
        .setValue(refreshCount > 0L ? refreshTotalTime / refreshCount : 0.0));

    // Field Cache
    fieldCacheMemory = nodesStats.getIndices().getFieldData().getMemorySizeInBytes();
    long fieldCacheEviction = nodesStats.getIndices().getFieldData().getEvictions();
    performanceStats.add(
      new Performance("Field Cache Eviction Count")
        .setWarnThreshold(1)
        .setErrorThreshold(1)
        .setMessage("Insufficient RAM available for queries")
        .setValue(fieldCacheEviction));

    // Filter Cache
    filterCacheMemory = nodesStats.getIndices().getFilterCache().getMemorySizeInBytes();
    long filterCacheEviction = nodesStats.getIndices().getFilterCache().getEvictions();
    performanceStats.add(
      new Performance("Filter Cache Eviction Count")
        .setWarnThreshold(1)
        .setErrorThreshold(1)
        .setMessage("Insufficient RAM or too many orphaned filters")
        .setValue(filterCacheEviction));
  }

  static public class Performance {

    public static enum Status {
      OK, WARN, ERROR
    }

    final private String name;
    private String message;
    private double value;
    private double warnThreshold;
    private double errorThreshold;

    public Performance(String name) {
      this.name = name;
    }

    public Status getStatus() {
      if (value >= errorThreshold) {
        return Status.ERROR;
      } else if (value >= warnThreshold) {
        return Status.WARN;
      } else {
        return Status.OK;
      }
    }

    public String getName() {
      return name;
    }

    public String getMessage() {
      return message;
    }

    public Performance setMessage(String message) {
      this.message = message;
      return this;
    }

    public double getValue() {
      return value;
    }

    public Performance setValue(double value) {
      this.value = value;
      return this;
    }

    public Performance setWarnThreshold(long warnThreshold) {
      this.warnThreshold = warnThreshold;
      return this;
    }

    public Performance setErrorThreshold(long errorThreshold) {
      this.errorThreshold = errorThreshold;
      return this;
    }
  }
}
