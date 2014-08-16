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

import static org.fest.assertions.Assertions.assertThat;

public class MonitorTest extends ProcessTest {


  Monitor monitor;

  @Before
  public void setUpMonitor() throws Exception {
    monitor = new Monitor();
  }

  @After
  public void downMonitor() throws Exception {
    if (monitor != null) {
      monitor.interrupt();
      monitor = null;
    }
  }

  @Test
  public void monitor_can_start_and_stop() {
    assertThat(monitor.isAlive()).isFalse();
    monitor.start();
    assertThat(monitor.isAlive()).isTrue();
    monitor.terminate();
    assertThat(monitor.isAlive()).isFalse();
  }

  @Test(timeout = 2500L)
  public void monitor_should_interrupt_process() throws Exception {
    // 0 start the dummyProcess
    ProcessWrapper process = new ProcessWrapper("DummyOkProcess")
      .addProperty(MonitoredProcess.NAME_PROPERTY, "DummyOkProcess")
      .addClasspath(dummyAppJar.getAbsolutePath())
      .setWorkDir(temp.getRoot())
      .setTempDirectory(temp.getRoot())
      .setJmxPort(freePort)
      .setClassName(DUMMY_OK_APP);

    assertThat(process.execute());


    // 1 start my monitor & register process
    monitor.start();
    monitor.registerProcess(process);

    // 2 terminate monitor, assert process is terminated
    monitor.terminate();
    assertThat(monitor.isAlive()).isFalse();
    assertThat(process.isAlive()).isFalse();
  }
}