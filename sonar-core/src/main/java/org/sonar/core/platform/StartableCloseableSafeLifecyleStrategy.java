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

import java.io.Closeable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Stream;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.LifecycleStrategy;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class StartableCloseableSafeLifecyleStrategy implements LifecycleStrategy, Serializable {
  private static final Class<?>[] TYPES_WITH_LIFECYCLE = new Class[] {Startable.class, org.sonar.api.Startable.class, Closeable.class, AutoCloseable.class};

  private static final Logger LOG = Loggers.get(StartableCloseableSafeLifecyleStrategy.class);

  @Override
  public void start(Object component) {
    if (component instanceof Startable) {
      ((Startable) component).start();
    } else if (component instanceof org.sonar.api.Startable) {
      ((org.sonar.api.Startable) component).start();
    }
  }

  @Override
  public void stop(Object component) {
    try {
      if (component instanceof Startable) {
        ((Startable) component).stop();
      } else if (component instanceof org.sonar.api.Startable) {
        ((org.sonar.api.Startable) component).stop();
      }
    } catch (RuntimeException | Error e) {
      Loggers.get(StartableCloseableSafeLifecyleStrategy.class)
        .warn("Stopping of component {} failed", component.getClass().getCanonicalName(), e);
    }
  }

  @Override
  public void dispose(Object component) {
    try {
      if (component instanceof Closeable) {
        ((Closeable) component).close();
      } else if (component instanceof AutoCloseable) {
        ((AutoCloseable) component).close();
      }
    } catch (Exception e) {
      Loggers.get(StartableCloseableSafeLifecyleStrategy.class)
        .warn("Dispose of component {} failed", component.getClass().getCanonicalName(), e);
    }
  }

  @Override
  public boolean hasLifecycle(Class<?> type) {
    if (Arrays.stream(TYPES_WITH_LIFECYCLE).anyMatch(t1 -> t1.isAssignableFrom(type))) {
      return true;
    }

    if (Stream.of("start", "stop").anyMatch(t -> hasMethod(type, t))) {
      LOG.warn("Component of type {} defines methods start() and/or stop(). Neither will be invoked to start/stop the component." +
        " Please implement either {} or {}",
        type, Startable.class.getName(), org.sonar.api.Startable.class.getName());
    }
    if (hasMethod(type, "close")) {
      LOG.warn("Component of type {} defines method close(). It won't be invoked to dispose the component." +
        " Please implement either {} or {}",
        type, Closeable.class.getName(), AutoCloseable.class.getName());
    }
    return false;
  }

  private static boolean hasMethod(Class<?> type, String methodName) {
    try {
      return type.getMethod(methodName) != null;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  @Override
  public boolean isLazy(ComponentAdapter<?> adapter) {
    return false;
  }
}
