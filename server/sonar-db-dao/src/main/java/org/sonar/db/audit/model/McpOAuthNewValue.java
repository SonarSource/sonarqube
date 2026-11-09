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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.DateUtils;

public class McpOAuthNewValue extends NewValue {

  @Nullable
  private final String userUuid;

  @Nullable
  private final String userLogin;

  @Nullable
  private final String clientId;

  @Nullable
  private final String scopes;

  @Nullable
  private final Long expiresAt;

  public McpOAuthNewValue(@Nullable String userUuid, @Nullable String userLogin, @Nullable String clientId, Set<String> scopes, @Nullable Long expiresAt) {
    this.userUuid = userUuid;
    this.userLogin = userLogin;
    this.clientId = clientId;
    this.scopes = scopes.isEmpty() ? null : String.join(" ", new TreeSet<>(scopes));
    this.expiresAt = expiresAt;
  }

  @CheckForNull
  public String getUserUuid() {
    return userUuid;
  }

  @CheckForNull
  public String getUserLogin() {
    return userLogin;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"userLogin\": ", userLogin, true);
    addField(sb, "\"clientId\": ", clientId, true);
    addField(sb, "\"scopes\": ", scopes, true);
    addField(sb, "\"expiresAt\": ", expiresAt == null ? "" : DateUtils.formatDateTime(expiresAt), true);
    endString(sb);
    return sb.toString();
  }
}
