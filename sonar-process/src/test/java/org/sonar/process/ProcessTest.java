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

import com.google.common.io.Resources;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URISyntaxException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProcessTest {

  @Test
  public void process_loads() throws SocketException, URISyntaxException, MalformedURLException {
    DatagramSocket socket = new DatagramSocket(0);
    testProcess("test", socket);
  }

  @Test(timeout = 5000L)
  public void heart_beats() throws InterruptedException, IOException, URISyntaxException {

    DatagramSocket socket = new DatagramSocket(0);
    Process test = testProcess("test", socket);

    int ping = 0;
    while (ping < 3) {
      DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
      try {
        socket.setSoTimeout(200);
        socket.receive(packet);
      } catch (Exception e) {
        // Do nothing
      }
      ping++;
    }

    socket.close();
  }

  private Process testProcess(final String name, final DatagramSocket socket) throws URISyntaxException, MalformedURLException {
    Env env = mock(Env.class);
    File propsFile = new File(Resources.getResource(getClass(), "ProcessTest/sonar.properties").getFile());
    when(env.file("conf/sonar.properties")).thenReturn(propsFile);
    return new Process(env, name, socket.getLocalPort()) {
      @Override
      public void execute() {
        try {
          Thread.sleep(10000L);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };
  }
}