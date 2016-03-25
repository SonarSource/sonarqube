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
import org.sonar.process.ProcessId;
import org.sonar.process.jmx.CeTasksMBean;
import org.sonar.process.jmx.JmxConnection;
import org.sonar.process.jmx.JmxConnectionFactory;

public class CeTasksMonitor implements Monitor {

  private final JmxConnectionFactory jmxConnectionFactory;

  public CeTasksMonitor(JmxConnectionFactory jmxConnectionFactory) {
    this.jmxConnectionFactory = jmxConnectionFactory;
  }

  @Override
  public String name() {
    return "Compute Engine Tasks";
  }

  @Override
  public LinkedHashMap<String, Object> attributes() {
    try (JmxConnection connection = jmxConnectionFactory.create(ProcessId.COMPUTE_ENGINE)) {
      LinkedHashMap<String, Object> result = new LinkedHashMap<>();
      CeTasksMBean ceMBean = connection.getMBean(CeTasksMBean.OBJECT_NAME, CeTasksMBean.class);
      result.put("Pending", ceMBean.getPendingCount());
      result.put("In Progress", ceMBean.getInProgressCount());
      result.put("Processed With Success", ceMBean.getSuccessCount());
      result.put("Processed With Error", ceMBean.getErrorCount());
      result.put("Processing Time (ms)", ceMBean.getProcessingTime());
      result.put("Worker Count", ceMBean.getWorkerCount());
      return result;
    }
  }
}
