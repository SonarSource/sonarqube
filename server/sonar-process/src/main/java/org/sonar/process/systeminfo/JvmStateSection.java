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
package org.sonar.process.systeminfo;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Locale;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static java.lang.String.format;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

/**
 * Dumps state of JVM (memory, threads)
 */
public class JvmStateSection implements SystemInfoSection {
  private static final long MEGABYTE = 1024L * 1024L;
  private final String name;

  public JvmStateSection(String name) {
    this.name = name;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    return toProtobuf(ManagementFactory.getMemoryMXBean());
  }

  // Visible for testing
  ProtobufSystemInfo.Section toProtobuf(MemoryMXBean memoryBean) {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName(name);
    addAttributeInMb(protobuf,"Max Memory (MB)", Runtime.getRuntime().maxMemory());
    addAttributeInMb(protobuf, "Free Memory (MB)", Runtime.getRuntime().freeMemory());
    MemoryUsage heap = memoryBean.getHeapMemoryUsage();
    addAttributeInMb(protobuf, "Heap Committed (MB)", heap.getCommitted());
    addAttributeInMb(protobuf, "Heap Init (MB)", heap.getInit());
    addAttributeInMb(protobuf, "Heap Max (MB)", heap.getMax());
    addAttributeInMb(protobuf, "Heap Used (MB)", heap.getUsed());
    MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
    addAttributeInMb(protobuf, "Non Heap Committed (MB)", nonHeap.getCommitted());
    addAttributeInMb(protobuf, "Non Heap Init (MB)", nonHeap.getInit());
    addAttributeInMb(protobuf, "Non Heap Max (MB)", nonHeap.getMax());
    addAttributeInMb(protobuf, "Non Heap Used (MB)", nonHeap.getUsed());
    OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
    setAttribute(protobuf,"System Load Average", format(Locale.ENGLISH, "%.1f%% (last minute)", os.getSystemLoadAverage() * 100.0));
    ThreadMXBean thread = ManagementFactory.getThreadMXBean();
    setAttribute(protobuf, "Threads", thread.getThreadCount());

    return protobuf.build();
  }

  private static void addAttributeInMb(ProtobufSystemInfo.Section.Builder protobuf, String key, long valueInBytes) {
    if (valueInBytes >= 0L) {
      setAttribute(protobuf, key, valueInBytes / MEGABYTE);
    }
  }
}
