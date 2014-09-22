/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.services;

/**
 * Get properties of the authenticated user.
 * 
 * @since 2.2
 */
public class UserPropertyQuery extends Query<Property> {
  public static final String BASE_URL = "/api/user_properties";

  private String key = null;

  /**
   * Get all user properties
   */
  public UserPropertyQuery() {
  }

  /**
   * Get only one specific user property
   */
  public UserPropertyQuery(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public UserPropertyQuery setKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public String getUrl() {
    String url = BASE_URL;
    if (key != null) {
      url += "/" + encode(key);
    }
    return url + "?";
  }

  @Override
  public Class<Property> getModelClass() {
    return Property.class;
  }
}
