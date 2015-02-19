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
import org.sonar.api.utils.text.JsonWriter;

import java.lang.management.*;
import java.util.Date;

import static org.sonar.api.utils.DateUtils.formatDateTime;

public class SystemMonitoring extends MonitoringMBean implements SystemMonitoringMBean {
  private final System2 system;

  public SystemMonitoring(System2 system) {
    this.system = system;
  }

  @Override
  public String name() {
    return "System";
  }

  @Override
  public String getSystemDate() {
    return formatDateTime(new Date(system.now()));
  }

  @Override
  public String getJvmVendor() {
    return runtimeMXBean().getVmVendor();
  }

  @Override
  public String getJvmName() {
    return runtimeMXBean().getVmName();
  }

  @Override
  public String getJvmVersion() {
    return runtimeMXBean().getVmVersion();
  }

  @Override
  public String getJavaVersion() {
    return javaProperty("java.runtime.version");
  }

  @Override
  public String getJavaHome() {
    return javaProperty("java.home");
  }

  @Override
  public String getJitCompiler() {
    return javaProperty("java.compiler");
  }

  @Override
  public int getProcessors() {
    return runtime().availableProcessors();
  }

  @Override
  public String getSystemClasspath() {
    return runtimeMXBean().getClassPath();
  }

  @Override
  public String getBootClasspath() {
    return runtimeMXBean().getBootClassPath();
  }

  @Override
  public String getLibraryPath() {
    return runtimeMXBean().getLibraryPath();
  }

  @Override
  public String getTotalMemory() {
    return formatMemory(runtime().totalMemory());
  }

  @Override
  public String getFreeMemory() {
    return formatMemory(runtime().freeMemory());
  }

  @Override
  public String getMaxMemory() {
    return formatMemory(runtime().maxMemory());
  }

  @Override
  public String getHeapMemory() {
    return memoryMXBean().getHeapMemoryUsage().toString();
  }

  @Override
  public String getNonHeapMemory() {
    return memoryMXBean().getNonHeapMemoryUsage().toString();
  }

  @Override
  public String getSystemLoadAverage() {
    return String.format("%.1f%% (last minute)", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage() * 100.0);
  }

  @Override
  public String getLoadedClasses() {
    return String.format("currently: %d, total: %d, unloaded: %d",
      classLoadingMXBean().getLoadedClassCount(),
      classLoadingMXBean().getTotalLoadedClassCount(),
      classLoadingMXBean().getUnloadedClassCount());
  }

  @Override
  public String getStartTime() {
    return formatDateTime(new Date(runtimeMXBean().getStartTime()));
  }

  @Override
  public String getThreads() {
    return String.format("total: %d, peak: %d, daemon: %d",
      threadMXBean().getThreadCount(),
      threadMXBean().getPeakThreadCount(),
      threadMXBean().getDaemonThreadCount());
  }

  @Override
  public void toJson(JsonWriter json) {
    json.beginObject()
      .prop("System Date", getSystemDate())
      .prop("JVM Vendor", getJvmVendor())
      .prop("JVM Name", getJvmName())
      .prop("JVM Version", getJvmVersion())
      .prop("Processors", getProcessors())
      .prop("System Classpath", getSystemClasspath())
      .prop("BootClassPath", getBootClasspath())
      .prop("Library Path", getLibraryPath())
      .prop("Total Memory", getTotalMemory())
      .prop("Free Memory", getFreeMemory())
      .prop("Max Memory", getMaxMemory())
      .prop("Heap", getHeapMemory())
      .prop("Non Heap", getNonHeapMemory())
      .prop("System Load Average", getSystemLoadAverage())
      .prop("Loaded Classes", getLoadedClasses())
      .prop("Start Time", getStartTime())
      .prop("Threads", getThreads())
      .endObject();
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

  private String javaProperty(String key) {
    return System.getProperty(key);
  }
}
