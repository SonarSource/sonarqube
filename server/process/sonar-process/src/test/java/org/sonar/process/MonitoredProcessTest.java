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
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Missing Name argument");
    }

    properties.setProperty(MonitoredProcess.NAME_PROPERTY, DummyProcess.NAME);
    DummyProcess dummyProcess = new DummyProcess(new Props(properties), true);
    assertThat(dummyProcess).isNotNull();
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
    assertThat(dummyProcess.isReady()).isFalse();
    assertThat(dummyProcess.isTerminated()).isFalse();
    process.start();
    Thread.sleep(100);
    assertThat(dummyProcess.isReady()).isTrue();
    assertThat(dummyProcess.isTerminated()).isFalse();
    process.join();
    assertThat(dummyProcess.isReady()).isTrue();
    assertThat(dummyProcess.isTerminated()).isTrue();

    assertThat(dummyProcess.getCheckFile()).isNotNull();
    assertThat(dummyProcess.getCheckFile().getName()).isEqualTo(DummyProcess.CHECKFILE_NAME);
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
    assertThat(dummyProcess.isReady()).isFalse();
    assertThat(dummyProcess.isTerminated()).isFalse();
    process.start();
    Thread.sleep(100);

    int count = 0;
    for (int i = 0; i < 3; i++) {
      dummyProcess.ping();
      assertThat(dummyProcess.isReady()).isTrue();
      assertThat(dummyProcess.isTerminated()).isFalse();
      Thread.sleep(300);
      count++;
    }
    assertThat(count).isEqualTo(3);
    process.join();
    assertThat(dummyProcess.isReady()).isTrue();
    assertThat(dummyProcess.isTerminated()).isTrue();

    assertThat(dummyProcess.getCheckFile()).isNotNull();
    assertThat(dummyProcess.getCheckFile().getName()).isEqualTo(DummyProcess.CHECKFILE_NAME);
  }

  @Test(timeout = 3000L)
  public void monitor_explicitely_shutdown() throws Exception {
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
    assertThat(dummyProcess.isReady()).isFalse();
    assertThat(dummyProcess.isTerminated()).isFalse();
    process.start();
    Thread.sleep(100);
    assertThat(dummyProcess.isReady()).isTrue();
    assertThat(dummyProcess.isTerminated()).isFalse();
    dummyProcess.terminate();
    Thread.sleep(100);
    assertThat(dummyProcess.isReady()).isTrue();
    assertThat(dummyProcess.isTerminated()).isTrue();

    assertThat(dummyProcess.getCheckFile()).isNotNull();
    assertThat(dummyProcess.getCheckFile().getName()).isEqualTo(DummyProcess.CHECKFILE_NAME);
  }

}