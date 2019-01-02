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
package org.sonar.core.platform;

import org.picocontainer.ComponentMonitor;
import org.picocontainer.lifecycle.ReflectionLifecycleStrategy;
import org.sonar.api.utils.log.Loggers;

/**
 * A {@link ReflectionLifecycleStrategy} which:
 * <li>
 *   <ul>implements support for methods {@code start()}, {@code stop()} and {@code close()} as methods of startable,
 *       stoppable and/or disposable components in a SonarQube container (whichever side the container is on)</ul>
 *   <ul>ensures that all stoppable and disposable components in a given container are stopped and/or disposed of
 *       even if a {@link RuntimeException} or a {@link Error} is thrown by one or more of those stoppable and/or
 *       disposable components</ul>
 * </li>
 */
public class StopSafeReflectionLifecycleStrategy extends ReflectionLifecycleStrategy {
  public StopSafeReflectionLifecycleStrategy(ComponentMonitor componentMonitor) {
    super(componentMonitor, "start", "stop", "close");
  }

  @Override
  public void stop(Object component) {
    try {
      super.stop(component);
    } catch (RuntimeException | Error e) {
      Loggers.get(StopSafeReflectionLifecycleStrategy.class)
        .warn("Stopping of component {} failed", component.getClass().getCanonicalName(), e);
    }
  }

  @Override
  public void dispose(Object component) {
    try {
      super.dispose(component);
    } catch (RuntimeException | Error e) {
      Loggers.get(StopSafeReflectionLifecycleStrategy.class)
        .warn("Dispose of component {} failed", component.getClass().getCanonicalName(), e);
    }
  }
}
