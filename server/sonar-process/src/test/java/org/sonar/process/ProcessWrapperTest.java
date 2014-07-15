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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import sun.tools.jconsole.LocalVirtualMachine;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.ServerSocket;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;

public class ProcessWrapperTest {

  int freePort;

  @Before
  public void setup() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    freePort = socket.getLocalPort();
    socket.close();
  }

  @Test
  @Ignore("Not a good idea to assert on # of VMs")
  public void process_should_run() throws IOException, MalformedObjectNameException, InterruptedException {

    LocalVirtualMachine.getAllVirtualMachines().size();
    int VMcount = LocalVirtualMachine.getAllVirtualMachines().size();

    System.out.println("LocalVirtualMachine.getAllVirtualMachines() = " + LocalVirtualMachine.getAllVirtualMachines());
    
    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    ProcessWrapper wrapper = wrapper = new ProcessWrapper(ProcessTest.TestProcess.class.getName(),
      Collections.EMPTY_MAP, "TEST", freePort, runtime.getClassPath());

    assertThat(wrapper).isNotNull();
    assertThat(wrapper.isReady()).isTrue();

    assertThat(LocalVirtualMachine.getAllVirtualMachines().size()).isEqualTo(VMcount + 1);

    wrapper.stop();
    assertThat(LocalVirtualMachine.getAllVirtualMachines().size()).isEqualTo(VMcount);

  }
}