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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public class CspFilter implements Filter {
  private static final String INDEX_HTML_PATH = "/index.html";
  private static final Pattern INDEX_SCRIPT_PATTERN = Pattern.compile("<script[^>]*>(\\s*window\\.__assetsPath[\\s\\S]*?)</script>");

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
    cspPolicies.add("frame-src");
    cspPolicies.add("img-src * data: blob:");
    cspPolicies.add("object-src 'none'");
    cspPolicies.add("script-src 'self' " + getIndexScriptCSPHash(filterConfig.getServletContext()));
    cspPolicies.add("style-src 'self' 'unsafe-inline'");
    cspPolicies.add("worker-src 'self'");
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

  private static String getIndexScriptCSPHash(ServletContext servletContext) throws ServletException {
    try (InputStream input = servletContext.getResourceAsStream(INDEX_HTML_PATH)) {
      if (input == null) {
        throw new ServletException(INDEX_HTML_PATH + " not found in the web context");
      }
      String indexHtml = IOUtils.toString(input, StandardCharsets.UTF_8);
      Matcher scriptMatcher = INDEX_SCRIPT_PATTERN.matcher(indexHtml);
      if (!scriptMatcher.find()) {
        throw new IllegalStateException("Unable to find the assets path script in " + INDEX_HTML_PATH);
      }
      String script = scriptMatcher.group(1);
      if (WebPagePlaceholders.containsServingTimePlaceholder(script)) {
        throw new IllegalStateException("The assets path script in " + INDEX_HTML_PATH + " must not contain serving-time placeholders");
      }
      return generateCSPHash(script.replace(WebPagePlaceholders.WEB_CONTEXT, servletContext.getContextPath()));
    } catch (IOException e) {
      throw new ServletException("Failed to load " + INDEX_HTML_PATH, e);
    }
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
