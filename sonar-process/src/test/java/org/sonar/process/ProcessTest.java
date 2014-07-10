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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ProcessTest {


  @Test(timeout = 5000L)
  public void heart_beats() throws InterruptedException, IOException {

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

  private Process testProcess(final String name, final DatagramSocket socket) {
    return new Process(name, socket.getLocalPort()) {
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