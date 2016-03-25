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

import java.io.File;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;

public class JmxConnectionFactoryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public JmxTestServer jmxServer = new JmxTestServer();

  @Test
  public void create_returns_null_if_process_is_down() throws Exception {
    File ipcSharedDir = temp.newFolder();
    JmxConnectionFactory underTest = new JmxConnectionFactory(ipcSharedDir);

    JmxConnection connection = underTest.create(ProcessId.COMPUTE_ENGINE);

    assertThat(connection).isNull();
  }

  @Test
  public void create_connection_if_process_is_up() throws Exception {
    File ipcSharedDir = temp.newFolder();
    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(ipcSharedDir, ProcessId.COMPUTE_ENGINE.getIpcIndex())) {
      processCommands.setUp();
      processCommands.setJmxUrl(jmxServer.getAddress().toString());
    }

    JmxConnection connection = new JmxConnectionFactory(ipcSharedDir).create(ProcessId.COMPUTE_ENGINE);
    assertThat(connection).isNotNull();
  }

  @Test
  public void create_throws_ISE_if_fails_to_connect_to_process() throws Exception {
    File ipcSharedDir = temp.newFolder();
    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(ipcSharedDir, ProcessId.COMPUTE_ENGINE.getIpcIndex())) {
      // process is up but does not have any JMX URL
      processCommands.setUp();
    }

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not connect to process " + ProcessId.COMPUTE_ENGINE.toString());
    new JmxConnectionFactory(ipcSharedDir).create(ProcessId.COMPUTE_ENGINE);
  }

  @Test
  public void load_ipc_shared_dir_from_props() throws Exception {
    File ipcSharedDir = temp.newFolder();
    Properties props = new Properties();
    props.setProperty("process.sharedDir", ipcSharedDir.getAbsolutePath());

    JmxConnectionFactory underTest = new JmxConnectionFactory(new Props(props));

    assertThat(underTest.getIpcSharedDir().getCanonicalPath()).isEqualTo(ipcSharedDir.getCanonicalPath());
  }

}
