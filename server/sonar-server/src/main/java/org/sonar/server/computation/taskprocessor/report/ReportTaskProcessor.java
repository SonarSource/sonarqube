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
package org.sonar.server.computation.taskprocessor.report;

import java.util.Collections;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.container.ComputeEngineContainer;
import org.sonar.server.computation.container.ContainerFactory;
import org.sonar.server.computation.queue.CeTask;
import org.sonar.server.computation.queue.CeTaskResult;
import org.sonar.server.computation.step.ComputationStepExecutor;
import org.sonar.server.computation.taskprocessor.CeTaskProcessor;
import org.sonar.server.computation.taskprocessor.TaskResultHolder;
import org.sonar.server.devcockpit.DevCockpitBridge;

public class ReportTaskProcessor implements CeTaskProcessor {

  private static final Set<String> HANDLED_TYPES = Collections.singleton(CeTaskTypes.REPORT);

  private final ContainerFactory containerFactory;
  private final ComponentContainer serverContainer;
  @CheckForNull
  private final DevCockpitBridge devCockpitBridge;

  /**
   * Used when Developer Cockpit plugin is installed
   */
  public ReportTaskProcessor(ContainerFactory containerFactory, ComponentContainer serverContainer, DevCockpitBridge devCockpitBridge) {
    this.containerFactory = containerFactory;
    this.serverContainer = serverContainer;
    this.devCockpitBridge = devCockpitBridge;
  }

  /**
   * Used when Developer Cockpit plugin is not installed
   */
  public ReportTaskProcessor(ContainerFactory containerFactory, ComponentContainer serverContainer) {
    this.containerFactory = containerFactory;
    this.serverContainer = serverContainer;
    this.devCockpitBridge = null;
  }

  @Override
  public Set<String> getHandledCeTaskTypes() {
    return HANDLED_TYPES;
  }

  @Override
  public CeTaskResult process(CeTask task) {
    ComputeEngineContainer ceContainer = containerFactory.create(serverContainer, task, devCockpitBridge);
    try {
      ceContainer.getComponentByType(ComputationStepExecutor.class).execute();
      return ceContainer.getComponentByType(TaskResultHolder.class).getResult();
    } finally {
      ceContainer.cleanup();
    }
  }
}
