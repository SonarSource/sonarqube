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
package org.sonar.server.user.ws;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import javax.annotation.Nonnull;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.authentication.IdentityProviderRepository;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.IdentityProvidersWsResponse;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class IdentityProvidersAction implements UsersWsAction {
  private final IdentityProviderRepository identityProviderRepository;

  public IdentityProvidersAction(IdentityProviderRepository identityProviderRepository) {
    this.identityProviderRepository = identityProviderRepository;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("identity_providers")
      .setDescription("List the external identity providers")
      .setResponseExample(getClass().getResource("identity_providers-example.json"))
      .setSince("5.5")
      .setInternal(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    writeProtobuf(buildResponse(), request, response);
  }

  private IdentityProvidersWsResponse buildResponse() {
    IdentityProvidersWsResponse.Builder response = IdentityProvidersWsResponse.newBuilder();
    response.addAllIdentityProviders(Lists.transform(identityProviderRepository.getAllEnabledAndSorted(), IdentityProviderToWsResponse.INSTANCE));
    return response.build();
  }

  private enum IdentityProviderToWsResponse implements Function<IdentityProvider, Users.IdentityProvider> {
    INSTANCE;

    @Override
    public Users.IdentityProvider apply(@Nonnull IdentityProvider input) {
      Users.IdentityProvider.Builder builder = Users.IdentityProvider.newBuilder()
        .setKey(input.getKey())
        .setName(input.getName());
      Display display = input.getDisplay();
      builder
        .setIconPath(display.getIconPath())
        .setBackgroundColor(display.getBackgroundColor());

      return builder.build();
    }
  }
}
