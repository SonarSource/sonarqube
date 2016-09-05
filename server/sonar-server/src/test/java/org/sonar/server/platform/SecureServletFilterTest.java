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

import org.sonar.api.config.Settings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SecureServletFilterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void servlet_should_add_headers() throws Exception {
	  HttpServletRequest request = mock(HttpServletRequest.class);
	  HttpServletResponse response = mock(HttpServletResponse.class);
	  Settings settings = new Settings();
	  settings.setProperty("sonar.web.xssProtection", "1; mode=block");
	  settings.setProperty("sonar.web.frameOptions", "SAMEORIGIN");
	  settings.setProperty("sonar.web.contentTypeOptions", "nosniff");
	  

	  SecureServletFilter filters = new SecureServletFilter(settings);
	  FilterChain chain = mock(FilterChain.class);
	  filters.doFilter(request, response, chain);

	  verify(response).setHeader("X-XSS-Protection", "1; mode=block");
	  verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
	  verify(response).setHeader("X-Content-Type-Options", "nosniff");
  }

  @Test
  public void servlet_should_only_addXrameOptions() throws Exception {
	  HttpServletRequest request = mock(HttpServletRequest.class);
	  HttpServletResponse response = mock(HttpServletResponse.class);
	  Settings settings = new Settings();
	  settings.setProperty("sonar.web.frameOptions", "SAMEORIGIN");

	  SecureServletFilter filters = new SecureServletFilter(settings);
	  FilterChain chain = mock(FilterChain.class);
	  filters.doFilter(request, response, chain);

	  verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
  }


}
