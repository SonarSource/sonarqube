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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.sonar.api.Property;
import org.sonar.api.Startable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    assertThat(startStop.start).isOne();
    assertThat(startStop.stop).isOne();
  }

  @Test
  public void add_registers_instance_with_toString() {
    SpringComponentContainer container = new SimpleContainer(new ToString("a"), new ToString("b"));
    container.startComponents();
    assertThat(container.context.getBeanDefinitionNames())
      .contains(
        this.getClass().getClassLoader() + "-org.sonar.core.platform.SpringComponentContainerTest.ToString-a",
        this.getClass().getClassLoader() + "-org.sonar.core.platform.SpringComponentContainerTest.ToString-b");
    assertThat(container.getComponentsByType(ToString.class)).hasSize(2);
  }

  @Test
  public void add_registers_class_with_classloader_and_fqcn() {
    SpringComponentContainer container = new SimpleContainer(A.class, B.class);
    container.startComponents();
    assertThat(container.context.getBeanDefinitionNames())
      .contains(
        this.getClass().getClassLoader() + "-org.sonar.core.platform.SpringComponentContainerTest.A",
        this.getClass().getClassLoader() + "-org.sonar.core.platform.SpringComponentContainerTest.B");
    assertThat(container.getComponentByType(A.class)).isNotNull();
    assertThat(container.getComponentByType(B.class)).isNotNull();
  }

  @Test
  public void addWebConfigurationClass_addsClass() {
    SpringComponentContainer container = new SpringComponentContainer();
    container.addWebApiV2ConfigurationClass(org.sonar.core.test.Test.class);
    assertThat(container.getWebApiV2ConfigurationClasses()).contains(org.sonar.core.test.Test.class);
  }

  @Test
  public void add_configures_module_instances() {
    SpringComponentContainer container = new SpringComponentContainer();
    container.add(new TestModule());
    container.startComponents();
    assertThat(container.getComponentByType(A.class)).isNotNull();
  }

  @Test
  public void get_optional_component_by_type_should_return_correctly() {
    SpringComponentContainer container = new SpringComponentContainer();
    container.add(A.class);
    container.startComponents();
    assertThat(container.getOptionalComponentByType(A.class)).containsInstanceOf(A.class);
    assertThat(container.getOptionalComponentByType(B.class)).isEmpty();
  }

  @Test
  public void createChild_method_should_spawn_a_child_container(){
    SpringComponentContainer parent = new SpringComponentContainer();
    SpringComponentContainer child = parent.createChild();
    assertThat(child).isNotEqualTo(parent);
    assertThat(child.parent).isEqualTo(parent);
    assertThat(parent.children).contains(child);
  }

  @Test
  public void get_component_by_type_should_throw_exception_when_type_does_not_exist() {
    SpringComponentContainer container = new SpringComponentContainer();
    container.add(A.class);
    container.startComponents();
    assertThatThrownBy(() -> container.getComponentByType(B.class))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Unable to load component class org.sonar.core.platform.SpringComponentContainerTest$B");
  }

  @Test
  public void add_fails_if_adding_module_class() {
    SpringComponentContainer container = new SpringComponentContainer();
    container.startComponents();
    assertThatThrownBy(() -> container.add(TestModule.class))
      .hasMessage("Modules should be added as instances")
      .isInstanceOf(IllegalStateException.class);
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
  public void addExtension_supports_extensions_without_annotations() {
    SpringComponentContainer container = new SimpleContainer(A.class, B.class);
    container.addExtension("", ExtensionWithMultipleConstructorsAndNoAnnotations.class);
    container.startComponents();
    assertThat(container.getComponentByType(ExtensionWithMultipleConstructorsAndNoAnnotations.class).gotBothArgs).isTrue();
  }

  @Test
  public void addExtension_supports_extension_instances_without_annotations() {
    SpringComponentContainer container = new SpringComponentContainer();
    container.addExtension("", new ExtensionWithMultipleConstructorsAndNoAnnotations(new A()));
    container.startComponents();
    assertThat(container.getComponentByType(ExtensionWithMultipleConstructorsAndNoAnnotations.class)).isNotNull();
  }

  @Test
  public void addExtension_resolves_iterables() {
    List<Class<?>> classes = Arrays.asList(A.class, B.class);
    SpringComponentContainer container = new SpringComponentContainer();
    container.addExtension("", classes);
    container.startComponents();
    assertThat(container.getComponentByType(A.class)).isNotNull();
    assertThat(container.getComponentByType(B.class)).isNotNull();
  }

  @Test
  public void addExtension_adds_property_with_PluginInfo() {
    PluginInfo info = new PluginInfo("plugin1").setName("plugin1");
    SpringComponentContainer container = new SpringComponentContainer();
    container.addExtension(info, A.class);

    container.startComponents();
    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    PropertyDefinition propertyDefinition = propertyDefinitions.get("k");
    assertThat(propertyDefinition.key()).isEqualTo("k");
    assertThat(propertyDefinitions.getCategory("k")).isEqualTo("plugin1");
  }

  @Test
  public void declareExtension_adds_property() {
    SpringComponentContainer container = new SpringComponentContainer();
    container.addExtension((PluginInfo) null, A.class);

    container.startComponents();
    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    PropertyDefinition propertyDefinition = propertyDefinitions.get("k");
    assertThat(propertyDefinition.key()).isEqualTo("k");
    assertThat(propertyDefinitions.getCategory("k")).isEmpty();
  }

  @Test
  public void stop_should_stop_children() {
    SpringComponentContainer parent = new SpringComponentContainer();
    ApiStartable s1 = new ApiStartable();
    parent.add(s1);
    parent.startComponents();
    SpringComponentContainer child = new SpringComponentContainer(parent);
    assertThat(child.getParent()).isEqualTo(parent);
    assertThat(parent.children).containsOnly(child);
    ApiStartable s2 = new ApiStartable();
    child.add(s2);
    child.startComponents();

    parent.stopComponents();
    assertThat(s1.stop).isOne();
    assertThat(s2.stop).isOne();
  }

  @Test
  public void stop_should_remove_container_from_parent() {
    SpringComponentContainer parent = new SpringComponentContainer();
    SpringComponentContainer child = new SpringComponentContainer(parent);
    assertThat(parent.children).containsOnly(child);
    child.stopComponents();
    assertThat(parent.children).isEmpty();
  }

  @Test
  public void bean_create_fails_if_class_has_default_constructor_and_other_constructors() {
    SpringComponentContainer container = new SpringComponentContainer();
    container.add(ClassWithMultipleConstructorsIncNoArg.class);
    container.startComponents();
    assertThatThrownBy(() -> container.getComponentByType(ClassWithMultipleConstructorsIncNoArg.class))
      .hasRootCauseMessage("Constructor annotations missing in: class org.sonar.core.platform.SpringComponentContainerTest$ClassWithMultipleConstructorsIncNoArg");
  }

  @Test
  public void support_start_stop_callbacks() {
    JsrLifecycleCallbacks jsr = new JsrLifecycleCallbacks();
    ApiStartable api = new ApiStartable();
    AutoClose closeable = new AutoClose();

    SpringComponentContainer container = new SimpleContainer(jsr, api, closeable) {
      @Override
      public void doAfterStart() {
        // force lazy instantiation
        getComponentByType(JsrLifecycleCallbacks.class);
        getComponentByType(ApiStartable.class);
        getComponentByType(AutoClose.class);
      }
    };
    container.execute();

    assertThat(closeable.closed).isOne();
    assertThat(jsr.postConstruct).isOne();
    assertThat(jsr.preDestroy).isOne();
    assertThat(api.start).isOne();
    assertThat(api.stop).isOne();
  }

  private static class TestModule extends Module {
    @Override
    protected void configureModule() {
      add(A.class);
    }
  }

  private static class JsrLifecycleCallbacks {
    private int postConstruct = 0;
    private int preDestroy = 0;

    @PostConstruct
    public void postConstruct() {
      postConstruct++;
    }

    @PreDestroy
    public void preDestroy() {
      preDestroy++;
    }
  }

  private static class AutoClose implements AutoCloseable {
    private int closed = 0;

    @Override
    public void close() {
      closed++;
    }
  }

  private static class ApiStartable implements Startable {
    private int start = 0;
    private int stop = 0;

    public void start() {
      start++;
    }

    public void stop() {
      stop++;
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

  @Property(key = "k", name = "name")
  private static class A {
  }

  private static class B {
  }

  private static class ClassWithMultipleConstructorsIncNoArg {
    public ClassWithMultipleConstructorsIncNoArg() {
    }

    public ClassWithMultipleConstructorsIncNoArg(A a) {
    }
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
