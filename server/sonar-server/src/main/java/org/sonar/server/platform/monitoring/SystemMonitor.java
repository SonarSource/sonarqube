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

/**
 * JVM runtime information. Not exported as a MXBean because these informations
 * are natively provided.
 */
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

  @Override
  public LinkedHashMap<String, Object> attributes() {
    LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("System Date", formatDateTime(new Date(system.now())));
    attributes.put("Start Time", formatDateTime(new Date(runtimeMXBean().getStartTime())));
    attributes.put("JVM Vendor", runtimeMXBean().getVmVendor());
    attributes.put("JVM Name", runtimeMXBean().getVmName());
    attributes.put("JVM Version", runtimeMXBean().getVmVersion());
    attributes.put("Processors", runtime().availableProcessors());
    attributes.put("System Classpath", runtimeMXBean().getClassPath());
    attributes.put("BootClassPath", runtimeMXBean().getBootClassPath());
    attributes.put("Library Path", runtimeMXBean().getLibraryPath());
    attributes.put("Total Memory", formatMemory(runtime().totalMemory()));
    attributes.put("Free Memory", formatMemory(runtime().freeMemory()));
    attributes.put("Max Memory", formatMemory(runtime().maxMemory()));
    attributes.put("Heap", memoryMXBean().getHeapMemoryUsage().toString());
    attributes.put("Non Heap", memoryMXBean().getNonHeapMemoryUsage().toString());
    attributes.put("System Load Average", String.format("%.1f%% (last minute)", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage() * 100.0));
    attributes.put("Loaded Classes", classLoadingMXBean().getLoadedClassCount());
    attributes.put("Total Loaded Classes", classLoadingMXBean().getTotalLoadedClassCount());
    attributes.put("Unloaded Classes", classLoadingMXBean().getUnloadedClassCount());
    attributes.put("Threads", threadMXBean().getThreadCount());
    attributes.put("Threads Peak", threadMXBean().getPeakThreadCount());
    attributes.put("Daemon Thread", threadMXBean().getDaemonThreadCount());
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
