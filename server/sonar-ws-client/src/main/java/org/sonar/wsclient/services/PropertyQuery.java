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

public class PropertyQuery extends Query<Property> {
  public static final String BASE_URL = "/api/properties";

  private String key = null;
  private String resourceKeyOrId = null;

  public String getKey() {
    return key;
  }

  public PropertyQuery setKey(String key) {
    this.key = key;
    return this;
  }

  public String getResourceKeyOrId() {
    return resourceKeyOrId;
  }

  public PropertyQuery setResourceKeyOrId(String resourceKeyOrId) {
    this.resourceKeyOrId = resourceKeyOrId;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    if (key != null) {
      url.append("/").append(encode(key));
    }
    url.append('?');
    appendUrlParameter(url, "resource", resourceKeyOrId);
    return url.toString();
  }

  @Override
  public Class<Property> getModelClass() {
    return Property.class;
  }

  public static PropertyQuery createForAll() {
    return new PropertyQuery();
  }

  public static PropertyQuery createForKey(String key) {
    return new PropertyQuery().setKey(key);
  }

  public static PropertyQuery createForResource(String key, String resourceKeyOrId) {
    return new PropertyQuery().setKey(key).setResourceKeyOrId(resourceKeyOrId);
  }
}
