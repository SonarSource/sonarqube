/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.sonar.server.platform.OfficialDistribution;
import org.sonar.server.platform.Platform;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.Platform.Status.BOOTING;
import static org.sonar.server.platform.Platform.Status.STARTING;
import static org.sonar.server.platform.Platform.Status.UP;

public class WebPagesCacheTest {

  private static final String TEST_CONTEXT = "/sonarqube";


  private ServletContext servletContext = mock(ServletContext.class);

  private final OfficialDistribution officialDistribution = mock(OfficialDistribution.class);
  private final Platform platform = mock(Platform.class);

  private final WebPagesCache underTest = new WebPagesCache(platform, officialDistribution);

  @Before
  public void setUp() {
    when(servletContext.getContextPath()).thenReturn(TEST_CONTEXT);
    when(servletContext.getResourceAsStream("/index.html")).thenAnswer(
      (Answer<InputStream>) invocationOnMock -> toInputStream("Content of default index.html with context [WEB_CONTEXT], status [%SERVER_STATUS%], instance [%INSTANCE%]",
        UTF_8));
  }

  @Test
  public void check_paths() {
    doInit();
    when(platform.status()).thenReturn(UP);

    assertThat(underTest.getContent("/foo")).contains(TEST_CONTEXT).contains("default");
    assertThat(underTest.getContent("/foo.html")).contains(TEST_CONTEXT).contains("default");
    assertThat(underTest.getContent("/index")).contains(TEST_CONTEXT).contains("default");
    assertThat(underTest.getContent("/index.html")).contains(TEST_CONTEXT).contains("default");
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
    assertThatThrownBy(() -> underTest.getContent("/foo"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("init has not been called");
  }

  private void doInit() {
    when(platform.status()).thenReturn(BOOTING);
    underTest.init(servletContext);
  }

}
