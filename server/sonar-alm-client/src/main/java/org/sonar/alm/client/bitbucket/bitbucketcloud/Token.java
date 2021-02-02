/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client.bitbucket.bitbucketcloud;

import com.google.gson.annotations.SerializedName;

public class Token {
  @SerializedName("scopes")
  private String scopes;
  @SerializedName("access_token")
  private String accessToken;
  @SerializedName("exires_in")
  private long expiresIn;
  @SerializedName("token_type")
  private String tokenType;
  @SerializedName("state")
  private String state;
  @SerializedName("refresh_token")
  private String refreshToken;

  public Token() {
    // nothing to do here
  }

  public String getScopes() {
    return scopes;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public String getTokenType() {
    return tokenType;
  }

  public String getState() {
    return state;
  }

  public String getRefreshToken() {
    return refreshToken;
  }
}
