/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.user;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.security.Authenticator;
import org.sonar.api.security.ExternalGroupsProvider;
import org.sonar.api.security.ExternalUsersProvider;
import org.sonar.api.security.LoginPasswordAuthenticator;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.security.UserDetails;
import org.sonar.api.utils.SonarException;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityRealmFactoryTest {

  Settings settings = new Settings();

  /**
   * Typical usage.
   */
  @Test
  public void should_select_realm_and_start() {
    SecurityRealm realm = spy(new FakeRealm());
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_REALM, realm.getName());

    SecurityRealmFactory factory = new SecurityRealmFactory(settings, new SecurityRealm[]{realm});
    factory.start();
    assertThat(factory.getRealm()).isSameAs(realm);
    verify(realm).init();

    factory.stop();
  }

  @Test
  public void do_not_fail_if_no_realms() {
    SecurityRealmFactory factory = new SecurityRealmFactory(settings);
    factory.start();
    assertThat(factory.getRealm()).isNull();
  }

  @Test
  public void realm_not_found() {
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_REALM, "Fake");

    try {
      new SecurityRealmFactory(settings).start();;
      fail();
    } catch (SonarException e) {
      assertThat(e.getMessage()).contains("Realm 'Fake' not found.");
    }
  }

  @Test
  public void should_provide_compatibility_for_authenticator() {
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_CLASS, FakeLoginPasswordAuthenticator.class.getName());
    LoginPasswordAuthenticator authenticator = new FakeLoginPasswordAuthenticator();

    SecurityRealmFactory factory = new SecurityRealmFactory(settings, new LoginPasswordAuthenticator[]{authenticator});
    factory.start();
    SecurityRealm realm = factory.getRealm();
    assertThat(realm).isInstanceOf(CompatibilityRealm.class);
  }

  @Test
  public void should_take_precedence_over_authenticator() {
    SecurityRealm realm = new FakeRealm();
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_REALM, realm.getName());
    LoginPasswordAuthenticator authenticator = new FakeLoginPasswordAuthenticator();
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_CLASS, FakeLoginPasswordAuthenticator.class.getName());

    SecurityRealmFactory factory = new SecurityRealmFactory(settings, new SecurityRealm[]{realm},
      new LoginPasswordAuthenticator[]{authenticator});
    factory.start();
    assertThat(factory.getRealm()).isSameAs(realm);
  }
  
  @Test
  public void should_allow_custom_realm_for_authentication() {
    settings.setProperty(CoreProperties.CORE_SECURITY_AUTHENTICATORS, FakeAuthenticator.class.getName());
    settings.setProperty(CoreProperties.CORE_SECURITY_USER_PROVIDERS, FakeUsersProvider.class.getName());
    settings.setProperty(CoreProperties.CORE_SECURITY_GROUP_PROVIDERS, FakeGroupsProvider.class.getName());
    
    SecurityRealm realmWithAuthenticator = mock(SecurityRealm.class);
    FakeAuthenticator authenticator = new FakeAuthenticator();
    List authenticators = Collections.singletonList(authenticator);
    when(realmWithAuthenticator.getAuthenticators()).thenReturn(authenticators);
    
    SecurityRealm realmWithUsersProvider = mock(SecurityRealm.class);
    FakeUsersProvider userProvider = new FakeUsersProvider();
    List usersProviders = Collections.singletonList(userProvider);
    when(realmWithUsersProvider.getUsersProviders()).thenReturn(usersProviders);
    
    SecurityRealm realmWithGroupsProvider = mock(SecurityRealm.class);
    FakeGroupsProvider gruopProvider = new FakeGroupsProvider();
    List gruopsProviders = Collections.singletonList(gruopProvider);
    when(realmWithGroupsProvider.getGroupsProviders()).thenReturn(gruopsProviders);
    
    SecurityRealm[] realms = new SecurityRealm[3];
    realms[0] = realmWithAuthenticator;
    realms[1] = realmWithUsersProvider;
    realms[2] = realmWithGroupsProvider;
    SecurityRealmFactory factory = new SecurityRealmFactory(settings, realms);
    factory.start();
    SecurityRealm realm = factory.getRealm();
    assertThat(realm).isInstanceOf(CustomSecurityRealm.class);
    assertThat(realm.getAuthenticators()).contains(authenticator);
    assertThat(realm.getUsersProviders()).contains(userProvider);
    assertThat(realm.getGroupsProviders()).contains(gruopProvider);
  }

  @Test
  public void authenticator_not_found() {
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_CLASS, "Fake");

    try {
      new SecurityRealmFactory(settings).start();
      fail();
    } catch (SonarException e) {
      assertThat(e.getMessage()).contains("Authenticator 'Fake' not found.");
    }
  }

  @Test
  public void ignore_startup_failure() {
    SecurityRealm realm = spy(new AlwaysFailsRealm());
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_REALM, realm.getName());
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE, true);

    new SecurityRealmFactory(settings, new SecurityRealm[]{realm}).start();
    verify(realm).init();
  }

  @Test
  public void should_fail() {
    SecurityRealm realm = spy(new AlwaysFailsRealm());
    settings.setProperty(CoreProperties.CORE_AUTHENTICATOR_REALM, realm.getName());

    try {
      new SecurityRealmFactory(settings, new SecurityRealm[]{realm}).start();
      fail();
    } catch (SonarException e) {
      assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Security realm fails to start");
    }
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

  
  
  private static class FakeAuthenticator extends Authenticator {

    @Override
    public boolean doAuthenticate(Context context) {
      return false;
    }
    
  }
  
  private static class FakeLoginPasswordAuthenticator implements LoginPasswordAuthenticator {
    public void init() {
    }

    public boolean authenticate(String login, String password) {
      return false;
    }
  }
  
  private static class FakeGroupsProvider extends ExternalGroupsProvider {

    @Override
    public Collection<String> doGetGroups(String username) {
      return Collections.emptyList();
    }
    
  }
  
  private static class FakeUsersProvider extends ExternalUsersProvider {
    
    @Override
    public UserDetails doGetUserDetails(Context context) {
      return null;
    }

  }

}
