/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process;

import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.slf4j.LoggerFactory;

/**
 * JMX utilities to register MBeans to JMX server
 */
public class Jmx {

  private Jmx() {
    // only statics
  }

  /**
   * Register a MBean to JMX server
   */
  public static void register(String name, Object instance) {
    try {
      Class mbeanInterface = guessMBeanInterface(instance);
      ManagementFactory.getPlatformMBeanServer().registerMBean(new StandardMBean(instance, mbeanInterface), new ObjectName(name));

    } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
      throw new IllegalStateException("Can not register MBean [" + name + "]", e);
    }
  }

  /**
   * MBeans have multiple conventions, including:
   * 1. name of interface is suffixed by "MBean"
   * 2. name of implementation is the name of the interface without "MBean"
   * 3. implementation and interface must be in the same package
   * To avoid the last convention, we wrap the mbean within a StandardMBean. That
   * requires to find the related interface.
   */
  private static Class guessMBeanInterface(Object instance) {
    Class mbeanInterface = null;
    Class<?>[] interfaces = instance.getClass().getInterfaces();
    for (Class<?> anInterface : interfaces) {
      if (anInterface.getName().endsWith("MBean")) {
        mbeanInterface = anInterface;
        break;
      }
    }
    if (mbeanInterface == null) {
      throw new IllegalArgumentException("Can not find the MBean interface of class " + instance.getClass().getName());
    }
    return mbeanInterface;
  }

  /**
   * Unregister a MBean from JMX server. Errors are ignored and logged as warnings.
   */
  public static void unregister(String name) {
    try {
      ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(name));
    } catch (Exception e) {
      LoggerFactory.getLogger(Jmx.class).warn("Can not unregister MBean [{}]", name, e);
    }
  }
}
