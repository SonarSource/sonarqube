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
package org.sonar.server.ws;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import org.sonar.api.impl.ws.PartImpl;
import org.sonar.api.impl.ws.ValidatingRequest;
import org.sonar.api.utils.log.Loggers;
import org.sonarqube.ws.MediaTypes;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.emptyList;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.MULTIPART;

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
  public BufferedReader getReader() {
    try {
      return source.getReader();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read", e);
    }
  }

  @Override
  public boolean hasParam(String key) {
    return source.getParameterMap().containsKey(key);
  }

  @Override
  public String readParam(String key) {
    return source.getParameter(key);
  }

  @Override
  public Map<String, String[]> getParams() {
    return source.getParameterMap();
  }

  @Override
  public List<String> readMultiParam(String key) {
    String[] values = source.getParameterValues(key);
    return values == null ? emptyList() : ImmutableList.copyOf(values);
  }

  @Override
  protected InputStream readInputStreamParam(String key) {
    Part part = readPart(key);
    return (part == null) ? null : part.getInputStream();
  }

  @Override
  @CheckForNull
  public Part readPart(String key) {
    try {
      if (!isMultipartContent()) {
        return null;
      }
      javax.servlet.http.Part part = source.getPart(key);
      if (part == null || part.getSize() == 0) {
        return null;
      }
      return new PartImpl(part.getInputStream(), part.getSubmittedFileName());
    } catch (Exception e) {
      Loggers.get(ServletRequest.class).warn("Can't read file part for parameter " + key, e);
      return null;
    }
  }

  private boolean isMultipartContent() {
    String contentType = source.getContentType();
    return contentType != null && contentType.toLowerCase(ENGLISH).startsWith(MULTIPART);
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
    return SUPPORTED_MEDIA_TYPES_BY_URL_SUFFIX.get(formatSuffix.toLowerCase(ENGLISH));
  }

  @Override
  public String getPath() {
    return source.getRequestURI().replaceFirst(source.getContextPath(), "");
  }

  @Override
  public Optional<String> header(String name) {
    return Optional.ofNullable(source.getHeader(name));
  }

  @Override
  public Map<String, String> getHeaders() {
    ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
    Enumeration<String> headerNames = source.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      mapBuilder.put(headerName, source.getHeader(headerName));
    }
    return mapBuilder.build();
  }
}
