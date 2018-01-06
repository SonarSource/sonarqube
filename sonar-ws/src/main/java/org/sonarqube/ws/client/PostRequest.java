/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

/**
 * @since 5.3
 */
public class PostRequest extends BaseRequest<PostRequest> {

  private final Map<String, Part> parts = new LinkedHashMap<>();

  public PostRequest(String path) {
    super(path);
  }

  @Override
  public Method getMethod() {
    return Method.POST;
  }

  public PostRequest setPart(String name, Part part) {
    this.parts.put(name, part);
    return this;
  }

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
