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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class ProcessTest {

  DatagramSocket socket;

  @Before
  public void setup() throws SocketException {
    this.socket = new DatagramSocket(0);
  }

  @After
  public void tearDown() {
    this.socket.close();
  }

  @Test
  public void fail_missing_properties() throws MalformedURLException, URISyntaxException {
    Properties properties = new Properties();
    try {
      testProcess(properties);
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(Process.MISSING_NAME_ARGUMENT);
    }

    properties.setProperty(Process.NAME_PROPERTY, "test");
    try {
      testProcess(properties);
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(Process.MISSING_PORT_ARGUMENT);
    }

    properties.setProperty(Process.HEARTBEAT_PROPERTY, Integer.toString(socket.getLocalPort()));
    assertThat(testProcess(properties)).isNotNull();
  }

  @Test
  public void heart_beats() {
    Properties properties = new Properties();
    properties.setProperty(Process.NAME_PROPERTY, "test");
    properties.setProperty(Process.HEARTBEAT_PROPERTY, Integer.toString(socket.getLocalPort()));
    Process test = testProcess(properties);

    assertThat(test).isNotNull();

    int ping = 0;
    int loop = 0;
    while (loop < 3) {
      DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
      try {
        socket.setSoTimeout(2000);
        socket.receive(packet);
        ping++;
      } catch (Exception e) {
        // Do nothing
      }
      loop++;
    }

    assertThat(ping).isEqualTo(loop);
  }

  private Process testProcess(Properties properties) {
    return new Process(Props.create(properties)) {
      @Override
      public void execute() {
        try {
          Thread.sleep(10000L);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void shutdown() {

      }
    };
  }
}