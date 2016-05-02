/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform;

import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.config.Settings;

/**
 * Defines some of the methods of {@link Settings} plus some specific to load db properties on the server side
 * (see {@link PersistentSettings}).
 */
public interface ServerSettings {
  ServerSettings activateDatabaseSettings(Map<String, String> databaseProperties);

  Settings getSettings();

  /**
   * @see Settings#getString(String)
   */
  String getString(String key);

  /**
   * @see Settings#getProperties()
   */
  Map<String, String> getProperties();

  /**
   * @see Settings#hasKey(String)
   */
  boolean hasKey(String foo);

  /**
   * @see Settings#setProperty(String, String)
   */
  Settings setProperty(String key, @Nullable String value);

  /**
   * @see Settings#removeProperty(String)
   */
  Settings removeProperty(String key);

  /**
   * @see Settings#clear()
   */
  Settings clear();

  /**
   * @see Settings#addProperties(Map)
   */
  Settings addProperties(Map<String, String> properties);
}
