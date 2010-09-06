/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.utils;

import org.picocontainer.Characteristics;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.OptInCaching;
import org.picocontainer.lifecycle.ReflectionLifecycleStrategy;
import org.picocontainer.monitors.NullComponentMonitor;


/**
 * Proxy to inject the container as a component$
 *
 * @since 1.10
 */
public class IocContainer {
  private final MutablePicoContainer pico;

  public IocContainer(MutablePicoContainer pico) {
    this.pico = pico;
  }

  public MutablePicoContainer getPicoContainer() {
    return pico;
  }

  public static MutablePicoContainer buildPicoContainer() {
    ReflectionLifecycleStrategy lifecycleStrategy = new ReflectionLifecycleStrategy(new
        NullComponentMonitor(), "start", "stop", "dispose");

    DefaultPicoContainer result = new DefaultPicoContainer(new OptInCaching(), lifecycleStrategy, null);
    result.as(Characteristics.CACHE).addComponent(new IocContainer(result)); // for components that directly inject other components (eg Plugins)
    return result;
  }
}
