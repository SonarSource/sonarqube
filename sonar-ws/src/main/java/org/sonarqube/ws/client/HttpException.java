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
package org.sonarqube.ws.client;

/**
 * @since 5.3
 */
public class HttpException extends RuntimeException {

  private final String url;
  private final int code;
  private final String content;

  public HttpException(String url, int code, String content) {
    super(String.format("Error %d on %s : %s", code, url, content));
    this.url = url;
    this.code = code;
    this.content = content;
  }
  
  public String content() {
    return content;
  }

  public String url() {
    return url;
  }

  /**
   * @see java.net.HttpURLConnection constants
   */
  public int code() {
    return code;
  }
}
