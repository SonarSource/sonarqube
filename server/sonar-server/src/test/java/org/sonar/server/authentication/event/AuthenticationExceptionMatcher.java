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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

/**
 * Matcher for {@link AuthenticationException} to be used with {@link org.junit.rules.ExpectedException} JUnit Rule.
 *
 * <p>
 * Usage:
 * <pre>
 * expectedException.expect(authenticationException().from(Source.local(Method.BASIC_TOKEN)).withLogin("foo").andNoPublicMessage());
 * </pre>
 * </p>
 */
public class AuthenticationExceptionMatcher extends TypeSafeMatcher<Throwable> {
  private final Source source;
  @CheckForNull
  private final String login;
  @CheckForNull
  private final String publicMessage;

  private AuthenticationExceptionMatcher(Source source, @Nullable String login, @Nullable String publicMessage) {
    this.source = requireNonNull(source, "source can't be null");
    this.login = login;
    this.publicMessage = publicMessage;
  }

  public static Builder authenticationException() {
    return new Builder();
  }

  public static class Builder {
    private Source source;
    private String login;

    public Builder from(Source source) {
      this.source = checkSource(source);
      return this;
    }

    public Builder withLogin(String login) {
      this.login = requireNonNull(login, "expected login can't be null");
      return this;
    }

    public Builder withoutLogin() {
      this.login = null;
      return this;
    }

    public AuthenticationExceptionMatcher andNoPublicMessage() {
      return new AuthenticationExceptionMatcher(this.source, this.login, null);
    }

    public AuthenticationExceptionMatcher andPublicMessage(String publicMessage){
      return new AuthenticationExceptionMatcher(this.source, this.login, requireNonNull(publicMessage));
    }

    private static Source checkSource(Source source) {
      return requireNonNull(source, "expected source can't be null");
    }
  }

  @Override
  protected boolean matchesSafely(Throwable throwable) {
    return check(throwable) == null;
  }

  private String check(Throwable throwable) {
    if (!throwable.getClass().isAssignableFrom(AuthenticationException.class)) {
      return "exception is not a AuthenticationException";
    }
    AuthenticationException authenticationException = (AuthenticationException) throwable;
    if (!this.source.equals(authenticationException.getSource())) {
      return "source is \"" + authenticationException.getSource() + "\"";
    }

    String login = authenticationException.getLogin();
    if (this.login == null) {
      if (login != null) {
        return "login is \"" + login + "\"";
      }
    } else if (login == null) {
      return "login is null";
    } else if (!this.login.equals(login)) {
      return "login is \"" + login + "\"";
    }

    String publicMessage = authenticationException.getPublicMessage();
    if (this.publicMessage == null) {
      if (publicMessage != null) {
        return "publicMessage is \"" + publicMessage + "\"";
      }
    } else if (publicMessage == null) {
      return "publicMessage is null";
    } else if (!this.publicMessage.equals(publicMessage)) {
      return "publicMessage is \"" + publicMessage + "\"";
    }

    return null;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("AuthenticationException with source ").appendText(source.toString());
    if (login == null) {
      description.appendText(", no login");
    } else {
      description.appendText(", login \"").appendText(login).appendText("\"");
    }
    if (publicMessage == null) {
      description.appendText(" and no publicMessage");
    } else {
      description.appendText(" and publicMessage \"").appendText(publicMessage).appendText("\"");
    }
  }

  @Override
  protected void describeMismatchSafely(Throwable item, Description mismatchDescription) {
    mismatchDescription.appendText(check(item));
  }
}
