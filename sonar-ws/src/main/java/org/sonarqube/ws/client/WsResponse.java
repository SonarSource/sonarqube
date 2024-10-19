/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.io.Closeable;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @since 5.3
 */
public interface WsResponse extends Closeable {

  /**
   * The absolute requested URL
   */
  String requestUrl();

  /**
   * HTTP status code
   */
  int code();

  /**
   * Returns true if the code is in [200..300), which means the request was
   * successfully received, understood, and accepted.
   */
  boolean isSuccessful() ;

  /**
   * Throws a {@link HttpException} if {@link #isSuccessful()} is false.
   */
  WsResponse failIfNotSuccessful();

  String contentType();

  Optional<String> header(String name);

  Map<String, List<String>> headers();

  boolean hasContent();

  InputStream contentStream();

  Reader contentReader();

  String content();
  
  @Override
  void close();

}
