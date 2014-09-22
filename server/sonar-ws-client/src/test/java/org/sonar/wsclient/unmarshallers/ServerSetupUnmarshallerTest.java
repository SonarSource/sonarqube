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
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.ServerSetup;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ServerSetupUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void testSuccessfulSetup() {
    ServerSetup setup = new ServerSetupUnmarshaller().toModel(loadFile("/server_setup/ok.json"));
    assertThat(setup.getStatus(), is("ok"));
    assertThat(setup.getMessage(), nullValue());
    assertThat(setup.isSuccessful(), is(true));
  }

  @Test
  public void testFailedSetup() {
    ServerSetup setup = new ServerSetupUnmarshaller().toModel(loadFile("/server_setup/ko.json"));
    assertThat(setup.getStatus(), is("ko"));
    assertThat(setup.getMessage(), is("error"));
    assertThat(setup.isSuccessful(), is(false));
  }

}
