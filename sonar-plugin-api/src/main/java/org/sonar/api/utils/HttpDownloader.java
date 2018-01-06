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
package org.sonar.api.utils;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.server.ServerSide;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

/**
 * This component is available in IoC container, so it should be injected through
 * a constructor parameter. It is available in both batch and server.
 */
@ScannerSide
@ServerSide
public abstract class HttpDownloader extends UriReader.SchemeProcessor {

  /**
   * Catch-all default timeout, replaced by
   *   {@link #DEFAULT_READ_TIMEOUT_IN_MILLISECONDS}
   *   {@link #DEFAULT_CONNECT_TIMEOUT_IN_MILLISECONDS}
   *
   * @deprecated since 7.0
   */
  @Deprecated
  public static final int TIMEOUT_MILLISECONDS = 20 * 1000;

  /**
   * @since 7.0
   */
  public static final int DEFAULT_READ_TIMEOUT_IN_MILLISECONDS = 60 * 1000;

  /**
   * @since 7.0
   */
  public static final int DEFAULT_CONNECT_TIMEOUT_IN_MILLISECONDS = 20 * 1000;

  public abstract String downloadPlainText(URI uri, String encoding);

  public abstract byte[] download(URI uri);

  public abstract InputStream openStream(URI uri);

  public abstract void download(URI uri, File toFile);

  public static class HttpException extends RuntimeException {
    private final URI uri;
    private final int responseCode;
    private final String responseContent;

    public HttpException(URI uri, int responseContent) {
      this(uri, responseContent, "");
    }

    public HttpException(URI uri, int responseCode, String responseContent) {
      super("Fail to download [" + uri + "]. Response code: " + responseCode);
      this.uri = uri;
      this.responseCode = responseCode;
      this.responseContent = responseContent;
    }

    public int getResponseCode() {
      return responseCode;
    }

    public URI getUri() {
      return uri;
    }

    public String getResponseContent() {
      return responseContent;
    }
  }
}
