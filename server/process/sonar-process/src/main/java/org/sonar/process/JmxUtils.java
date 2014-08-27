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

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import java.lang.management.ManagementFactory;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;

public class JmxUtils {

  private JmxUtils() {
    // only static stuff
  }

  public static final String DOMAIN = "org.sonar";
  public static final String NAME_PROPERTY = "name";

  public static final String WEB_SERVER_NAME = "web";
  public static final String SEARCH_SERVER_NAME = "search";

  public static ObjectName objectName(String name) {
    try {
      return new ObjectName(DOMAIN, NAME_PROPERTY, name);
    } catch (MalformedObjectNameException e) {
      throw new IllegalStateException("Cannot create ObjectName for " + name, e);
    }
  }

  public static void registerMBean(Object mbean, String name) {
    try {
      MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
      // Check if already registered in JVM (might run multiple instance in JUnits)
      if (mbeanServer.isRegistered(objectName(name))) {
        mbeanServer.unregisterMBean(objectName(name));
      }
      mbeanServer.registerMBean(mbean, objectName(name));
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to register JMX MBean named " + name, e);
    }
  }

  public static JMXServiceURL serviceUrl(InetAddress host, int port) {
    String address = host.getHostAddress();
    if (host instanceof Inet6Address) {
      // See http://docs.oracle.com/javase/7/docs/api/javax/management/remote/JMXServiceURL.html
      // "The host is a host name, an IPv4 numeric host address, or an IPv6 numeric address enclosed in square brackets."
      address = String.format("[%s]", address);
    }
    try {
      return new JMXServiceURL("rmi", address, port, String.format("/jndi/rmi://%s:%d/jmxrmi", address, port));
    } catch (MalformedURLException e) {
      throw new IllegalStateException("JMX url does not look well formed", e);
    }
  }
}
