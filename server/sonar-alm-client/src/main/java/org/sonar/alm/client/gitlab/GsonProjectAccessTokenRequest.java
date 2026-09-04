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
package org.sonar.alm.client.gitlab;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Request body for {@code POST /projects/:id/access_tokens}.
 * https://docs.gitlab.com/ee/api/project_access_tokens.html#create-a-project-access-token
 */
public class GsonProjectAccessTokenRequest {

  private static final int DEVELOPER_ACCESS_LEVEL = 40;

  @SerializedName("name")
  private final String name;

  @SerializedName("scopes")
  private final List<String> scopes;

  @SerializedName("expires_at")
  private final String expiresAt;

  @SerializedName("access_level")
  private final int accessLevel;

  public GsonProjectAccessTokenRequest(String name, List<String> scopes, String expiresAt) {
    this.name = name;
    this.scopes = scopes;
    this.expiresAt = expiresAt;
    this.accessLevel = DEVELOPER_ACCESS_LEVEL;
  }

  public String getName() {
    return name;
  }

  public List<String> getScopes() {
    return scopes;
  }

  public String getExpiresAt() {
    return expiresAt;
  }

  public int getAccessLevel() {
    return accessLevel;
  }
}
