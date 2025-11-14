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
package org.sonar.core.platform;

import java.lang.reflect.Constructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;

/**
 * Taken from Spring's GenericApplicationContext.ClassDerivedBeanDefinition.
 * The goal is to support multiple constructors when adding extensions for plugins when no annotations are present.
 * Spring will pick the constructor with the highest number of arguments that it can inject.
 */
public class ClassDerivedBeanDefinition extends RootBeanDefinition {
  public ClassDerivedBeanDefinition(Class<?> beanClass) {
    super(beanClass);
  }

  public ClassDerivedBeanDefinition(ClassDerivedBeanDefinition original) {
    super(original);
  }

  /**
   * This method gets called from AbstractAutowireCapableBeanFactory#createBeanInstance when a bean is instantiated.
   * It first tries to look at annotations or any other methods provided by bean post processors. If a constructor can't be determined, it will fallback to this method.
   */
  @Override
  @Nullable
  public Constructor<?>[] getPreferredConstructors() {
    Class<?> clazz = getBeanClass();
    Constructor<?> primaryCtor = BeanUtils.findPrimaryConstructor(clazz);
    if (primaryCtor != null) {
      return new Constructor<?>[] {primaryCtor};
    }
    Constructor<?>[] publicCtors = clazz.getConstructors();
    if (publicCtors.length > 0) {
      return publicCtors;
    }
    return null;
  }

  @Override
  public RootBeanDefinition cloneBeanDefinition() {
    return new ClassDerivedBeanDefinition(this);
  }
}
