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

import com.google.common.base.Optional;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.process.ProcessId;
import org.sonar.process.jmx.CeDatabaseMBean;
import org.sonar.process.jmx.JmxConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CeDatabaseMonitorTest {

  JmxConnectionFactory jmxConnectionFactory = mock(JmxConnectionFactory.class, Mockito.RETURNS_DEEP_STUBS);
  CeDatabaseMonitor underTest = new CeDatabaseMonitor(jmxConnectionFactory);

  @Test
  public void testName() {
    assertThat(underTest.name()).isNotEmpty();
  }

  @Test
  public void attributes() {
    CeDatabaseMBean mbean = mock(CeDatabaseMBean.class, Mockito.RETURNS_DEFAULTS);

    when(jmxConnectionFactory.create(ProcessId.COMPUTE_ENGINE).getMBean(CeDatabaseMBean.OBJECT_NAME, CeDatabaseMBean.class))
      .thenReturn(mbean);
    Optional<Map<String, Object>> attributes = underTest.attributes();
    assertThat(attributes.get()).containsKeys("Pool Initial Size", "Pool Active Connections");
    assertThat(attributes.get()).hasSize(9);
  }

  @Test
  public void absent_attributes_if_CE_is_down() {
    when(jmxConnectionFactory.create(ProcessId.COMPUTE_ENGINE)).thenReturn(null);
    Optional<Map<String, Object>> attributes = underTest.attributes();
    assertThat(attributes.isPresent()).isFalse();
  }
}
