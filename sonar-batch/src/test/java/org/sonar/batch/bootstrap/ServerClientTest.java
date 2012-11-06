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
package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerClientTest {

  @Test
  public void shouldRemoveUrlEndingSlash() throws Exception {
    BootstrapSettings settings = mock(BootstrapSettings.class);
    when(settings.getProperty(eq("sonar.host.url"), anyString())).thenReturn("http://localhost:8080/sonar/");

    ServerClient server = new ServerClient(settings, new EnvironmentInformation("Junit", "4"));

    assertThat(server.getURL()).isEqualTo("http://localhost:8080/sonar");
  }
}
