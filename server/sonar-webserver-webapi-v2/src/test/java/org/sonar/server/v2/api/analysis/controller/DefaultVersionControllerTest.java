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
package org.sonar.server.v2.api.analysis.controller;

import org.junit.jupiter.api.Test;
import org.sonar.api.platform.Server;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.VERSION_ENDPOINT;
import static org.sonar.server.v2.api.ControllerTester.getMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultVersionControllerTest {

  private final Server server = mock(Server.class);

  private final MockMvc mockMvc = getMockMvc(new DefaultVersionController(server));

  @Test
  void getVersion_shouldReturnServerVersion() throws Exception {
    String serverVersion = "10.6";
    when(server.getVersion()).thenReturn(serverVersion);

    mockMvc.perform(get(VERSION_ENDPOINT))
      .andExpect(status().isOk())
      .andExpect(content().string(serverVersion));
  }

}
