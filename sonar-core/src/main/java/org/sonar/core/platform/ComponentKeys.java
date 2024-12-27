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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.util.Uuids;

public class ComponentKeys {
  private static final Logger LOG = LoggerFactory.getLogger(ComponentKeys.class);
  private static final Pattern IDENTITY_HASH_PATTERN = Pattern.compile(".+@[a-f0-9]+");
  private final Set<Class> objectsWithoutToString = new HashSet<>();

  Object of(Object component) {
    return of(component, LOG);
  }

  Object of(Object component, Logger log) {
    if (component instanceof Class) {
      return component;
    }
    return ofInstance(component, log);
  }

  public String ofInstance(Object component) {
    return ofInstance(component, LOG);
  }

  public String ofClass(Class<?> clazz) {
    return clazz.getClassLoader() + "-" + clazz.getCanonicalName();
  }

  String ofInstance(Object component, Logger log) {
    String key = component.toString();
    if (IDENTITY_HASH_PATTERN.matcher(key).matches()) {
      if (!objectsWithoutToString.add(component.getClass())) {
        log.warn("Bad component key: {}. Please implement toString() method on class {}", key, component.getClass().getName());
      }
      key += Uuids.create();
    }
    return ofClass(component.getClass()) + "-" + key;
  }
}
