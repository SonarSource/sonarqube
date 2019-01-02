/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.async;

import org.picocontainer.Startable;
import org.sonar.process.Jmx;

public class AsyncExecutionMBeanImpl implements AsyncExecutionMBean, Startable {

  private final AsyncExecutionMonitoring asyncExecutionMonitoring;

  public AsyncExecutionMBeanImpl(AsyncExecutionMonitoring asyncExecutionMonitoring) {
    this.asyncExecutionMonitoring = asyncExecutionMonitoring;
  }

  @Override
  public void start() {
    Jmx.register(OBJECT_NAME, this);
  }

  @Override
  public void stop() {
    Jmx.unregister(OBJECT_NAME);
  }

  @Override
  public long getQueueSize() {
    return asyncExecutionMonitoring.getQueueSize();
  }

  @Override
  public long getWorkerCount() {
    return asyncExecutionMonitoring.getWorkerCount();
  }

  @Override
  public long getLargestWorkerCount() {
    return asyncExecutionMonitoring.getLargestWorkerCount();
  }
}
