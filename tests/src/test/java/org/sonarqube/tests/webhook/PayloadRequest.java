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
package org.sonarqube.tests.webhook;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Request received by {@link ExternalServer}
 */
class PayloadRequest {
  private final String path;
  private final Map<String, String> httpHeaders;
  private final String json;

  PayloadRequest(String path, Map<String, String> httpHeaders, String json) {
    this.path = requireNonNull(path);
    this.httpHeaders = requireNonNull(httpHeaders);
    this.json = requireNonNull(json);
  }

  Map<String, String> getHttpHeaders() {
    return httpHeaders;
  }

  String getJson() {
    return json;
  }

  String getPath() {
    return path;
  }
}
