/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.authentication;

@SuppressWarnings("java:S1075")
public interface McpOAuthDiscovery {
  String MCP_PATH = "/mcp";
  String RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource/mcp";
  String AUTHORIZATION_METADATA_PATH = "/.well-known/oauth-authorization-server";

  boolean isEnabled();

  String challenge();

  static boolean isMetadataPath(String path) {
    return RESOURCE_METADATA_PATH.equals(path) || AUTHORIZATION_METADATA_PATH.equals(path) || path.startsWith(AUTHORIZATION_METADATA_PATH + "/");
  }

  static boolean isMcpPath(String path) {
    return MCP_PATH.equals(path) || path.startsWith(MCP_PATH + "/");
  }
}

