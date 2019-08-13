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
package org.sonar.server.ui;

import org.junit.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.api.web.WebAnalytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebAnalyticsLoaderImplTest {

  @Test
  public void return_empty_if_no_analytics_plugin() {
    assertThat(new WebAnalyticsLoaderImpl().getUrlPathToJs()).isEmpty();
    assertThat(new WebAnalyticsLoaderImpl(new WebAnalytics[0]).getUrlPathToJs()).isEmpty();
  }

  @Test
  public void return_js_path_if_analytics_plugin_is_installed() {
    WebAnalytics analytics = newWebAnalytics("api/google/analytics");
    WebAnalyticsLoaderImpl underTest = new WebAnalyticsLoaderImpl(new WebAnalytics[] {analytics});

    assertThat(underTest.getUrlPathToJs()).hasValue("/api/google/analytics");
  }

  @Test
  public void return_empty_if_path_starts_with_slash() {
    WebAnalytics analytics = newWebAnalytics("/api/google/analytics");
    WebAnalyticsLoaderImpl underTest = new WebAnalyticsLoaderImpl(new WebAnalytics[] {analytics});

    assertThat(underTest.getUrlPathToJs()).isEmpty();
  }

  @Test
  public void return_empty_if_path_is_an_url() {
    WebAnalytics analytics = newWebAnalytics("http://foo");
    WebAnalyticsLoaderImpl underTest = new WebAnalyticsLoaderImpl(new WebAnalytics[] {analytics});

    assertThat(underTest.getUrlPathToJs()).isEmpty();
  }

  @Test
  public void return_empty_if_path_has_up_operation() {
    WebAnalytics analytics = newWebAnalytics("foo/../bar");
    WebAnalyticsLoaderImpl underTest = new WebAnalyticsLoaderImpl(new WebAnalytics[] {analytics});

    assertThat(underTest.getUrlPathToJs()).isEmpty();
  }

  @Test
  public void fail_if_multiple_analytics_plugins_are_installed() {
    WebAnalytics analytics1 = newWebAnalytics("foo");
    WebAnalytics analytics2 = newWebAnalytics("bar");

    Throwable thrown = catchThrowable(() -> new WebAnalyticsLoaderImpl(new WebAnalytics[] {analytics1, analytics2}));

    assertThat(thrown)
      .isInstanceOf(MessageException.class)
      .hasMessage("Limited to only one web analytics plugin. Found multiple implementations: [" +
        analytics1.getClass().getName() + ", " + analytics2.getClass().getName() + "]");
  }

  private static WebAnalytics newWebAnalytics(String path) {
    WebAnalytics analytics = mock(WebAnalytics.class);
    when(analytics.getUrlPathToJs()).thenReturn(path);
    return analytics;
  }
}
