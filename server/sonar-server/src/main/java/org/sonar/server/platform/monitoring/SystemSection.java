/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.monitoring;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import org.sonar.api.utils.System2;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static java.lang.String.format;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

/**
 * JVM runtime information. Not exported as a MXBean because these information
 * are natively provided.
 */
public class SystemSection implements SystemInfoSection {
  private final System2 system;

  public SystemSection() {
    this(System2.INSTANCE);
  }

  SystemSection(System2 system) {
    this.system = system;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("System");

    setAttribute(protobuf, "System Date", formatDateTime(new Date(system.now())));
    setAttribute(protobuf, "Start Time", formatDateTime(new Date(runtimeMXBean().getStartTime())));
    setAttribute(protobuf, "JVM Vendor", runtimeMXBean().getVmVendor());
    setAttribute(protobuf, "JVM Name", runtimeMXBean().getVmName());
    setAttribute(protobuf, "JVM Version", runtimeMXBean().getVmVersion());
    setAttribute(protobuf, "Processors", runtime().availableProcessors());
    setAttribute(protobuf, "System Classpath", runtimeMXBean().getClassPath());
    setAttribute(protobuf, "BootClassPath", runtimeMXBean().getBootClassPath());
    setAttribute(protobuf, "Library Path", runtimeMXBean().getLibraryPath());
    setAttribute(protobuf, "Total Memory", formatMemory(runtime().totalMemory()));
    setAttribute(protobuf, "Free Memory", formatMemory(runtime().freeMemory()));
    setAttribute(protobuf, "Max Memory", formatMemory(runtime().maxMemory()));
    setAttribute(protobuf, "Heap", memoryMXBean().getHeapMemoryUsage().toString());
    setAttribute(protobuf, "Non Heap", memoryMXBean().getNonHeapMemoryUsage().toString());
    setAttribute(protobuf, "System Load Average", format("%.1f%% (last minute)", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage() * 100.0));
    setAttribute(protobuf, "Loaded Classes", classLoadingMXBean().getLoadedClassCount());
    setAttribute(protobuf, "Total Loaded Classes", classLoadingMXBean().getTotalLoadedClassCount());
    setAttribute(protobuf, "Unloaded Classes", classLoadingMXBean().getUnloadedClassCount());
    setAttribute(protobuf, "Threads", threadMXBean().getThreadCount());
    setAttribute(protobuf, "Threads Peak", threadMXBean().getPeakThreadCount());
    setAttribute(protobuf, "Daemon Thread", threadMXBean().getDaemonThreadCount());
    return protobuf.build();
  }

  private static RuntimeMXBean runtimeMXBean() {
    return ManagementFactory.getRuntimeMXBean();
  }

  private static Runtime runtime() {
    return Runtime.getRuntime();
  }

  private static MemoryMXBean memoryMXBean() {
    return ManagementFactory.getMemoryMXBean();
  }

  private static ClassLoadingMXBean classLoadingMXBean() {
    return ManagementFactory.getClassLoadingMXBean();
  }

  private static ThreadMXBean threadMXBean() {
    return ManagementFactory.getThreadMXBean();
  }

  private static String formatMemory(long memoryInBytes) {
    return format("%d MB", memoryInBytes / 1_000_000);
  }
}
