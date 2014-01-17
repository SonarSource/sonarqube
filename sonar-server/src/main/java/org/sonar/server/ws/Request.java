/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.ws;

import javax.annotation.CheckForNull;
import java.util.Map;

/**
 * @since 4.2
 */
public class Request {

  private final Map<String,String> params;
  private String mediaType = "application/json";
  private boolean post = false;

  public Request(Map<String, String> params) {
    this.params = params;
  }

  @CheckForNull
  public String param(String key) {
    return params.get(key);
  }

  @CheckForNull
  public Integer intParam(String key) {
    String s = params.get(key);
    return s == null ? null : Integer.parseInt(s);
  }

  public int intParam(String key, int defaultValue) {
    String s = params.get(key);
    return s == null ? defaultValue : Integer.parseInt(s);
  }

  public String mediaType() {
    return mediaType;
  }

  public Request setMediaType(String s) {
    this.mediaType = s;
    return this;
  }

  public boolean isPost() {
    return post;
  }

  public Request setPost(boolean b) {
    this.post = b;
    return this;
  }
}
