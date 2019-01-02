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
package org.sonar.server.webhook;

import com.google.common.base.Supplier;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.repeat;

public final class HttpUrlHelper {
  private HttpUrlHelper() {
    // prevents instantiation
  }

  public static String obfuscateCredentials(String originalUrl) {
    HttpUrl parsedUrl = HttpUrl.parse(originalUrl);
    if (parsedUrl != null) {
      return obfuscateCredentials(originalUrl, parsedUrl);
    }
    return originalUrl;
  }

  /**
   * According to inline comment in {@link okhttp3.HttpUrl.Builder#parse(HttpUrl base, String input)}:
   * <blockquote>
   * Username, password and port are optional.
   * [username[:password]@]host[:port]
   * </blockquote>
   * <p>
   * This function replaces the chars of the username and the password from the {@code originalUrl} by '*' chars
   * based on username and password parsed in {@code parsedUrl}.
   */
  static String obfuscateCredentials(String originalUrl, HttpUrl parsedUrl) {
    String username = parsedUrl.username();
    String password = parsedUrl.password();
    if (username.isEmpty() && password.isEmpty()) {
      return originalUrl;
    }

    if (!username.isEmpty() && !password.isEmpty()) {
      String encodedUsername = parsedUrl.encodedUsername();
      String encodedPassword = parsedUrl.encodedPassword();
      return Stream.<Supplier<String>>of(
        () -> replaceOrDie(originalUrl, username, password),
        () -> replaceOrDie(originalUrl, encodedUsername, encodedPassword),
        () -> replaceOrDie(originalUrl, encodedUsername, password),
        () -> replaceOrDie(originalUrl, username, encodedPassword))
        .map(Supplier::get)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(originalUrl);
    }
    if (!username.isEmpty()) {
      return Stream.<Supplier<String>>of(
        () -> replaceOrDie(originalUrl, username, null),
        () -> replaceOrDie(originalUrl, parsedUrl.encodedUsername(), null))
        .map(Supplier::get)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(originalUrl);
    }
    checkState(password.isEmpty(), "having a password without a username should never occur");
    return originalUrl;
  }

  @CheckForNull
  private static String replaceOrDie(String original, String username, @Nullable String password) {
    return replaceOrDieImpl(original, authentStringOf(username, password), obfuscatedAuthentStringOf(username, password));
  }

  private static String authentStringOf(String username, @Nullable String password) {
    if (password == null) {
      return username + "@";
    }
    return username + ":" + password + "@";
  }

  private static String obfuscatedAuthentStringOf(String userName, @Nullable String password) {
    return authentStringOf(repeat("*", userName.length()), password == null ? null : repeat("*", password.length()));
  }

  @CheckForNull
  private static String replaceOrDieImpl(String original, String target, String replacement) {
    String res = original.replace(target, replacement);
    if (!res.equals(original)) {
      return res;
    }
    return null;
  }
}
