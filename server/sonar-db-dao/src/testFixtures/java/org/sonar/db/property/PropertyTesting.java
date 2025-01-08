/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.security.SecureRandom;
import java.util.Random;
import javax.annotation.Nullable;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkNotNull;

public class PropertyTesting {

  private static final Random RANDOM = new SecureRandom();
  private static int cursor = RANDOM.nextInt(100);

  private PropertyTesting() {
    // static methods only
  }

  public static PropertyDto newGlobalPropertyDto(String key, String value) {
    return newPropertyDto(key, value, (String) null, null);
  }

  public static PropertyDto newGlobalPropertyDto() {
    return newPropertyDto((String) null, null);
  }

  public static PropertyDto newComponentPropertyDto(String key, String value, EntityDto component) {
    checkNotNull(component.getUuid());
    return newPropertyDto(key, value, component.getUuid(), null);
  }

  public static PropertyDto newComponentPropertyDto(EntityDto entity) {
    checkNotNull(entity.getUuid());
    return newPropertyDto(entity.getUuid(), null);
  }

  public static PropertyDto newUserPropertyDto(String key, String value, UserDto user) {
    checkNotNull(user.getUuid());
    return newPropertyDto(key, value, null, user.getUuid());
  }

  public static PropertyDto newUserPropertyDto(UserDto user) {
    checkNotNull(user.getUuid());
    return newPropertyDto(null, user.getUuid());
  }

  private static PropertyDto newPropertyDto(@Nullable String componentUuid, @Nullable String userUuid) {
    String key = String.valueOf(cursor);
    cursor++;
    String value = String.valueOf(cursor);
    cursor++;
    return newPropertyDto(key, value, componentUuid, userUuid);
  }

  private static PropertyDto newPropertyDto(String key, String value, @Nullable String componentUuid, @Nullable String userUuid) {
    PropertyDto propertyDto = new PropertyDto()
      .setKey(key)
      .setValue(value);
    if (componentUuid != null) {
      propertyDto.setEntityUuid(componentUuid);
    }
    if (userUuid != null) {
      propertyDto.setUserUuid(userUuid);
    }
    return propertyDto;
  }

}
