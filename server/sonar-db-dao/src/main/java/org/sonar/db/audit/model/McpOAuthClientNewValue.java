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
package org.sonar.db.audit.model;

import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

public class McpOAuthClientNewValue extends NewValue {

  private final String clientId;

  @Nullable
  private final String clientName;

  @Nullable
  private final String redirectUris;

  public McpOAuthClientNewValue(String clientId, @Nullable String clientName, Set<String> redirectUris) {
    this.clientId = clientId;
    this.clientName = clientName;
    this.redirectUris = redirectUris.isEmpty() ? null : String.join(" ", new TreeSet<>(redirectUris));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"clientId\": ", clientId, true);
    addField(sb, "\"clientName\": ", clientName, true);
    addField(sb, "\"redirectUris\": ", redirectUris, true);
    endString(sb);
    return sb.toString();
  }
}
