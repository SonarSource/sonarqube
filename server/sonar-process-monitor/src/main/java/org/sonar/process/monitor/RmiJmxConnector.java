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
     Prevents such warnings :

     WARNING: Failed to restart: java.io.IOException: Failed to get a RMI stub: javax.naming.ServiceUnavailableException [Root exception is java.rmi.ConnectException: Connection refused to host: 127.0.0.1; nested exception is:
     java.net.ConnectException: Connection refused]
     Sep 11, 2014 7:32:32 PM RMIConnector RMIClientCommunicatorAdmin-doStop
     WARNING: Failed to call the method close():java.rmi.ConnectException: Connection refused to host: 127.0.0.1; nested exception is: 
     java.net.ConnectException: Connection refused
     Sep 11, 2014 7:32:32 PM ClientCommunicatorAdmin Checker-run
     WARNING: Failed to check connection: java.net.ConnectException: Connection refused
     Sep 11, 2014 7:32:32 PM ClientCommunicatorAdmin Checker-run
     WARNING: stopping
     */
    System.setProperty("sun.rmi.transport.tcp.logLevel", "SEVERE");
  }

  private final Map<ProcessRef, ProcessMXBean> mbeans = new IdentityHashMap<ProcessRef, ProcessMXBean>();
  private final Timeouts timeouts;

  RmiJmxConnector(Timeouts timeouts) {
    this.timeouts = timeouts;
  }

  @Override
  public synchronized void connect(final JavaCommand command, ProcessRef processRef) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    ConnectorCallable callable = new ConnectorCallable(command, processRef.getProcess());
    try {
      Future<ProcessMXBean> future = executor.submit(callable);
      ProcessMXBean mxBean = future.get(timeouts.getJmxConnectionTimeout(), TimeUnit.MILLISECONDS);
      if (mxBean != null) {
        mbeans.put(processRef, mxBean);
      }
    } catch (Exception e) {
      if (callable.latestException != null) {
        throw callable.latestException;
      }
      throw new IllegalStateException("Fail to connect to JMX", e);
    } finally {
      executor.shutdownNow();
    }
  }

  @Override
  public void ping(ProcessRef processRef) {
    mbeans.get(processRef).ping();
  }

  @Override
  public boolean isReady(final ProcessRef processRef) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> future = executor.submit(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return mbeans.get(processRef).isReady();
        }
      });
      return future.get(timeouts.getMonitorIsReadyTimeout(), TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      throw new IllegalStateException("Fail send JMX request (isReady)", e);
    } finally {
      executor.shutdownNow();
    }
  }

  @Override
  public void terminate(ProcessRef processRef) {
    mbeans.get(processRef).terminate();
  }

  private static class ConnectorCallable implements Callable<ProcessMXBean> {
    private final JavaCommand command;
    private final Process process;
    private RuntimeException latestException;

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
        } catch (Exception e) {
          latestException = new IllegalStateException(String.format(
            "Fail to connect to JMX bean of %s [%s] ", command.getKey(), jmxUrl), e);
        }
        Thread.sleep(300L);
      }

      // process went down, no need to connect
      return null;
    }
  }
}
