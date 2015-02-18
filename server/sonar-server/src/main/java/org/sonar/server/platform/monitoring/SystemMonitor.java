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

package org.sonar.server.platform.monitoring;

import org.sonar.api.utils.System2;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.LinkedHashMap;

import static org.sonar.api.utils.DateUtils.formatDateTime;

public class SystemMonitor implements Monitor {
  private final System2 system;

  public SystemMonitor() {
    this(System2.INSTANCE);
  }

  SystemMonitor(System2 system) {
    this.system = system;
  }

  @Override
  public String name() {
    return "System";
  }

  public String getSystemDate() {
    return formatDateTime(new Date(system.now()));
  }

  public String getJvmVendor() {
    return runtimeMXBean().getVmVendor();
  }

  public String getJvmName() {
    return runtimeMXBean().getVmName();
  }

  public String getJvmVersion() {
    return runtimeMXBean().getVmVersion();
  }

  public int getProcessors() {
    return runtime().availableProcessors();
  }

  public String getSystemClasspath() {
    return runtimeMXBean().getClassPath();
  }

  public String getBootClasspath() {
    return runtimeMXBean().getBootClassPath();
  }

  public String getLibraryPath() {
    return runtimeMXBean().getLibraryPath();
  }

  public String getTotalMemory() {
    return formatMemory(runtime().totalMemory());
  }

  public String getFreeMemory() {
    return formatMemory(runtime().freeMemory());
  }

  public String getMaxMemory() {
    return formatMemory(runtime().maxMemory());
  }

  public String getHeapMemory() {
    return memoryMXBean().getHeapMemoryUsage().toString();
  }

  public String getNonHeapMemory() {
    return memoryMXBean().getNonHeapMemoryUsage().toString();
  }

  public String getSystemLoadAverage() {
    return String.format("%.1f%% (last minute)", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage() * 100.0);
  }

  public String getLoadedClasses() {
    return String.format("currently: %d, total: %d, unloaded: %d",
      classLoadingMXBean().getLoadedClassCount(),
      classLoadingMXBean().getTotalLoadedClassCount(),
      classLoadingMXBean().getUnloadedClassCount());
  }

  public String getStartTime() {
    return formatDateTime(new Date(runtimeMXBean().getStartTime()));
  }

  public String getThreads() {
    return String.format("total: %d, peak: %d, daemon: %d",
      threadMXBean().getThreadCount(),
      threadMXBean().getPeakThreadCount(),
      threadMXBean().getDaemonThreadCount());
  }

  @Override
  public LinkedHashMap<String, Object> attributes() {
    LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("System Date", getSystemDate());
    attributes.put("JVM Vendor", getJvmVendor());
    attributes.put("JVM Name", getJvmName());
    attributes.put("JVM Version", getJvmVersion());
    attributes.put("Processors", getProcessors());
    attributes.put("System Classpath", getSystemClasspath());
    attributes.put("BootClassPath", getBootClasspath());
    attributes.put("Library Path", getLibraryPath());
    attributes.put("Total Memory", getTotalMemory());
    attributes.put("Free Memory", getFreeMemory());
    attributes.put("Max Memory", getMaxMemory());
    attributes.put("Heap", getHeapMemory());
    attributes.put("Non Heap", getNonHeapMemory());
    attributes.put("System Load Average", getSystemLoadAverage());
    attributes.put("Loaded Classes", getLoadedClasses());
    attributes.put("Start Time", getStartTime());
    attributes.put("Threads", getThreads());
    return attributes;
  }

  private RuntimeMXBean runtimeMXBean() {
    return ManagementFactory.getRuntimeMXBean();
  }

  private Runtime runtime() {
    return Runtime.getRuntime();
  }

  private MemoryMXBean memoryMXBean() {
    return ManagementFactory.getMemoryMXBean();
  }

  private ClassLoadingMXBean classLoadingMXBean() {
    return ManagementFactory.getClassLoadingMXBean();
  }

  private ThreadMXBean threadMXBean() {
    return ManagementFactory.getThreadMXBean();
  }

  private String formatMemory(long memoryInBytes) {
    return String.format("%d MB", memoryInBytes / 1_000_000);
  }
}
