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
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.text.JsonWriter;

import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.OperationsException;

import java.lang.management.ManagementFactory;

public abstract class MonitoringMBean implements Startable, ServerComponent {

  abstract String name();

  abstract void toJson(JsonWriter json);

  @Override
  public void start() {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      ObjectName mbeanName = new ObjectName(String.format("SonarQube:name=%s", name()));
      server.registerMBean(this, mbeanName);
    } catch (OperationsException | MBeanRegistrationException e) {
      // TODO add logs as soon as the new log api is available
    }
  }

  @Override
  public void stop() {
    // do nothing
  }
}
