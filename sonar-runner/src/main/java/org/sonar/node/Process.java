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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @Since 4.5
 */
public abstract class Process implements Runnable {

  private final static Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

  final String name;
  final int port;

  public Process(String name, int port) {
    this.name = name;
    this.port = port;
  }

  public abstract void execute();

  @Override
  public void run() {
    DatagramPacket client = null;
    try {
      byte[] data = new byte[name.length()];
      name.getBytes(0, name.length(), data, 0);
      DatagramPacket pack =
        new DatagramPacket(data, data.length, InetAddress.getLocalHost(), port);
      while (true) {
        DatagramSocket ds = new DatagramSocket();
        ds.send(pack);
        ds.close();
        Thread.sleep(1000);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Monitoring Thread for " + name + " could not communicate to socket", e);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Monitoring Thread for " + name + " is interrupted ", e);
    }
  }

  public static void main(final String... args) {

    Process process = new Process(args[0], Integer.parseInt(args[1])) {
      @Override
      public void execute() {
        while (true) {
          LOGGER.info("pseudo running for process {}", args[0]);
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };
    Thread monitor = new Thread(process);
    monitor.start();
    process.execute();
    monitor.interrupt();
  }
}