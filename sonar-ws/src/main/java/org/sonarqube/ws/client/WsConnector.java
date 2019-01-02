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
package org.sonarqube.ws.client;

/**
 * @since 5.3
 */
public interface WsConnector {

  /**
   * Server base URL, always with trailing slash, for instance "http://localhost:9000/"
   */
  String baseUrl();

  /**
   * @throws IllegalStateException if the request could not be executed due to
   *     a connectivity problem or timeout. Because networks can
   *     fail during an exchange, it is possible that the remote server
   *     accepted the request before the failure
   */
  WsResponse call(WsRequest wsRequest);

}
