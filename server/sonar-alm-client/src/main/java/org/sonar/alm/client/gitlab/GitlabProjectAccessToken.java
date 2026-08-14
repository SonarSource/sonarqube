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
import javax.annotation.Nullable;

/**
 * Response body of {@code POST /projects/:id/access_tokens} — the {@code token} field (the secret
 * itself) is present only on creation, never on subsequent reads of the token.
 * https://docs.gitlab.com/ee/api/project_access_tokens.html#create-a-project-access-token
 *
 * <p>{@code id} is kept on this internal DTO even though it does not cross the {@code ScmAccessToken}
 * envelope boundary to external callers — it is the handle a future best-effort revoke call
 * ({@code DELETE /projects/:id/access_tokens/:token_id}) would need.
 */
public class GitlabProjectAccessToken {

  @SerializedName("id")
  private final long id;

  @SerializedName("name")
  private final String name;

  @SerializedName("token")
  private final String token;

  @SerializedName("expires_at")
  private final String expiresAt;

  @SerializedName("scopes")
  private final List<String> scopes;

  public GitlabProjectAccessToken() {
    // http://stackoverflow.com/a/18645370/229031
    this(0, null, null, null, List.of());
  }

  public GitlabProjectAccessToken(long id, @Nullable String name, @Nullable String token, @Nullable String expiresAt, List<String> scopes) {
    this.id = id;
    this.name = name;
    this.token = token;
    this.expiresAt = expiresAt;
    this.scopes = scopes;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getToken() {
    return token;
  }

  public String getExpiresAt() {
    return expiresAt;
  }

  public List<String> getScopes() {
    return scopes;
  }
}
