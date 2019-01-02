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
package org.sonar.server.property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Map based implementation of {@link InternalProperties} to be used for unit testing.
 */
public class MapInternalProperties implements InternalProperties {
  private final Map<String, String> values = new HashMap<>(1);

  @Override
  public Optional<String> read(String propertyKey) {
    checkPropertyKey(propertyKey);
    return Optional.ofNullable(values.get(propertyKey));
  }

  @Override
  public void write(String propertyKey, @Nullable String value) {
    checkPropertyKey(propertyKey);
    values.put(propertyKey, value);
  }

  private static void checkPropertyKey(@Nullable String propertyKey) {
    checkArgument(propertyKey != null && !propertyKey.isEmpty(), "property key can't be null nor empty");
  }
}
