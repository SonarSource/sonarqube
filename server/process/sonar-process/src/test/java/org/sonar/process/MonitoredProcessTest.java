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

import org.junit.Test;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class MonitoredProcessTest {

  @Test
  public void fail_on_missing_name() throws Exception {
    Properties properties = new Properties();

    try {
      new DummyProcess(new Props(properties), true);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Missing property: pName");
    }

    properties.setProperty(MonitoredProcess.NAME_PROPERTY, DummyProcess.NAME);
    DummyProcess dummyProcess = new DummyProcess(new Props(properties), true);
    assertThat(dummyProcess).isNotNull();
  }

  @Test
  public void should_not_monitor_debug() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(MonitoredProcess.NAME_PROPERTY, DummyProcess.NAME);
    DummyProcess dummyProcess = new DummyProcess(new Props(properties), false);

    assertThat(dummyProcess.isMonitored()).isFalse();
  }

  @Test
  public void should_monitor_by_default() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(MonitoredProcess.NAME_PROPERTY, DummyProcess.NAME);
    properties.setProperty("sonar.search.javaOpts", "hello world");
    DummyProcess dummyProcess = new DummyProcess(new Props(properties));

    assertThat(dummyProcess.isMonitored()).isTrue();
  }

  @Test(timeout = 3000L)
  public void monitor_dies_when_no_pings() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(MonitoredProcess.NAME_PROPERTY, DummyProcess.NAME);
    final DummyProcess dummyProcess = new DummyProcess(new Props(properties), true);
    dummyProcess.setTimeout(1000L)
      .setCheckDelay(500L);
    Thread process = new Thread(new Runnable() {
      @Override
      public void run() {
        dummyProcess.start();
      }
    });
    assertProcessNotYetRunning(dummyProcess);
    process.start();
    Thread.sleep(100);

    assertProcessRunning(dummyProcess);
    assertJoinAndTerminate(dummyProcess, process);
  }

  @Test(timeout = 3000L)
  public void monitor_dies_after_stopping_to_ping() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(MonitoredProcess.NAME_PROPERTY, DummyProcess.NAME);
    final DummyProcess dummyProcess = new DummyProcess(new Props(properties), true);
    dummyProcess.setTimeout(1000L)
      .setCheckDelay(500L);
    Thread process = new Thread(new Runnable() {
      @Override
      public void run() {
        dummyProcess.start();
      }
    });
    assertProcessNotYetRunning(dummyProcess);
    process.start();
    Thread.sleep(100);

    int count = 0;
    for (int i = 0; i < 3; i++) {
      dummyProcess.ping();
      assertProcessRunning(dummyProcess);
      Thread.sleep(300);
      count++;
    }
    assertThat(count).isEqualTo(3);
    assertJoinAndTerminate(dummyProcess, process);
  }

  @Test(timeout = 3000L)
  public void monitor_explicitly_shutdown() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(MonitoredProcess.NAME_PROPERTY, DummyProcess.NAME);
    final DummyProcess dummyProcess = new DummyProcess(new Props(properties), true);
    dummyProcess.setTimeout(Long.MAX_VALUE).setCheckDelay(500L);
    Thread process = new Thread(new Runnable() {
      @Override
      public void run() {
        dummyProcess.start();
      }
    });
    assertProcessNotYetRunning(dummyProcess);
    process.start();
    Thread.sleep(100);
    assertProcessRunning(dummyProcess);
    dummyProcess.terminate();
    Thread.sleep(100);
    assertProcessTerminated(dummyProcess);
  }

  @Test(timeout = 1000L)
  public void process_does_not_die_when_debugged() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(MonitoredProcess.NAME_PROPERTY, DummyProcess.NAME);

    final DummyProcess dummyProcess = new DummyProcess(new Props(properties), false);
    assertThat(dummyProcess.isMonitored()).isFalse();

    dummyProcess.setTimeout(100L).setCheckDelay(100L);
    Thread process = new Thread(new Runnable() {
      @Override
      public void run() {
        dummyProcess.start();
      }
    });
    process.start();
    Thread.sleep(600);

    assertProcessRunning(dummyProcess);
    dummyProcess.terminate();
    assertProcessTerminated(dummyProcess);
  }

  private void assertJoinAndTerminate(DummyProcess dummyProcess, Thread process) throws InterruptedException {
    process.join();
    assertProcessTerminated(dummyProcess);
  }

  private void assertProcessTerminated(DummyProcess dummyProcess) {
    assertThat(dummyProcess.isReady()).isTrue();
    assertThat(dummyProcess.isTerminated()).isTrue();
    assertProcessCreatedFile(dummyProcess);
  }

  private void assertProcessNotYetRunning(DummyProcess dummyProcess) {
    assertThat(dummyProcess.isReady()).isFalse();
    assertThat(dummyProcess.isTerminated()).isFalse();
  }

  private void assertProcessRunning(DummyProcess dummyProcess) throws InterruptedException {
    assertThat(dummyProcess.isReady()).isTrue();
    assertThat(dummyProcess.isTerminated()).isFalse();
  }

  private void assertProcessCreatedFile(DummyProcess dummyProcess) {
    assertThat(dummyProcess.getCheckFile()).isNotNull();
    assertThat(dummyProcess.getCheckFile().getName()).isEqualTo(DummyProcess.CHECKFILE_NAME);
  }

}
