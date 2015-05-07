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
package org.sonar.server.platform;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessProperties;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

public class ServerImplTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public TemporaryFolder sonarHome = new TemporaryFolder();

  Settings settings;

  ServerImpl server;

  @Before
  public void setUp() {
    settings = new Settings().setProperty(ProcessProperties.PATH_HOME, sonarHome.getRoot().getAbsolutePath());
    new File(sonarHome.getRoot(), "web/deploy").mkdirs();

    server = new ServerImpl(settings, "/org/sonar/server/platform/ServerImplTest/build.properties", "/org/sonar/server/platform/ServerImplTest/version.txt");
  }

  @Test
  public void always_return_the_same_values() {
    server.start();

    assertThat(server.getId()).isNotNull();
    assertThat(server.getId()).isEqualTo(server.getId());

    assertThat(server.getVersion()).isNotNull();
    assertThat(server.getVersion()).isEqualTo(server.getVersion());

    assertThat(server.getStartedAt()).isNotNull();
    assertThat(server.getStartedAt()).isEqualTo(server.getStartedAt());
  }

  @Test
  public void read_version_from_file() {
    server.start();

    assertThat(server.getVersion()).isEqualTo("1.0");
  }

  @Test
  public void read_implementation_build_from_manifest() {
    server.start();

    assertThat(server.getImplementationBuild()).isEqualTo("0b9545a8b74aca473cb776275be4dc93a327c363");
  }

  @Test
  public void read_file_with_no_version() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unknown SonarQube version");

    ServerImpl server = new ServerImpl(settings, "", "/org/sonar/server/platform/ServerImplTest/empty-version.txt");
    server.start();
  }

  @Test
  public void read_file_with_empty_version() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unknown SonarQube version");

    ServerImpl server = new ServerImpl(settings, "", "/org/sonar/server/platform/ServerImplTest/empty-version.txt");
    server.start();
  }

  @Test
  public void fail_if_version_file_not_found() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unknown SonarQube version");

    ServerImpl server = new ServerImpl(settings, "", "/org/sonar/server/platform/ServerImplTest/unknown-file.properties");
    server.start();
  }

  @Test
  public void load_server_id_from_database() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PERMANENT_SERVER_ID, "abcde");

    ServerImpl server = new ServerImpl(settings);

    assertThat(server.getPermanentServerId(), Is.is("abcde"));
  }

  @Test
  public void use_default_context_path() {
    server.start();
    assertThat(server.getContextPath()).isEqualTo("");
  }

  @Test
  public void get_context_path_from_settings() {
    settings.setProperty("sonar.web.context", "/my_path");
    server.start();
    assertThat(server.getContextPath()).isEqualTo("/my_path");
  }

  @Test
  public void sanitize_context_path_from_settings() {
    settings.setProperty("sonar.web.context", "/my_path///");
    server.start();
    assertThat(server.getContextPath()).isEqualTo("/my_path");
  }

}
