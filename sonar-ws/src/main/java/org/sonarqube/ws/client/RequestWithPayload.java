/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonarqube.ws.client;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * @since 5.3
 */
public abstract class RequestWithPayload<T extends RequestWithPayload<T>> extends BaseRequest<RequestWithPayload<T>> {

  private String body;
  private String contentType = null;
  private final Map<String, Part> parts = new LinkedHashMap<>();

  protected RequestWithPayload(String path) {
    super(path);
  }

  public T setBody(String body) {
    this.body = body;
    return (T) this;
  }

  public String getBody() {
    return body;
  }

  public boolean hasBody() {
    return this.body != null;
  }

  public T setContentType(String contentType) {
    this.contentType = contentType;
    return (T) this;
  }

  public Optional<String> getContentType() {
    return Optional.ofNullable(contentType);
  }

  public T setPart(String name, Part part) {
    this.parts.put(name, part);
    return (T) this;
  }

  abstract Function<Request.Builder, Request.Builder> addVerbToBuilder(RequestBody body);

  public Map<String, Part> getParts() {
    return parts;
  }

  public static class Part {
    private final String mediaType;
    private final File file;

    public Part(String mediaType, File file) {
      this.mediaType = mediaType;
      this.file = file;
    }

    public String getMediaType() {
      return mediaType;
    }

    public File getFile() {
      return file;
    }
  }

}
