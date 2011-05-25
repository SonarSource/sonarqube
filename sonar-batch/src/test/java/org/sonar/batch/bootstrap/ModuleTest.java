/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
import org.sonar.batch.bootstrap.Module;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ModuleTest {

  @Test
  public void shouldInitModule() {
    Module module = new FakeModule(FakeService.class).init();

    FakeService service = module.getComponent(FakeService.class);
    assertThat(service, not(nullValue()));
    assertThat(service.started, is(false));
    assertThat(module.getContainer(), not(nullValue()));
  }

  @Test
  public void shouldStartAndStopModule() {
    Module module = new FakeModule(FakeService.class).init();
    module.start();

    FakeService service = module.getComponent(FakeService.class);
    assertThat(service.started, is(true));

    module.stop();
    assertThat(service.started, is(false));
  }

  @Test(expected = RuntimeException.class)
  public void shouldNotIgnoreStartFailures() {
    Module module = new FakeModule(NonStartableService.class).init();
    module.start();
  }

  @Test
  public void shouldIgnoreStopFailures() {
    Module module = new FakeModule(NonStoppableService.class).init();
    module.start();
    module.stop(); // no exception is raised
  }

  @Test
  public void componentsShouldBeSingletons() {
    Module module = new FakeModule(FakeService.class).init();

    assertThat(module.getComponent(FakeService.class)==module.getComponent(FakeService.class), is(true));
  }

  @Test
  public void shouldInstallChildModule() {
    Module parent = new FakeModule(FakeService.class).init();
    parent.start();

    Module child = parent.installChild(new FakeModule(ChildService.class));

    assertThat(parent.getComponent(ChildService.class), Matchers.nullValue());// child not accessible from parent
    assertThat(child.getComponent(FakeService.class), not(nullValue()));
    assertThat(child.getComponent(ChildService.class).started, is(false));
    assertThat(child.getComponent(ChildService.class).dependency, not(nullValue()));

    child.start();
    assertThat(child.getComponent(ChildService.class).started, is(true));

    child.stop();
    assertThat(child.getComponent(ChildService.class).started, is(false));
  }

  public static class FakeModule extends Module {
    private Class[] components;

    public FakeModule(Class... components) {
      this.components = components;
    }

    @Override
    protected void configure() {
      for (Class component : components) {
        addComponent(component);
      }
    }

    public boolean equals(Object obj) {
      return false;
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
