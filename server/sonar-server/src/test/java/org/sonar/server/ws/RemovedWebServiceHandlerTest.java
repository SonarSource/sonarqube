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
package org.sonar.server.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Request;
import org.sonar.server.exceptions.ServerException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemovedWebServiceHandlerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void throw_server_exception() throws Exception {
    Request request = mock(Request.class);
    when(request.getPath()).thenReturn("/api/resources/index");

    try {
      RemovedWebServiceHandler.INSTANCE.handle(request, null);
      fail();
    } catch (ServerException e) {
      assertThat(e.getMessage()).isEqualTo("The web service '/api/resources/index' doesn't exist anymore, please read its documentation to use alternatives");
      assertThat(e.httpCode()).isEqualTo(410);
    }
  }
}
