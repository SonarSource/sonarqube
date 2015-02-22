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

package org.sonar.server.platform.monitoring;

import org.picocontainer.Startable;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.OperationsException;

import java.lang.management.ManagementFactory;

/**
 * Base implementation of {@link org.sonar.server.platform.monitoring.Monitor}
 * that is exported as a JMX bean
 */
public abstract class BaseMonitorMBean implements Monitor, Startable {

  /**
   * Auto-registers to MBean server
   */
  @Override
  public void start() {
    try {
      ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName());
    } catch (OperationsException | MBeanRegistrationException e) {
      throw new IllegalStateException("Fail to register MBean " + name(), e);
    }
  }

  /**
   * Unregister, if needed
   */
  @Override
  public void stop() {
    try {
      ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName());
    } catch (InstanceNotFoundException ignored) {
      // ignore, was not correctly started
    } catch (Exception e) {
      throw new IllegalStateException("Fail to unregister MBean " + name(), e);
    }
  }

  ObjectName objectName() throws MalformedObjectNameException {
    return new ObjectName(String.format("SonarQube:name=%s", name()));
  }
}
