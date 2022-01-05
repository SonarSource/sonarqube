/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import org.sonar.api.Startable;
import org.sonar.api.utils.log.Loggers;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.lang.Nullable;

public class StartableBeanPostProcessor implements DestructionAwareBeanPostProcessor {
  @Override
  @Nullable
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof Startable) {
      ((Startable) bean).start();
    } else if (bean instanceof org.picocontainer.Startable) {
      ((org.picocontainer.Startable) bean).start();
    }
    return bean;
  }

  @Override
  public boolean requiresDestruction(Object bean) {
    return (bean instanceof Startable) || (bean instanceof org.picocontainer.Startable) || (bean instanceof AutoCloseable);
  }

  @Override
  public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
    try {
      if (bean instanceof Startable) {
        ((Startable) bean).stop();
      } else if (bean instanceof org.picocontainer.Startable) {
        ((org.picocontainer.Startable) bean).stop();
      } else if (bean instanceof AutoCloseable) {
        ((AutoCloseable) bean).close();
      }
    } catch (Exception e) {
      Loggers.get(StartableBeanPostProcessor.class)
        .warn("Dispose of component {} failed", bean.getClass().getCanonicalName(), e);
    }
  }
}
