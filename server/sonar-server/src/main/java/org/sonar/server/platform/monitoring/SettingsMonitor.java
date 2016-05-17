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
package org.sonar.server.platform.monitoring;

import com.google.common.collect.ImmutableSortedMap;
import java.util.Map;
import java.util.SortedMap;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;

import static org.apache.commons.lang.StringUtils.abbreviate;

public class SettingsMonitor implements Monitor {

  static final int MAX_VALUE_LENGTH = 500;
  private final Settings settings;

  public SettingsMonitor(Settings settings) {
    this.settings = settings;
  }

  @Override
  public String name() {
    return "Settings";
  }

  @Override
  public SortedMap<String, Object> attributes() {
    PropertyDefinitions definitions = settings.getDefinitions();
    ImmutableSortedMap.Builder<String, Object> builder = ImmutableSortedMap.naturalOrder();
    for (Map.Entry<String, String> prop : settings.getProperties().entrySet()) {
      String key = prop.getKey();
      PropertyDefinition def = definitions.get(key);
      if (def == null || def.type() != PropertyType.PASSWORD) {
        builder.put(key, abbreviate(prop.getValue(), MAX_VALUE_LENGTH));
      }
    }
    return builder.build();
  }
}
