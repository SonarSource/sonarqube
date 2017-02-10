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
package org.sonar.process.systeminfo;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

public class ProcessStateSystemInfo implements SystemInfoSection {
  private static final long MEGABYTE = 1024L * 1024L;
  private final String name;

  public ProcessStateSystemInfo(String name) {
    this.name = name;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    return toProtobuf(ManagementFactory.getMemoryMXBean());
  }

  // Visible for testing
  ProtobufSystemInfo.Section toProtobuf(MemoryMXBean memoryBean) {
    ProtobufSystemInfo.Section.Builder builder = ProtobufSystemInfo.Section.newBuilder();
    builder.setName(name);
    MemoryUsage heap = memoryBean.getHeapMemoryUsage();
    addAttributeInMb(builder, "Heap Committed (MB)", heap.getCommitted());
    addAttributeInMb(builder, "Heap Init (MB)", heap.getInit());
    addAttributeInMb(builder, "Heap Max (MB)", heap.getMax());
    addAttributeInMb(builder, "Heap Used (MB)", heap.getUsed());
    MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
    addAttributeInMb(builder, "Non Heap Committed (MB)", nonHeap.getCommitted());
    addAttributeInMb(builder, "Non Heap Init (MB)", nonHeap.getInit());
    addAttributeInMb(builder, "Non Heap Max (MB)", nonHeap.getMax());
    addAttributeInMb(builder, "Non Heap Used (MB)", nonHeap.getUsed());
    ThreadMXBean thread = ManagementFactory.getThreadMXBean();
    builder.addAttributesBuilder().setKey("Thread Count").setLongValue(thread.getThreadCount()).build();
    return builder.build();
  }

  private static void addAttributeInMb(ProtobufSystemInfo.Section.Builder builder, String key, long valueInBytes) {
    if (valueInBytes >= 0L) {
      builder.addAttributesBuilder().setKey(key).setLongValue(valueInBytes / MEGABYTE).build();
    }
  }
}
