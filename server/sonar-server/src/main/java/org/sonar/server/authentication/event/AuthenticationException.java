/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.authentication.event;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Exception thrown in case of authentication failure.
 * <p>
 * This exception contains the source of authentication and, if present, the login on which the login attempt occurred.
 * </p>
 * <p>
 * Given that {@link #source} and {@link #login} will be logged to file, be very careful <strong>not to set the login
 * when the login is a security token</strong>.
 * </p>
 */
public class AuthenticationException extends RuntimeException {
  private final AuthenticationEvent.Source source;
  @CheckForNull
  private final String login;
  private final String publicMessage;

  private AuthenticationException(Builder builder) {
    super(builder.message);
    this.source = requireNonNull(builder.source, "source can't be null");
    this.login = builder.login;
    this.publicMessage = builder.publicMessage;
  }

  public AuthenticationEvent.Source getSource() {
    return source;
  }

  @CheckForNull
  public String getLogin() {
    return login;
  }

  @CheckForNull
  public String getPublicMessage() {
    return publicMessage;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    @CheckForNull
    private AuthenticationEvent.Source source;
    @CheckForNull
    private String login;
    @CheckForNull
    private String message;
    @CheckForNull
    private String publicMessage;

    private Builder() {
      // use static factory method
    }

    public Builder setSource(AuthenticationEvent.Source source) {
      this.source = source;
      return this;
    }

    public Builder setLogin(@Nullable String login) {
      this.login = login;
      return this;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public Builder setPublicMessage(String publicMessage) {
      this.publicMessage = publicMessage;
      return this;
    }

    public AuthenticationException build() {
      return new AuthenticationException(this);
    }
  }
}
