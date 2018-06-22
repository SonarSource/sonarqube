/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.container;

import java.util.List;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.ComponentMonitor;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.OptInCaching;
import org.picocontainer.lifecycle.ReflectionLifecycleStrategy;
import org.picocontainer.monitors.NullComponentMonitor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.ContainerPopulator;
import org.sonar.core.platform.Module;
import org.sonar.core.platform.StopSafeReflectionLifecycleStrategy;

import static java.util.Objects.requireNonNull;

public class TaskContainerImpl extends ComponentContainer implements TaskContainer {

  public TaskContainerImpl(ComponentContainer parent, ContainerPopulator<TaskContainer> populator) {
    super(createContainer(requireNonNull(parent)), parent.getComponentByType(PropertyDefinitions.class));

    populateContainer(requireNonNull(populator));
  }

  private void populateContainer(ContainerPopulator<TaskContainer> populator) {
    populator.populateContainer(this);
    populateFromModules();
  }

  private void populateFromModules() {
    List<Module> modules = getComponentsByType(Module.class);
    for (Module module : modules) {
      module.configure(this);
    }
  }

  /**
   * Creates a PicContainer which extends the specified ComponentContainer <strong>but is not referenced in return</strong>
   * and lazily starts its components.
   */
  private static MutablePicoContainer createContainer(ComponentContainer parent) {
    ComponentMonitor componentMonitor = new NullComponentMonitor();
    ReflectionLifecycleStrategy lifecycleStrategy = new StopSafeReflectionLifecycleStrategy(componentMonitor) {
      @Override
      public boolean isLazy(ComponentAdapter<?> adapter) {
        return adapter.getComponentImplementation().getAnnotation(EagerStart.class) == null;
      }
    };

    return new DefaultPicoContainer(new OptInCaching(), lifecycleStrategy, parent.getPicoContainer(), componentMonitor);
  }

  @Override
  public void bootup() {
    startComponents();
  }

  @Override
  public String toString() {
    return "TaskContainerImpl";
  }

  @Override
  public void close() {
    try {
      stopComponents();
    } catch (Throwable t) {
      Loggers.get(TaskContainerImpl.class).error("Cleanup of container failed", t);
    }
  }
}
