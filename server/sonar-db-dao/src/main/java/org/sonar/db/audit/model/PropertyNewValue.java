/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.audit.model;

import org.sonar.db.user.UserPropertyDto;

public class PropertyNewValue implements NewValue {
  private String propertyKey;
  private String propertyValue;
  private String userUuid;
  private String userLogin;

  public PropertyNewValue(UserPropertyDto userPropertyDto, String login) {
    this.propertyKey = userPropertyDto.getKey();
    this.userUuid = userPropertyDto.getUserUuid();
    this.userLogin = login;

    if(!propertyKey.contains(".secured")) {
      this.propertyValue = userPropertyDto.getValue();
    }
  }

  public String getPropertyKey() {
    return this.propertyKey;
  }

  public String getPropertyValue() {
    return this.propertyValue;
  }

  public String getUserUuid() {
    return this.userUuid;
  }

  public String getUserLogin() {
    return this.userLogin;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "'propertyKey':", this.propertyKey, true);
    addField(sb, "'propertyValue':", this.propertyValue, true);
    addField(sb, "'userUuid':", this.userUuid, true);
    addField(sb, "'userLogin':", this.userLogin, true);
    sb.append("}");
    return sb.toString();
  }

}
