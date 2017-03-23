/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import javax.annotation.CheckForNull;

public class TestResponse {

  private final DumbResponse dumbResponse;

  TestResponse(DumbResponse dumbResponse) {
    this.dumbResponse = dumbResponse;
  }

  public InputStream getInputStream() {
    return new ByteArrayInputStream(dumbResponse.getFlushedOutput());
  }

  public String getInput() {
    return new String(dumbResponse.getFlushedOutput(), StandardCharsets.UTF_8);
  }

  public String getMediaType() {
    return dumbResponse.stream().mediaType();
  }

  public int getStatus() {
    return dumbResponse.stream().status();
  }

  @CheckForNull
  public String getHeader(String headerKey) {
    return dumbResponse.getHeader(headerKey);
  }
}
