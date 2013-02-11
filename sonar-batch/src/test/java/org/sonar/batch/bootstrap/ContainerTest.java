/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.bootstrap;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ContainerTest {

  @Test
  public void shouldInitModule() {
    Container module = new FakeModule(FakeService.class);
    module.init();

    FakeService service = module.container.getComponentByType(FakeService.class);
    assertThat(service, not(nullValue()));
    assertThat(service.started, is(false));
    assertThat(module.container, notNullValue());
  }

  @Test
  public void shouldStartAndStopModule() {
    Container module = new FakeModule(FakeService.class);
    module.init();
    module.start();

    FakeService service = module.container.getComponentByType(FakeService.class);
    assertThat(service.started, is(true));

    module.stop();
    assertThat(service.started, is(false));
  }

  @Test(expected = RuntimeException.class)
  public void shouldNotIgnoreStartFailures() {
    Container module = new FakeModule(NonStartableService.class);
    module.init();
    module.start();
  }

  @Test
  public void shouldIgnoreStopFailures() {
    Container module = new FakeModule(NonStoppableService.class);
    module.init();
    module.start();
    module.stop(); // no exception is raised
  }

  @Test
  public void componentsShouldBeSingletons() {
    Container module = new FakeModule(FakeService.class);
    module.init();

    assertThat(module.container.getComponentByType(FakeService.class) == module.container.getComponentByType(FakeService.class), is(true));
  }

  @Test
  public void shouldInstallChildModule() {
    Container parent = new FakeModule(FakeService.class);
    parent.init();
    parent.start();

    Container child = parent.installChild(new FakeModule(ChildService.class));

    assertThat(parent.container.getComponentByType(ChildService.class), Matchers.nullValue());// child not accessible from parent
    assertThat(child.container.getComponentByType(FakeService.class), not(nullValue()));
    assertThat(child.container.getComponentByType(ChildService.class).started, is(false));
    assertThat(child.container.getComponentByType(ChildService.class).dependency, not(nullValue()));

    child.start();
    assertThat(child.container.getComponentByType(ChildService.class).started, is(true));

    child.stop();
    assertThat(child.container.getComponentByType(ChildService.class).started, is(false));
  }

  public static class FakeModule extends Container {
    private Class[] components;

    public FakeModule(Class... components) {
      this.components = components;
    }

    @Override
    protected void configure() {
      for (Class component : components) {
        container.addSingleton(component);
      }
    }

    @Override
    public boolean equals(Object obj) {
      return false;
    }

    @Override
    public int hashCode() {
      return 42;
    }
  }

  public static class FakeService {
    boolean started = false;

    public void start() {
      this.started = true;
    }

    public void stop() {
      this.started = false;
    }
  }

  public static class ChildService {
    private FakeService dependency;
    private boolean started = false;

    public ChildService(FakeService dependency) {
      this.dependency = dependency;
    }

    public void start() {
      this.started = true;
    }

    public void stop() {
      this.started = false;
    }
  }

  public static class NonStoppableService {
    public void stop() {
      throw new IllegalStateException("Can not stop !");
    }
  }

  public static class NonStartableService {
    public void start() {
      throw new IllegalStateException("Can not start !");
    }
  }

}
