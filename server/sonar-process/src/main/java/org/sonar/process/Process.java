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

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public abstract class Process implements ProcessMXBean {

  public static final String NAME_PROPERTY = "pName";
  public static final String PORT_PROPERTY = "pPort";

  public static final String MISSING_NAME_ARGUMENT = "Missing Name argument";
  public static final String MISSING_PORT_ARGUMENT = "Missing Port argument";

  private final Thread monitoringThread;
  private static final long MAX_ALLOWED_TIME = 3000L;

  private final static Logger LOGGER = LoggerFactory.getLogger(Process.class);

  protected Long lastPing;

  final String name;
  final Integer port;

  final protected Props props;

  public Process(Props props) {

    // Loading all Properties from file
    this.props = props;
    this.name = props.of(NAME_PROPERTY, null);
    this.port = props.intOf(PORT_PROPERTY);


    // Testing required properties
    if (StringUtils.isEmpty(this.name)) {
      throw new IllegalStateException(MISSING_NAME_ARGUMENT);
    }

    if (this.port != null) {
      this.monitoringThread = new Thread(new Monitor(this));
    } else {
      this.monitoringThread = null;
    }

    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      mbeanServer.registerMBean(this, this.getObjectName());
    } catch (InstanceAlreadyExistsException e) {
      throw new IllegalStateException("Process already exists in current JVM", e);
    } catch (MBeanRegistrationException e) {
      throw new IllegalStateException("Could not register process as MBean", e);
    } catch (NotCompliantMBeanException e) {
      throw new IllegalStateException("Process is not a compliant MBean", e);
    }
  }

  public ObjectName getObjectName() {
    try {
      return new ObjectName("org.sonar", "name", name);
    } catch (MalformedObjectNameException e) {
      throw new IllegalStateException("Cannot create ObjectName for " + name, e);
    }
  }

  public void ping() {
    this.lastPing = System.currentTimeMillis();
  }

  public abstract void onStart();

  public abstract void onStop();

  public final void start() {
    LOGGER.info("Process[{}]::start START", name);
    if (monitoringThread != null) {
      this.lastPing = System.currentTimeMillis();
      monitoringThread.start();
    }
    this.onStart();
    LOGGER.info("Process[{}]::start END", name);
  }

  public final void stop() {
    LOGGER.info("Process[{}]::shutdown START", name);
    if (monitoringThread != null) {
      monitoringThread.interrupt();
    }
    this.onStop();
    LOGGER.info("Process[{}]::shutdown END", name);
  }


  private class Monitor implements Runnable {

    final Process process;

    private Monitor(Process process) {
      this.process = process;
    }

    @Override
    public void run() {
      while (monitoringThread != null && !monitoringThread.isInterrupted()) {
        long time = System.currentTimeMillis();
        LOGGER.info("Process[{}]::Monitor::run - last checked-in is {}ms", name, time - lastPing);
        if (time - lastPing > MAX_ALLOWED_TIME) {
          process.stop();
          break;
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }
}