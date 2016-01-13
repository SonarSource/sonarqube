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
package org.sonarqube.ws.client;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonarqube.ws.MediaTypes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

abstract class BaseRequest<SELF extends BaseRequest> implements WsRequest {

  private final String path;

  private String mediaType = MediaTypes.JSON;

  // keep the same order -> do not use HashMap
  private final Map<String, String> params = new LinkedHashMap<>();

  BaseRequest(String path) {
    this.path = path;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getMediaType() {
    return mediaType;
  }

  /**
   * Expected media type of response. Default is {@link MediaTypes#JSON}.
   */
  public SELF setMediaType(String s) {
    requireNonNull(s, "media type of response cannot be null");
    this.mediaType = s;
    return (SELF) this;
  }

  public SELF setParam(String key, @Nullable Object value) {
    checkArgument(!isNullOrEmpty(key), "a WS parameter key cannot be null");
    if (value != null) {
      this.params.put(key, value.toString());
    }
    return (SELF) this;
  }

  @Override
  public Map<String, String> getParams() {
    return params;
  }
}
