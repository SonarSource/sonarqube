/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.almintegration.ws.github;

import java.util.Set;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GithubProvisioningWsTest {
  @Test
  public void define_createsOneController() {
    WebService.Context context = mock(WebService.Context.class);
    GithubProvisioningAction action = mock(GithubProvisioningAction.class);
    WebService.NewController controller = mock(WebService.NewController.class);
    GithubProvisioningWs scimWs = new GithubProvisioningWs(Set.of(action));

    when(context.createController("api/github_provisioning")).thenReturn(controller);
    when(controller.setDescription(any())).thenReturn(controller);
    when(controller.setSince(any())).thenReturn(controller);

    scimWs.define(context);

    verify(context, only()).createController("api/github_provisioning");
    verify(action, only()).define(any());
  }
}
