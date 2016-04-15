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
package org.sonar.api.server.ws.internal;

import com.google.common.collect.Maps;
import java.io.InputStream;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.api.server.ws.Request;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fake implementation of {@link org.sonar.api.server.ws.Request} used
 * for testing. Call the method {@link #setParam(String, String)} to
 * emulate some parameter values.
 */
public class SimpleGetRequest extends Request {

  private final Map<String, String> params = Maps.newHashMap();
  private String mediaType = "application/json";

  @Override
  public String method() {
    return "GET";
  }

  @Override
  public String getMediaType() {
    return mediaType;
  }

  public SimpleGetRequest setMediaType(String mediaType) {
    checkNotNull(mediaType);
    this.mediaType = mediaType;
    return this;
  }

  @Override
  public boolean hasParam(String key) {
    return params.keySet().contains(key);
  }

  @Override
  public String param(String key) {
    return params.get(key);
  }

  @Override
  public InputStream paramAsInputStream(String key) {
    return IOUtils.toInputStream(param(key));
  }

  public SimpleGetRequest setParam(String key, @Nullable String value) {
    if (value != null) {
      params.put(key, value);
    }
    return this;
  }

  public Map<String, String> getParams() {
    return params;
  }

  @Override
  public LocalConnector localConnector() {
    throw new UnsupportedOperationException();
  }
}
