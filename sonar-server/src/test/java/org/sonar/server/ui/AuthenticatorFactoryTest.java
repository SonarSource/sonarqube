/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.ui;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.security.LoginPasswordAuthenticator;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class AuthenticatorFactoryTest {

  @Test
  public void doNotFailIfNoAuthenticationPlugins() {
    AuthenticatorFactory factory = new AuthenticatorFactory(new PropertiesConfiguration());
    assertThat(factory.getAuthenticator(), nullValue());
  }

  @Test
  public void startSelectedAuthenticator() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.CORE_AUTHENTICATOR_CLASS, FakeAuthenticator.class.getName());

    LoginPasswordAuthenticator authenticator = new FakeAuthenticator();
    AuthenticatorFactory factory = new AuthenticatorFactory(configuration, new LoginPasswordAuthenticator[]{authenticator});
    factory.start();
    assertThat(factory.getAuthenticator(), is(authenticator));
  }

  @Test(expected = ConnectionException.class)
  public void authenticatorDoesNotStart() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.CORE_AUTHENTICATOR_CLASS, FailAuthenticator.class.getName());

    AuthenticatorFactory factory = new AuthenticatorFactory(configuration, new LoginPasswordAuthenticator[]{new FakeAuthenticator(), new FailAuthenticator()});
    factory.start();
    factory.getAuthenticator();
  }

  @Test(expected = AuthenticatorNotFoundException.class)
  public void authenticatorNotFound() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.CORE_AUTHENTICATOR_CLASS, "foo");

    AuthenticatorFactory factory = new AuthenticatorFactory(configuration, new LoginPasswordAuthenticator[]{new FakeAuthenticator(), new FailAuthenticator()});
    factory.start();
    factory.getAuthenticator();
  }

  @Test
  public void ignoreStartupFailure() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.CORE_AUTHENTICATOR_CLASS, FailAuthenticator.class.getName());
    configuration.setProperty(CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE, Boolean.TRUE.toString());

    AuthenticatorFactory factory = new AuthenticatorFactory(configuration, new LoginPasswordAuthenticator[]{new FakeAuthenticator(), new FailAuthenticator()});
    factory.start();
    assertThat(factory.getAuthenticator(), not(nullValue()));
  }

  static class FakeAuthenticator implements LoginPasswordAuthenticator {
    public void init() {
    }

    public boolean authenticate(String login, String password) {
      return false;
    }
  }

  static class ConnectionException extends RuntimeException {
  }

  static class FailAuthenticator implements LoginPasswordAuthenticator {
    public void init() {
      throw new ConnectionException();
    }

    public boolean authenticate(String login, String password) {
      return false;
    }
  }
}
