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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class Process implements ProcessMXBean {

  public static final String NAME_PROPERTY = "pName";
  public static final String PORT_PROPERTY = "pPort";

  public static final String MISSING_NAME_ARGUMENT = "Missing Name argument";


  private final static Logger LOGGER = LoggerFactory.getLogger(Process.class);

  protected Long lastPing;

  final String name;
  final Integer port;

  final protected Props props;

  private static final long MAX_ALLOWED_TIME = 3000L;
  private ScheduledFuture<?> pingTask = null;
  final ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
  final Runnable breakOnMissingPing = new Runnable() {
    public void run() {
      long time = System.currentTimeMillis();
      LOGGER.info("last check-in was {}ms ago.", time - lastPing);
      if (time - lastPing > MAX_ALLOWED_TIME) {
        LOGGER.warn("Did not get a check-in since {}ms. Initiate shutdown", time - lastPing);
        stop();
      }
    }
  };

  public Process(Props props) {

    // Loading all Properties from file
    this.props = props;
    this.name = props.of(NAME_PROPERTY, null);
    this.port = props.intOf(PORT_PROPERTY);


    // Testing required properties
    if (StringUtils.isEmpty(this.name)) {
      throw new IllegalStateException(MISSING_NAME_ARGUMENT);
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
    return objectNameFor(name);
  }

  static public ObjectName objectNameFor(String name) {
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
    if (this.port != null) {
      lastPing = System.currentTimeMillis();
      pingTask = monitor.scheduleWithFixedDelay(breakOnMissingPing, 5, 5, TimeUnit.SECONDS);
    }
    this.onStart();
    LOGGER.info("Process[{}]::start END", name);
  }

  public final void stop() {
    LOGGER.info("Process[{}]::shutdown START", name);
    if (pingTask != null) {
      pingTask.cancel(true);
    }
    monitor.shutdownNow();
    this.onStop();
    LOGGER.info("Process[{}]::shutdown END", name);
  }
}