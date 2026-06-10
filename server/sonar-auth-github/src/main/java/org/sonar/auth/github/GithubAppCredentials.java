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
package org.sonar.auth.github;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;

/**
 * Credentials returned by GitHub when converting a temporary code obtained through the
 * <a href="https://docs.github.com/en/apps/sharing-github-apps/registering-a-github-app-from-a-manifest">App Manifest flow</a>
 * (response of {@code POST /app-manifests/{code}/conversions}).
 */
public record GithubAppCredentials(
  @SerializedName("id") long id,
  @SerializedName("slug") @Nullable String slug,
  @SerializedName("client_id") String clientId,
  @SerializedName("client_secret") String clientSecret,
  @SerializedName("webhook_secret") @Nullable String webhookSecret,
  @SerializedName("pem") String pem,
  @SerializedName("html_url") @Nullable String htmlUrl) {

  /**
   * The GitHub App ID, as stored in SonarQube's ALM settings (a string).
   */
  public String getAppId() {
    return Long.toString(id);
  }
}
