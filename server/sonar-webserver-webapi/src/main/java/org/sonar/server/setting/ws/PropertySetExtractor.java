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
package org.sonar.server.setting.ws;

import com.google.common.base.Splitter;
import java.util.HashSet;
import java.util.Set;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.db.property.PropertyDto;

public class PropertySetExtractor {

  private static final Splitter COMMA_SPLITTER = Splitter.on(",");

  private PropertySetExtractor() {
    // Only static stuff
  }

  public static Set<String> extractPropertySetKeys(PropertyDto propertyDto, PropertyDefinition definition) {
    Set<String> propertySetKeys = new HashSet<>();
    definition.fields()
      .forEach(field -> COMMA_SPLITTER.splitToList(propertyDto.getValue())
        .forEach(setId -> propertySetKeys.add(generatePropertySetKey(propertyDto.getKey(), setId, field.key()))));
    return propertySetKeys;
  }

  private static String generatePropertySetKey(String propertyBaseKey, String id, String fieldKey) {
    return propertyBaseKey + "." + id + "." + fieldKey;
  }
}
