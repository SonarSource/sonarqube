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
package org.sonar.server.computation.container;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.List;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.ComponentMonitor;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.behaviors.OptInCaching;
import org.picocontainer.lifecycle.ReflectionLifecycleStrategy;
import org.picocontainer.monitors.ComponentMonitorHelper;
import org.picocontainer.monitors.NullComponentMonitor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.ContainerPopulator;
import org.sonar.core.platform.Module;

import static java.util.Objects.requireNonNull;
import static org.picocontainer.monitors.ComponentMonitorHelper.ctorToString;
import static org.picocontainer.monitors.ComponentMonitorHelper.format;
import static org.picocontainer.monitors.ComponentMonitorHelper.methodToString;
import static org.picocontainer.monitors.ComponentMonitorHelper.parmsToString;

public class ComputeEngineContainerImpl extends ComponentContainer implements ComputeEngineContainer {
  private static final Logger LOG = Loggers.get(ComputeEngineContainerImpl.class);

  public ComputeEngineContainerImpl(ComponentContainer parent, ContainerPopulator<ComputeEngineContainer> populator) {
    super(createContainer(requireNonNull(parent)));

    populateContainer(requireNonNull(populator));
    startComponents();
  }

  private void populateContainer(ContainerPopulator<ComputeEngineContainer> populator) {
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
    ComponentMonitor componentMonitor = instanceComponentMonitor();
    ReflectionLifecycleStrategy lifecycleStrategy = new ReflectionLifecycleStrategy(componentMonitor, "start", "stop", "close") {
      @Override
      public boolean isLazy(ComponentAdapter<?> adapter) {
        return true;
      }
    };

    return new DefaultPicoContainer(new OptInCaching(), lifecycleStrategy, parent.getPicoContainer(), componentMonitor);
  }

  private static ComponentMonitor instanceComponentMonitor() {
    if (!LOG.isTraceEnabled()) {
      return new NullComponentMonitor();
    }
    return new ComputeEngineComponentMonitor();
  }

  private static class ComputeEngineComponentMonitor extends NullComponentMonitor {

    @Override
    public <T> void instantiated(PicoContainer container, ComponentAdapter<T> componentAdapter,
      Constructor<T> constructor, Object instantiated, Object[] parameters, long duration) {
      LOG.trace(format(ComponentMonitorHelper.INSTANTIATED, ctorToString(constructor), duration, instantiated.getClass().getName(), parmsToString(parameters)));
    }

    @Override
    public void invoked(PicoContainer container, ComponentAdapter<?> componentAdapter, Member member, Object instance, long duration, Object[] args, Object retVal) {
      LOG.trace(format(ComponentMonitorHelper.INVOKED, methodToString(member), instance, duration));
    }

  }

  @Override
  public void cleanup() {
    try {
      stopComponents();
    } catch (Throwable t) {
      Loggers.get(ComputeEngineContainerImpl.class).error("Cleanup of container failed", t);
    }
  }

  @Override
  public String toString() {
    return "ComputeEngineContainerImpl";
  }
}
