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

import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public interface ExtensionContainer extends Container {
  ExtensionContainer addExtension(@Nullable PluginInfo pluginInfo, Object extension);

  ExtensionContainer addExtension(@Nullable String defaultCategory, Object extension);

  ExtensionContainer declareExtension(@Nullable PluginInfo pluginInfo, Object extension);

  ExtensionContainer declareExtension(@Nullable String defaultCategory, Object extension);

  void addWebApiV2ConfigurationClass(Class<?> clazz);

  Set<Class<?>> getWebApiV2ConfigurationClasses();

  <T> T getParentComponentByType(Class<T> type);

  <T> List<T> getParentComponentsByType(Class<T> type);

  @CheckForNull
  ExtensionContainer getParent();
}
