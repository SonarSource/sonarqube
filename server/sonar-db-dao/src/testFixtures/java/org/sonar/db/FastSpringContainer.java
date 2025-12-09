/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.sonar.core.platform.Container;
import org.sonar.core.platform.StartableBeanPostProcessor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;

/**
 * A fast(er) Spring container. It doesn't support several things that are supported in
 * {@link org.sonar.core.platform.SpringComponentContainer}, such as:
 * <ul>
 *   <li>Adding extensions</li>
 *   <li>Use of annotations</li>
 *   <li>Adding duplicate components from different Classloaders</li>
 *   <li>Hierarchy of container</li>
 *   <li>Different initialization strategies</li>
 * </ul>
 */
public class FastSpringContainer implements Container {
  private final GenericApplicationContext context = new GenericApplicationContext();

  public FastSpringContainer() {
    ((AbstractAutowireCapableBeanFactory) context.getBeanFactory()).setParameterNameDiscoverer(null);
    add(StartableBeanPostProcessor.class);
  }

  @Override
  public Container add(Object... objects) {
    for (Object o : objects) {
      if (o instanceof Class) {
        Class<?> clazz = (Class<?>) o;
        context.registerBean(clazz.getName(), clazz);
      } else {
        registerInstance(o);
      }
    }
    return this;
  }

  public void start() {
    context.refresh();
  }

  private <T> void registerInstance(T instance) {
    Supplier<T> supplier = () -> instance;
    Class<T> clazz = (Class<T>) instance.getClass();
    context.registerBean(clazz.getName(), clazz, supplier);
  }

  @Override
  public <T> T getComponentByType(Class<T> type) {
    try {
      return context.getBean(type);
    } catch (Exception t) {
      throw new IllegalStateException("Unable to load component " + type, t);
    }
  }

  @Override public <T> Optional<T> getOptionalComponentByType(Class<T> type) {
    try {
      return Optional.of(context.getBean(type));
    } catch (NoSuchBeanDefinitionException t) {
      return Optional.empty();
    }
  }

  @Override
  public <T> List<T> getComponentsByType(Class<T> type) {
    try {
      return new ArrayList<>(context.getBeansOfType(type).values());
    } catch (Exception t) {
      throw new IllegalStateException("Unable to load components " + type, t);
    }
  }

  @Override
  public Container getParent() {
    return null;
  }
}
