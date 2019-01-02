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
package org.sonar.server.platform.web;

import java.io.InputStream;
import javax.servlet.ServletContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.stubbing.Answer;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.platform.OfficialDistribution;
import org.sonar.server.platform.Platform;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.Platform.Status.BOOTING;
import static org.sonar.server.platform.Platform.Status.STARTING;
import static org.sonar.server.platform.Platform.Status.UP;

public class WebPagesCacheTest {

  private static final String TEST_CONTEXT = "/sonarqube";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ServletContext servletContext = mock(ServletContext.class);

  private OfficialDistribution officialDistribution = mock(OfficialDistribution.class);
  private Platform platform = mock(Platform.class);
  private MapSettings mapSettings = new MapSettings();

  private WebPagesCache underTest = new WebPagesCache(platform, mapSettings.asConfig(), officialDistribution);

  @Before
  public void setUp() throws Exception {
    when(servletContext.getContextPath()).thenReturn(TEST_CONTEXT);
    when(servletContext.getResourceAsStream("/index.html")).thenAnswer(
      (Answer<InputStream>) invocationOnMock -> toInputStream("Content of default index.html with context [%WEB_CONTEXT%], status [%SERVER_STATUS%], instance [%INSTANCE%]",
        UTF_8));
    when(servletContext.getResourceAsStream("/integration/vsts/index.html"))
      .thenAnswer((Answer<InputStream>) invocationOnMock -> toInputStream("Content of vsts index.html with context [%WEB_CONTEXT%]", UTF_8));
  }

  @Test
  public void check_paths() {
    doInit();
    when(platform.status()).thenReturn(UP);

    assertThat(underTest.getContent("/foo")).contains(TEST_CONTEXT).contains("default");
    assertThat(underTest.getContent("/foo.html")).contains(TEST_CONTEXT).contains("default");
    assertThat(underTest.getContent("/index")).contains(TEST_CONTEXT).contains("default");
    assertThat(underTest.getContent("/index.html")).contains(TEST_CONTEXT).contains("default");
    assertThat(underTest.getContent("/integration/vsts/index.html")).contains(TEST_CONTEXT).contains("vsts");
  }

  @Test
  public void contains_web_context() {
    doInit();

    assertThat(underTest.getContent("/foo"))
      .contains(TEST_CONTEXT);
  }

  @Test
  public void status_is_starting() {
    doInit();
    when(platform.status()).thenReturn(STARTING);

    assertThat(underTest.getContent("/foo"))
      .contains(STARTING.name());
  }

  @Test
  public void status_is_up() {
    doInit();
    when(platform.status()).thenReturn(UP);

    assertThat(underTest.getContent("/foo"))
      .contains(UP.name());
  }

  @Test
  public void no_sonarcloud_setting() {
    doInit();

    assertThat(underTest.getContent("/foo"))
      .contains("SonarQube");
  }

  @Test
  public void sonarcloud_setting_is_false() {
    mapSettings.setProperty("sonar.sonarcloud.enabled", false);
    doInit();

    assertThat(underTest.getContent("/foo"))
      .contains("SonarQube");
  }

  @Test
  public void sonarcloud_setting_is_true() {
    mapSettings.setProperty("sonar.sonarcloud.enabled", true);
    doInit();

    assertThat(underTest.getContent("/foo"))
      .contains("SonarCloud");
  }

  @Test
  public void content_is_updated_when_status_has_changed() {
    doInit();
    when(platform.status()).thenReturn(STARTING);
    assertThat(underTest.getContent("/foo"))
      .contains(STARTING.name());

    when(platform.status()).thenReturn(UP);
    assertThat(underTest.getContent("/foo"))
      .contains(UP.name());
  }

  @Test
  public void content_is_not_updated_when_status_is_up() {
    doInit();
    when(platform.status()).thenReturn(UP);
    assertThat(underTest.getContent("/foo"))
      .contains(UP.name());

    when(platform.status()).thenReturn(STARTING);
    assertThat(underTest.getContent("/foo"))
      .contains(UP.name());
  }

  @Test
  public void fail_to_get_content_when_init_has_not_been_called() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("init has not been called");

    underTest.getContent("/foo");
  }

  private void doInit() {
    when(platform.status()).thenReturn(BOOTING);
    underTest.init(servletContext);
  }

}
