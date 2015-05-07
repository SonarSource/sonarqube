/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.config;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.ServerSide;

import javax.annotation.Nullable;

/**
 * Observe changes of global properties done from web application. It does not support :
 * <ul>
 * <li>changes done by end-users from the page "Project Settings"</li>
 * <li>changes done programmatically on the component org.sonar.api.config.Settings</li>
 * <li>changes done when restoring settings from XML using backup/restore feature</li>
 * </ul>
 *
 * @since 3.0
 */
@ServerSide
@ExtensionPoint
public abstract class GlobalPropertyChangeHandler {

  public static final class PropertyChange {
    private String key;
    private String newValue;

    private PropertyChange(String key, @Nullable String newValue) {
      this.key = key;
      this.newValue = newValue;
    }

    public static PropertyChange create(String key, @Nullable String newValue) {
      return new PropertyChange(key, newValue);
    }

    public String getKey() {
      return key;
    }

    public String getNewValue() {
      return newValue;
    }

    @Override
    public String toString() {
      return String.format("[key=%s, newValue=%s]", key, newValue);
    }
  }

  /**
   * This method gets called when a property is changed.
   */
  public abstract void onChange(PropertyChange change);

}
