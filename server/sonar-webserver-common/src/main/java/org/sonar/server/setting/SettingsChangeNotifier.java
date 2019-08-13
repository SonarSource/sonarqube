/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.setting;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.config.GlobalPropertyChangeHandler;

import javax.annotation.Nullable;

public class SettingsChangeNotifier {

  @VisibleForTesting
  GlobalPropertyChangeHandler[] changeHandlers;

  public SettingsChangeNotifier(GlobalPropertyChangeHandler[] changeHandlers) {
    this.changeHandlers = changeHandlers;
  }

  public SettingsChangeNotifier() {
    this(new GlobalPropertyChangeHandler[0]);
  }

  public void onGlobalPropertyChange(String key, @Nullable String value) {
    GlobalPropertyChangeHandler.PropertyChange change = GlobalPropertyChangeHandler.PropertyChange.create(key, value);
    for (GlobalPropertyChangeHandler changeHandler : changeHandlers) {
      changeHandler.onChange(change);
    }
  }
}
