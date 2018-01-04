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
package org.sonar.api.server.ws;

import java.io.OutputStream;
import java.util.Collection;
import javax.annotation.CheckForNull;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.utils.text.XmlWriter;

/**
 * HTTP response
 *
 * @since 4.2
 */
public interface Response {

  interface Stream {
    Stream setMediaType(String s);

    /**
     * HTTP status code. See https://en.wikipedia.org/wiki/List_of_HTTP_status_codes.
     * By default value is set to 200.
     */
    Stream setStatus(int httpStatus);

    /**
     * Response stream. Beware that proper error recovery is not possible.
     */
    OutputStream output();
  }

  /**
   * Non streamable {@link JsonWriter}. Response is effectively written when closing the resource.
   */
  JsonWriter newJsonWriter();

  XmlWriter newXmlWriter();

  Response noContent();

  Response setHeader(String name, String value);

  Collection<String> getHeaderNames();

  @CheckForNull
  String getHeader(String name);

  Stream stream();

}
