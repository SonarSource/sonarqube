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
package org.sonarqube.ws.client.github.provisioning.permissions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GithubPermissionsServiceTest {

  @Mock
  private WsConnector wsConnector;

  @InjectMocks
  private GithubPermissionsService githubPermissionsService;

  @Test
  public void addPermissionMapping_whenRequestIsSuccessful_returns() {
    AddGithubPermissionMappingRequest addGithubPermissionMappingRequest = new AddGithubPermissionMappingRequest("admin",
      new SonarqubePermissions(true, true, true, true, true, true));

    WsResponse response = mock(WsResponse.class);
    when(response.failIfNotSuccessful()).thenReturn(response);
    when(wsConnector.call(any(PostRequest.class))).thenReturn(response);

    githubPermissionsService.addPermissionMapping(addGithubPermissionMappingRequest);

    ArgumentCaptor<WsRequest> wsRequestArgumentCaptor = ArgumentCaptor.forClass(WsRequest.class);
    verify(wsConnector).call(wsRequestArgumentCaptor.capture());

    WsRequest request = wsRequestArgumentCaptor.getValue();
    assertThat(request.getMethod()).isEqualTo(WsRequest.Method.POST);
    assertThat(request.getPath()).isEqualTo("api/v2/dop-translation/github-permission-mappings");

  }

}
