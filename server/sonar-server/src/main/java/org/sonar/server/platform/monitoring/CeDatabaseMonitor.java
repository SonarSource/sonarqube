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
import java.util.LinkedHashMap;
import java.util.Map;
import org.sonar.process.ProcessId;
import org.sonar.process.jmx.CeDatabaseMBean;
import org.sonar.process.jmx.JmxConnection;
import org.sonar.process.jmx.JmxConnectionFactory;

public class CeDatabaseMonitor implements Monitor {

  private static final int NUMBER_OF_ATTRIBUTES = 9;

  private final JmxConnectionFactory jmxConnectionFactory;

  public CeDatabaseMonitor(JmxConnectionFactory jmxConnectionFactory) {
    this.jmxConnectionFactory = jmxConnectionFactory;
  }

  @Override
  public String name() {
    return "Compute Engine Database Connection";
  }

  @Override
  public Optional<Map<String, Object>> attributes() {
    try (JmxConnection connection = jmxConnectionFactory.create(ProcessId.COMPUTE_ENGINE)) {
      if (connection == null) {
        return Optional.absent();
      }
      Map<String, Object> result = new LinkedHashMap<>(NUMBER_OF_ATTRIBUTES);
      CeDatabaseMBean mbean = connection.getMBean(CeDatabaseMBean.OBJECT_NAME, CeDatabaseMBean.class);
      result.put("Pool Initial Size", mbean.getPoolInitialSize());
      result.put("Pool Active Connections", mbean.getPoolActiveConnections());
      result.put("Pool Idle Connections", mbean.getPoolIdleConnections());
      result.put("Pool Max Active Connections", mbean.getPoolMaxActiveConnections());
      result.put("Pool Max Idle Connections", mbean.getPoolMaxIdleConnections());
      result.put("Pool Min Idle Connections", mbean.getPoolMinIdleConnections());
      result.put("Pool Max Wait (ms)", mbean.getPoolMaxWaitMillis());
      result.put("Pool Remove Abandoned", mbean.getPoolRemoveAbandoned());
      result.put("Pool Remove Abandoned Timeout (sec)", mbean.getPoolRemoveAbandonedTimeoutSeconds());
      return Optional.of(result);
    }
  }
}
