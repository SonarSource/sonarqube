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
package org.sonar.alm.client.github.config;

import com.google.common.base.MoreObjects;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.lang.String.format;

public class GithubAppConfiguration {

  private static final Pattern TRAILING_SLASHES = Pattern.compile("/+$");

  private final Long id;
  private final String privateKey;
  private final String apiEndpoint;

  public GithubAppConfiguration(@Nullable Long id, @Nullable String privateKey, @Nullable String apiEndpoint) {
    this.id = id;
    this.privateKey = privateKey;
    this.apiEndpoint = sanitizedEndPoint(apiEndpoint);
  }

  /**
   * Check configuration is complete with {@link #isComplete()} before calling this method.
   *
   * @throws IllegalStateException if configuration is not complete
   */
  public long getId() {
    checkConfigurationComplete();
    return id;
  }

  public String getApiEndpoint() {
    checkConfigurationComplete();
    return apiEndpoint;
  }

  /**
   * Check configuration is complete with {@link #isComplete()} before calling this method.
   *
   * @throws IllegalStateException if configuration is not complete
   */
  public String getPrivateKey() {
    checkConfigurationComplete();
    return privateKey;
  }

  private void checkConfigurationComplete() {
    if (!isComplete()) {
      throw new IllegalStateException(format("Configuration is not complete : %s", toString()));
    }
  }

  public boolean isComplete() {
    return id != null &&
      privateKey != null &&
      apiEndpoint != null;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(GithubAppConfiguration.class)
      .add("id", id)
      .add("privateKey", secureToString(privateKey))
      .add("apiEndpoint", toString(apiEndpoint))
      .toString();
  }

  @CheckForNull
  private static String toString(@Nullable String s) {
    if (s == null) {
      return null;
    }
    return '\'' + s + '\'';
  }

  @CheckForNull
  private static String secureToString(@Nullable String token) {
    if (token == null) {
      return null;
    }
    return "'***(" + token.length() + ")***'";
  }

  private static String sanitizedEndPoint(@Nullable String endPoint) {
    if (endPoint == null) {
      return null;
    }
    return TRAILING_SLASHES.matcher(endPoint).replaceAll("");
  }

}
