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
package org.sonar.server.platform;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertThat;

public class ServerImplTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void alwaysReturnTheSameValues() {
    ServerImpl server = new ServerImpl(new Settings(), "", "/org/sonar/server/platform/ServerImplTest/pom-with-version.properties");
    server.start();

    assertThat(server.getId()).isNotNull();
    assertThat(server.getId()).isEqualTo(server.getId());

    assertThat(server.getVersion()).isNotNull();
    assertThat(server.getVersion()).isEqualTo(server.getVersion());

    assertThat(server.getStartedAt()).isNotNull();
    assertThat(server.getStartedAt()).isEqualTo(server.getStartedAt());
  }

  @Test
  public void getVersionFromFile() {
    ServerImpl server = new ServerImpl(new Settings(), "", "/org/sonar/server/platform/ServerImplTest/pom-with-version.properties");
    server.start();

    assertThat(server.getVersion()).isEqualTo("1.0");
  }

  @Test
  public void getImplementationBuildFromManifest() {
    ServerImpl server = new ServerImpl(new Settings(),
        "/org/sonar/server/platform/ServerImplTest/build.properties",
        "/org/sonar/server/platform/ServerImplTest/pom-with-version.properties");
    server.start();

    assertThat(server.getImplementationBuild()).isEqualTo("0b9545a8b74aca473cb776275be4dc93a327c363");
  }

  @Test
  public void testFileWithNoVersion() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unknown Sonar version");

    ServerImpl server = new ServerImpl(new Settings(), "", "/org/sonar/server/platform/ServerImplTest/pom-without-version.properties");
    server.start();
  }

  @Test
  public void testFileWithEmptyVersionParameter() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unknown Sonar version");

    ServerImpl server = new ServerImpl(new Settings(), "", "/org/sonar/server/platform/ServerImplTest/pom-with-empty-version.properties");
    server.start();
  }

  @Test
  public void shouldFailIfFileNotFound() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unknown Sonar version");

    ServerImpl server = new ServerImpl(new Settings(), "", "/org/sonar/server/platform/ServerImplTest/unknown-file.properties");
    server.start();
  }

  @Test
  public void shouldLoadServerIdFromDatabase() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, "abcde");

    ServerImpl server = new ServerImpl(settings);

    assertThat(server.getPermanentServerId(), Is.is("abcde"));
  }
}
