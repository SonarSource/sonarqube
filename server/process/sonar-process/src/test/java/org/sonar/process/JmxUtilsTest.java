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

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class JmxUtilsTest {

  class MyBean implements ProcessMXBean {

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public long ping() {
      return 0;
    }

    @Override
    public void terminate() {

    }
  }

  @Test
  public void construct_jmx_objectName() throws Exception {
    MyBean mxBean = new MyBean();
    ObjectName objectName = JmxUtils.objectName(mxBean.getClass().getSimpleName());
    assertThat(objectName).isNotNull();
    assertThat(objectName.getDomain()).isEqualTo(JmxUtils.DOMAIN);
    assertThat(objectName.getKeyProperty(JmxUtils.NAME_PROPERTY)).isEqualTo(mxBean.getClass().getSimpleName());
  }

  @Test
  public void fail_jmx_objectName() throws Exception {
    try {
      JmxUtils.objectName(":");
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Cannot create ObjectName for :");
    }
  }

  @Test
  public void testRegisterMBean() throws Exception {

    // 0 Get mbServer and create out test MXBean
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    MyBean mxBean = new MyBean();
    ObjectName objectName = JmxUtils.objectName(mxBean.getClass().getSimpleName());

    // 1 assert that mxBean gets registered
    assertThat(mbeanServer.isRegistered(objectName)).isFalse();
    JmxUtils.registerMBean(mxBean, mxBean.getClass().getSimpleName());
    assertThat(mbeanServer.isRegistered(objectName)).isTrue();

    // 2 assert that we can over-register
    assertThat(mbeanServer.isRegistered(objectName)).isTrue();
    JmxUtils.registerMBean(mxBean, mxBean.getClass().getSimpleName());
    assertThat(mbeanServer.isRegistered(objectName)).isTrue();
  }

  @Test
  public void serviceUrl_ipv4() throws Exception {
    JMXServiceURL url = JmxUtils.serviceUrl(ip(Inet4Address.class), 1234);
    assertThat(url).isNotNull();
    assertThat(url.getPort()).isEqualTo(1234);
  }

  @Test
  public void serviceUrl_ipv6() throws Exception {
    JMXServiceURL url = JmxUtils.serviceUrl(ip(Inet6Address.class), 1234);
    assertThat(url).isNotNull();
    assertThat(url.getPort()).isEqualTo(1234);
  }

  private static InetAddress ip(Class inetAddressClass) throws SocketException {
    Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
    while (ifaces.hasMoreElements()) {
      NetworkInterface iface = ifaces.nextElement();
      Enumeration<InetAddress> addresses = iface.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress addr = addresses.nextElement();
        if (addr.getClass().isAssignableFrom(inetAddressClass)) {
          return addr;
        }
      }
    }
    throw new IllegalStateException("no ipv4 address");
  }
}
