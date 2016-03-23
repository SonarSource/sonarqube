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
package org.sonar.server.platform.monitoring;

import java.util.LinkedHashMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.process.ProcessId;
import org.sonar.process.jmx.CeDatabaseMBean;
import org.sonar.process.jmx.JmxConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CeDatabaseMonitorTest {

  JmxConnector jmxConnector = mock(JmxConnector.class, Mockito.RETURNS_DEEP_STUBS);
  CeDatabaseMonitor underTest = new CeDatabaseMonitor(jmxConnector);

  @Test
  public void testName() {
    assertThat(underTest.name()).isNotEmpty();
  }

  @Test
  public void testAttributes() {
    CeDatabaseMBean mbean = mock(CeDatabaseMBean.class, Mockito.RETURNS_DEFAULTS);

    when(jmxConnector.connect(ProcessId.COMPUTE_ENGINE).getMBean(CeDatabaseMBean.OBJECT_NAME, CeDatabaseMBean.class))
      .thenReturn(mbean);
    LinkedHashMap<String, Object> attributes = underTest.attributes();
    assertThat(attributes).containsKeys("Pool Initial Size", "Pool Active Connections");
    assertThat(attributes).hasSize(9);
  }
}
