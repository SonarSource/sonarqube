/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.process.jmx;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class JmxConnectionTest {

  private static final String CUSTOM_OBJECT_NAME = "Test:name=test";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public JmxTestServer jmxServer = new JmxTestServer();

  JmxConnection underTest;

  @Before
  public void setUp() throws Exception {
    jmxServer.getMBeanServer().registerMBean(new Fake(), new ObjectName(CUSTOM_OBJECT_NAME));
    JMXConnector jmxConnector = JMXConnectorFactory.newJMXConnector(jmxServer.getAddress(), null);
    jmxConnector.connect();
    underTest = new JmxConnection(jmxConnector);
  }

  @After
  public void tearDown() throws Exception {
    underTest.close();
    jmxServer.getMBeanServer().unregisterMBean(new ObjectName(CUSTOM_OBJECT_NAME));
  }

  @Test
  public void toMegaBytes() {
    assertThat(JmxConnection.toMegaBytes(-1)).isNull();
    assertThat(JmxConnection.toMegaBytes(0L)).isEqualTo(0L);
    assertThat(JmxConnection.toMegaBytes(500L)).isEqualTo(0L);
    assertThat(JmxConnection.toMegaBytes(500_000L)).isEqualTo(0L);
    assertThat(JmxConnection.toMegaBytes(500_000_000L)).isEqualTo(476L);
  }

  @Test
  public void get_platform_mbean() throws Exception {
    RuntimeMXBean runtimeMBean = underTest.getMBean(ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);

    assertThat(runtimeMBean).isNotNull();
  }

  @Test
  public void get_custom_mbean() throws Exception {
    FakeMBean mbean = underTest.getMBean(CUSTOM_OBJECT_NAME, FakeMBean.class);

    assertThat(mbean).isNotNull();
  }

  @Test
  public void getMBean_fails_if_does_not_exist() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to connect to MBean [unknown]");
    underTest.getMBean("unknown", FakeMBean.class);
  }

  @Test
  public void getSystemState() throws Exception {
    Map<String, Object> state = underTest.getSystemState();
    assertThat(state).containsKey("Heap Max (MB)");
    assertThat((int) state.get("Thread Count")).isGreaterThan(0);
  }
}
