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
package org.sonar.db.property;

import javax.annotation.Nullable;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkNotNull;

public class PropertyTesting {

  private static int cursor = RandomUtils.nextInt(100);

  private PropertyTesting() {
    // static methods only
  }

  public static PropertyDto newGlobalPropertyDto(String key, String value) {
    return newPropertyDto(key, value, (Long) null, null);
  }

  public static PropertyDto newGlobalPropertyDto() {
    return newPropertyDto((Long) null, null);
  }

  public static PropertyDto newComponentPropertyDto(String key, String value, ComponentDto component) {
    checkNotNull(component.getId());
    return newPropertyDto(key, value, component.getId(), null);
  }

  public static PropertyDto newComponentPropertyDto(ComponentDto component) {
    checkNotNull(component.getId());
    return newPropertyDto(component.getId(), null);
  }

  public static PropertyDto newUserPropertyDto(String key, String value, UserDto user) {
    checkNotNull(user.getId());
    return newPropertyDto(key, value, null, user.getId());
  }

  public static PropertyDto newUserPropertyDto(UserDto user) {
    checkNotNull(user.getId());
    return newPropertyDto(null, user.getId());
  }

  public static PropertyDto newPropertyDto(String key, String value, ComponentDto component, UserDto user) {
    checkNotNull(component.getId());
    checkNotNull(user.getId());
    return newPropertyDto(key, value, component.getId(), user.getId());
  }

  public static PropertyDto newPropertyDto(ComponentDto component, UserDto user) {
    checkNotNull(component.getId());
    checkNotNull(user.getId());
    return newPropertyDto(component.getId(), user.getId());
  }

  private static PropertyDto newPropertyDto(@Nullable Long componentId, @Nullable Integer userId) {
    String key = String.valueOf(cursor);
    cursor++;
    String value = String.valueOf(cursor);
    cursor++;
    return newPropertyDto(key, value, componentId, userId);
  }

  private static PropertyDto newPropertyDto(String key, String value, @Nullable Long componentId, @Nullable Integer userId) {
    PropertyDto propertyDto = new PropertyDto()
      .setKey(key)
      .setValue(value);
    if (componentId != null) {
      propertyDto.setResourceId(componentId);
    }
    if (userId != null) {
      propertyDto.setUserId(userId);
    }
    return propertyDto;
  }

}
