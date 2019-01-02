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
package org.sonar.core.platform;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.core.util.Uuids;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

class ComponentKeys {

  private static final Pattern IDENTITY_HASH_PATTERN = Pattern.compile(".+@[a-f0-9]+");
  private final Set<Class> objectsWithoutToString = new HashSet<>();

  Object of(Object component) {
    return of(component, Loggers.get(ComponentKeys.class));
  }

  Object of(Object component, Logger log) {
    if (component instanceof Class) {
      return component;
    }
    String key = component.toString();
    if (IDENTITY_HASH_PATTERN.matcher(key).matches()) {
      if (!objectsWithoutToString.add(component.getClass())) {
        log.warn(String.format("Bad component key: %s. Please implement toString() method on class %s", key, component.getClass().getName()));
      }
      key += Uuids.create();
    }
    return new StringBuilder().append(component.getClass().getCanonicalName()).append("-").append(key).toString();
  }
}
