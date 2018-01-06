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
package org.sonar.server.ws;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.api.server.ws.internal.ValidatingRequest;

public class LocalRequestAdapter extends ValidatingRequest {

  private final LocalConnector.LocalRequest localRequest;

  public LocalRequestAdapter(LocalConnector.LocalRequest localRequest) {
    this.localRequest = localRequest;
  }

  @Override
  protected String readParam(String key) {
    return localRequest.getParam(key);
  }

  @Override
  protected List<String> readMultiParam(String key) {
    return localRequest.getMultiParam(key);
  }

  @Override
  protected InputStream readInputStreamParam(String key) {
    String value = readParam(key);
    if (value == null) {
      return null;
    }
    return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected Part readPart(String key) {
    throw new UnsupportedOperationException("reading part is not supported yet by local WS calls");
  }

  @Override
  public boolean hasParam(String key) {
    return localRequest.hasParam(key);
  }

  @Override
  public String getPath() {
    return localRequest.getPath();
  }

  @Override
  public String method() {
    return localRequest.getMethod();
  }

  @Override
  public String getMediaType() {
    return localRequest.getMediaType();
  }

  @Override
  public Optional<String> header(String name) {
    return localRequest.getHeader(name);
  }
}
