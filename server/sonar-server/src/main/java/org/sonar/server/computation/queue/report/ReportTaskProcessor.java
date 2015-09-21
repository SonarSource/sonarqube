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
package org.sonar.server.computation.queue.report;

import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.computation.ComputationStepExecutor;
import org.sonar.server.computation.container.ComputeEngineContainer;
import org.sonar.server.computation.container.ContainerFactory;
import org.sonar.server.computation.queue.CeTask;

public class ReportTaskProcessor {

  private final ContainerFactory containerFactory;
  private final ComponentContainer serverContainer;

  public ReportTaskProcessor(ContainerFactory containerFactory, ComponentContainer serverContainer) {
    this.containerFactory = containerFactory;
    this.serverContainer = serverContainer;
  }

  public void process(CeTask task) {
    ComputeEngineContainer ceContainer = containerFactory.create(serverContainer, task);
    try {
      ceContainer.getComponentByType(ComputationStepExecutor.class).execute();
    } finally {
      ceContainer.cleanup();
    }
  }
}
