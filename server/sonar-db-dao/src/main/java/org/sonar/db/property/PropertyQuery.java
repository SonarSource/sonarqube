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
package org.sonar.db.property;

import javax.annotation.Nullable;

public class PropertyQuery {

  private final String key;
  private final String componentUuid;
  private final String userUuid;

  private PropertyQuery(Builder builder) {
    this.key = builder.key;
    this.componentUuid = builder.componentUuid;
    this.userUuid = builder.userUuid;
  }

  public String key() {
    return key;
  }

  public String componentUuid() {
    return componentUuid;
  }

  public String userUuid() {
    return userUuid;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String key;
    private String componentUuid;
    private String userUuid;

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public Builder setComponentUuid(@Nullable String componentUuid) {
      this.componentUuid = componentUuid;
      return this;
    }

    public Builder setUserUuid(String userUuid) {
      this.userUuid = userUuid;
      return this;
    }

    public PropertyQuery build() {
      return new PropertyQuery(this);
    }
  }

}
