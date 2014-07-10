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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * @Since 4.5
 */
public class Launcher extends Thread {

  private final static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

  final String name;
  final DatagramSocket socket;
  java.lang.Process process;


  public Launcher(String name, DatagramSocket socket) {
    LOGGER.info("Creating Launcher for '{}' with base port: {}", name, socket.getLocalPort());
    this.name = name;
    this.socket = socket;
  }

  private void launch() {
//    new Thread(new Runnable() {
//      @Override
//      public void run() {
//        Runner.main(name, socket.getLocalPort() + "");
//      }
//    }).start();
  }

  private void shutdown() {
    process.destroy();
  }

  private void monitor() {
    long ping = Long.MAX_VALUE;
    while (true) {
      LOGGER.info("My heart is beating");
      DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
      try {
        socket.setSoTimeout(3000);
        socket.receive(packet);
      } catch (Exception e) {
        // Do nothing
      }
      long newPing = System.currentTimeMillis();
      String message = new String(packet.getData(), 0, 0, packet.getLength());
      LOGGER.info("{} last seen since {}ms", message, (newPing - ping));
      if ((newPing - ping) > 3000) {
        // close everything here...
      }
      ping = newPing;
    }
  }

  @Override
  public void run() {
    LOGGER.info("launching child VM for " + name);
    launch();

    LOGGER.info("Monitoring VM for " + name);
    while (true) {
      monitor();
    }
  }
}
