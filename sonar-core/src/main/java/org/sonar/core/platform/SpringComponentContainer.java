/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.System2;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class SpringComponentContainer implements StartableContainer {
  protected final AnnotationConfigApplicationContext context;
  @Nullable
  protected final SpringComponentContainer parent;
  protected final List<SpringComponentContainer> children = new ArrayList<>();
  private final Set<Class<?>> webConfigurationClasses = new HashSet<>();

  private final PropertyDefinitions propertyDefinitions;
  private final ComponentKeys componentKeys = new ComponentKeys();

  public SpringComponentContainer() {
    this(null, new PropertyDefinitions(System2.INSTANCE), emptyList(), new LazyUnlessStartableStrategy());
  }

  protected SpringComponentContainer(List<?> externalExtensions) {
    this(null, new PropertyDefinitions(System2.INSTANCE), externalExtensions, new LazyUnlessStartableStrategy());
  }

  protected SpringComponentContainer(SpringComponentContainer parent) {
    this(parent, parent.propertyDefinitions, emptyList(), new LazyUnlessStartableStrategy());
  }

  protected SpringComponentContainer(SpringComponentContainer parent, List<?> externalExtensions) {
    this(parent, parent.propertyDefinitions, externalExtensions, new LazyUnlessStartableStrategy());
  }

  protected SpringComponentContainer(SpringComponentContainer parent, SpringInitStrategy initStrategy) {
    this(parent, parent.propertyDefinitions, emptyList(), initStrategy);
  }

  protected SpringComponentContainer(@Nullable SpringComponentContainer parent, PropertyDefinitions propertyDefs, List<?> externalExtensions, SpringInitStrategy initStrategy) {
    this.parent = parent;
    this.propertyDefinitions = propertyDefs;
    this.context = new AnnotationConfigApplicationContext(new PriorityBeanFactory());
    this.context.setAllowBeanDefinitionOverriding(false);
    ((AbstractAutowireCapableBeanFactory) context.getBeanFactory()).setParameterNameDiscoverer(null);
    if (parent != null) {
      context.setParent(parent.context);
      parent.children.add(this);
    }
    add(initStrategy);
    add(this);
    add(new StartableBeanPostProcessor());
    add(externalExtensions);
    add(propertyDefs);
  }

  //TODO: To be removed, added for moving on with the non matching LanguagesRepository beans
  public void addIfMissing(Object object, Class<?> objectType) {
    try {
      getParentComponentByType(objectType);
    } catch (IllegalStateException e) {
      add(object);
    }
  }

  /**
   * Beans need to have a unique name, otherwise they'll override each other.
   * The strategy is:
   * - For classes, use the classloader + fully qualified class name as the name of the bean
   * - For instances, use the Classloader + FQCN + toString()
   * - If the object is a collection, iterate through the elements and apply the same strategy for each of them
   */
  @Override
  public Container add(Object... objects) {
    for (Object o : objects) {
      if (o instanceof Class) {
        Class<?> clazz = (Class<?>) o;
        if (Module.class.isAssignableFrom(clazz)) {
          throw new IllegalStateException("Modules should be added as instances");
        }
        context.registerBean(componentKeys.ofClass(clazz), clazz);
        declareExtension("", o);
      } else if (o instanceof Module module) {
        module.configure(this);
      } else if (o instanceof Iterable) {
        add(Iterables.toArray((Iterable<?>) o, Object.class));
      } else {
        registerInstance(o);
        declareExtension("", o);
      }
    }
    return this;
  }

  @Override
  public void addWebApiV2ConfigurationClass(Class<?> clazz) {
    webConfigurationClasses.add(clazz);
  }

  @Override
  public Set<Class<?>> getWebApiV2ConfigurationClasses() {
    return Set.copyOf(webConfigurationClasses);
  }

  @Override
  public <T> T getParentComponentByType(Class<T> type) {
    if (parent == null) {
      throw new IllegalStateException("No parent container");
    } else {
      return parent.getComponentByType(type);
    }
  }

  @Override
  public <T> List<T> getParentComponentsByType(Class<T> type) {
    if (parent == null) {
      throw new IllegalStateException("No parent container");
    } else {
      return parent.getComponentsByType(type);
    }
  }

  private <T> void registerInstance(T instance) {
    Supplier<T> supplier = () -> instance;
    Class<T> clazz = (Class<T>) instance.getClass();
    context.registerBean(componentKeys.ofInstance(instance), clazz, supplier);
  }

  /**
   * Extensions are usually added by plugins and we assume they don't support any injection-related annotations.
   * Spring contexts supporting annotations will fail if multiple constructors are present without any annotations indicating which one to use for injection.
   * For that reason, we need to create the beans ourselves, using ClassDerivedBeanDefinition, which will declare that all constructors can be used for injection.
   */
  private Container addExtension(Object o) {
    if (o instanceof Class) {
      Class<?> clazz = (Class<?>) o;
      ClassDerivedBeanDefinition bd = new ClassDerivedBeanDefinition(clazz);
      context.registerBeanDefinition(componentKeys.ofClass(clazz), bd);
    } else if (o instanceof Iterable) {
      ((Iterable<?>) o).forEach(this::addExtension);
    } else {
      registerInstance(o);
    }
    return this;
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

  public AnnotationConfigApplicationContext context() {
    return context;
  }

  public void execute() {
    RuntimeException r = null;
    try {
      startComponents();
    } catch (RuntimeException e) {
      r = e;
    } finally {
      try {
        stopComponents();
      } catch (RuntimeException e) {
        if (r == null) {
          r = e;
        }
      }
    }
    if (r != null) {
      throw r;
    }
  }

  @Override
  public SpringComponentContainer startComponents() {
    doBeforeStart();
    context.refresh();
    doAfterStart();
    return this;
  }

  public SpringComponentContainer stopComponents() {
    try {
      stopChildren();
      if (context.isActive()) {
        context.close();
      }
    } finally {
      if (parent != null) {
        parent.children.remove(this);
      }
    }
    return this;
  }

  private void stopChildren() {
    // loop over a copy of list of children in reverse order
    Lists.reverse(new ArrayList<>(this.children)).forEach(SpringComponentContainer::stopComponents);
  }

  public SpringComponentContainer createChild() {
    return new SpringComponentContainer(this);
  }

  @Override
  @CheckForNull
  public SpringComponentContainer getParent() {
    return parent;
  }

  @Override
  public SpringComponentContainer addExtension(@Nullable PluginInfo pluginInfo, Object extension) {
    addExtension(extension);
    declareExtension(pluginInfo, extension);
    return this;
  }

  @Override
  public SpringComponentContainer addExtension(@Nullable String defaultCategory, Object extension) {
    addExtension(extension);
    declareExtension(defaultCategory, extension);
    return this;
  }

  @Override
  public SpringComponentContainer declareExtension(@Nullable PluginInfo pluginInfo, Object extension) {
    declareExtension(pluginInfo != null ? pluginInfo.getName() : "", extension);
    return this;
  }

  @Override
  public SpringComponentContainer declareExtension(@Nullable String defaultCategory, Object extension) {
    this.propertyDefinitions.addComponent(extension, ofNullable(defaultCategory).orElse(""));
    return this;
  }

  /**
   * This method aims to be overridden
   */
  protected void doBeforeStart() {
    // nothing
  }

  /**
   * This method aims to be overridden
   */
  protected void doAfterStart() {
    // nothing
  }
}
