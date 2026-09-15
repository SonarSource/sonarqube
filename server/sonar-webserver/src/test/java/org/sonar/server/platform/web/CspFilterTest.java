/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CspFilterTest {

  private static final String TEST_CONTEXT = "/sonarqube";
  private static final String INDEX_HTML = """
    <script defer>
          window.__assetsPath = function (filename) {
            return 'WEB_CONTEXT/' + filename;
          };

          let themeMode = 'system';
          try {
            const storedThemeMode = localStorage.getItem('sonarqube.theme_mode');
            themeMode = storedThemeMode ? JSON.parse(storedThemeMode) : themeMode;
          } catch {
            // noop
          }
          const useDarkTheme =
            themeMode === 'dark-theme' ||
            (themeMode === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
          if (useDarkTheme) {
            document.documentElement.dataset.echoesTheme = 'dark';
          }
        </script>
    """;
  private static final String EXPECTED = "default-src 'self'; " +
    "base-uri 'none'; " +
    "connect-src 'self' http: https:; " +
    "font-src 'self' data:; " +
    "frame-src; " +
    "img-src * data: blob:; " +
    "object-src 'none'; " +
    "script-src 'self' 'sha256-F9cDkTPdWFdfrHs0WmmvW1C05uRXeP2Px3F8BCnlC+8='; " +
    "style-src 'self' 'unsafe-inline'; " +
    "worker-src 'self'";
  private final ServletContext servletContext = mock(ServletContext.class, RETURNS_MOCKS);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final FilterChain chain = mock(FilterChain.class);
  private final CspFilter underTest = new CspFilter();
  FilterConfig config = mock(FilterConfig.class);

  @Before
  public void setUp() throws ServletException {
    when(config.getServletContext()).thenReturn(servletContext);
    when(servletContext.getResourceAsStream("/index.html"))
      .thenAnswer(invocation -> new ByteArrayInputStream(INDEX_HTML.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void set_content_security_headers() throws Exception {
    when(servletContext.getContextPath()).thenReturn(TEST_CONTEXT);
    doInit();
    HttpServletRequest request = newRequest("/");
    underTest.doFilter(request, response, chain);
    verify(response).setHeader("Content-Security-Policy", EXPECTED);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void csp_hash_should_be_correct_without_a_context_path() throws Exception {
    when(servletContext.getContextPath()).thenReturn("");
    doInit();
    HttpServletRequest request = newRequest("/");
    underTest.doFilter(request, response, chain);
    verify(response).setHeader(eq("Content-Security-Policy"), contains("script-src 'self' 'sha256-oih2JXtlruHFLwqXZ0sBVpl+P8ux3mGb96nJG12Hw+I='; "));
    verify(chain).doFilter(request, response);
  }

  @Test
  public void should_fail_when_assets_path_script_contains_a_serving_time_placeholder() {
    when(servletContext.getResourceAsStream("/index.html"))
      .thenAnswer(invocation -> new ByteArrayInputStream(INDEX_HTML.replace("WEB_CONTEXT", "%SERVER_STATUS%").getBytes(StandardCharsets.UTF_8)));

    assertThatThrownBy(this::doInit)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The assets path script in /index.html must not contain serving-time placeholders");
  }

  @Test
  public void should_fail_with_a_clear_message_when_index_html_is_missing() {
    when(servletContext.getResourceAsStream("/index.html")).thenReturn(null);

    assertThatThrownBy(this::doInit)
      .isInstanceOf(ServletException.class)
      .hasMessage("/index.html not found in the web context");
  }

  private void doInit() throws ServletException {
    underTest.init(config);
  }

  private HttpServletRequest newRequest(String path) {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getMethod()).thenReturn("GET");
    when(req.getRequestURI()).thenReturn(path);
    when(req.getContextPath()).thenReturn("");
    when(req.getServletContext()).thenReturn(this.servletContext);
    return req;
  }
}
