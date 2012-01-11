/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.security.LoginPasswordAuthenticator;
import org.sonar.api.security.SecurityRealm;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SecurityRealmFactoryTest {

  private Settings settings;

  @Before
  public void setUp() {
    settings = new Settings();
  }

  /**
   * Typical usage.
   */
  @Test
  public void shouldSelectRealmAndStart() {
    SecurityRealm realm = spy(new FakeRealm());
    settings.setProperty(SecurityRealmFactory.REALM_PROPERTY, realm.getName());

    SecurityRealmFactory factory = new SecurityRealmFactory(settings, new SecurityRealm[] {realm});
    factory.start();
    assertThat(factory.getRealm(), is(realm));
    verify(realm).init();
  }

  @Test
  public void doNotFailIfNoRealms() {
    SecurityRealmFactory factory = new SecurityRealmFactory(settings);
    factory.start();
    assertThat(factory.getRealm(), nullValue());
  }

  @Test(expected = AuthenticatorNotFoundException.class)
  public void realmNotFound() {
    settings.setProperty(SecurityRealmFactory.REALM_PROPERTY, "Fake");

    new SecurityRealmFactory(settings);
  }

  @Test
  public void shouldProvideCompatibilityForAuthenticator() {
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_CLASS, FakeAuthenticator.class.getName());
    LoginPasswordAuthenticator authenticator = new FakeAuthenticator();

    SecurityRealmFactory factory = new SecurityRealmFactory(settings, new LoginPasswordAuthenticator[] {authenticator});
    SecurityRealm realm = factory.getRealm();
    assertThat(realm, instanceOf(CompatibilityRealm.class));
  }

  @Test
  public void shouldTakePrecedenceOverAuthenticator() {
    SecurityRealm realm = new FakeRealm();
    settings.setProperty(SecurityRealmFactory.REALM_PROPERTY, realm.getName());
    LoginPasswordAuthenticator authenticator = new FakeAuthenticator();
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_CLASS, FakeAuthenticator.class.getName());

    SecurityRealmFactory factory = new SecurityRealmFactory(settings, new SecurityRealm[] {realm},
        new LoginPasswordAuthenticator[] {authenticator});
    assertThat(factory.getRealm(), is(realm));
  }

  @Test(expected = AuthenticatorNotFoundException.class)
  public void authenticatorNotFound() {
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_CLASS, "Fake");

    new SecurityRealmFactory(settings);
  }

  @Test
  public void ignoreStartupFailure() {
    SecurityRealm realm = spy(new AlwaysFailsRealm());
    settings.setProperty(SecurityRealmFactory.REALM_PROPERTY, realm.getName());
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE, true);

    new SecurityRealmFactory(settings, new SecurityRealm[] {realm}).start();
    verify(realm).init();
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFail() {
    SecurityRealm realm = spy(new AlwaysFailsRealm());
    settings.setProperty(SecurityRealmFactory.REALM_PROPERTY, realm.getName());

    new SecurityRealmFactory(settings, new SecurityRealm[] {realm}).start();
  }

  private static class AlwaysFailsRealm extends FakeRealm {
    @Override
    public void init() {
      throw new IllegalStateException();
    }
  }

  private static class FakeRealm extends SecurityRealm {
    @Override
    public LoginPasswordAuthenticator getLoginPasswordAuthenticator() {
      return null;
    }
  }

  private static class FakeAuthenticator implements LoginPasswordAuthenticator {
    public void init() {
    }

    public boolean authenticate(String login, String password) {
      return false;
    }
  }

}
