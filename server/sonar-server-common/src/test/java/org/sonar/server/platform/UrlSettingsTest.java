/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.config.CorePropertyDefinitions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.CoreProperties.SERVER_BASE_URL;

public class UrlSettingsTest {

  private static final String HOST_PROPERTY = "sonar.web.host";
  private static final String PORT_PORPERTY = "sonar.web.port";
  private static final String CONTEXT_PROPERTY = "sonar.web.context";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings(new PropertyDefinitions(CorePropertyDefinitions.all()));

  @Test
  public void use_default_context_path() {
    assertThat(underTest().getContextPath()).isEqualTo("");
  }

  @Test
  public void default_url() {
    assertThat(underTest().getBaseUrl()).isEqualTo("http://localhost:9000");
  }

  @Test
  public void base_url_is_configured() {
    settings.setProperty("sonar.core.serverBaseURL", "http://mydomain.com");

    assertThat(underTest().getBaseUrl()).isEqualTo("http://mydomain.com");
  }

  @Test
  public void remove_trailing_slash() {
    settings.setProperty("sonar.core.serverBaseURL", "http://mydomain.com/");

    assertThat(underTest().getBaseUrl()).isEqualTo("http://mydomain.com");
  }

  @Test
  public void is_secured_on_https_server() {
    settings.setProperty("sonar.core.serverBaseURL", "https://mydomain.com");

    assertThat(underTest().isSecured()).isTrue();
  }

  @Test
  public void is_not_secured_if_http() {
    settings.setProperty("sonar.core.serverBaseURL", "http://mydomain.com");

    assertThat(underTest().isSecured()).isFalse();
  }

  @Test
  public void context_path_is_configured() {
    settings.setProperty(CONTEXT_PROPERTY, "/my_path");

    assertThat(underTest().getContextPath()).isEqualTo("/my_path");
  }

  @Test
  public void sanitize_context_path_from_settings() {
    settings.setProperty(CONTEXT_PROPERTY, "/my_path///");

    assertThat(underTest().getContextPath()).isEqualTo("/my_path");
  }

  @Test
  public void base_url_is_http_localhost_9000_when_serverBaseUrl_is_null() {
    settings.setProperty(SERVER_BASE_URL, (String) null);

    assertThat(underTest().getBaseUrl()).isEqualTo("http://localhost:9000");
  }

  @Test
  public void base_url_is_serverBaseUrl_if_non_empty() {
    String serverBaseUrl = "whatever";
    settings.setProperty(SERVER_BASE_URL, serverBaseUrl);

    assertThat(underTest().getBaseUrl()).isEqualTo(serverBaseUrl);
  }

  @Test
  public void base_url_is_http_localhost_9000_when_serverBaseUrl_is_empty() {
    settings.setProperty(SERVER_BASE_URL, "");

    assertThat(underTest().getBaseUrl()).isEqualTo("http://localhost:9000");
  }

  @Test
  public void base_url_is_http_localhost_9000_when_host_set_to_0_0_0_0() {
    settings.setProperty(HOST_PROPERTY, "0.0.0.0");

    assertThat(underTest().getBaseUrl()).isEqualTo("http://localhost:9000");
  }

  @Test
  public void base_url_is_http_specified_host_9000_when_host_is_set() {
    settings.setProperty(HOST_PROPERTY, "my_host");

    assertThat(underTest().getBaseUrl()).isEqualTo("http://my_host:9000");
  }

  @Test
  public void base_url_is_http_localhost_specified_port_when_port_is_set() {
    settings.setProperty(PORT_PORPERTY, 951);

    assertThat(underTest().getBaseUrl()).isEqualTo("http://localhost:951");
  }

  @Test
  public void base_url_is_http_localhost_no_port_when_port_is_80() {
    settings.setProperty(PORT_PORPERTY, 80);

    assertThat(underTest().getBaseUrl()).isEqualTo("http://localhost");
  }

  @Test
  public void base_url_is_http_localhost_9000_when_port_is_0() {
    settings.setProperty(PORT_PORPERTY, 0);

    assertThat(underTest().getBaseUrl()).isEqualTo("http://localhost:9000");
  }

  @Test
  public void base_url_is_http_localhost_9000_when_port_is_negative() {
    settings.setProperty(PORT_PORPERTY, -23);

    assertThat(underTest().getBaseUrl()).isEqualTo("http://localhost:9000");
  }

  @Test
  public void getBaseUrl_throws_when_port_not_an_int() {
    settings.setProperty(PORT_PORPERTY, "not a number");

    expectedException.expect(IllegalStateException.class);
    underTest().getBaseUrl();
  }

  @Test
  public void base_url_is_http_localhost_900_specified_context_when_context_is_set() {
    settings.setProperty(CONTEXT_PROPERTY, "sdsd");
    assertThat(underTest().getBaseUrl()).isEqualTo("http://localhost:9000sdsd");
  }

  @Test
  public void base_url_is_http_specified_host_no_port_when_host_is_set_and_port_is_80() {
    settings.setProperty(HOST_PROPERTY, "foo");
    settings.setProperty(PORT_PORPERTY, 80);
    assertThat(underTest().getBaseUrl()).isEqualTo("http://foo");
  }

  @Test
  public void getBaseUrl_does_not_cache_returned_value() {
    assertThat(underTest().getBaseUrl()).isEqualTo("http://localhost:9000");
    settings.setProperty(HOST_PROPERTY, "foo");
    assertThat(underTest().getBaseUrl()).isEqualTo("http://foo:9000");
    settings.setProperty(PORT_PORPERTY, 666);
    assertThat(underTest().getBaseUrl()).isEqualTo("http://foo:666");
    settings.setProperty(CONTEXT_PROPERTY, "/bar");
    assertThat(underTest().getBaseUrl()).isEqualTo("http://foo:666/bar");
  }

  private UrlSettings underTest() {
    return new UrlSettings(settings.asConfig());
  }
}
