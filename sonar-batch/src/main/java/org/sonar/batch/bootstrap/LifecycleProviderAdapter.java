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
package org.sonar.batch.bootstrap;

import org.picocontainer.lifecycle.ReflectionLifecycleStrategy;
import org.picocontainer.monitors.NullComponentMonitor;

import org.picocontainer.LifecycleStrategy;
import org.picocontainer.PicoContainer;
import org.picocontainer.ComponentLifecycle;
import org.picocontainer.injectors.ProviderAdapter;
import org.picocontainer.Startable;

public abstract class LifecycleProviderAdapter extends ProviderAdapter implements Startable, ComponentLifecycle<Object> {
  private LifecycleStrategy lifecycleStrategy;
  protected Object instance;

  public LifecycleProviderAdapter() {
    this(new ReflectionLifecycleStrategy(new NullComponentMonitor()));
  }

  public LifecycleProviderAdapter(LifecycleStrategy lifecycleStrategy) {
    this.lifecycleStrategy = lifecycleStrategy;
  }

  @Override
  public final void start() {
    if (instance != null) {
      lifecycleStrategy.start(instance);
    }
  }

  @Override
  public final void stop() {
    if (instance != null) {
      lifecycleStrategy.stop(instance);
    }
  }

  @Override
  public void start(PicoContainer container) {
    start();
    started = true;
  }

  @Override
  public void stop(PicoContainer container) {
    stop();
    started = false;
  }

  @Override
  public void dispose(PicoContainer container) {
  }

  @Override
  public boolean componentHasLifecycle() {
    return true;
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  private boolean started = false;

}
