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
package org.sonar.wsclient.component;

import org.sonar.wsclient.unmarshallers.JsonUtils;

import javax.annotation.CheckForNull;

import java.util.Map;

public class Component {
  private final Map json;

  public Component(Map json) {
    this.json = json;
  }

  public String qualifier() {
    return JsonUtils.getString(json, "qualifier");
  }

  public String key() {
    return JsonUtils.getString(json, "key");
  }

  /**
   * @since 4.2
   */
  public Long id() {
    return JsonUtils.getLong(json, "id");
  }

  public String name() {
    return JsonUtils.getString(json, "name");
  }

  @CheckForNull
  public String longName() {
    return JsonUtils.getString(json, "longName");
  }

  /**
   * @since 4.2
   */
  @CheckForNull
  public Long subProjectId() {
    return JsonUtils.getLong(json, "subProjectId");
  }

  /**
   * @since 4.2
   */
  @CheckForNull
  public Long projectId() {
    return JsonUtils.getLong(json, "projectId");
  }

  /**
   * @since 4.2
   */
  @CheckForNull
  public String path() {
    return JsonUtils.getString(json, "path");
  }

}
