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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

public class PriorityBeanFactory extends DefaultListableBeanFactory {
  /**
   * Determines highest priority of the bean candidates.
   * Does not take into account the @Primary annotations.
   * This gets called from {@link DefaultListableBeanFactory#determineAutowireCandidate} when the bean factory is finding the beans to autowire. That method
   * checks for @Primary before calling this method.
   *
   * The strategy is to look at the @Priority annotations. If there are ties, we give priority to components that were added to child containers over their parents.
   * If there are still ties, null is returned, which will ultimately cause Spring to throw a NoUniqueBeanDefinitionException.
   */
  @Override
  @Nullable
  protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
    List<Bean> candidateBeans = candidates.entrySet().stream()
      .filter(e -> e.getValue() != null)
      .map(e -> new Bean(e.getKey(), e.getValue()))
      .toList();

    List<Bean> beansAfterPriority = highestPriority(candidateBeans, b -> getPriority(b.getInstance()));
    if (beansAfterPriority.isEmpty()) {
      return null;
    } else if (beansAfterPriority.size() == 1) {
      return beansAfterPriority.get(0).getName();
    }

    List<Bean> beansAfterHierarchy = highestPriority(beansAfterPriority, b -> getHierarchyPriority(b.getName()));
    if (beansAfterHierarchy.size() == 1) {
      return beansAfterHierarchy.get(0).getName();
    }

    return null;
  }

  private static List<Bean> highestPriority(List<Bean> candidates, PriorityFunction function) {
    List<Bean> highestPriorityBeans = new ArrayList<>();
    Integer highestPriority = null;

    for (Bean candidate : candidates) {
      Integer candidatePriority = function.classify(candidate);
      if (candidatePriority == null) {
        candidatePriority = Integer.MAX_VALUE;
      }
      if (highestPriority == null) {
        highestPriority = candidatePriority;
        highestPriorityBeans.add(candidate);
      } else if (candidatePriority < highestPriority) {
        highestPriorityBeans.clear();
        highestPriority = candidatePriority;
        highestPriorityBeans.add(candidate);
      } else if (candidatePriority.equals(highestPriority)) {
        highestPriorityBeans.add(candidate);
      }
    }
    return highestPriorityBeans;
  }

  @CheckForNull
  private Integer getHierarchyPriority(String beanName) {
    DefaultListableBeanFactory factory = this;
    int i = 1;
    while (factory != null) {
      if (factory.containsBeanDefinition(beanName)) {
        return i;
      }
      factory = (DefaultListableBeanFactory) factory.getParentBeanFactory();
      i++;
    }
    return null;
  }

  /**
   * A common mistake when migrating from Pico Container to Spring is to forget to add @Inject or @Autowire annotations to classes that have multiple constructors.
   * Spring will fail if there is no default no-arg constructor, but it will silently use the no-arg constructor if there is one, never calling the other constructors.
   * We override this method to fail fast if a class has multiple constructors.
   */
  @Override
  protected BeanWrapper instantiateBean(String beanName, RootBeanDefinition mbd) {
    if (mbd.hasBeanClass() && mbd.getBeanClass().getConstructors().length > 1) {
      throw new IllegalStateException("Constructor annotations missing in: " + mbd.getBeanClass());
    }
    return super.instantiateBean(beanName, mbd);
  }

  private static class Bean {
    private final String name;
    private final Object instance;

    public Bean(String name, Object instance) {
      this.name = name;
      this.instance = instance;
    }

    public String getName() {
      return name;
    }

    public Object getInstance() {
      return instance;
    }
  }

  @FunctionalInterface
  private interface PriorityFunction {
    @CheckForNull
    Integer classify(Bean candidate);
  }
}
