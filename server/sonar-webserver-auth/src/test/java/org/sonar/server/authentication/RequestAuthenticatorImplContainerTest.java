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
package org.sonar.server.authentication;

import java.util.Optional;
import org.junit.Test;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserSessionFactory;
import org.sonar.server.usertoken.UserTokenAuthentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ServiceAuthentication} is registered by core extensions, which the platform installs into the
 * container <em>after</em> {@link AuthenticationModule}. These tests pin what a unit test constructing
 * the class directly cannot show: the container still builds {@link RequestAuthenticatorImpl} when
 * nothing implements the interface, and an implementation registered later is still injected.
 */
public class RequestAuthenticatorImplContainerTest {

  private final HttpRequest request = mock(HttpRequest.class);
  private final HttpResponse response = mock(HttpResponse.class);

  @Test
  public void container_should_start_when_no_ServiceAuthentication_is_registered() {
    SpringComponentContainer container = newContainerWithAuthDependencies();
    container.add(RequestAuthenticatorImpl.class);

    container.startComponents();

    assertThat(container.getComponentByType(RequestAuthenticatorImpl.class)).isNotNull();
  }

  @Test
  public void ServiceAuthentication_should_be_injected_when_registered_after_the_authenticator() {
    UserSession serviceSession = mock(UserSession.class);
    ServiceAuthentication serviceAuthentication = mock(ServiceAuthentication.class);
    when(serviceAuthentication.authenticate(request)).thenReturn(Optional.of(serviceSession));

    SpringComponentContainer container = newContainerWithAuthDependencies();
    container.add(RequestAuthenticatorImpl.class);
    // Registered last, exactly as a core extension is.
    container.add(serviceAuthentication);

    container.startComponents();

    assertThat(container.getComponentByType(RequestAuthenticatorImpl.class).authenticate(request, response)).isSameAs(serviceSession);
  }

  private static SpringComponentContainer newContainerWithAuthDependencies() {
    SpringComponentContainer container = new SpringComponentContainer();
    container.add(
      mock(JwtHttpHandler.class),
      mock(BasicAuthentication.class),
      mock(UserTokenAuthentication.class),
      mock(HttpHeadersAuthentication.class),
      mock(GithubWebhookAuthentication.class),
      mock(UserSessionFactory.class));
    return container;
  }
}
