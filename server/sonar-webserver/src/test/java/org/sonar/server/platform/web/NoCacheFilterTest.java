/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;
import org.junit.Test;
import org.sonar.api.web.UrlPattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NoCacheFilterTest {

  private final NoCacheFilter filter = new NoCacheFilter();

  @Test
  public void doGetPattern_whenAPIv2_patternMatches() {
    UrlPattern urlPattern = filter.doGetPattern();

    assertThat(urlPattern.matches("/api/v2/whatever")).isTrue();
  }

  @Test
  public void doGetPattern_whenAPIv1_patternDoesNotMatch() {
    UrlPattern urlPattern = filter.doGetPattern();

    assertThat(urlPattern.matches("/api/whatever")).isFalse();
  }

  @Test
  public void doFilter_setResponseHeader() throws Exception{
     HttpResponse response = mock();
     HttpRequest request = mock();
     FilterChain chain = mock();
     
     filter.doFilter(request, response, chain);
     verify(response).setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
  }
}
