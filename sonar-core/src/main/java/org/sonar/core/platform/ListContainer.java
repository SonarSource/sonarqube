/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

import static java.util.Collections.unmodifiableList;

/**
 * Intended to be used in tests
 */
public class ListContainer implements ExtensionContainer {
  private final List<Object> objects = new ArrayList<>();
  private final Set<Class<?>> webConfigurationClasses = new HashSet<>();

  @Override
  public Container add(Object... objects) {
    for (Object o : objects) {
      if (o instanceof Module module) {
        module.configure(this);
      } else if (o instanceof Iterable) {
        add(Iterables.toArray((Iterable<?>) o, Object.class));
      } else {
        this.objects.add(o);
      }
    }
    return this;
  }

  public List<Object> getAddedObjects() {
    return unmodifiableList(new ArrayList<>(objects));
  }

  @Override
  public <T> T getComponentByType(Class<T> type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> Optional<T> getOptionalComponentByType(Class<T> type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<T> getComponentsByType(Class<T> type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExtensionContainer addExtension(@Nullable PluginInfo pluginInfo, Object extension) {
    add(extension);
    return this;
  }

  @Override
  public ExtensionContainer addExtension(@Nullable String defaultCategory, Object extension) {
    add(extension);
    return this;
  }

  @Override
  public ExtensionContainer declareExtension(@Nullable PluginInfo pluginInfo, Object extension) {
    return this;
  }

  @Override
  public ExtensionContainer declareExtension(@Nullable String defaultCategory, Object extension) {
    return this;
  }

  @Override
  public void addWebApiV2ConfigurationClass(Class<?> clazz) {
    webConfigurationClasses.add(clazz);
  }

  @Override
  public Set<Class<?>> getWebApiV2ConfigurationClasses() {
    return webConfigurationClasses;
  }

  @Override
  public <T> T getParentComponentByType(Class<T> type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<T> getParentComponentsByType(Class<T> type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExtensionContainer getParent() {
    throw new UnsupportedOperationException();
  }
}
