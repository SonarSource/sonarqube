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
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.Server;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ServerUnmarshallerTest {

  @Test
  public void testToModel() throws IOException {
    Server server = new ServerUnmarshaller().toModel("{\"id\":\"123456789\", \"version\":\"2.0-SNAPSHOT\", \"status\":\"UP\", \"status_msg\":\"everything is under control\"}");
    assertThat(server.getId(), is("123456789"));
    assertThat(server.getVersion(), is("2.0-SNAPSHOT"));
    assertThat(server.getStatus(), is(Server.Status.UP));
    assertThat(server.getStatusMessage(), is("everything is under control"));
  }

  @Test
  public void shouldNotFailIfStatusIsMissing() throws IOException {
    Server server = new ServerUnmarshaller().toModel("{\"id\":\"123456789\", \"version\":\"2.0-SNAPSHOT\"}");
    assertThat(server.getId(), is("123456789"));
    assertThat(server.getVersion(), is("2.0-SNAPSHOT"));
    assertThat(server.getStatus(), nullValue());
    assertThat(server.getStatusMessage(), nullValue());
  }
}
