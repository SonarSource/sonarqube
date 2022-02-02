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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.junit.Test;
import org.sonar.api.Startable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class SpringComponentContainerTest {

  @Test
  public void should_stop_after_failing() {
    ApiStartable startStop = new ApiStartable();
    SpringComponentContainer container = new SpringComponentContainer() {
      @Override
      public void doBeforeStart() {
        add(startStop);
      }

      @Override
      public void doAfterStart() {
        getComponentByType(ApiStartable.class);
        throw new IllegalStateException("doBeforeStart");
      }
    };

    assertThrows("doBeforeStart", IllegalStateException.class, container::execute);
    assertThat(startStop.start).isTrue();
    assertThat(startStop.stop).isTrue();
  }

  @Test
  public void register_instance_with_toString() {
    SpringComponentContainer container = new SimpleContainer(new ToString("a"), new ToString("b"));
    container.startComponents();
    assertThat(container.context.getBeanDefinitionNames())
      .contains(
        this.getClass().getClassLoader() + "-org.sonar.scanner.bootstrap.SpringComponentContainerTest.ToString-a",
        this.getClass().getClassLoader() + "-org.sonar.scanner.bootstrap.SpringComponentContainerTest.ToString-b");
    assertThat(container.getComponentsByType(ToString.class)).hasSize(2);
  }

  @Test
  public void register_class_with_classloader_and_fqcn() {
    SpringComponentContainer container = new SimpleContainer(A.class, B.class);
    container.startComponents();
    assertThat(container.context.getBeanDefinitionNames())
      .contains(
        this.getClass().getClassLoader() + "-org.sonar.scanner.bootstrap.SpringComponentContainerTest.A",
        this.getClass().getClassLoader() + "-org.sonar.scanner.bootstrap.SpringComponentContainerTest.B");
    assertThat(container.getComponentByType(A.class)).isNotNull();
    assertThat(container.getComponentByType(B.class)).isNotNull();
  }

  @Test
  public void should_throw_start_exception_if_stop_also_throws_exception() {
    ErrorStopClass errorStopClass = new ErrorStopClass();
    SpringComponentContainer container = new SpringComponentContainer() {
      @Override
      public void doBeforeStart() {
        add(errorStopClass);
      }

      @Override
      public void doAfterStart() {
        getComponentByType(ErrorStopClass.class);
        throw new IllegalStateException("doBeforeStart");
      }
    };
    assertThrows("doBeforeStart", IllegalStateException.class, container::execute);
    assertThat(errorStopClass.stopped).isTrue();
  }

  @Test
  public void should_support_extensions_without_annotations() {
    SpringComponentContainer container = new SimpleContainer(A.class, B.class);
    container.addExtension("", ExtensionWithMultipleConstructorsAndNoAnnotations.class);
    container.startComponents();
    assertThat(container.getComponentByType(ExtensionWithMultipleConstructorsAndNoAnnotations.class).gotBothArgs).isTrue();
  }

  @Test
  public void support_start_stop_callbacks() {
    JsrLifecycleCallbacks jsr = new JsrLifecycleCallbacks();
    ApiStartable api = new ApiStartable();
    PicoStartable pico = new PicoStartable();

    SpringComponentContainer container = new SimpleContainer(jsr, api, pico) {
      @Override
      public void doAfterStart() {
        // force lazy instantiation
        getComponentByType(JsrLifecycleCallbacks.class);
        getComponentByType(ApiStartable.class);
        getComponentByType(PicoStartable.class);
      }
    };
    container.execute();

    assertThat(jsr.postConstruct).isTrue();
    assertThat(jsr.preDestroy).isTrue();
    assertThat(api.start).isTrue();
    assertThat(api.stop).isTrue();
    assertThat(pico.start).isTrue();
    assertThat(pico.stop).isTrue();
  }

  private static class JsrLifecycleCallbacks {
    private boolean postConstruct = false;
    private boolean preDestroy = false;

    @PostConstruct
    public void postConstruct() {
      postConstruct = true;
    }

    @PreDestroy
    public void preDestroy() {
      preDestroy = true;
    }
  }

  private static class ApiStartable implements Startable {
    private boolean start = false;
    private boolean stop = false;

    public void start() {
      start = true;
    }

    public void stop() {
      stop = true;
    }
  }

  private static class PicoStartable implements org.picocontainer.Startable {
    private boolean start = false;
    private boolean stop = false;

    public void start() {
      start = true;
    }

    public void stop() {
      stop = true;
    }
  }

  private static class ToString {
    private final String toString;

    public ToString(String toString) {
      this.toString = toString;
    }

    @Override
    public String toString() {
      return toString;
    }
  }

  private static class A {
  }

  private static class B {
  }

  private static class ExtensionWithMultipleConstructorsAndNoAnnotations {
    private boolean gotBothArgs = false;
    public ExtensionWithMultipleConstructorsAndNoAnnotations(A a) {
    }

    public ExtensionWithMultipleConstructorsAndNoAnnotations(A a, B b) {
      gotBothArgs = true;
    }
  }

  private static class ErrorStopClass implements Startable {
    private boolean stopped = false;

    @Override
    public void start() {
    }

    @Override
    public void stop() {
      stopped = true;
      throw new IllegalStateException("stop");
    }
  }

  private static class SimpleContainer extends SpringComponentContainer {
    public SimpleContainer(Object... objects) {
      add(objects);
    }
  }
}
