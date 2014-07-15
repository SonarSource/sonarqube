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
package org.sonar.process;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.management.JMX;
import javax.management.MBeanServer;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.util.Properties;

import static junit.framework.TestCase.fail;
import static org.fest.assertions.Assertions.assertThat;

public class ProcessTest {

  int freePort;

  @Before
  public void setup() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    freePort = socket.getLocalPort();
    socket.close();
  }

  Process process;

  @After
  public void tearDown() throws Exception {
    if (process != null) {
      MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
      mbeanServer.unregisterMBean(process.getObjectName());
    }
  }

  @Test
  public void fails_invalid_name() {
    try {
      Process.objectNameFor("::");
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Cannot create ObjectName for ::");
    }
  }

  @Test
  public void fail_missing_properties() {
    Properties properties = new Properties();
    try {
      new TestProcess(Props.create(properties));
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(Process.MISSING_NAME_ARGUMENT);
    }
  }

  @Test
  public void should_register_mbean() throws Exception {

    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    Properties properties = new Properties();
    properties.setProperty(Process.NAME_PROPERTY, "TEST");
    Props props = Props.create(properties);
    process = new TestProcess(props);

    // 0 Can have a valid ObjectName
    assertThat(process.getObjectName()).isNotNull();

    // 1 assert that process MBean is registered
    assertThat(mbeanServer.isRegistered(process.getObjectName())).isTrue();

    // 2 assert that we cannot make another Process in the same JVM
    try {
      process = new TestProcess(props);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Process already exists in current JVM");
    }
  }

  @Test(timeout = 5000L)
  public void should_stop_explicit() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(Process.NAME_PROPERTY, "TEST");
    Props props = Props.create(properties);
    process = new TestProcess(props);

    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    final ProcessMXBean processMXBean = JMX.newMBeanProxy(mbeanServer, process.getObjectName(), ProcessMXBean.class);

    Thread procThread = new Thread(new Runnable() {
      @Override
      public void run() {
        process.start();
      }
    });

    // 0. Process is loaded but not ready yet.
    assertThat(processMXBean.isReady()).isFalse();

    // 1. Pretend the process has started
    procThread.start();
    Thread.sleep(200);
    assertThat(procThread.isAlive()).isTrue();
    assertThat(processMXBean.isReady()).isTrue();

    // 2. Stop the process through Management
    processMXBean.stop();
    procThread.join();
  }

  @Test(timeout = 5000L)
  public void should_stop_implicit() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(Process.NAME_PROPERTY, "TEST");
    properties.setProperty(Process.PORT_PROPERTY, Integer.toString(freePort));
    Props props = Props.create(properties);
    process = new TestProcess(props);

    process.start();
  }

  public static class TestProcess extends Process {

    private boolean ready = false;
    private boolean running = false;

    public TestProcess(Props props) {
      super(props);
      running = true;
    }

    @Override
    public void onStart() {
      ready = true;
      while (running) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public void onStop() {
      running = false;
    }

    @Override
    public boolean isReady() {
      return ready;
    }

    public static void main(String... args) {
      System.out.println("Starting child process");
      Props props = Props.create(System.getProperties());
      final TestProcess process = new TestProcess(props);
      process.start();
    }
  }
}