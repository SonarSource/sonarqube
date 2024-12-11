/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import jakarta.annotation.Priority;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PriorityBeanFactoryTest {
  private final DefaultListableBeanFactory parentBeanFactory = new PriorityBeanFactory();
  private final DefaultListableBeanFactory beanFactory = new PriorityBeanFactory();

  @Before
  public void setUp() {
    // needed to support autowiring with @Inject
    beanFactory.addBeanPostProcessor(new AutowiredAnnotationBeanPostProcessor());
    //needed to read @Priority
    beanFactory.setDependencyComparator(new AnnotationAwareOrderComparator());
    beanFactory.setParentBeanFactory(parentBeanFactory);
  }

  @Test
  public void give_priority_to_child_container() {
    parentBeanFactory.registerBeanDefinition("A1", new RootBeanDefinition(A1.class));

    beanFactory.registerBeanDefinition("A2", new RootBeanDefinition(A2.class));
    beanFactory.registerBeanDefinition("B", new RootBeanDefinition(B.class));

    assertThat(beanFactory.getBean(B.class).dep.getClass()).isEqualTo(A2.class);
  }

  @Test
  public void follow_priority_annotations() {
    parentBeanFactory.registerBeanDefinition("A3", new RootBeanDefinition(A3.class));

    beanFactory.registerBeanDefinition("A1", new RootBeanDefinition(A1.class));
    beanFactory.registerBeanDefinition("A2", new RootBeanDefinition(A2.class));
    beanFactory.registerBeanDefinition("B", new RootBeanDefinition(B.class));

    assertThat(beanFactory.getBean(B.class).dep.getClass()).isEqualTo(A3.class);
  }

  @Test
  public void throw_NoUniqueBeanDefinitionException_if_cant_find_single_bean_with_higher_priority() {
    beanFactory.registerBeanDefinition("A1", new RootBeanDefinition(A1.class));
    beanFactory.registerBeanDefinition("A2", new RootBeanDefinition(A2.class));
    beanFactory.registerBeanDefinition("B", new RootBeanDefinition(B.class));

    assertThatThrownBy(() -> beanFactory.getBean(B.class))
      .hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
  }

  private static class B {
    private final A dep;

    public B(A dep) {
      this.dep = dep;
    }
  }

  private interface A {

  }

  private static class A1 implements A {

  }

  private static class A2 implements A {

  }

  @Priority(1)
  private static class A3 implements A {

  }

}
