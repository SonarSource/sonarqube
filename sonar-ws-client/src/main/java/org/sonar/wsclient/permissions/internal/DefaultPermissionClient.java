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

package org.sonar.wsclient.permissions.internal;

import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.permissions.PermissionClient;
import org.sonar.wsclient.permissions.PermissionParameters;

public class DefaultPermissionClient implements PermissionClient {

  private static final String BASE_URL = "/api/permissions/";
  private static final String ADD_URL = BASE_URL + "add";
  private static final String REMOVE_URL = BASE_URL + "remove";

  private final HttpRequestFactory requestFactory;

  /**
   * For internal use. Use {@link org.sonar.wsclient.SonarClient} to get an instance.
   */
  public DefaultPermissionClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public void addPermission(PermissionParameters permissionParameters) {
    requestFactory.post(ADD_URL, permissionParameters.urlParams());
  }

  @Override
  public void removePermission(PermissionParameters permissionParameters) {
    requestFactory.post(REMOVE_URL, permissionParameters.urlParams());
  }
}
