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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public class CspFilter implements Filter {

  private final List<String> cspHeaders = new ArrayList<>();
  private String policies = null;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    cspHeaders.add("Content-Security-Policy");

    List<String> cspPolicies = new ArrayList<>();
    cspPolicies.add("default-src 'self'");
    cspPolicies.add("base-uri 'none'");
    cspPolicies.add("connect-src 'self' http: https:");
    cspPolicies.add("font-src 'self' data:");
    cspPolicies.add("img-src * data: blob:");
    cspPolicies.add("object-src 'none'");
    // the hash below corresponds to the window.__assetsPath script in index.html
    cspPolicies.add("script-src 'self' " + getAssetsPathScriptCSPHash(filterConfig.getServletContext().getContextPath()));
    cspPolicies.add("style-src 'self' 'unsafe-inline'");
    cspPolicies.add("worker-src 'none'");
    this.policies = String.join("; ", cspPolicies).trim();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    // Add policies to all HTTP headers
    for (String header : this.cspHeaders) {
      ((HttpServletResponse) response).setHeader(header, this.policies);
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // Not used
  }

  private static String getAssetsPathScriptCSPHash(String contextPath) {
    final String WEB_CONTEXT_PLACEHOLDER = "WEB_CONTEXT";
    final String ASSETS_PATH_SCRIPT = """

            window.__assetsPath = function (filename) {
              return 'WEB_CONTEXT/' + filename;
            };
          \
      """;

    String assetsPathScriptWithContextPath = ASSETS_PATH_SCRIPT.replace(WEB_CONTEXT_PLACEHOLDER, contextPath);
    return generateCSPHash(assetsPathScriptWithContextPath);
  }

  private static String generateCSPHash(String str) {
    try {
      byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
      byte[] digestBytes = MessageDigest.getInstance("SHA-256").digest(bytes);
      String rawHash = Base64.getMimeEncoder().encodeToString(digestBytes);
      return String.format("'%s-%s'", "sha256", rawHash);
    } catch (NoSuchAlgorithmException e) {
      return "";
    }
  }
}
