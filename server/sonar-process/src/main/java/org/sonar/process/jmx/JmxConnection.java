/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.process.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public class JmxConnection implements AutoCloseable {

  private static final long MEGABYTE = 1024L * 1024L;
  private final JMXConnector jmxConnector;

  JmxConnection(JMXConnector jmxConnector) {
    this.jmxConnector = jmxConnector;
  }

  /**
   * Get a MBean from a remote JMX server.
   * @throws IllegalStateException if a valid
   * connection to remote server cannot be created, for instance because the connection to has
   * not yet been established (with {@link JMXConnector#connect()}), or it has been closed/broken.
   */
  public <M> M getMBean(String mBeanName, Class<M> mBeanInterfaceClass) {
    try {
      MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();
      if (mBeanName.startsWith("java.lang")) {
        return ManagementFactory.newPlatformMXBeanProxy(connection, mBeanName, mBeanInterfaceClass);
      }
      return JMX.newMBeanProxy(connection, new ObjectName(mBeanName), mBeanInterfaceClass);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to connect to MBean " + mBeanName, e);
    }
  }

  public SortedMap<String, Object> getSystemState() {
    SortedMap<String, Object> props = new TreeMap<>();
    MemoryMXBean memory = getMBean(ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
    MemoryUsage heap = memory.getHeapMemoryUsage();
    props.put("Heap Committed (MB)", toMegaBytes(heap.getCommitted()));
    props.put("Heap Init (MB)", toMegaBytes(heap.getInit()));
    props.put("Heap Max (MB)", toMegaBytes(heap.getMax()));
    props.put("Heap Used (MB)", toMegaBytes(heap.getUsed()));
    MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
    props.put("Non Heap Committed (MB)", toMegaBytes(nonHeap.getCommitted()));
    props.put("Non Heap Init (MB)", toMegaBytes(nonHeap.getInit()));
    props.put("Non Heap Max (MB)", toMegaBytes(nonHeap.getMax()));
    props.put("Non Heap Used (MB)", toMegaBytes(nonHeap.getUsed()));
    ThreadMXBean thread = getMBean(ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
    props.put("Thread Count", thread.getThreadCount());
    return props;
  }

  // visible for testing
  @CheckForNull
  static Long toMegaBytes(long bytes) {
    if (bytes < 0L) {
      return null;
    }
    return bytes / MEGABYTE;
  }

  @Override
  public void close() {
    try {
      jmxConnector.close();
    } catch (IOException e) {
      throw new IllegalStateException("Can not close JMX connector", e);
    }
  }
}
