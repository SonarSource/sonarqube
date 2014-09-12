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

import org.sonar.process.JmxUtils;
import org.sonar.process.LoopbackAddress;
import org.sonar.process.ProcessMXBean;
import org.sonar.process.ProcessUtils;

import javax.annotation.CheckForNull;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class RmiJmxConnector implements JmxConnector {

  static {
    /*
     * Prevents such warnings :
     * 
     * WARNING: Failed to restart: java.io.IOException: Failed to get a RMI stub: javax.naming.ServiceUnavailableException [Root exception
     * is java.rmi.ConnectException: Connection refused to host: 127.0.0.1; nested exception is:
     * java.net.ConnectException: Connection refused]
     * Sep 11, 2014 7:32:32 PM RMIConnector RMIClientCommunicatorAdmin-doStop
     * WARNING: Failed to call the method close():java.rmi.ConnectException: Connection refused to host: 127.0.0.1; nested exception is:
     * java.net.ConnectException: Connection refused
     * Sep 11, 2014 7:32:32 PM ClientCommunicatorAdmin Checker-run
     * WARNING: Failed to check connection: java.net.ConnectException: Connection refused
     * Sep 11, 2014 7:32:32 PM ClientCommunicatorAdmin Checker-run
     * WARNING: stopping
     */
    System.setProperty("sun.rmi.transport.tcp.logLevel", "SEVERE");
  }

  private final Map<ProcessRef, ProcessMXBean> mbeans = new IdentityHashMap<ProcessRef, ProcessMXBean>();

  @Override
  public synchronized void connect(final JavaCommand command, ProcessRef processRef, long timeoutMs) {
    ConnectorCallable callable = new ConnectorCallable(command, processRef.getProcess());
    ProcessMXBean mxBean = execute(callable, timeoutMs);
    if (mxBean != null) {
      register(processRef, mxBean);
    } else if (!processRef.isTerminated()) {
      throw new IllegalStateException("Fail to connect to JMX RMI server of " + processRef);
    }
  }

  @Override
  public void ping(ProcessRef processRef) {
    mbeans.get(processRef).ping();
  }

  @Override
  public boolean isReady(final ProcessRef processRef, long timeoutMs) {
    return execute(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return mbeans.get(processRef).isReady();
      }
    }, timeoutMs);
  }

  @Override
  public void terminate(final ProcessRef processRef, long timeoutMs) {
    execute(new Callable() {
      @Override
      public Void call() throws Exception {
        mbeans.get(processRef).terminate();
        return null;
      }
    }, timeoutMs);
  }

  void register(ProcessRef processRef, ProcessMXBean mxBean) {
    mbeans.put(processRef, mxBean);
  }

  private <T> T execute(Callable<T> callable, long timeoutMs) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<T> future = executor.submit(callable);
      return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      throw new IllegalStateException("Fail send JMX request", e);
    } finally {
      executor.shutdownNow();
    }
  }

  private static class ConnectorCallable implements Callable<ProcessMXBean> {
    private final JavaCommand command;
    private final Process process;

    private ConnectorCallable(JavaCommand command, Process process) {
      this.command = command;
      this.process = process;
    }

    @Override
    @CheckForNull
    public ProcessMXBean call() throws Exception {
      JMXServiceURL jmxUrl = JmxUtils.serviceUrl(LoopbackAddress.get(), command.getJmxPort());
      while (ProcessUtils.isAlive(process)) {
        try {
          JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl, null);
          MBeanServerConnection mBeanServer = jmxConnector.getMBeanServerConnection();
          return JMX.newMBeanProxy(mBeanServer, JmxUtils.objectName(command.getKey()), ProcessMXBean.class);
        } catch (Exception ignored) {
          // ignored, RMI server is probably not started yet
        }
        Thread.sleep(300L);
      }

      // process went down, no need to connect
      return null;
    }
  }
}
