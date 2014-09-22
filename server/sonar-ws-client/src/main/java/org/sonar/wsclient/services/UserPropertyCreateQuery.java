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
 * @since 2.2
 */
public class UserPropertyCreateQuery extends CreateQuery<Property> {

  private String key;
  private String value;

  public UserPropertyCreateQuery() {
  }

  public UserPropertyCreateQuery(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public UserPropertyCreateQuery(Property property) {
    this.key = property.getKey();
    this.value = property.getValue();
  }

  public String getKey() {
    return key;
  }

  public UserPropertyCreateQuery setKey(String key) {
    this.key = key;
    return this;
  }

  public String getValue() {
    return value;
  }

  public UserPropertyCreateQuery setValue(String value) {
    this.value = value;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder sb = new StringBuilder();
    sb.append(UserPropertyQuery.BASE_URL);
    sb.append('?');
    appendUrlParameter(sb, "key", key);
    appendUrlParameter(sb, "value", value);
    return sb.toString();
  }

  @Override
  public Class<Property> getModelClass() {
    return Property.class;
  }
}
