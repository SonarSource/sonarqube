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
package org.sonar.server.platform.web.requestid;

import org.slf4j.MDC;

import static java.util.Objects.requireNonNull;

/**
 * Wraps MDC calls to store the HTTP request ID in the {@link MDC} into an {@link AutoCloseable}.
 */
public class RequestIdMDCStorage implements AutoCloseable {
  public static final String HTTP_REQUEST_ID_MDC_KEY = "HTTP_REQUEST_ID";

  public RequestIdMDCStorage(String requestId) {
    MDC.put(HTTP_REQUEST_ID_MDC_KEY, requireNonNull(requestId, "Request ID can't be null"));
  }

  @Override
  public void close() {
    MDC.remove(HTTP_REQUEST_ID_MDC_KEY);
  }
}
