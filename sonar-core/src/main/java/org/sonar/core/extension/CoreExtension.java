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
package org.sonar.core.extension;

import java.util.Collection;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;

import static java.util.Arrays.asList;

public interface CoreExtension {

  /**
   * Name of the core extension.
   * <p>
   * Used in the same fashion as the key for a plugin.
   */
  String getName();

  interface Context {
    SonarRuntime getRuntime();

    Configuration getBootConfiguration();

    Context addExtension(Object component);

    <T> Context addExtensions(Collection<T> o);

    default Context addExtensions(Object component, Object... otherComponents) {
      addExtension(component);
      addExtensions(asList(otherComponents));
      return this;
    }
  }

  void load(Context context);
}
