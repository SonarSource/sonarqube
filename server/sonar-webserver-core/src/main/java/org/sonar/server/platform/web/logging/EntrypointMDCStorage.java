/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.web.logging;

import javax.annotation.Nullable;
import org.slf4j.MDC;

import static org.apache.commons.lang.StringUtils.isBlank;

public class EntrypointMDCStorage implements AutoCloseable {
  public static final String ENTRYPOINT_MDC_KEY = "ENTRYPOINT";

  public EntrypointMDCStorage(@Nullable String entrypoint) {
    MDC.put(ENTRYPOINT_MDC_KEY, isBlank(entrypoint) ? "-" : entrypoint);
  }

  @Override
  public void close() {
    MDC.remove(ENTRYPOINT_MDC_KEY);
  }
}
