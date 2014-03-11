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
 * @since 2.4
 */
public final class UpdateCenterQuery extends Query<Plugin> {

  public static final String BASE_URL = "/api/updatecenter/";
  private String action;

  private UpdateCenterQuery(String action) {
    this.action = action;
  }

  @Override
  public Class<Plugin> getModelClass() {
    return Plugin.class;
  }

  @Override
  public String getUrl() {
    return BASE_URL + action;
  }

  public static UpdateCenterQuery createForInstalledPlugins() {
    return new UpdateCenterQuery("installed_plugins");
  }

}
