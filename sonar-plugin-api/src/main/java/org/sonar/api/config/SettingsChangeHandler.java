/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.sonar.api.ServerExtension;

import javax.annotation.Nullable;

/**
 * Observe changes of properties done from web application. It does not support :
 * <ul>
 * <li>changes done programmatically on the component org.sonar.api.config.Settings</li>
 * </ul>
 *
 * @since 4.0
 */
public interface SettingsChangeHandler extends ServerExtension {

  public static final class SettingsChange {
    private String key;
    private String newValue;
    private String componentKey;
    private String userLogin;

    private SettingsChange(String key, @Nullable String newValue, @Nullable String componentKey, @Nullable String userLogin) {
      this.key = key;
      this.newValue = newValue;
      this.componentKey = componentKey;
      this.userLogin = userLogin;
    }

    public static SettingsChange create(String key, @Nullable String newValue, @Nullable String componentKey, @Nullable String userLogin) {
      return new SettingsChange(key, newValue, componentKey, userLogin);
    }

    public String key() {
      return key;
    }

    public String newValue() {
      return newValue;
    }

    public String componentKey() {
      return componentKey;
    }

    public String userLogin() {
      return userLogin;
    }

    public boolean isGlobal() {
      return componentKey == null && userLogin == null;
    }

    @Override
    public String toString() {
      return String.format("[key=%s, newValue=%s, componentKey=%s]", key, newValue, componentKey);
    }
  }

  /**
   * This method gets called when a property is changed.
   */
  public void onChange(SettingsChange change);

}
