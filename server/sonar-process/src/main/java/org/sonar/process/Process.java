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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public abstract class Process implements Runnable {

  public static final String NAME_PROPERTY = "pName";
  public static final String HEARTBEAT_PROPERTY = "pPort";

  public static final String MISSING_NAME_ARGUMENT = "Missing Name argument";
  public static final String MISSING_PORT_ARGUMENT = "Missing Port argument";


  private final static Logger LOGGER = LoggerFactory.getLogger(Process.class);

  protected Long heartBeatInterval = 1000L;
  private final Thread monitor;

  final String name;
  final Integer port;

  final protected Props props;

  public Process(Props props) {

    // Loading all Properties from file
    this.props = props;
    this.name = props.of(NAME_PROPERTY, null);
    this.port = props.intOf(HEARTBEAT_PROPERTY);


    // Testing required properties
    if (StringUtils.isEmpty(this.name)) {
      throw new IllegalStateException(MISSING_NAME_ARGUMENT);
    }

    if (this.port == null) {
      throw new IllegalStateException(MISSING_PORT_ARGUMENT);
    }

    // Adding a shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        shutdown();
      }
    });

    //Starting monitoring thread
    this.monitor = new Thread(this);
    this.monitor.start();
  }

  public abstract void execute();

  public void shutdown() {
    this.monitor.interrupt();
  }

  @Override
  public void run() {
    LOGGER.info("Setting up heartbeat on port '{}'", port);
    DatagramPacket client = null;
    try {
      byte[] data = new byte[name.length()];
      name.getBytes(0, name.length(), data, 0);
      DatagramPacket pack =
        new DatagramPacket(data, data.length, InetAddress.getLocalHost(), port);
      while (!Thread.currentThread().isInterrupted()) {
        LOGGER.trace("My heart is beating");
        DatagramSocket ds = new DatagramSocket();
        ds.send(pack);
        ds.close();
        try {
          Thread.sleep(heartBeatInterval);
        } catch (InterruptedException e) {
          break;
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Monitoring Thread for " + name + " could not communicate to socket", e);
    }
    System.out.println("Closing  application");
  }
}