/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.server.ui;

import org.apache.commons.lang.StringUtils;
import org.jruby.rack.RackFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

public class SonarRackFilter extends RackFilter {

  private Set<String> exclusions;

  @Override
  public void init(FilterConfig config) throws ServletException {
    super.init(config);
    String[] exclusionsStr = StringUtils.split(config.getInitParameter("exclusions"), ',');
    exclusions = new HashSet<String>(Arrays.asList(exclusionsStr));
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest)request;
    String requestURI = httpRequest.getRequestURI();
    int extIndex = requestURI.lastIndexOf('.');
    String fileExtension = extIndex != -1 ? requestURI.substring(extIndex + 1, requestURI.length()) : null;
    if (fileExtension == null || !exclusions.contains(fileExtension)) {
      super.doFilter(request, response, chain);
    } else {
      chain.doFilter(request, response);
    }
  }

}
