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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;


public class ProcessWrapperTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private static final String DUMMY_OK_APP = "org.sonar.application.DummyOkProcess";

  int freePort;
  File dummyAppJar;
  Process proc;

  @Before
  public void setup() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    freePort = socket.getLocalPort();
    socket.close();

    dummyAppJar = FileUtils.toFile(getClass().getResource("/sonar-dummy-app.jar"));
  }

  @After
  public void tearDown() {
    if (proc != null) {
      proc.destroy();
    }
  }

  @Test
  public void has_dummy_app() {
    assertThat(dummyAppJar).isFile();
    assertThat(dummyAppJar).exists();
  }

  private void assertCanStart(ProcessWrapper process) {
    assertThat(process.execute()).isTrue();
    proc = process.process();
  }

  private void assertCanBeReady(ProcessWrapper process) throws InterruptedException {
    int count = 0;
    while (!process.isReady() && count < 5) {
      Thread.sleep(500);
    }
    assertThat(process.getProcessMXBean().isReady()).isTrue();
  }

  private void assertPing(ProcessWrapper process) {
    long now = System.currentTimeMillis();
    long ping = process.getProcessMXBean().ping();
    assertThat(ping - now).isLessThan(3000L);
  }


  @Test
  public void execute_dummy_app() throws Exception {

    ProcessWrapper process = new ProcessWrapper("DummyOkProcess")
      .addProperty(MonitoredProcess.NAME_PROPERTY, "DummyOkProcess")
      .addClasspath(dummyAppJar.getAbsolutePath())
      .setWorkDir(temp.getRoot())
      .setTempDirectory(temp.getRoot())
      .setJmxPort(freePort)
      .setClassName(DUMMY_OK_APP);

    assertThat(process.isAlive()).isFalse();
    assertCanStart(process);
    process.start();
    assertCanBeReady(process);
    assertThat(process.isAlive()).isTrue();
    assertPing(process);
    process.terminate();
    try {
      assertPing(process);
      fail();
    } catch (Exception e) {

    }
  }


  @Test
  public void execute_dummy_in_space_folder_app() throws Exception {

    // 0 create a home with space...
    File home = temp.newFolder("t est");
    assertThat(home.canWrite()).isTrue();
    File lib = new File(home, "lib");
    File tempdir = new File(home, "temp");
    FileUtils.copyFileToDirectory(dummyAppJar, lib);

    // 1 Create Properties
    Props props = new Props(new Properties());
    props.set("spaceHome", home.getAbsolutePath());

    // 3 start dummy app
    File effectiveHome = props.fileOf("spaceHome");

    String cp = FilenameUtils.concat(new File(effectiveHome, "lib").getAbsolutePath(), "*");
    System.out.println("cp = " + cp);
    ProcessWrapper process = new ProcessWrapper("DummyOkProcess")
      .addProperty(MonitoredProcess.NAME_PROPERTY, "DummyOkProcess")
      .setTempDirectory(tempdir)
      .addClasspath(cp)
      .setWorkDir(home)
      .setJmxPort(freePort)
      .setClassName(DUMMY_OK_APP);

    assertThat(process.isAlive()).isFalse();
    assertCanStart(process);
  }
}