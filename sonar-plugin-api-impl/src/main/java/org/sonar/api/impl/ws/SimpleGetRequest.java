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
package org.sonar.api.impl.ws;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.api.server.ws.Request;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Fake implementation of {@link org.sonar.api.server.ws.Request} used
 * for testing. Call the method {@link #setParam(String, String)} to
 * emulate some parameter values.
 */
public class SimpleGetRequest extends Request {

  private final Map<String, String[]> params = new HashMap<>();
  private final Map<String, Part> parts = new HashMap<>();
  private final Map<String, String> headers = new HashMap<>();
  private String mediaType = "application/json";
  private String path;

  @Override
  public String method() {
    return "GET";
  }

  @Override
  public String getMediaType() {
    return mediaType;
  }

  public SimpleGetRequest setMediaType(String mediaType) {
    requireNonNull(mediaType);
    this.mediaType = mediaType;
    return this;
  }

  @Override
  public boolean hasParam(String key) {
    return params.keySet().contains(key);
  }

  @Override
  public String param(String key) {
    String[] strings = params.get(key);
    return strings == null || strings.length == 0 ? null : strings[0];
  }

  @Override
  public List<String> multiParam(String key) {
    String value = param(key);
    return value == null ? emptyList() : singletonList(value);
  }

  @Override
  @CheckForNull
  public List<String> paramAsStrings(String key) {
    String value = param(key);
    if (value == null) {
      return null;
    }

    return Arrays.stream(value.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toList());
  }

  @Override
  public InputStream paramAsInputStream(String key) {
    return IOUtils.toInputStream(param(key), UTF_8);
  }

  public SimpleGetRequest setParam(String key, @Nullable String value) {
    if (value != null) {
      params.put(key, new String[] {value});
    }
    return this;
  }

  @Override
  public Map<String, String[]> getParams() {
    return params;
  }

  @Override
  public Part paramAsPart(String key) {
    return parts.get(key);
  }

  public SimpleGetRequest setPart(String key, InputStream input, String fileName) {
    parts.put(key, new PartImpl(input, fileName));
    return this;
  }

  @Override
  public LocalConnector localConnector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getPath() {
    return path;
  }

  public SimpleGetRequest setPath(String path) {
    this.path = path;
    return this;
  }

  @Override
  public Optional<String> header(String name) {
    return Optional.ofNullable(headers.get(name));
  }

  public SimpleGetRequest setHeader(String name, String value) {
    headers.put(name, value);
    return this;
  }
}
