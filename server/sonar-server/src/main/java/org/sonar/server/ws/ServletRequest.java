/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.ws;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.apache.commons.lang.StringUtils.substringAfterLast;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import org.sonar.api.server.ws.internal.ValidatingRequest;
import org.sonarqube.ws.MediaTypes;

public class ServletRequest extends ValidatingRequest {

  private final HttpServletRequest source;

  static final Map<String, String> SUPPORTED_MEDIA_TYPES_BY_URL_SUFFIX = ImmutableMap.of(
    "json", MediaTypes.JSON,
    "protobuf", MediaTypes.PROTOBUF,
    "text", MediaTypes.TXT);

  public ServletRequest(HttpServletRequest source) {
    this.source = source;
  }

  @Override
  public String method() {
    return source.getMethod();
  }

  @Override
  public String getMediaType() {
    return firstNonNull(
      mediaTypeFromUrl(source.getRequestURI()),
      firstNonNull(
        acceptedContentTypeInResponse(),
        MediaTypes.DEFAULT));
  }

  @Override
  public boolean hasParam(String key) {
    return source.getParameterMap().containsKey(key);
  }

  @Override
  protected String readParam(String key) {
    return source.getParameter(key);
  }

  @Override
  protected InputStream readInputStreamParam(String key) {
    try {
      Part part = source.getPart(key);
      return part == null ? null : part.getInputStream();
    } catch (Exception e) {
      throw new IllegalStateException("Can't read file part", e);
    }
  }

  @Override
  public String toString() {
    StringBuffer url = source.getRequestURL();
    String query = source.getQueryString();
    if (query != null) {
      url.append("?").append(query);
    }
    return url.toString();
  }

  @CheckForNull
  private String acceptedContentTypeInResponse() {
    return source.getHeader(HttpHeaders.ACCEPT);
  }

  @CheckForNull
  private static String mediaTypeFromUrl(String url) {
    String formatSuffix = substringAfterLast(url, ".");
    return SUPPORTED_MEDIA_TYPES_BY_URL_SUFFIX.get(formatSuffix.toLowerCase(Locale.ENGLISH));
  }
}
