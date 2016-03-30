/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

public class ServerImplTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();
  @Rule
  public TemporaryFolder sonarHome = new TemporaryFolder();

  private Date someDate;
  private Settings settings;

  ServerImpl underTest;

  @Before
  public void setUp() throws ParseException {
    this.someDate = new SimpleDateFormat("ddMMyyyy").parse("24101236");
    this.settings = new Settings().setProperty(ProcessProperties.PATH_HOME, sonarHome.getRoot().getAbsolutePath());
    this.settings.setProperty(ProcessProperties.STARTED_AT, someDate.getTime());
    new File(sonarHome.getRoot(), "web/deploy").mkdirs();

    underTest = new ServerImpl(settings, "/org/sonar/server/platform/ServerImplTest/build.properties", "/org/sonar/server/platform/ServerImplTest/version.txt");
  }

  @Test
  public void getStartedAt_throws_NPE_if_start_has_not_been_called() {
    exception.expect(NullPointerException.class);
    exception.expectMessage("start() method has not been called");

    underTest.getStartedAt();
  }

  @Test
  public void getStartedAt_is_date_from_sonar_core_startedAt() throws ParseException {
    underTest.start();

    assertThat(underTest.getStartedAt()).isEqualTo(someDate);
  }

  @Test
  public void start_fails_with_NFE_if_date_from_sonar_core_startedAt_is_invalid() throws ParseException {
    settings.setProperty(ProcessProperties.STARTED_AT, "aasasa");

    ServerImpl server = new ServerImpl(settings, "/org/sonar/server/platform/ServerImplTest/build.properties", "/org/sonar/server/platform/ServerImplTest/version.txt");

    exception.expect(NumberFormatException.class);

    server.start();
  }

  @Test
  public void start_fails_with_ISE_sonar_core_startedAt_is_not_set() throws ParseException {
    settings.removeProperty(ProcessProperties.STARTED_AT);

    ServerImpl server = new ServerImpl(settings, "/org/sonar/server/platform/ServerImplTest/build.properties", "/org/sonar/server/platform/ServerImplTest/version.txt");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("property sonar.core.startedAt must be set");

    server.start();
  }

  @Test
  public void always_return_the_same_values() {
    underTest.start();

    assertThat(underTest.getId()).isNotNull();
    assertThat(underTest.getId()).isEqualTo(underTest.getId());

    assertThat(underTest.getVersion()).isNotNull();
    assertThat(underTest.getVersion()).isEqualTo(underTest.getVersion());

    assertThat(underTest.getStartedAt()).isNotNull();
    assertThat(underTest.getStartedAt()).isEqualTo(underTest.getStartedAt());
  }

  @Test
  public void read_version_from_file() {
    underTest.start();

    assertThat(underTest.getVersion()).isEqualTo("1.0");
  }

  @Test
  public void read_implementation_build_from_manifest() {
    underTest.start();

    assertThat(underTest.getImplementationBuild()).isEqualTo("0b9545a8b74aca473cb776275be4dc93a327c363");
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
    underTest.start();
    assertThat(underTest.getContextPath()).isEqualTo("");
  }

  @Test
  public void is_dev() throws Exception {
    settings.setProperty("sonar.web.dev", true);
    underTest.start();
    assertThat(underTest.isDev()).isTrue();
  }

  @Test
  public void get_default_public_root_url() throws Exception {
    underTest.start();
    assertThat(underTest.getPublicRootUrl()).isEqualTo("http://localhost:9000");
  }

  @Test
  public void get_public_root_url() throws Exception {
    settings.setProperty("sonar.core.serverBaseURL", "http://mydomain.com");
    underTest.start();
    assertThat(underTest.getPublicRootUrl()).isEqualTo("http://mydomain.com");
  }

  @Test
  public void is_secured_on_secured_server() throws Exception {
    settings.setProperty("sonar.core.serverBaseURL", "https://mydomain.com");
    underTest.start();
    assertThat(underTest.isSecured()).isTrue();
  }

  @Test
  public void is_secured_on_not_secured_server() throws Exception {
    settings.setProperty("sonar.core.serverBaseURL", "http://mydomain.com");
    underTest.start();
    assertThat(underTest.isSecured()).isFalse();
  }

  @Test
  public void get_context_path_from_settings() {
    settings.setProperty("sonar.web.context", "/my_path");
    underTest.start();
    assertThat(underTest.getContextPath()).isEqualTo("/my_path");
  }

  @Test
  public void sanitize_context_path_from_settings() {
    settings.setProperty("sonar.web.context", "/my_path///");
    underTest.start();
    assertThat(underTest.getContextPath()).isEqualTo("/my_path");
  }
}
